package i_itsm.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import i_itsm.model.Incident;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.UserRepository;
import i_itsm.notification.NotificationService;
import i_itsm.repository.TechnicianRepository;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final PriorityService priorityService;
    private final AuditService auditService;
    private final SLAService slaService;
	private final NotificationService notificationService;
	private final TechnicianRepository technicianRepository;
	private final UserRepository userRepository;

    public IncidentService(
    		IncidentRepository incidentRepository,
    		PriorityService priorityService,
    		AuditService auditService,
    		SLAService slaService,
    		NotificationService notificationService,
    		TechnicianRepository technicianRepository,
    		UserRepository userRepository) {

    	this.incidentRepository = incidentRepository;
    	this.priorityService = priorityService;
    	this.auditService = auditService;
    	this.slaService = slaService;
		this.notificationService = notificationService;
		this.technicianRepository = technicianRepository;
		this.userRepository = userRepository;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private boolean currentUserHasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    // A technician may only update incidents assigned to them; administrators may update any incident
    private void checkCanUpdate(Incident incident) {
        if (!currentUserHasRole("TECHNICIAN")) {
            return;
        }
        Technician me = technicianRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new AccessDeniedException(
                        "Your login is not linked to a technician. Ask an administrator to link it on the Technicians page."));
        Technician assigned = incident.getAssignedTechnician();
        if (assigned == null || !assigned.getId().equals(me.getId())) {
            throw new AccessDeniedException("You can only update incidents assigned to you");
        }
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private User currentUser() {
        String username = currentUsername();
        return userRepository.findByUsername(username).orElse(null);
    }

    public Incident createIncident(Incident incident) {
    	incident.setPriority(
    		priorityService.calculatePriority(
        		incident.getImpact(),
        		incident.getUrgency(),
        		incident.getBusinessCriticality()
    		)
		);

    	incident.setStatus("OPEN");
    	incident.setCreatedAt(java.time.LocalDateTime.now());
    	incident.setResolutionNotes(null);

    	// Set the reporter
    	User reporter = currentUser();
    	if (reporter != null) {
    	    incident.setReportedBy(reporter);
    	}

    	slaService.calculateDeadline(incident);
    	
    	Incident savedIncident = incidentRepository.save(incident);

    	auditService.recordAction(
    			savedIncident.getId(),
    			"INCIDENT_CREATED",
    			"Incident created with priority "
    					+ savedIncident.getPriority()
    					+ " and SLA deadline "
    					+ savedIncident.getSlaDeadline(),
    			currentUsername());
		
		notificationService.sendNotification(
        savedIncident.getId(),
        "Your incident '" + savedIncident.getTitle()
                + "' has been created with priority "
                + savedIncident.getPriority(),
        "IN_APP");

		notificationService.sendNotification(
        savedIncident.getId(),
        "A new incident '" + savedIncident.getTitle()
                + "' has been created.",
        "EMAIL");

    	return savedIncident;
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Incident not found: " + id));
    }
    
    
    public Incident updateStatus(Long id, String newStatus) {
        return updateStatus(id, newStatus, null);
    }

    public Incident updateStatus(Long id, String newStatus, String notes) {
    	Incident incident = getIncidentById(id);

    	if (newStatus == null) {
    		throw new IllegalArgumentException("Status is required");
    	}

    	String status = newStatus.toUpperCase();

    	if (!java.util.Set.of(
    			"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")
    			.contains(status)) {
    		throw new IllegalArgumentException("Invalid incident status");
    	}

    	checkCanUpdate(incident);

    	String oldStatus = incident.getStatus();
        boolean wasDone = "RESOLVED".equals(oldStatus) || "CLOSED".equals(oldStatus);
        boolean nowDone = status.equals("RESOLVED") || status.equals("CLOSED");

        // Resolving needs notes on what was done
        String cleanNotes = notes == null ? "" : notes.trim();
        if (status.equals("RESOLVED") && !wasDone && cleanNotes.isEmpty()) {
            throw new IllegalArgumentException("Resolution notes are required to resolve an incident");
        }
        if (cleanNotes.length() > 2000) {
            throw new IllegalArgumentException("Resolution notes must not exceed 2000 characters");
        }

    	incident.setStatus(status);

    	if ((status.equals("RESOLVED") || status.equals("CLOSED"))
        	&& !("RESOLVED".equals(oldStatus)
             		|| "CLOSED".equals(oldStatus))) {

    	    incident.setResolvedAt(java.time.LocalDateTime.now());

    	    if (incident.getAssignedTechnician() != null) {
        	    var technician = incident.getAssignedTechnician();
        	    technician.setActiveIncidentCount(
                 		Math.max(0, technician.getActiveIncidentCount() - 1));

        	    technicianRepository.save(technician);
    	    }
		}

        if (nowDone && !cleanNotes.isEmpty()) {
            incident.setResolutionNotes(cleanNotes);
        }

        // Reopening a resolved/closed incident makes it active again
        if (wasDone && !nowDone) {
            incident.setResolvedAt(null);
            incident.setResolutionNotes(null); // the old notes stay in the audit log
            if (incident.getAssignedTechnician() != null) {
                var technician = incident.getAssignedTechnician();
                technician.setActiveIncidentCount(technician.getActiveIncidentCount() + 1);
                technicianRepository.save(technician);
            }
        }

    	Incident savedIncident = incidentRepository.save(incident);

    	auditService.recordAction(
    			savedIncident.getId(),
    			"STATUS_UPDATED",
    			oldStatus + " -> " + status
    					+ (cleanNotes.isEmpty() ? "" : " | Notes: " + shorten(cleanNotes, 150)),
    			currentUsername());

		notificationService.sendNotification(
		savedIncident.getId(),
		"The status of your incident '"
				+ savedIncident.getTitle()
				+ "' has been updated to " + status,
		"IN_APP");

		notificationService.sendNotification(
        savedIncident.getId(),
        "Incident '" + savedIncident.getTitle()
                + "' status updated to " + status,
        "EMAIL");

    	return savedIncident;
    }


	public List<Incident> searchIncidents(
    	    Long incidentId,
         String priority,
         String status,
         String category,
         Long technicianId,
         String title) {

    	return incidentRepository.searchIncidents(
    	    incidentId,
        	priority,
        	status,
        	category,
        	technicianId,
        	title
	);
	}

	public List<Incident> getMyIncidents() {
	    User user = currentUser();
	    if (user == null) {
	        return List.of();
	    }
	    return incidentRepository.findByReportedByOrderByCreatedAtDesc(user);
	}
}