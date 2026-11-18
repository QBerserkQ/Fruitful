package volodea.fruitful.config.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import volodea.fruitful.dto.chat.MessageSentEvent;

@Component
@RequiredArgsConstructor
public class ChatNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(event.recipientId()),
                "/queue/messages",
                event.message()
        );
    }
}
