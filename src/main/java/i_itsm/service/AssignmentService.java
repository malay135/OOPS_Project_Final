package i_itsm.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import i_itsm.exception.IncidentAssignmentException;
import i_itsm.model.DelegationRequest;
import i_itsm.model.Incident;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.notification.NotificationService;
import i_itsm.repository.DelegationRequestRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.strategy.LeastLoadedAssignment;
import i_itsm.strategy.SkillBasedAssignment;

@Service
public class AssignmentService {
    private final SkillBasedAssignment skillBasedAssignment;
    private final LeastLoadedAssignment leastLoadedAssignment;
    private final TechnicianRepository technicianRepository;
    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final DelegationRequestRepository delegationRepository;

    // Compatibility constructor for existing unit tests and integrations.
    public AssignmentService(SkillBasedAssignment skillBasedAssignment, LeastLoadedAssignment leastLoadedAssignment,
            TechnicianRepository technicianRepository, IncidentRepository incidentRepository,
            NotificationService notificationService, AuditService auditService) {
        this(skillBasedAssignment, leastLoadedAssignment, technicianRepository, incidentRepository,
                notificationService, auditService, null, null);
    }

    @Autowired
    public AssignmentService(SkillBasedAssignment skillBasedAssignment, LeastLoadedAssignment leastLoadedAssignment,
            TechnicianRepository technicianRepository, IncidentRepository incidentRepository,
            NotificationService notificationService, AuditService auditService, UserRepository userRepository,
            DelegationRequestRepository delegationRepository) {
        this.delegationRepository = delegationRepository;
        this.skillBasedAssignment = skillBasedAssignment;
        this.leastLoadedAssignment = leastLoadedAssignment;
        this.technicianRepository = technicianRepository;
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private Incident getAssignableIncident(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));
        if ("RESOLVED".equals(incident.getStatus()) || "CLOSED".equals(incident.getStatus()))
            throw new IncidentAssignmentException("Cannot assign a resolved or closed incident");
        return incident;
    }

    /** Automatic policy: best skill match first; if none, least-loaded active technician. */
    @Transactional
    public Technician autoAssignIncident(Long incidentId) {
        Incident incident = getAssignableIncident(incidentId);
        List<Technician> technicians = technicianRepository.findByActiveTrue();
        Technician selected = skillBasedAssignment.assign(incident, technicians);
        if (selected == null) selected = leastLoadedAssignment.assign(incident, technicians);
        if (selected == null) throw new IncidentAssignmentException("No active technician available");
        return persistAssignment(incident, selected, "AUTOMATIC_SKILL_THEN_WORKLOAD");
    }

    /** Admin-only override entry point; caller authorization is enforced by SecurityConfig. */
    @Transactional
    public Technician overrideAssignment(Long incidentId, Long technicianId) {
        if (technicianId == null) throw new IllegalArgumentException("technicianId is required");
        Incident incident = getAssignableIncident(incidentId);
        Technician selected = technicianRepository.findById(technicianId)
                .filter(Technician::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active technician not found: " + technicianId));
        if (delegationRepository != null) {
            for (DelegationRequest pending : delegationRepository.findByIncidentIdAndStatus(incidentId, DelegationRequest.PENDING)) {
                pending.setStatus(DelegationRequest.CANCELLED);
                pending.setDecidedBy(currentUsername());
                pending.setDecidedAt(LocalDateTime.now());
                delegationRepository.save(pending);
            }
        }
        return persistAssignment(incident, selected, "ADMIN_ASSIGNMENT_OVERRIDE");
    }

    /** Backwards-compatible endpoint: only automatic policy names are accepted. */
    @Transactional
    public Technician assignIncident(Long incidentId, String strategy) {
        if (strategy == null || !(strategy.equalsIgnoreCase("SKILL") || strategy.equalsIgnoreCase("LEAST_LOADED")
                || strategy.equalsIgnoreCase("SKILL_THEN_WORKLOAD")))
            throw new IllegalArgumentException("Only automatic SKILL_THEN_WORKLOAD assignment is supported");
        return autoAssignIncident(incidentId);
    }

    /**
     * The assigned technician asks to hand the incident to another technician.
     * Nothing moves yet: the incident stays with the requester until an administrator approves.
     */
    @Transactional
    public DelegationRequest requestDelegation(Long incidentId, Long targetTechnicianId) {
        Incident incident = getAssignableIncident(incidentId);
        Technician current = technicianRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new AccessDeniedException("Your login is not linked to a technician"));
        if (incident.getAssignedTechnician() == null || !incident.getAssignedTechnician().getId().equals(current.getId()))
            throw new AccessDeniedException("Only the currently assigned technician can delegate this incident");
        if (targetTechnicianId == null) throw new IllegalArgumentException("technicianId is required");
        Technician target = technicianRepository.findById(targetTechnicianId)
                .filter(Technician::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active target technician not found: " + targetTechnicianId));
        if (target.getId().equals(current.getId())) throw new IllegalArgumentException("Choose another technician");
        if (delegationRepository.existsByIncidentIdAndStatus(incidentId, DelegationRequest.PENDING))
            throw new IncidentAssignmentException("A delegation request for this incident is already waiting for admin approval");

        DelegationRequest request = delegationRepository.save(
                new DelegationRequest(incident.getId(), incident.getTitle(), current, target, currentUsername()));

        String message = "Technician " + current.getName() + " requests to delegate incident '" + incident.getTitle()
                + "' (ID: " + incident.getId() + ") to " + target.getName() + ". Your approval is required.";
        List<User> admins = userRepository == null ? List.of() : userRepository.findByRole("ADMINISTRATOR");
        for (User admin : admins) {
            notificationService.sendNotificationTo(incident.getId(), message, "IN_APP", admin.getUsername());
        }
        // One email (the email channel already reaches every administrator with a saved address)
        if (!admins.isEmpty())
            notificationService.sendNotificationTo(incident.getId(), message, "EMAIL", admins.get(0).getUsername());
        auditService.recordAction(incident.getId(), "DELEGATION_REQUESTED",
                current.getName() + " requested delegation to " + target.getName() + "; waiting for admin approval",
                currentUsername());
        return request;
    }

    /** Administrator approves: only now does the incident move to the target technician. */
    @Transactional(noRollbackFor = IncidentAssignmentException.class)
    public DelegationRequest approveDelegation(Long requestId) {
        DelegationRequest request = getPendingRequest(requestId);
        Technician from = request.getFromTechnician();
        Incident incident = getAssignableIncident(request.getIncidentId());
        if (incident.getAssignedTechnician() == null || !incident.getAssignedTechnician().getId().equals(from.getId())) {
            close(request, DelegationRequest.CANCELLED);
            throw new IncidentAssignmentException("This incident is no longer assigned to " + from.getName()
                    + ", so the request was cancelled");
        }
        Technician target = technicianRepository.findById(request.getToTechnician().getId())
                .filter(Technician::isActive)
                .orElseThrow(() -> new IllegalArgumentException("The target technician is no longer active. Reject this request or use Override assignment"));

        persistAssignment(incident, target, "DELEGATION_APPROVED");
        close(request, DelegationRequest.APPROVED);
        if (from.getUsername() != null)
            notificationService.sendNotificationTo(incident.getId(), "Your request to delegate incident '" + incident.getTitle()
                    + "' to " + target.getName() + " was approved.", "IN_APP", from.getUsername());
        return request;
    }

    /** Administrator rejects: the incident stays with the requesting technician. */
    @Transactional
    public DelegationRequest rejectDelegation(Long requestId) {
        DelegationRequest request = getPendingRequest(requestId);
        close(request, DelegationRequest.REJECTED);
        Technician from = request.getFromTechnician();
        auditService.recordAction(request.getIncidentId(), "DELEGATION_REJECTED",
                "Delegation from " + from.getName() + " to " + request.getToTechnician().getName() + " was rejected",
                currentUsername());
        if (from.getUsername() != null)
            notificationService.sendNotificationTo(request.getIncidentId(), "Your request to delegate incident '"
                    + request.getIncidentTitle() + "' to " + request.getToTechnician().getName()
                    + " was rejected. It stays with you.", "IN_APP", from.getUsername());
        return request;
    }

    public List<DelegationRequest> listDelegations(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL"))
            return delegationRepository.findAllByOrderByRequestedAtDesc();
        return delegationRepository.findByStatusOrderByRequestedAtDesc(status.toUpperCase());
    }

    public List<DelegationRequest> myDelegations() {
        Technician me = technicianRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new AccessDeniedException("Your login is not linked to a technician"));
        return delegationRepository.findByFromTechnicianIdOrderByRequestedAtDesc(me.getId());
    }

    private DelegationRequest getPendingRequest(Long id) {
        DelegationRequest request = delegationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Delegation request not found: " + id));
        if (!DelegationRequest.PENDING.equals(request.getStatus()))
            throw new IncidentAssignmentException("This request was already " + request.getStatus().toLowerCase());
        return request;
    }

    private void close(DelegationRequest request, String status) {
        request.setStatus(status);
        request.setDecidedBy(currentUsername());
        request.setDecidedAt(LocalDateTime.now());
        delegationRepository.save(request);
    }

    private Technician persistAssignment(Incident incident, Technician assigned, String action) {
        Technician previous = incident.getAssignedTechnician();
        if (previous == null || !previous.getId().equals(assigned.getId())) {
            if (previous != null) {
                previous.setActiveIncidentCount(Math.max(0, previous.getActiveIncidentCount() - 1));
                technicianRepository.save(previous);
            }
            assigned.setActiveIncidentCount(assigned.getActiveIncidentCount() + 1);
            technicianRepository.save(assigned);
        }
        incident.setAssignedTechnician(assigned);
        incidentRepository.save(incident);
        auditService.recordAction(incident.getId(), action,
                "Incident assigned to " + assigned.getName() + " (Technician ID: " + assigned.getId() + ")", currentUsername());
        // Tell the assigned technician (only if they have a login; a recipient-less alert would show to everyone).
        String message = "Incident '" + incident.getTitle() + "' has been assigned to " + assigned.getName();
        if (assigned.getUsername() != null)
            notificationService.sendNotificationTo(incident.getId(), message, "IN_APP", assigned.getUsername());

        // Tell the employee who reported the incident: one message for the first assignment,
        // and (in-app + email) when the technician later changes.
        User reporter = incident.getReportedBy();
        if (reporter != null && reporter.getUsername() != null) {
            if (previous == null) {
                notificationService.sendNotificationTo(incident.getId(), "Your incident '" + incident.getTitle()
                        + "' has been assigned to " + assigned.getName() + ".", "IN_APP", reporter.getUsername());
            } else if (!previous.getId().equals(assigned.getId())) {
                String employeeMessage = "Your incident '" + incident.getTitle() + "' has been reassigned from "
                        + previous.getName() + " to " + assigned.getName() + ".";
                notificationService.sendNotificationTo(incident.getId(), employeeMessage, "IN_APP", reporter.getUsername());
                notificationService.sendNotificationTo(incident.getId(), employeeMessage, "EMAIL", reporter.getUsername());
            }
        }
        return assigned;
    }
}
