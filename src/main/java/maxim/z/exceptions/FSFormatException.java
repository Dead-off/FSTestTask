package maxim.z.exceptions;

public class FSFormatException extends FSException {

    public FSFormatException(String message) {
        super(message);
    }

    public FSFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
