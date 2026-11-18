package volodea.fruitful.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import volodea.fruitful.entity.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByUserA_IdAndUserB_Id(Long userAId, Long userBId);

    @Query(""" 
        SELECT c
        FROM Conversation c
        JOIN FETCH c.userA
        JOIN FETCH c.userB
        WHERE c.userA.id = :userId OR c.userB.id = :userId
    """)
    List<Conversation> findAllByUserId(@Param("userId") Long userId);
}
