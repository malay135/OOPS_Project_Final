package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
import i_itsm.service.AssignmentService;
import i_itsm.service.AuditService;
import i_itsm.strategy.LeastLoadedAssignment;
import i_itsm.strategy.SkillBasedAssignment;

/** Delegation needs admin approval: a request changes nothing until the admin approves it. */
class DelegationApprovalTest {

    @Mock private SkillBasedAssignment skill;
    @Mock private LeastLoadedAssignment leastLoaded;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private UserRepository userRepository;
    @Mock private DelegationRequestRepository delegationRepository;

    private AssignmentService service;
    private Technician asha;
    private Technician ravi;
    private Incident incident;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AssignmentService(skill, leastLoaded, technicianRepository, incidentRepository,
                notificationService, auditService, userRepository, delegationRepository);
        when(delegationRepository.save(any(DelegationRequest.class))).thenAnswer(c -> c.getArgument(0));

        asha = new Technician(1L, "Asha Rao", Set.of("NETWORK"), 2);
        asha.setUsername("asha");
        ravi = new Technician(2L, "Ravi Kumar", Set.of("HARDWARE"), 0);
        ravi.setUsername("ravi");

        incident = new Incident();
        incident.setId(5L);
        incident.setTitle("Wi-Fi down");
        incident.setStatus("OPEN");
        incident.setAssignedTechnician(asha);

        when(incidentRepository.findById(5L)).thenReturn(Optional.of(incident));
        when(technicianRepository.findByUsername("asha")).thenReturn(Optional.of(asha));
        when(technicianRepository.findById(2L)).thenReturn(Optional.of(ravi));
        when(userRepository.findByRole("ADMINISTRATOR")).thenReturn(List.of(new User("admin", "x", "ADMINISTRATOR")));
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private DelegationRequest pendingRequest() {
        DelegationRequest request = new DelegationRequest(5L, "Wi-Fi down", asha, ravi, "asha");
        request.setId(9L);
        when(delegationRepository.findById(9L)).thenReturn(Optional.of(request));
        return request;
    }

    @Test
    void requestingDelegationDoesNotMoveTheIncidentAndNotifiesAdmin() {
        loginAs("asha", "TECHNICIAN");

        DelegationRequest request = service.requestDelegation(5L, 2L);

        assertEquals("PENDING", request.getStatus());
        assertSame(asha, incident.getAssignedTechnician());
        assertEquals(2, asha.getActiveIncidentCount());
        assertEquals(0, ravi.getActiveIncidentCount());
        verify(notificationService).sendNotificationTo(eq(5L), anyString(), eq("IN_APP"), eq("admin"));
    }

    @Test
    void secondRequestWhileOnePendingIsRefused() {
        loginAs("asha", "TECHNICIAN");
        when(delegationRepository.existsByIncidentIdAndStatus(5L, "PENDING")).thenReturn(true);

        assertThrows(IncidentAssignmentException.class, () -> service.requestDelegation(5L, 2L));
    }

    @Test
    void approvingMovesTheIncidentToTheTarget() {
        DelegationRequest request = pendingRequest();
        loginAs("admin", "ADMINISTRATOR");

        service.approveDelegation(9L);

        assertEquals("APPROVED", request.getStatus());
        assertSame(ravi, incident.getAssignedTechnician());
        assertEquals(1, asha.getActiveIncidentCount());
        assertEquals(1, ravi.getActiveIncidentCount());
    }

    @Test
    void rejectingLeavesTheIncidentWithTheRequester() {
        DelegationRequest request = pendingRequest();
        loginAs("admin", "ADMINISTRATOR");

        service.rejectDelegation(9L);

        assertEquals("REJECTED", request.getStatus());
        assertSame(asha, incident.getAssignedTechnician());
        assertEquals(0, ravi.getActiveIncidentCount());
    }

    @Test
    void alreadyDecidedRequestCannotBeApprovedAgain() {
        DelegationRequest request = pendingRequest();
        request.setStatus("REJECTED");
        loginAs("admin", "ADMINISTRATOR");

        assertThrows(IncidentAssignmentException.class, () -> service.approveDelegation(9L));
    }

    @Test
    void approvingNotifiesTheEmployeeWhoReportedTheIncident() {
        incident.setReportedBy(new User("employee", "x", "EMPLOYEE"));
        pendingRequest();
        loginAs("admin", "ADMINISTRATOR");

        service.approveDelegation(9L);

        verify(notificationService).sendNotificationTo(eq(5L), anyString(), eq("IN_APP"), eq("employee"));
    }

    @Test
    void adminOverrideNotifiesTheEmployee() {
        incident.setReportedBy(new User("employee", "x", "EMPLOYEE"));
        ravi.setActive(true);
        loginAs("admin", "ADMINISTRATOR");

        service.overrideAssignment(5L, 2L);

        verify(notificationService).sendNotificationTo(eq(5L), anyString(), eq("IN_APP"), eq("employee"));
        assertSame(ravi, incident.getAssignedTechnician());
    }

    @Test
    void firstAssignmentNotifiesTheEmployeeOnce() {
        incident.setReportedBy(new User("employee", "x", "EMPLOYEE"));
        incident.setAssignedTechnician(null);
        ravi.setActive(true);
        loginAs("admin", "ADMINISTRATOR");

        service.overrideAssignment(5L, 2L);

        verify(notificationService, org.mockito.Mockito.times(1))
                .sendNotificationTo(eq(5L), anyString(), eq("IN_APP"), eq("employee"));
    }
}
