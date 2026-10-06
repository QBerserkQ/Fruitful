package volodea.fruitful.dto;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(
        @NotNull String refreshToken
) {
}
