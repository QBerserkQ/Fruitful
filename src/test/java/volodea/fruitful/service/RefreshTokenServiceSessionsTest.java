package volodea.fruitful.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import volodea.fruitful.dto.SessionResponse;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;
import volodea.fruitful.repository.RefreshTokenRepository;
import volodea.fruitful.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceSessionsTest {

    private static final String EMAIL = "test@mail.com";

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private RefreshTokenService refreshTokenService;

    private static User userOf() {
        return User.builder()
                .id(1L)
                .email(EMAIL)
                .username("john")
                .passwordHash("hashed")
                .role(Role.USER)
                .build();
    }

    // Сущность замокана, чтобы тест не зависел от полей builder'а RefreshToken.
    private static RefreshToken tokenOf(Long id) {
        RefreshToken token = RefreshToken.builder().build();
        ReflectionTestUtils.setField(token, "id", id, Long.class);
        return token;
    }

    @Test
    @DisplayName("getAllSessions: только сессия с совпадающим id помечается current, порядок из репозитория сохраняется")
    void getAllSessions_marksCurrentSession() {
        User user = userOf();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserAndExpiresAtAfterOrderByLastUsedAtDesc(
                eq(user), any(OffsetDateTime.class)))
                .thenReturn(List.of(tokenOf(10L), tokenOf(11L)));

        List<SessionResponse> result = refreshTokenService.getAllSessions(EMAIL, 11L);

        assertThat(result)
                .extracting(SessionResponse::id, SessionResponse::current)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, false),
                        org.assertj.core.groups.Tuple.tuple(11L, true)
                );
    }

    @Test
    @DisplayName("getAllSessions: токен без sid (null) -> ни одна сессия не current, без исключений")
    void getAllSessions_nullCurrentSessionId_noneCurrent() {
        User user = userOf();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserAndExpiresAtAfterOrderByLastUsedAtDesc(
                eq(user), any(OffsetDateTime.class)))
                .thenReturn(List.of(tokenOf(10L), tokenOf(11L)));

        List<SessionResponse> result = refreshTokenService.getAllSessions(EMAIL, null);

        assertThat(result).extracting(SessionResponse::current).containsOnly(false);
    }

    @Test
    @DisplayName("deleteSession: удаляет сессию по id только в рамках текущего пользователя")
    void deleteSession_deletesByIdAndOwner() {
        User user = userOf();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        refreshTokenService.deleteSession(EMAIL, 5L);

        verify(refreshTokenRepository).deleteByIdAndUser(5L, user);
    }
}