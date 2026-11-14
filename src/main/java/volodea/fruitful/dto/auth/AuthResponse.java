package volodea.fruitful.dto.auth;

public record AuthResponse(
        String accessToken
        , String refreshToken
) {
}
