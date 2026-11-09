package volodea.fruitful.dto;

import volodea.fruitful.entity.User;

public record RotatedSession(
        User user
        , String refreshToken
        , Long sessionId
) {
}
