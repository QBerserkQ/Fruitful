package volodea.fruitful.exception;

import java.time.OffsetDateTime;

public class RefreshTokenExpiredException extends UnauthorizedException {
    public RefreshTokenExpiredException(OffsetDateTime expiryDate) {
        super("RefreshToken expired " + expiryDate);
    }
}
