package volodea.fruitful.exception;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String email) {
        super("user with email: " + email + "not found!");
    }
}
