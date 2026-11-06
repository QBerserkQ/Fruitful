package volodea.fruitful.exception;

public class CurrentPasswordMismatchException extends ChangePasswordException {
    public CurrentPasswordMismatchException() {
        super("Current password is incorrect");
    }
}
