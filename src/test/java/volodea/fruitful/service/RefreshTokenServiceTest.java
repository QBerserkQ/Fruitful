package volodea.fruitful.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import volodea.fruitful.dto.SessionTokens;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.InvalidRefreshTokenException;
import volodea.fruitful.exception.RefreshTokenExpiredException;
import volodea.fruitful.repository.RefreshTokenRepository;
import volodea.fruitful.util.TokenHasher;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long EXPIRATION_MS = 604_800_000L; // 7 days

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        ReflectionTestUtils.setField(refreshTokenService, "expMs", EXPIRATION_MS);
    }

    // CREATE SESSION
    @Test
    void createSession_whenNoSessions_savesHashedTokenWithExpectedFields() {
        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of());

        SessionTokens session = refreshTokenService.createSession(user, "Mozilla/5.0");
        String rawToken = session.refreshToken();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(rawToken).isNotBlank();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");

        // в базе только хеш, сырой токен не хранится
        assertThat(saved.getTokenHash()).isEqualTo(TokenHasher.sha256(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);

        // срок жизни = now + 7 дней (с допуском на время выполнения теста)
        assertThat(saved.getExpiresAt()).isCloseTo(
                OffsetDateTime.now(ZoneOffset.UTC).plus(EXPIRATION_MS, ChronoUnit.MILLIS),
                within(5, ChronoUnit.SECONDS)
        );

        verify(refreshTokenRepository, never()).deleteAll(anyList());
    }

    @ParameterizedTest(name = "{0} existing sessions -> evicts {1}")
    @CsvSource({
            "0, 0",
            "1, 0",
            "2, 0",
            "3, 1",
            "4, 2",
            "5, 3"
    })
    void createSession_evictsOldestSessionsOnlyWhenLimitReached(int existing, int expectedEvicted) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // порядок как из findByUserOrderByLastUsedAtAsc: самая старая первая
        List<RefreshToken> sessions = IntStream.range(0, existing)
                .mapToObj(i -> buildSession("hash-" + i, now.minusDays(existing - i)))
                .toList();

        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(sessions);

        refreshTokenService.createSession(user, "Chrome");

        if (expectedEvicted == 0) {
            verify(refreshTokenRepository, never()).deleteAll(anyList());
        } else {
            verify(refreshTokenRepository).deleteAll(sessions.subList(0, expectedEvicted));
        }
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createSession_whenUserAgentIsNull_savesNullUserAgent() {
        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of());

        refreshTokenService.createSession(user, null);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getUserAgent()).isNull();
    }

    @Test
    void createSession_whenUserAgentIsTooLong_truncatesTo255() {
        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of());

        refreshTokenService.createSession(user, "A".repeat(300));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getUserAgent()).hasSize(255);
    }

    // ROTATE
    @Test
    void rotate_whenTokenIsValid_replacesHashAndKeepsFixedExpiry() {
        String rawToken = "valid-refresh-token";
        OffsetDateTime oldLastUsedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);

        RefreshToken stored = buildSession(TokenHasher.sha256(rawToken), oldLastUsedAt, expiresAt);

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)))
                .thenReturn(Optional.of(stored));

        SessionTokens result = refreshTokenService.rotate(rawToken);

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.refreshToken())
                .isNotBlank()
                .isNotEqualTo(rawToken);

        // в записи теперь хеш нового токена, старый больше не подойдёт
        assertThat(stored.getTokenHash()).isEqualTo(TokenHasher.sha256(result.refreshToken()));
        assertThat(stored.getTokenHash()).isNotEqualTo(TokenHasher.sha256(rawToken));

        // lastUsedAt реально обновился, а срок жизни сессии не продлился (non-sliding)
        assertThat(stored.getLastUsedAt()).isAfter(oldLastUsedAt);
        assertThat(stored.getExpiresAt()).isEqualTo(expiresAt);

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void rotate_whenTokenDoesNotExist_throwsInvalidRefreshTokenException() {
        String rawToken = "unknown-token";

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void rotate_whenTokenIsExpired_deletesSessionAndThrows() {
        String rawToken = "expired-token";
        String tokenHash = TokenHasher.sha256(rawToken);

        RefreshToken stored = buildSession(
                tokenHash,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(8),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        );

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(RefreshTokenExpiredException.class);

        verify(refreshTokenRepository).delete(stored);
        // хеш не должен меняться: просроченная сессия не выдаёт новый токен
        assertThat(stored.getTokenHash()).isEqualTo(tokenHash);
    }

    // REVOKE
    @Test
    void revoke_whenTokenExists_deletesSession() {
        String rawToken = "refresh-token";

        RefreshToken stored = buildSession(
                TokenHasher.sha256(rawToken),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)))
                .thenReturn(Optional.of(stored));

        refreshTokenService.revoke(rawToken);

        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void revoke_whenTokenDoesNotExist_doesNothing() {
        String rawToken = "unknown-token";

        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)))
                .thenReturn(Optional.empty());

        assertThatCode(() -> refreshTokenService.revoke(rawToken))
                .doesNotThrowAnyException();

        verify(refreshTokenRepository, never()).delete(any());
    }

    // HELPERS
    private RefreshToken buildSession(String tokenHash, OffsetDateTime lastUsedAt) {
        return buildSession(tokenHash, lastUsedAt, OffsetDateTime.now(ZoneOffset.UTC).plusDays(7));
    }

    private RefreshToken buildSession(String tokenHash,
                                      OffsetDateTime lastUsedAt,
                                      OffsetDateTime expiresAt) {
        return RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .userAgent("Test User Agent")
                .createdAt(lastUsedAt)
                .lastUsedAt(lastUsedAt)
                .expiresAt(expiresAt)
                .build();
    }
}