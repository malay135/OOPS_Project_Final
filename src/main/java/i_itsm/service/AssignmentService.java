package i_itsm.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import i_itsm.model.Incident;
import i_itsm.model.Technician;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.strategy.AssignmentStrategy;
import i_itsm.strategy.LeastLoadedAssignment;
import i_itsm.strategy.SkillBasedAssignment;
import i_itsm.exception.IncidentAssignmentException;
import i_itsm.exception.InvalidIncidentException;

@Service
public class AssignmentService {

    private final SkillBasedAssignment skillBasedAssignment;
    private final LeastLoadedAssignment leastLoadedAssignment;
    private final TechnicianRepository technicianRepository;
    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AssignmentService(
            SkillBasedAssignment skillBasedAssignment,
            LeastLoadedAssignment leastLoadedAssignment,
            TechnicianRepository technicianRepository,
            IncidentRepository incidentRepository,
            NotificationService notificationService,
            AuditService auditService) {

        this.skillBasedAssignment = skillBasedAssignment;
        this.leastLoadedAssignment = leastLoadedAssignment;
        this.technicianRepository = technicianRepository;
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    @Transactional 
    public Technician assignIncident(Long incidentId, String strategy) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Incident not found: " + incidentId));
        
        if ("RESOLVED".equals(incident.getStatus())
                || "CLOSED".equals(incident.getStatus())) {
            throw new IncidentAssignmentException(
                    "Cannot assign a resolved or closed incident");
}

        List<Technician> technicians =
                technicianRepository.findByActiveTrue();

        AssignmentStrategy assignmentStrategy;

        if ("SKILL".equalsIgnoreCase(strategy)) {
            assignmentStrategy = skillBasedAssignment;
        } else if ("LEAST_LOADED".equalsIgnoreCase(strategy)) {
            assignmentStrategy = leastLoadedAssignment;
        } else {
            throw new InvalidIncidentException(
                    "Unknown assignment strategy: " + strategy);
        }

        Technician assigned = assignmentStrategy.assign(
                incident, technicians);

        if (assigned == null) {
            throw new IncidentAssignmentException(
                    "No suitable technician found");
        }

        Technician previousTechnician =
        incident.getAssignedTechnician();

// Decrease the previous technician's workload
        if (previousTechnician != null
                && !previousTechnician.getId().equals(assigned.getId())) {

            previousTechnician.setActiveIncidentCount(
                    Math.max(0, previousTechnician.getActiveIncidentCount() - 1));

            technicianRepository.save(previousTechnician);
        }

// Increase workload only if this is a new assignment
        if (previousTechnician == null
                || !previousTechnician.getId().equals(assigned.getId())) {

            assigned.setActiveIncidentCount(
                    assigned.getActiveIncidentCount() + 1);

            technicianRepository.save(assigned);
        }

        incident.setAssignedTechnician(assigned);
        incidentRepository.save(incident);

        auditService.recordAction(
                incident.getId(),
                "TECHNICIAN_ASSIGNED",
                "Incident assigned to " + assigned.getName()
                        + " (Technician ID: " + assigned.getId() + ")",
                currentUsername());

        String message = "Incident '"
                + incident.getTitle()
                + "' has been assigned to "
                + assigned.getName();

        notificationService.sendNotification(
                incident.getId(), message, "IN_APP");

        notificationService.sendNotification(
                incident.getId(), message, "EMAIL");

        return assigned;
    }
}
