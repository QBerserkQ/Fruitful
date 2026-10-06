package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.RefreshTokenExpiredException;
import volodea.fruitful.exception.RefreshTokenNotFoundException;
import volodea.fruitful.repository.RefreshTokenRepository;

import java.time.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${fruitful.security.jwt.refreh-token-expiration-ms}")
    private long expMs;

    @Transactional
    public RefreshToken generateRefreshToken(User user) {
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseGet(() -> RefreshToken.builder().user(user).build());

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(expMs).atOffset(ZoneOffset.UTC));

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(noRollbackFor = RefreshTokenExpiredException.class)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RefreshTokenNotFoundException(token));

        if(refreshToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenExpiredException(refreshToken.getExpiryDate());
        }

        return refreshToken;
    }

    @Transactional
    public void deleteRefreshTokenByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
