package volodea.fruitful.dto;

import volodea.fruitful.entity.User;

public record SessionTokens(
        User user
        , String refreshToken
        , Long sessionId
) {
}
