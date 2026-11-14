package volodea.fruitful.dto.auth;

import volodea.fruitful.entity.User;

public record SessionTokens(
        User user
        , String refreshToken
        , Long sessionId
) {
}
