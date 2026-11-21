package volodea.fruitful.dto.post;

import volodea.fruitful.dto.user.UserSummaryResponse;

import java.time.OffsetDateTime;

public record PostResponse(
        Long id
        , String content
        , OffsetDateTime createdAt
        , UserSummaryResponse author
) {
}
