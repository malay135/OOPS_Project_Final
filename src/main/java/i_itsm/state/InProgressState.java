package i_itsm.state;

import java.time.LocalDateTime;
import java.util.Set;

import i_itsm.model.Incident;

/** The assigned technician is investigating. */
public class InProgressState implements IncidentState {

    @Override
    public String name() {
        return "IN_PROGRESS";
    }

    @Override
    public Set<String> allowedNext() {
        return Set.of("RESOLVED");
    }

    /** Starting work is the first response, and also acknowledges the incident if the technician skipped that step. */
    @Override
    public void enter(Incident incident, LocalDateTime now) {
        if (incident.getFirstResponseAt() == null) incident.setFirstResponseAt(now);
        if (incident.getAcknowledgedAt() == null) incident.setAcknowledgedAt(now);
    }
}
