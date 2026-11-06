package volodea.fruitful.exception;

public class SessionVerificationException extends ChangePasswordException {
    public SessionVerificationException() {
        super("Session could not be verified");
    }
}
