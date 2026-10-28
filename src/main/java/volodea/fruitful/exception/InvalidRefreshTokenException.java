package volodea.fruitful.exception;

public class InvalidRefreshTokenException extends UnauthorizedException{
    public InvalidRefreshTokenException(String token) {
        super("RefreshToken with token " + token + " not found");
    }
}
