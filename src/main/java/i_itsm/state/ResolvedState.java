package i_itsm.state;

import java.time.LocalDateTime;
import java.util.Set;

import i_itsm.model.Incident;

/** The technician fixed it. The employee gives feedback and the incident is closed (verify step), or it is reopened. */
public class ResolvedState implements IncidentState {

    @Override
    public String name() {
        return "RESOLVED";
    }

    @Override
    public Set<String> allowedNext() {
        return Set.of("CLOSED", "OPEN");
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean needsResolutionNotes() {
        return true;
    }

    @Override
    public void enter(Incident incident, LocalDateTime now) {
        incident.setResolvedAt(now);
    }
}
