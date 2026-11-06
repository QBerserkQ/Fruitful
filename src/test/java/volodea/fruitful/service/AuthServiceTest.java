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
import volodea.fruitful.dto.RotatedSession;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.InvalidCredentialsException;
import volodea.fruitful.exception.InvalidRefreshTokenException;
import volodea.fruitful.exception.RefreshTokenExpiredException;
import volodea.fruitful.exception.UserAlreadyExistException;
import volodea.fruitful.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USER_AGENT = "test-agent";

    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

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
    @DisplayName("register: сохраняет нормализованные email и username, создаёт сессию с user-agent, возвращает оба токена")
    void register_success_normalizesAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("  john  ", "  Test@Mail.COM ", "password123");

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateJwtToken(any(User.class))).thenReturn("jwt");
        when(refreshTokenService.createSession(any(User.class), eq(USER_AGENT))).thenReturn("refresh");

        AuthResponse response = authService.register(request, USER_AGENT);

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

        assertThatThrownBy(() -> authService.register(request, USER_AGENT))
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

        assertThatThrownBy(() -> authService.register(request, USER_AGENT))
                .isInstanceOf(UserAlreadyExistException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    // ---------- login ----------

    @Test
    @DisplayName("login: успех, email нормализуется, сессия создаётся с user-agent")
    void login_success() {
        User user = userOf();
        LoginRequest request = new LoginRequest("  Test@Mail.COM ", "password123");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateJwtToken(user)).thenReturn("jwt");
        when(refreshTokenService.createSession(user, USER_AGENT)).thenReturn("refresh");

        AuthResponse response = authService.login(request, USER_AGENT);

        assertThat(response).isEqualTo(new AuthResponse("jwt", "refresh"));
    }

    @Test
    @DisplayName("login: отсутствующий User-Agent (null) не мешает входу")
    void login_nullUserAgent_works() {
        User user = userOf();
        LoginRequest request = new LoginRequest("test@mail.com", "password123");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateJwtToken(user)).thenReturn("jwt");
        when(refreshTokenService.createSession(user, null)).thenReturn("refresh");

        AuthResponse response = authService.login(request, null);

        assertThat(response).isEqualTo(new AuthResponse("jwt", "refresh"));
    }

    @Test
    @DisplayName("login: неизвестный email -> InvalidCredentialsException, пароль не проверяется")
    void login_unknownEmail_throws() {
        LoginRequest request = new LoginRequest("nobody@mail.com", "password123");

        when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, USER_AGENT))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("login: неверный пароль -> то же InvalidCredentialsException, сессия не создаётся")
    void login_wrongPassword_throws() {
        User user = userOf();
        LoginRequest request = new LoginRequest("test@mail.com", "wrong-password");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, USER_AGENT))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    // ---------- refresh ----------

    @Test
    @DisplayName("refresh: валидный токен -> новый access и ротированный refresh, новая сессия НЕ создаётся")
    void refresh_success() {
        User user = userOf();

        when(refreshTokenService.rotate("old")).thenReturn(new RotatedSession(user, "new-refresh"));
        when(jwtService.generateJwtToken(user)).thenReturn("new-jwt");

        AuthResponse response = authService.refresh(new RefreshTokenRequest("old"));

        assertThat(response).isEqualTo(new AuthResponse("new-jwt", "new-refresh"));
        verify(refreshTokenService, never()).createSession(any(), any());
    }

    @Test
    @DisplayName("refresh: просроченный токен -> исключение пробрасывается, access-токен не выдаётся")
    void refresh_expired_throws() {
        when(refreshTokenService.rotate("expired")).thenThrow(new RefreshTokenExpiredException());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired")))
                .isInstanceOf(RefreshTokenExpiredException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("refresh: неизвестный токен -> InvalidRefreshTokenException, access-токен не выдаётся")
    void refresh_unknownToken_throws() {
        when(refreshTokenService.rotate("unknown")).thenThrow(new InvalidRefreshTokenException());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("unknown")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(jwtService);
    }

    // ---------- logout ----------

    @Test
    @DisplayName("logout: делегирует отзыв сессии в RefreshTokenService")
    void logout_revokesSession() {
        authService.logout(new RefreshTokenRequest("token"));

        verify(refreshTokenService).revoke("token");
    }
}