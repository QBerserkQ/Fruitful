package volodea.fruitful.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import volodea.fruitful.dto.ChangePasswordRequest;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.CurrentPasswordMismatchException;
import volodea.fruitful.exception.NewPasswordSameAsCurrentException;
import volodea.fruitful.exception.SessionVerificationException;
import volodea.fruitful.exception.UserNotFoundException;
import volodea.fruitful.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceChangePasswordTest {

    private static final String EMAIL = "user@mail.com";
    private static final String OLD_PASSWORD = "old-password";
    private static final String NEW_PASSWORD = "new-password";
    private static final String OLD_HASH = "old-hash";
    private static final String NEW_HASH = "new-hash";
    private static final String REFRESH_TOKEN = "raw-refresh-token";
    private static final Long SESSION_ID = 5L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setPasswordHash(OLD_HASH);
    }

    // ---------------------------------------------------------
    // SUCCESS
    // ---------------------------------------------------------

    @Test
    void changePassword_whenAllChecksPass_updatesHashAndRevokesOtherSessions() {
        ChangePasswordRequest request =
                new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, REFRESH_TOKEN);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(refreshTokenService.getValidSession(user, REFRESH_TOKEN)).thenReturn(SESSION_ID);
        when(passwordEncoder.matches(OLD_PASSWORD, OLD_HASH)).thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

        authService.changePassword(EMAIL, request);

        assertThat(user.getPasswordHash()).isEqualTo(NEW_HASH);

        // сначала меняем пароль, потом отзываем остальные сессии
        InOrder inOrder = inOrder(userRepository, refreshTokenService);
        inOrder.verify(userRepository).save(user);
        inOrder.verify(refreshTokenService).revokeAllExcept(user, SESSION_ID);
    }

    // ---------------------------------------------------------
    // FAILURES: ничего не должно измениться
    // ---------------------------------------------------------

    @Test
    void changePassword_whenUserNotFound_throwsAndTouchesNothing() {
        ChangePasswordRequest request =
                new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, REFRESH_TOKEN);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(refreshTokenService, passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_whenSessionIsInvalid_throwsAndChangesNothing() {
        ChangePasswordRequest request =
                new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, REFRESH_TOKEN);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(refreshTokenService.getValidSession(user, REFRESH_TOKEN))
                .thenThrow(new SessionVerificationException());

        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(SessionVerificationException.class);

        assertThat(user.getPasswordHash()).isEqualTo(OLD_HASH);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllExcept(any(), any());
    }

    @Test
    void changePassword_whenOldPasswordIsWrong_throwsAndChangesNothing() {
        ChangePasswordRequest request =
                new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, REFRESH_TOKEN);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(refreshTokenService.getValidSession(user, REFRESH_TOKEN)).thenReturn(SESSION_ID);
        when(passwordEncoder.matches(OLD_PASSWORD, OLD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(CurrentPasswordMismatchException.class);

        assertThat(user.getPasswordHash()).isEqualTo(OLD_HASH);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllExcept(any(), any());
    }

    @Test
    void changePassword_whenNewPasswordEqualsOld_throwsAndChangesNothing() {
        ChangePasswordRequest request =
                new ChangePasswordRequest(OLD_PASSWORD, OLD_PASSWORD, REFRESH_TOKEN);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(refreshTokenService.getValidSession(user, REFRESH_TOKEN)).thenReturn(SESSION_ID);
        when(passwordEncoder.matches(OLD_PASSWORD, OLD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(NewPasswordSameAsCurrentException.class);

        assertThat(user.getPasswordHash()).isEqualTo(OLD_HASH);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllExcept(any(), any());
    }
}