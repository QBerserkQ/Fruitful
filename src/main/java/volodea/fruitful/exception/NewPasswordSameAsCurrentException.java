package volodea.fruitful.exception;

public class NewPasswordSameAsCurrentException extends ChangePasswordException {
    public NewPasswordSameAsCurrentException() {
        super("New password must be different from current password");
    }
}
