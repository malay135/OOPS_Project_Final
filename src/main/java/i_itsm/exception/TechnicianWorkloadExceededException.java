package i_itsm.exception;

/**
 * A technician already has the maximum number of active incidents the administrator allows.
 * Returned to the browser as HTTP 409 with the message (it is a kind of IncidentAssignmentException).
 */
public class TechnicianWorkloadExceededException extends IncidentAssignmentException {
    public TechnicianWorkloadExceededException(String message) {
        super(message);
    }
}
