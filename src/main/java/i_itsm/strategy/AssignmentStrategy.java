package i_itsm.strategy;

import java.util.List;

import i_itsm.model.Incident;
import i_itsm.model.Technician;

public interface AssignmentStrategy {

    Technician assign(Incident incident, List<Technician> technicians);
}
