package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import volodea.fruitful.util.TokenHasher;
import volodea.fruitful.dto.RotatedSession;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.InvalidRefreshTokenException;
import volodea.fruitful.exception.RefreshTokenExpiredException;
import volodea.fruitful.repository.RefreshTokenRepository;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${fruitful.security.jwt.refreh-token-expiration-ms}")
    private long expMs;

    private static final int MAX_SESSIONS = 3;

    @Transactional
    public String createSession(User user, String userAgent) {
        List<RefreshToken> sessions = refreshTokenRepository.findByUserOrderByLastUsedAtAsc(user);
        int excess = sessions.size() - MAX_SESSIONS + 1;

        if(excess > 0) {
            refreshTokenRepository.deleteAll(sessions.subList(0, excess));
        }

        String rawToken = TokenHasher.generateToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(rawToken))
                .userAgent(truncate(userAgent, 255))
                .expiresAt(Instant.now().plusMillis(expMs).atOffset(ZoneOffset.UTC))
                .build();

        refreshTokenRepository.save(refreshToken);

        return  rawToken;
    }

    @Transactional(noRollbackFor = RefreshTokenExpiredException.class)
    public RotatedSession rotate(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(
                TokenHasher.sha256(rawToken)
        ).orElseThrow(InvalidRefreshTokenException::new);

        if(refreshToken.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenExpiredException();
        }

        String newToken = TokenHasher.generateToken();

        refreshToken.setTokenHash(TokenHasher.sha256(newToken));
        refreshToken.setLastUsedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return new RotatedSession(refreshToken.getUser(), newToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
