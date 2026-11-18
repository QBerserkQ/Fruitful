package volodea.fruitful.dto.chat;

public record MessageSentEvent(
        Long recipientId
        , MessageResponse message
) {
}
