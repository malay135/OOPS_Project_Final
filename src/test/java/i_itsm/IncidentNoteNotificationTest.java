package i_itsm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import i_itsm.model.Incident;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentNoteRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.AuditService;
import i_itsm.service.IncidentNoteService;

/** Support requests and replies notify the other side, and employees can only add their own kinds of notes. */
class IncidentNoteNotificationTest {

    @Mock private IncidentNoteRepository noteRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private IncidentNoteService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new IncidentNoteService(noteRepository, incidentRepository, userRepository, auditService, notificationService);

        User employee = new User("riya", "x", "EMPLOYEE");
        ReflectionTestUtils.setField(employee, "id", 7L);
        User techLogin = new User("arjun", "x", "TECHNICIAN");
        ReflectionTestUtils.setField(techLogin, "id", 8L);
        Technician arjun = new Technician();
        arjun.setId(3L); arjun.setName("Arjun Mehta"); arjun.setUsername("arjun");

        Incident incident = new Incident();
        incident.setId(12L); incident.setTitle("Payment app down"); incident.setStatus("IN_PROGRESS");
        incident.setReportedBy(employee); incident.setAssignedTechnician(arjun);

        when(incidentRepository.findById(12L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("riya")).thenReturn(Optional.of(employee));
        when(userRepository.findByUsername("arjun")).thenReturn(Optional.of(techLogin));
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    @Test
    void technicianQuestionNotifiesTheEmployee() {
        loginAs("arjun");
        service.add(12L, "SUPPORT_REQUEST", "Which browser are you using?");
        verify(notificationService).sendNotificationTo(eq(12L), anyString(), eq("IN_APP"), eq("riya"));
        verify(notificationService).sendNotification(eq(12L), anyString(), eq("EMAIL"));
    }

    @Test
    void employeeReplyNotifiesTheTechnician() {
        loginAs("riya");
        service.add(12L, "RESPONSE", "Chrome on Windows.");
        verify(notificationService).sendNotificationTo(eq(12L), anyString(), eq("IN_APP"), eq("arjun"));
    }

    @Test
    void investigationNoteSendsNoNotification() {
        loginAs("arjun");
        service.add(12L, "INVESTIGATION", "Checked the payment service logs.");
        verify(notificationService, never()).sendNotificationTo(any(), anyString(), anyString(), anyString());
    }

    @Test
    void employeeCannotAddAnInvestigationNote() {
        loginAs("riya");
        assertThrows(AccessDeniedException.class, () -> service.add(12L, "INVESTIGATION", "I fixed it myself"));
        verify(noteRepository, never()).save(any());
    }
}
