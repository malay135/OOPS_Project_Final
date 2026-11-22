package i_itsm.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import i_itsm.model.Incident;
import i_itsm.model.Technician;

/**
 * Default strategy, built by composing the two simple strategies:
 * try the skill-based one first, and fall back to the least-loaded one.
 */
@Component
public class SkillThenWorkloadAssignment implements AssignmentStrategy {

    private final SkillBasedAssignment skillBased;
    private final LeastLoadedAssignment leastLoaded;

    public SkillThenWorkloadAssignment(SkillBasedAssignment skillBased, LeastLoadedAssignment leastLoaded) {
        this.skillBased = skillBased;
        this.leastLoaded = leastLoaded;
    }

    @Override
    public Technician assign(Incident incident, List<Technician> technicians) {
        Technician bySkill = skillBased.assign(incident, technicians);
        return bySkill != null ? bySkill : leastLoaded.assign(incident, technicians);
    }
}
