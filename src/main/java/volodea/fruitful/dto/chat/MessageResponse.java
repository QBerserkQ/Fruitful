package volodea.fruitful.dto.chat;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long id
        , Long conversationId
        , Long senderId
        , String content
        , OffsetDateTime createdAt
) {
}
