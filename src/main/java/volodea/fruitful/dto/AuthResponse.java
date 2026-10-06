package volodea.fruitful.dto;

public record AuthResponse(
        String accesToken
        , String refreshToken
) {
}
