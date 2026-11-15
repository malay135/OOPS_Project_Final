package i_itsm.strategy;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import i_itsm.model.Incident;
import i_itsm.model.Technician;

@Component
public class SkillBasedAssignment implements AssignmentStrategy {
    @Override
    public Technician assign(Incident incident, List<Technician> technicians) {
        return technicians.stream()
            .filter(t -> t.getSkills() != null && t.getSkills().stream()
                .anyMatch(skill -> skill.equalsIgnoreCase(incident.getCategory())))
            .min(Comparator.comparingInt(Technician::getActiveIncidentCount)
                .thenComparing(Technician::getId))
            .orElse(null);
    }
}
