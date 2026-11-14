package volodea.fruitful.exception;

public class UserWithEmailNotFoundException extends NotFoundException {
    public UserWithEmailNotFoundException(String email) {
        super("user with email: " + email + "not found!");
    }
}
