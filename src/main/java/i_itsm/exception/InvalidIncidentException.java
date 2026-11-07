package i_itsm.exception;

public class InvalidIncidentException extends IllegalArgumentException {

    public InvalidIncidentException(String message) {
        super(message);
    }
}