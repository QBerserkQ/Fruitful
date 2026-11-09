package volodea.fruitful.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    List<RefreshToken> findByUserOrderByLastUsedAtAsc(User user);
    List<RefreshToken> findByUserAndExpiresAtAfterOrderByLastUsedAtDesc(User user, OffsetDateTime now);

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndUser(String tokenHash, User user);

    void deleteAllByUserAndIdNot(User user, Long id);
    void deleteByIdAndUser(Long id, User user);
}
