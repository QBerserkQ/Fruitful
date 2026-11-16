package volodea.fruitful.exception;

public class SelfConversationNotAllowedException extends RuntimeException {
    public SelfConversationNotAllowedException() {
        super("Self conversation not allowed!");
    }
}
