package volodea.fruitful.dto.auth;

import java.time.OffsetDateTime;

public record SessionResponse(
    Long id
    , String userAgent
    , OffsetDateTime lastUsedAt
    , boolean current
) {
}
