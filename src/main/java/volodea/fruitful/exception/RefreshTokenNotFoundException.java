package volodea.fruitful.exception;

public class RefreshTokenNotFoundException extends NotFoundException{
    public RefreshTokenNotFoundException(String token) {
        super("RefreshToken with token " + token + " not found");
    }
}
