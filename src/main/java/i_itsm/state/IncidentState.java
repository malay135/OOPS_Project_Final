package i_itsm.state;

import java.time.LocalDateTime;
import java.util.Set;

import i_itsm.model.Incident;

/**
 * One step of the incident lifecycle (State pattern).
 *
 * Each status is its own class that knows:
 *  - which statuses it may move to next,
 *  - what it allows (acknowledging, editing the details),
 *  - what changes on the incident when the incident enters it.
 *
 * IncidentService asks the current state "can you move to X?" instead of using a big switch,
 * so adding a new status (for example VERIFIED) means adding one class and one line in IncidentStates.
 *
 * Lifecycle:  OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED, and RESOLVED/CLOSED can be reopened (-> OPEN).
 */
public interface IncidentState {

    /** The status text stored in the database, for example "OPEN". */
    String name();

    /** The statuses this one may move to. */
    Set<String> allowedNext();

    default boolean canMoveTo(String nextStatus) {
        return allowedNext().contains(nextStatus);
    }

    /** Resolved and closed incidents are finished: they no longer count towards a technician's workload. */
    default boolean isFinished() {
        return false;
    }

    /** Only a new (open) incident can be acknowledged by its technician. */
    default boolean canAcknowledge() {
        return false;
    }

    /** Title, description, priority factors and service can be changed until the incident is closed. */
    default boolean canEditDetails() {
        return true;
    }

    /** Moving into this state needs resolution notes (what was wrong and how it was fixed). */
    default boolean needsResolutionNotes() {
        return false;
    }

    /** What changes on the incident when it enters this state. */
    void enter(Incident incident, LocalDateTime now);
}
