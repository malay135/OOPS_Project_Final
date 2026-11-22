package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import i_itsm.state.ClosedState;
import i_itsm.state.InProgressState;
import i_itsm.state.IncidentState;
import i_itsm.state.IncidentStates;
import i_itsm.state.OpenState;

/** The incident lifecycle rules live in the state classes (State pattern). */
class IncidentStateTest {

    @Test
    void eachStateKnowsItsNextSteps() {
        assertEquals(Set.of("IN_PROGRESS"), IncidentStates.of("OPEN").allowedNext());
        assertEquals(Set.of("RESOLVED"), IncidentStates.of("IN_PROGRESS").allowedNext());
        assertEquals(Set.of("CLOSED", "OPEN"), IncidentStates.of("RESOLVED").allowedNext());
        assertEquals(Set.of("OPEN"), IncidentStates.of("CLOSED").allowedNext());
    }

    @Test
    void skippingStepsIsNotAllowed() {
        assertFalse(IncidentStates.of("OPEN").canMoveTo("CLOSED"));
        assertFalse(IncidentStates.of("OPEN").canMoveTo("RESOLVED"));
        assertFalse(IncidentStates.of("IN_PROGRESS").canMoveTo("CLOSED"));
        assertFalse(IncidentStates.of("CLOSED").canMoveTo("RESOLVED"));
    }

    @Test
    void statusTextFindsTheRightClass() {
        assertInstanceOf(InProgressState.class, IncidentStates.of(" in_progress "));
        assertThrows(IllegalArgumentException.class, () -> IncidentStates.of("VERIFIED"));
        assertThrows(IllegalArgumentException.class, () -> IncidentStates.of(null));
    }

    @Test
    void whatEachStateAllows() {
        assertTrue(IncidentStates.of("OPEN").canAcknowledge());
        assertFalse(IncidentStates.of("IN_PROGRESS").canAcknowledge());
        assertTrue(IncidentStates.of("RESOLVED").canEditDetails());
        assertFalse(IncidentStates.of("CLOSED").canEditDetails());
        assertTrue(IncidentStates.of("RESOLVED").needsResolutionNotes());
        assertTrue(IncidentStates.of("CLOSED").isFinished());
        assertFalse(IncidentStates.of("IN_PROGRESS").isFinished());
    }

    @Test
    void enteringAStateUpdatesTheDates() {
        Incident incident = new Incident();
        LocalDateTime now = LocalDateTime.now();

        new InProgressState().enter(incident, now);
        assertEquals(now, incident.getFirstResponseAt());
        assertEquals(now, incident.getAcknowledgedAt());

        incident.setResolvedAt(now);
        IncidentState closed = new ClosedState();
        closed.enter(incident, now.plusHours(1));
        assertEquals(now.plusHours(1), incident.getClosedAt());
        assertEquals(now, incident.getResolvedAt(), "closing keeps the resolved time");

        incident.setResolutionNotes("Restarted the service");
        new OpenState().enter(incident, now.plusHours(2));
        assertNull(incident.getResolvedAt());
        assertNull(incident.getClosedAt());
        assertNull(incident.getResolutionNotes());
    }

    // ---- The same rules through IncidentService ----

    @Mock private IncidentRepository incidentRepository;
    @Mock private AuditService auditService;
    @Mock private SLAService slaService;
    @Mock private NotificationService notificationService;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private UserRepository userRepository;
    private IncidentService service;
    private Technician ravi;
    private Incident incident;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new IncidentService(incidentRepository, new PriorityService(), auditService, slaService,
                notificationService, technicianRepository, userRepository);
        ravi = new Technician(1L, "Ravi Kumar", Set.of("HARDWARE"), 1);
        ravi.setUsername("technician");
        when(technicianRepository.findByUsername("technician")).thenReturn(Optional.of(ravi));
        incident = new Incident();
        incident.setId(3L); incident.setTitle("Laptop will not start"); incident.setStatus("OPEN"); incident.setAssignedTechnician(ravi);
        when(incidentRepository.findById(3L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void fullLifecycleSetsDatesAndWorkload() {
        loginAs("technician", "TECHNICIAN");
        service.updateStatus(3L, "IN_PROGRESS");
        service.updateStatus(3L, "RESOLVED", "Replaced the charger");
        assertNotNull(incident.getResolvedAt());
        assertEquals(0, ravi.getActiveIncidentCount(), "a resolved incident no longer counts as work");

        loginAs("admin", "ADMINISTRATOR");
        service.updateStatus(3L, "CLOSED");
        assertNotNull(incident.getClosedAt(), "closing records the closed time");
        assertEquals(0, ravi.getActiveIncidentCount(), "closing a resolved incident does not change the workload again");

        service.updateStatus(3L, "OPEN");
        assertNull(incident.getClosedAt());
        assertNull(incident.getResolutionNotes());
        assertEquals(1, ravi.getActiveIncidentCount(), "a reopened incident counts again");
    }

    @Test
    void openToClosedIsRefusedWithAClearMessage() {
        loginAs("admin", "ADMINISTRATOR");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.updateStatus(3L, "CLOSED"));
        assertEquals("Invalid status transition: OPEN -> CLOSED", e.getMessage());
    }

    @Test
    void closedIncidentCannotBeEdited() {
        loginAs("admin", "ADMINISTRATOR");
        incident.setStatus("CLOSED");
        assertThrows(IllegalStateException.class, () -> service.updateIncident(3L, "t", "d", "HARDWARE", "LOW", "LOW", "LOW", null));
    }
}
