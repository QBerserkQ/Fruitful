package volodea.fruitful.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.SessionVerificationException;
import volodea.fruitful.repository.RefreshTokenRepository;
import volodea.fruitful.util.TokenHasher;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceSessionCheckTest {

    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final Long SESSION_ID = 7L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    // ---------------------------------------------------------
    // GET VALID SESSION
    // ---------------------------------------------------------

    @Test
    void getValidSession_whenSessionIsValid_returnsSessionId() {
        RefreshToken session = buildSession(OffsetDateTime.now(ZoneOffset.UTC).plusDays(3));

        when(refreshTokenRepository.findByTokenHashAndUser(TokenHasher.sha256(RAW_TOKEN), user))
                .thenReturn(Optional.of(session));

        Long result = refreshTokenService.getValidSession(user, RAW_TOKEN);

        assertThat(result).isEqualTo(SESSION_ID);
        // это только проверка: сессия не должна ни удаляться, ни ротироваться
        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void getValidSession_whenTokenNotFoundOrNotOwnedByUser_throwsSessionVerificationException() {
        when(refreshTokenRepository.findByTokenHashAndUser(TokenHasher.sha256(RAW_TOKEN), user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.getValidSession(user, RAW_TOKEN))
                .isInstanceOf(SessionVerificationException.class);
    }

    @Test
    void getValidSession_whenSessionIsExpired_throwsSessionVerificationException() {
        RefreshToken session = buildSession(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

        when(refreshTokenRepository.findByTokenHashAndUser(TokenHasher.sha256(RAW_TOKEN), user))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> refreshTokenService.getValidSession(user, RAW_TOKEN))
                .isInstanceOf(SessionVerificationException.class);

        verify(refreshTokenRepository, never()).delete(any());
    }

    // ---------------------------------------------------------
    // REVOKE ALL EXCEPT
    // ---------------------------------------------------------

    @Test
    void revokeAllExcept_deletesEverySessionOfUserExceptGivenOne() {
        refreshTokenService.revokeAllExcept(user, SESSION_ID);

        verify(refreshTokenRepository).deleteAllByUserAndIdNot(user, SESSION_ID);
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private RefreshToken buildSession(OffsetDateTime expiresAt) {
        RefreshToken session = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(RAW_TOKEN))
                .userAgent("Test User Agent")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                .lastUsedAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .expiresAt(expiresAt)
                .build();

        // id генерирует БД, поэтому в тесте выставляем его через рефлексию
        ReflectionTestUtils.setField(session, "id", SESSION_ID);
        return session;
    }
}