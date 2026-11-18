package volodea.fruitful.dto.auth;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(
        @NotNull String refreshToken
) {
}
