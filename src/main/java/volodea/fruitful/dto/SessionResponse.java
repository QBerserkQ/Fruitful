package volodea.fruitful.dto;

import java.time.OffsetDateTime;

public record SessionResponse(
    Long id
    , String userAgent
    , OffsetDateTime lastUsedAt
    , boolean current
) {
}
