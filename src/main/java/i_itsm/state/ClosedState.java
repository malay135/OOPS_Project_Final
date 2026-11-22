package i_itsm.state;

import java.time.LocalDateTime;
import java.util.Set;

import i_itsm.model.Incident;

/** Finished and confirmed. Can only be reopened. */
public class ClosedState implements IncidentState {

    @Override
    public String name() {
        return "CLOSED";
    }

    @Override
    public Set<String> allowedNext() {
        return Set.of("OPEN");
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean canEditDetails() {
        return false;
    }

    @Override
    public void enter(Incident incident, LocalDateTime now) {
        incident.setClosedAt(now);
        if (incident.getResolvedAt() == null) incident.setResolvedAt(now);
    }
}
