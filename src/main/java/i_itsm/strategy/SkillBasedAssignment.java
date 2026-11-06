package i_itsm.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import i_itsm.model.Incident;
import i_itsm.model.Technician;

@Component
public class SkillBasedAssignment implements AssignmentStrategy{

	@Override
	public Technician assign(Incident incident, List<Technician> technicians) {
	    for (Technician technician : technicians) {
	        if (technician.getSkills().stream()
	                .anyMatch(skill -> skill.equalsIgnoreCase(
	                        incident.getCategory()))) {
	            return technician;
	        }
	    }
	    return null;
	}
}
