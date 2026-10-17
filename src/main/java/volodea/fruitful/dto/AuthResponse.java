package volodea.fruitful.dto;

public record AuthResponse(
        String accessToken
        , String refreshToken
) {
}
