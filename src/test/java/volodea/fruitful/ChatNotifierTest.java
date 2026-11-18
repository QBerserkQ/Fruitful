package volodea.fruitful;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import volodea.fruitful.config.websocket.ChatNotifier;
import volodea.fruitful.dto.chat.MessageResponse;
import volodea.fruitful.dto.chat.MessageSentEvent;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatNotifierTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    ChatNotifier chatNotifier;

    @Test
    void sendsMessageToRecipientUserQueue() {
        MessageResponse message = new MessageResponse(
                1L, 10L, 2L, "привет", OffsetDateTime.now());

        chatNotifier.onMessageSent(new MessageSentEvent(3L, message));

        verify(messagingTemplate).convertAndSendToUser("3", "/queue/messages", message);
    }
}