package volodea.fruitful.dto.chat;

import java.time.OffsetDateTime;

public record ConversationResponse(
        Long id
        , Long partnerId
        , String partnerUsername
        , String lastMessage
        , OffsetDateTime createdAt
        , OffsetDateTime lastMessageAt
) {
}
