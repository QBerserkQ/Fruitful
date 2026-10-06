package volodea.fruitful.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.RefreshTokenExpiredException;
import volodea.fruitful.exception.RefreshTokenNotFoundException;
import volodea.fruitful.repository.RefreshTokenRepository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long EXP_MS = 60_000L;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "expMs", EXP_MS);
        user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .username("john")
                .passwordHash("hashed")
                .role(Role.USER)
                .build();
    }

    // ---------- generateRefreshToken ----------

    @Test
    @DisplayName("generate: у пользователя нет токена -> создаётся новый с UUID и сроком жизни")
    void generate_noExistingToken_createsNew() {
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        RefreshToken result = refreshTokenService.generateRefreshToken(user);
        Instant after = Instant.now();

        assertThat(result.getUser()).isSameAs(user);
        assertThat(UUID.fromString(result.getToken()).toString()).isEqualTo(result.getToken());
        assertThat(result.getExpiryDate().toInstant())
                .isBetween(before.plusMillis(EXP_MS), after.plusMillis(EXP_MS));
        verify(refreshTokenRepository).save(result);
    }

    @Test
    @DisplayName("generate: токен уже есть -> перезаписывается та же запись (ротация), а не создаётся вторая")
    void generate_existingToken_overwritesSameEntity() {
        RefreshToken existing = RefreshToken.builder()
                .id(5L)
                .user(user)
                .token("old-token")
                .expiryDate(OffsetDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        RefreshToken result = refreshTokenService.generateRefreshToken(user);
        Instant after = Instant.now();

        assertThat(result).isSameAs(existing);
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getToken()).isNotEqualTo("old-token");
        assertThat(result.getExpiryDate().toInstant())
                .isBetween(before.plusMillis(EXP_MS), after.plusMillis(EXP_MS));
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    @DisplayName("generate: каждый вызов выдаёт новое значение токена")
    void generate_producesDifferentTokens() {
        RefreshToken stored = RefreshToken.builder().id(5L).user(user).build();

        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String first = refreshTokenService.generateRefreshToken(user).getToken();
        String second = refreshTokenService.generateRefreshToken(user).getToken();

        assertThat(first).isNotEqualTo(second);
    }

    // ---------- verifyRefreshToken ----------

    @Test
    @DisplayName("verify: валидный токен возвращается, ничего не удаляется")
    void verify_validToken_returnsToken() {
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .token("valid")
                .expiryDate(OffsetDateTime.now().plusMinutes(10))
                .build();

        when(refreshTokenRepository.findByToken("valid")).thenReturn(Optional.of(stored));

        RefreshToken result = refreshTokenService.verifyRefreshToken("valid");

        assertThat(result).isSameAs(stored);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("verify: неизвестный токен -> RefreshTokenNotFoundException")
    void verify_unknownToken_throws() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("unknown"))
                .isInstanceOf(RefreshTokenNotFoundException.class);

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("verify: просроченный токен удаляется из БД и бросает RefreshTokenExpiredException")
    void verify_expiredToken_deletesAndThrows() {
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .token("expired")
                .expiryDate(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("expired"))
                .isInstanceOf(RefreshTokenExpiredException.class);

        verify(refreshTokenRepository).delete(stored);
    }

    // ---------- deleteRefreshTokenByUser ----------

    @Test
    @DisplayName("deleteByUser: делегирует удаление репозиторию")
    void deleteByUser_delegatesToRepository() {
        refreshTokenService.deleteRefreshTokenByUser(user);

        verify(refreshTokenRepository).deleteByUser(user);
    }
}