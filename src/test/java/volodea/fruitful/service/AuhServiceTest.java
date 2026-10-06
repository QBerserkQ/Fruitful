package volodea.fruitful.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import volodea.fruitful.config.jwt.JwtService;
import volodea.fruitful.dto.AuthResponse;
import volodea.fruitful.dto.LoginRequest;
import volodea.fruitful.dto.RefreshTokenRequest;
import volodea.fruitful.dto.RegisterRequest;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.InvalidCredentialsException;
import volodea.fruitful.exception.RefreshTokenExpiredException;
import volodea.fruitful.exception.RefreshTokenNotFoundException;
import volodea.fruitful.exception.UserAlreadyExistException;
import volodea.fruitful.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private static RefreshToken tokenOf(String value) {
        return RefreshToken.builder().token(value).build();
    }

    private static User userOf() {
        return User.builder()
                .id(1L)
                .email("test@mail.com")
                .username("john")
                .passwordHash("hashed")
                .role(Role.USER)
                .build();
    }

    // ---------- register ----------

    @Test
    @DisplayName("register: сохраняет нормализованные email и username, возвращает оба токена")
    void register_success_normalizesAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("  john  ", "  Test@Mail.COM ", "password123");

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateJwtToken(any(User.class))).thenReturn("jwt");
        when(refreshTokenService.generateRefreshToken(any(User.class))).thenReturn(tokenOf("refresh"));

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("test@mail.com");
        assertThat(saved.getUsername()).isEqualTo("john");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(response).isEqualTo(new AuthResponse("jwt", "refresh"));
    }

    @Test
    @DisplayName("register: занятый email -> UserAlreadyExistException, ничего не сохраняется")
    void register_emailTaken_throws() {
        RegisterRequest request = new RegisterRequest("john", "Test@Mail.com", "password123");

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("register: занятый username -> UserAlreadyExistException, ничего не сохраняется")
    void register_usernameTaken_throws() {
        RegisterRequest request = new RegisterRequest("john", "new@mail.com", "password123");

        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    // ---------- login ----------

    @Test
    @DisplayName("login: успех, email нормализуется перед поиском")
    void login_success() {
        User user = userOf();
        LoginRequest request = new LoginRequest("  Test@Mail.COM ", "password123");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateJwtToken(user)).thenReturn("jwt");
        when(refreshTokenService.generateRefreshToken(user)).thenReturn(tokenOf("refresh"));

        AuthResponse response = authService.login(request);

        assertThat(response).isEqualTo(new AuthResponse("jwt", "refresh"));
    }

    @Test
    @DisplayName("login: неизвестный email -> InvalidCredentialsException, пароль не проверяется")
    void login_unknownEmail_throws() {
        LoginRequest request = new LoginRequest("nobody@mail.com", "password123");

        when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("login: неверный пароль -> то же InvalidCredentialsException, токены не выдаются")
    void login_wrongPassword_throws() {
        User user = userOf();
        LoginRequest request = new LoginRequest("test@mail.com", "wrong-password");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    // ---------- refresh ----------

    @Test
    @DisplayName("refresh: валидный токен -> новая пара токенов для владельца токена")
    void refresh_success() {
        User user = userOf();
        RefreshToken oldToken = RefreshToken.builder().user(user).token("old").build();

        when(refreshTokenService.verifyRefreshToken("old")).thenReturn(oldToken);
        when(jwtService.generateJwtToken(user)).thenReturn("new-jwt");
        when(refreshTokenService.generateRefreshToken(user)).thenReturn(tokenOf("new-refresh"));

        AuthResponse response = authService.refresh(new RefreshTokenRequest("old"));

        assertThat(response).isEqualTo(new AuthResponse("new-jwt", "new-refresh"));
        verify(refreshTokenService).generateRefreshToken(user);
    }

    @Test
    @DisplayName("refresh: просроченный токен -> исключение пробрасывается, новые токены не выдаются")
    void refresh_expired_throws() {
        when(refreshTokenService.verifyRefreshToken("expired"))
                .thenThrow(new RefreshTokenExpiredException(OffsetDateTime.now().minusDays(1)));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired")))
                .isInstanceOf(RefreshTokenExpiredException.class);

        verifyNoInteractions(jwtService);
        verify(refreshTokenService, never()).generateRefreshToken(any());
    }

    @Test
    @DisplayName("refresh: неизвестный токен -> RefreshTokenNotFoundException, новые токены не выдаются")
    void refresh_unknownToken_throws() {
        when(refreshTokenService.verifyRefreshToken("unknown"))
                .thenThrow(new RefreshTokenNotFoundException("unknown"));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("unknown")))
                .isInstanceOf(RefreshTokenNotFoundException.class);

        verifyNoInteractions(jwtService);
        verify(refreshTokenService, never()).generateRefreshToken(any());
    }
}