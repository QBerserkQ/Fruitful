package volodea.fruitful.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import volodea.fruitful.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversation_Id(
            Long conversationId
            , Pageable pageable
    );
}
