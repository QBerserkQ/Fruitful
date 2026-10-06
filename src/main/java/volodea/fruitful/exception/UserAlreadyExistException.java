package volodea.fruitful.exception;

public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException() {
        super("User with email or username already exist");
    }
}
