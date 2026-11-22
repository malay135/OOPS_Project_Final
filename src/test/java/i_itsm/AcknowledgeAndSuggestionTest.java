package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import i_itsm.dto.ResolutionSuggestion;
import i_itsm.model.Incident;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.AuditService;
import i_itsm.service.IncidentService;
import i_itsm.service.PriorityService;
import i_itsm.service.ResolutionSuggestionService;
import i_itsm.service.SLAService;

/** Technicians acknowledge their own new incidents; similar fixed incidents are suggested. */
class AcknowledgeAndSuggestionTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private SLAService slaService;
    @Mock private NotificationService notificationService;

    private IncidentService incidentService;
    private Incident incident;
    private Technician ravi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        incidentService = new IncidentService(incidentRepository, new PriorityService(), auditService, slaService,
                notificationService, technicianRepository, userRepository);
        ravi = new Technician(1L, "Ravi Kumar", Set.of("HARDWARE"), 1);
        ravi.setUsername("technician");
        Technician priya = new Technician(2L, "Priya Sharma", Set.of("NETWORK"), 0);
        priya.setUsername("priya");
        when(technicianRepository.findByUsername("technician")).thenReturn(Optional.of(ravi));
        when(technicianRepository.findByUsername("priya")).thenReturn(Optional.of(priya));

        incident = new Incident();
        incident.setId(4L); incident.setTitle("Laptop will not turn on"); incident.setStatus("OPEN");
        incident.setAssignedTechnician(ravi); incident.setReportedBy(new User("employee", "x", "EMPLOYEE"));
        when(incidentRepository.findById(4L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void assignedTechnicianAcknowledges() {
        loginAs("technician", "TECHNICIAN");
        Incident saved = incidentService.acknowledge(4L);
        assertNotNull(saved.getAcknowledgedAt());
        assertNotNull(saved.getFirstResponseAt(), "acknowledging counts as the first response");
        assertEquals("OPEN", saved.getStatus(), "it stays open until work starts");
        verify(auditService).recordAction(eq(4L), eq("ACKNOWLEDGED"), anyString(), eq("technician"));
        verify(notificationService).sendNotificationTo(eq(4L), anyString(), eq("IN_APP"), eq("employee"));
    }

    @Test
    void cannotAcknowledgeTwice() {
        loginAs("technician", "TECHNICIAN");
        incidentService.acknowledge(4L);
        assertThrows(IllegalStateException.class, () -> incidentService.acknowledge(4L));
    }

    @Test
    void anotherTechnicianCannotAcknowledge() {
        loginAs("priya", "TECHNICIAN");
        assertThrows(AccessDeniedException.class, () -> incidentService.acknowledge(4L));
    }

    @Test
    void adminCannotAcknowledgeForATechnician() {
        loginAs("admin", "ADMINISTRATOR");
        assertThrows(AccessDeniedException.class, () -> incidentService.acknowledge(4L));
    }

    @Test
    void onlyOpenIncidentsCanBeAcknowledged() {
        loginAs("technician", "TECHNICIAN");
        incident.setStatus("IN_PROGRESS");
        assertThrows(IllegalStateException.class, () -> incidentService.acknowledge(4L));
    }

    private Incident finished(long id, String title, String description, String category, String status, String notes) {
        Incident i = new Incident();
        i.setId(id); i.setTitle(title); i.setDescription(description); i.setCategory(category); i.setStatus(status); i.setResolutionNotes(notes);
        return i;
    }

    @Test
    void similarFixedIncidentsAreSuggestedBestFirst() {
        Incident current = finished(10L, "Payment app down", "Users cannot complete payments", "SOFTWARE", "OPEN", null);
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(current));
        when(incidentRepository.findAll()).thenReturn(List.of(
                current,
                finished(1L, "Payment app down again", "Users cannot complete payments today", "SOFTWARE", "CLOSED", "Restarted the payment service"),
                finished(2L, "Payment page slow", "Payments take long", "SOFTWARE", "RESOLVED", "Cleared the cache"),
                finished(3L, "Payment app down", "Users cannot complete payments", "SOFTWARE", "IN_PROGRESS", null), // not finished
                finished(4L, "Printer offline", "Floor 2 printer offline", "HARDWARE", "RESOLVED", "Replaced cable"),  // not alike
                finished(5L, "Payment app down", "Users cannot complete payments", "SOFTWARE", "RESOLVED", " ")));   // no notes

        List<ResolutionSuggestion> suggestions = new ResolutionSuggestionService(incidentRepository).suggestFor(10L);

        assertEquals(1L, suggestions.get(0).incidentId());
        assertEquals("Restarted the payment service", suggestions.get(0).resolutionNotes());
        assertTrue(suggestions.stream().noneMatch(s -> s.incidentId() == 3L || s.incidentId() == 4L || s.incidentId() == 5L));
        assertTrue(suggestions.get(0).matchPercent() > 50);
    }
}
