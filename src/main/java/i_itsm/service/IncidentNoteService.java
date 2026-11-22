package i_itsm.service;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import i_itsm.model.Incident;
import i_itsm.model.IncidentNote;
import i_itsm.model.User;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentNoteRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.UserRepository;

@Service
public class IncidentNoteService {
    private static final java.util.Set<String> TYPES = java.util.Set.of("ADDITIONAL_INFO", "INVESTIGATION", "SUPPORT_REQUEST", "RESPONSE");
    private final IncidentNoteRepository noteRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public IncidentNoteService(IncidentNoteRepository noteRepository, IncidentRepository incidentRepository,
                               UserRepository userRepository, AuditService auditService, NotificationService notificationService) {
        this.noteRepository = noteRepository; this.incidentRepository = incidentRepository;
        this.userRepository = userRepository; this.auditService = auditService; this.notificationService = notificationService;
    }
    private String username() { Authentication a = SecurityContextHolder.getContext().getAuthentication(); return a == null ? "system" : a.getName(); }
    private User user() { return userRepository.findByUsername(username()).orElseThrow(() -> new AccessDeniedException("User not found")); }
    public List<IncidentNote> list(Long incidentId) { incidentRepository.findById(incidentId).orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId)); return noteRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId); }
    public IncidentNote add(Long incidentId, String type, String message) {
        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
        if (type == null || !TYPES.contains(type.toUpperCase())) throw new IllegalArgumentException("Invalid note type");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Note message is required");
        if (message.length() > 3000) throw new IllegalArgumentException("Note must not exceed 3000 characters");
        User author = user();
        String role = author.getRole();
        if ("EMPLOYEE".equals(role)) {
            if (incident.getReportedBy() == null || !incident.getReportedBy().getId().equals(author.getId())) throw new AccessDeniedException("You can only add information to your own incidents");
            if (!"ADDITIONAL_INFO".equals(type.toUpperCase()) && !"RESPONSE".equals(type.toUpperCase())) throw new AccessDeniedException("Employees may only add information or respond");
        }
        if ("TECHNICIAN".equals(role)) {
            if (incident.getAssignedTechnician() == null || incident.getAssignedTechnician().getUsername() == null || !username().equals(incident.getAssignedTechnician().getUsername())) throw new AccessDeniedException("Only the assigned technician can add technician notes");
        }
        IncidentNote note = new IncidentNote(); note.setIncident(incident); note.setAuthor(author); note.setType(type.toUpperCase()); note.setMessage(message.trim());
        IncidentNote saved = noteRepository.save(note);
        auditService.recordAction(incidentId, "NOTE_ADDED", saved.getType() + ": " + message.trim().substring(0, Math.min(120, message.trim().length())), username());
        notifyAboutNote(incident, saved.getType());
        return saved;
    }

    // A question from the technician goes to the employee; the employee's reply or extra information goes to the technician.
    // The EMAIL copy reaches everyone involved who saved an email address.
    private void notifyAboutNote(Incident incident, String type) {
        String title = "'" + incident.getTitle() + "'";
        if ("SUPPORT_REQUEST".equals(type) && incident.getReportedBy() != null) {
            String message = "The technician needs more information about your incident " + title + ". Please open it and reply.";
            notificationService.sendNotificationTo(incident.getId(), message, "IN_APP", incident.getReportedBy().getUsername());
            notificationService.sendNotification(incident.getId(), message, "EMAIL");
        } else if (("RESPONSE".equals(type) || "ADDITIONAL_INFO".equals(type))
                && incident.getAssignedTechnician() != null && incident.getAssignedTechnician().getUsername() != null
                && !username().equals(incident.getAssignedTechnician().getUsername())) {
            String message = "New information was added to incident " + title + ".";
            notificationService.sendNotificationTo(incident.getId(), message, "IN_APP", incident.getAssignedTechnician().getUsername());
        }
    }
}
