package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import i_itsm.model.Incident;
import i_itsm.model.Technician;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.AuditService;
import i_itsm.service.IncidentService;
import i_itsm.service.PriorityService;
import i_itsm.service.SLAService;

/** Status changes: technicians may only update their own incidents, and resolving needs notes. */
class IncidentServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private PriorityService priorityService;
    @Mock private AuditService auditService;
    @Mock private SLAService slaService;
    @Mock private NotificationService notificationService;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private UserRepository userRepository;

    private IncidentService incidentService;
    private Technician ravi;
    private Technician priya;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        incidentService = new IncidentService(incidentRepository, priorityService, auditService,
                slaService, notificationService, technicianRepository, userRepository);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(call -> call.getArgument(0));

        ravi = new Technician(1L, "Ravi Kumar", Set.of("HARDWARE"), 2);
        ravi.setUsername("technician");
        priya = new Technician(2L, "Priya Sharma", Set.of("NETWORK"), 1);
        priya.setUsername("priya");
        when(technicianRepository.findByUsername("technician")).thenReturn(Optional.of(ravi));
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Incident incident(String status, Technician assignedTo) {
        Incident incident = new Incident();
        incident.setId(5L);
        incident.setTitle("Laptop will not turn on");
        incident.setStatus(status);
        incident.setAssignedTechnician(assignedTo);
        when(incidentRepository.findById(5L)).thenReturn(Optional.of(incident));
        return incident;
    }

    @Test
    void technicianCanStartWorkOnOwnIncident() {
        loginAs("technician", "TECHNICIAN");
        Incident incident = incident("OPEN", ravi);

        incidentService.updateStatus(5L, "IN_PROGRESS", null);

        assertEquals("IN_PROGRESS", incident.getStatus());
    }

    @Test
    void technicianCannotUpdateSomeoneElsesIncident() {
        loginAs("technician", "TECHNICIAN");
        incident("OPEN", priya);

        assertThrows(AccessDeniedException.class,
                () -> incidentService.updateStatus(5L, "IN_PROGRESS", null));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void technicianWithoutLinkedLoginIsRejected() {
        loginAs("newtech", "TECHNICIAN");
        when(technicianRepository.findByUsername("newtech")).thenReturn(Optional.empty());
        incident("OPEN", ravi);

        assertThrows(AccessDeniedException.class,
                () -> incidentService.updateStatus(5L, "IN_PROGRESS", null));
    }

    @Test
    void resolvingWithoutNotesIsRejected() {
        loginAs("technician", "TECHNICIAN");
        incident("IN_PROGRESS", ravi);

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.updateStatus(5L, "RESOLVED", "   "));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resolvingSavesNotesAndLowersWorkload() {
        loginAs("technician", "TECHNICIAN");
        Incident incident = incident("IN_PROGRESS", ravi);

        incidentService.updateStatus(5L, "RESOLVED", "Replaced the power adapter");

        assertEquals("RESOLVED", incident.getStatus());
        assertEquals("Replaced the power adapter", incident.getResolutionNotes());
        assertNotNull(incident.getResolvedAt());
        assertEquals(1, ravi.getActiveIncidentCount());
    }

    @Test
    void adminReopeningClearsResolutionAndRaisesWorkload() {
        loginAs("admin", "ADMINISTRATOR");
        Incident incident = incident("RESOLVED", ravi);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolutionNotes("Replaced the power adapter");

        incidentService.updateStatus(5L, "OPEN", null);

        assertEquals("OPEN", incident.getStatus());
        assertNull(incident.getResolvedAt());
        assertNull(incident.getResolutionNotes());
        assertEquals(3, ravi.getActiveIncidentCount());
    }
}
