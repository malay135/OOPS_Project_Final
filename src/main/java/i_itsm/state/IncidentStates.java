package i_itsm.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Finds the state object for a status text. The state objects hold no data, so one of each is shared. */
public final class IncidentStates {

    private static final Map<String, IncidentState> STATES = new LinkedHashMap<>();

    static {
        register(new OpenState());
        register(new InProgressState());
        register(new ResolvedState());
        register(new ClosedState());
    }

    private IncidentStates() {
    }

    private static void register(IncidentState state) {
        STATES.put(state.name(), state);
    }

    /** "in_progress" -> InProgressState. Unknown or empty statuses are refused with a clear message. */
    public static IncidentState of(String status) {
        if (status == null || status.isBlank()) throw new IllegalArgumentException("Status is required");
        IncidentState state = STATES.get(status.trim().toUpperCase());
        if (state == null) throw new IllegalArgumentException("Invalid incident status: " + status);
        return state;
    }

    /** All statuses, in lifecycle order. */
    public static Set<String> names() {
        return STATES.keySet();
    }
}
