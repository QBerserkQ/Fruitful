package volodea.fruitful.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import volodea.fruitful.entity.Message;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversation_Id(
            Long conversationId
            , Pageable pageable
    );

    Optional<Message> findFirstByConversation_IdOrderByCreatedAtDesc(Long conversationId);
}
