package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import i_itsm.model.Incident;
import i_itsm.model.SLAPolicy;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.SLAPolicyRepository;
import i_itsm.service.SLAPolicyService;
import i_itsm.service.SLAService;

/** Admin SLA settings are checked, and the background SLA check survives a missing policy. */
class SlaPolicyValidationTest {

    @Mock private SLAPolicyRepository policyRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(policyRepository.findByPriority(any())).thenReturn(Optional.empty());
        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private SLAPolicy policy(int response, int resolution, int warning) {
        SLAPolicy p = new SLAPolicy();
        p.setResponseTimeMinutes(response);
        p.setResolutionTimeMinutes(resolution);
        p.setWarningTimeMinutes(warning);
        p.setActive(true);
        return p;
    }

    @Test
    void validPolicyIsSaved() {
        SLAPolicy saved = new SLAPolicyService(policyRepository).upsert("p1", policy(15, 240, 30));
        assertEquals("P1", saved.getPriority());
        assertEquals(240, saved.getResolutionTimeMinutes());
    }

    @Test
    void zeroOrNegativeTimesAreRejected() {
        SLAPolicyService service = new SLAPolicyService(policyRepository);
        assertThrows(IllegalArgumentException.class, () -> service.upsert("P1", policy(0, 240, 30)));
        assertThrows(IllegalArgumentException.class, () -> service.upsert("P1", policy(15, -5, 0)));
        verify(policyRepository, never()).save(any());
    }

    @Test
    void responseLongerThanResolutionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SLAPolicyService(policyRepository).upsert("P2", policy(500, 480, 60)));
    }

    @Test
    void warningMustBeBeforeTheDeadline() {
        assertThrows(IllegalArgumentException.class, () -> new SLAPolicyService(policyRepository).upsert("P3", policy(60, 1440, 1440)));
    }

    @Test
    void unknownPriorityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SLAPolicyService(policyRepository).upsert("P9", policy(60, 120, 30)));
    }

    @Test
    void backgroundCheckSkipsAnIncidentWithNoPolicyAndCarriesOn() {
        Incident noPolicy = new Incident();
        noPolicy.setId(1L); noPolicy.setTitle("No policy"); noPolicy.setPriority("P1");
        noPolicy.setCreatedAt(LocalDateTime.now().minusHours(2)); noPolicy.setSlaDeadline(LocalDateTime.now().plusHours(2));

        Incident overdue = new Incident();
        overdue.setId(2L); overdue.setTitle("Overdue"); overdue.setPriority("P2");
        overdue.setCreatedAt(LocalDateTime.now().minusHours(10)); overdue.setSlaDeadline(LocalDateTime.now().minusHours(1));

        when(incidentRepository.findAll()).thenReturn(List.of(noPolicy, overdue));
        when(policyRepository.findByPriorityAndActiveTrue("P1")).thenReturn(Optional.empty()); // P1 policy switched off
        when(policyRepository.findByPriorityAndActiveTrue("P2")).thenReturn(Optional.of(policy(30, 480, 60)));

        new SLAService(incidentRepository, notificationService, policyRepository).flagOverdueIncidents();

        assertTrue(overdue.isSlaViolated(), "the second incident is still checked and flagged");
        verify(notificationService).sendNotification(2L, "SLA breached for incident 'Overdue' (Priority: P2)", "IN_APP");
    }
}
