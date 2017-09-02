package maxim.z.exceptions;

public class FSException extends RuntimeException {

    public FSException() {
    }

    public FSException(String message) {
        super(message);
    }

    public FSException(String message, Throwable cause) {
        super(message, cause);
    }
}
