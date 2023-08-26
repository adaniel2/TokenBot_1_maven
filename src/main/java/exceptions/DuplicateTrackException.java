package exceptions;

public class DuplicateTrackException extends RuntimeException {

    public DuplicateTrackException(String message) {
        super(message);
    }
}