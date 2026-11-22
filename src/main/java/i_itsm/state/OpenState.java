package i_itsm.state;

import java.time.LocalDateTime;
import java.util.Set;

import i_itsm.model.Incident;

/** A new incident, or one that was reopened. Waiting for the technician to acknowledge it and start work. */
public class OpenState implements IncidentState {

    @Override
    public String name() {
        return "OPEN";
    }

    @Override
    public Set<String> allowedNext() {
        return Set.of("IN_PROGRESS");
    }

    @Override
    public boolean canAcknowledge() {
        return true;
    }

    /** Reopened: the old fix did not work, so the resolution is cleared. */
    @Override
    public void enter(Incident incident, LocalDateTime now) {
        incident.setResolvedAt(null);
        incident.setClosedAt(null);
        incident.setResolutionNotes(null);
    }
}
