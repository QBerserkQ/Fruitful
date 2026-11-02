package volodea.fruitful.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import volodea.fruitful.dto.RotatedSession;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.InvalidRefreshTokenException;
import volodea.fruitful.exception.RefreshTokenExpiredException;
import volodea.fruitful.repository.RefreshTokenRepository;
import volodea.fruitful.util.TokenHasher;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    private static final long EXPIRATION_MS = 604_800_000L;

    @BeforeEach
    void setUp() {
        user = new User();

        ReflectionTestUtils.setField(
                refreshTokenService,
                "expMs",
                EXPIRATION_MS
        );
    }

    // ---------------------------------------------------------
    // CREATE SESSION
    // ---------------------------------------------------------

    @Test
    void createSession_whenNoSessions_createsFirstSession() {
        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of());

        String rawToken = refreshTokenService.createSession(
                user,
                "Mozilla/5.0"
        );

        assertThat(rawToken).isNotBlank();

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(savedToken.getTokenHash())
                .isEqualTo(TokenHasher.sha256(rawToken));
        assertThat(savedToken.getExpiresAt())
                .isAfter(OffsetDateTime.now(ZoneOffset.UTC));

        verify(refreshTokenRepository, never()).deleteAll(anyList());
    }

    @Test
    void createSession_whenOneSessionExists_createsSecondSession() {
        RefreshToken existingSession = createSession(
                "old-hash",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        );

        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of(existingSession));

        String rawToken = refreshTokenService.createSession(
                user,
                "Chrome"
        );

        assertThat(rawToken).isNotBlank();

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).deleteAll(anyList());
    }

    @Test
    void createSession_whenTwoSessionsExist_createsThirdSession() {
        RefreshToken first = createSession(
                "hash-1",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(3)
        );

        RefreshToken second = createSession(
                "hash-2",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        );

        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of(first, second));

        String rawToken = refreshTokenService.createSession(
                user,
                "Firefox"
        );

        assertThat(rawToken).isNotBlank();

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).deleteAll(anyList());
    }

    @Test
    void createSession_whenThreeSessionsExist_deletesOldestSession() {
        RefreshToken oldest = createSession(
                "hash-1",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(5)
        );

        RefreshToken middle = createSession(
                "hash-2",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(3)
        );

        RefreshToken newest = createSession(
                "hash-3",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        );

        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of(oldest, middle, newest));

        refreshTokenService.createSession(user, "Safari");

        verify(refreshTokenRepository).deleteAll(
                List.of(oldest)
        );

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createSession_whenFourSessionsExist_deletesTwoOldestSessions() {
        RefreshToken first = createSession(
                "hash-1",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(6)
        );

        RefreshToken second = createSession(
                "hash-2",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(4)
        );

        RefreshToken third = createSession(
                "hash-3",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(2)
        );

        RefreshToken fourth = createSession(
                "hash-4",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        );

        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of(first, second, third, fourth));

        refreshTokenService.createSession(user, "Edge");

        verify(refreshTokenRepository).deleteAll(
                List.of(first, second)
        );

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createSession_whenUserAgentIsNull_savesNullUserAgent() {
        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of());

        refreshTokenService.createSession(user, null);

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getUserAgent()).isNull();
    }

    @Test
    void createSession_whenUserAgentIsTooLong_truncatesIt() {
        when(refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user))
                .thenReturn(List.of());

        String longUserAgent = "A".repeat(300);

        refreshTokenService.createSession(user, longUserAgent);

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getUserAgent())
                .hasSize(255);
    }

    // ---------------------------------------------------------
    // ROTATE
    // ---------------------------------------------------------

    @Test
    void rotate_whenTokenIsValid_rotatesTokenSuccessfully() {
        String rawToken = "valid-refresh-token";

        RefreshToken storedToken = createSession(
                TokenHasher.sha256(rawToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)
        );

        when(refreshTokenRepository.findByTokenHash(
                TokenHasher.sha256(rawToken)
        )).thenReturn(Optional.of(storedToken));

        RotatedSession result = refreshTokenService.rotate(rawToken);

        assertThat(result).isNotNull();
        assertThat(result.user()).isEqualTo(user);
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotEqualTo(rawToken);

        assertThat(storedToken.getTokenHash())
                .isEqualTo(TokenHasher.sha256(result.refreshToken()));

        assertThat(storedToken.getLastUsedAt())
                .isNotNull();

        verify(refreshTokenRepository).findByTokenHash(
                TokenHasher.sha256(rawToken)
        );

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void rotate_whenTokenDoesNotExist_throwsInvalidRefreshTokenException() {
        String rawToken = "unknown-token";

        when(refreshTokenRepository.findByTokenHash(
                TokenHasher.sha256(rawToken)
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void rotate_whenTokenIsExpired_deletesTokenAndThrowsException() {
        String rawToken = "expired-token";
        String tokenHash = TokenHasher.sha256(rawToken);

        RefreshToken storedToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .userAgent("Test User Agent")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(110))
                .lastUsedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(100))
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(RefreshTokenExpiredException.class);

        verify(refreshTokenRepository).delete(storedToken);
    }

    // ---------------------------------------------------------
    // REVOKE
    // ---------------------------------------------------------

    @Test
    void revoke_whenTokenExists_deletesSession() {
        String rawToken = "refresh-token";

        RefreshToken storedToken = createSession(
                TokenHasher.sha256(rawToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)
        );

        when(refreshTokenRepository.findByTokenHash(
                TokenHasher.sha256(rawToken)
        )).thenReturn(Optional.of(storedToken));

        refreshTokenService.revoke(rawToken);

        verify(refreshTokenRepository).delete(storedToken);
    }

    @Test
    void revoke_whenTokenDoesNotExist_doesNothing() {
        String rawToken = "unknown-token";

        when(refreshTokenRepository.findByTokenHash(
                TokenHasher.sha256(rawToken)
        )).thenReturn(Optional.empty());

        assertThatCode(() -> refreshTokenService.revoke(rawToken))
                .doesNotThrowAnyException();

        verify(refreshTokenRepository, never()).delete(any());
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private RefreshToken createSession(
            String tokenHash,
            OffsetDateTime lastUsedAt
    ) {
        return RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .userAgent("Test User Agent")
                .createdAt(lastUsedAt)
                .lastUsedAt(lastUsedAt)
                .expiresAt(
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)
                )
                .build();
    }
}
