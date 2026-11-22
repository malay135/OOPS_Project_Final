package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import i_itsm.exception.InvalidIncidentException;
import i_itsm.model.ITService;
import i_itsm.model.Incident;
import i_itsm.model.SLAPolicy;
import i_itsm.model.ServiceSlaPolicy;
import i_itsm.notification.NotificationService;
import i_itsm.repository.ITServiceRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.SLAPolicyRepository;
import i_itsm.repository.ServiceSlaPolicyRepository;
import i_itsm.service.PriorityService;
import i_itsm.service.SLAPolicyService;
import i_itsm.service.SLAService;

/** More affected users raise the priority; a service's own SLA replaces the standard one. */
class AffectedUsersAndServiceSlaTest {

    private final PriorityService priority = new PriorityService();

    @Test
    void fewUsersDoNotChangeThePriority() {
        assertEquals("P3", priority.calculatePriority("MEDIUM", "MEDIUM", "LOW", 1));
        assertEquals("P3", priority.calculatePriority("MEDIUM", "MEDIUM", "LOW", 9));
        assertEquals("P3", priority.calculatePriority("MEDIUM", "MEDIUM", "LOW", null), "unknown counts as one person");
    }

    @Test
    void manyUsersRaiseThePriority() {
        assertEquals("P2", priority.calculatePriority("MEDIUM", "MEDIUM", "LOW", 10));
        assertEquals("P1", priority.calculatePriority("MEDIUM", "MEDIUM", "LOW", 100));
        assertEquals("P2", priority.calculatePriority("LOW", "LOW", "LOW", 250));
    }

    @Test
    void priorityNeverGoesAboveP1() {
        assertEquals("P1", priority.calculatePriority("HIGH", "HIGH", "HIGH", 5000));
    }

    @Test
    void zeroUsersIsRefused() {
        assertThrows(InvalidIncidentException.class, () -> priority.calculatePriority("LOW", "LOW", "LOW", 0));
    }

    // ---- SLA per service ----

    @Mock private IncidentRepository incidentRepository;
    @Mock private NotificationService notificationService;
    @Mock private SLAPolicyRepository policyRepository;
    @Mock private ServiceSlaPolicyRepository servicePolicyRepository;
    @Mock private ITServiceRepository itServiceRepository;
    private SLAService slaService;
    private ITService payments;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        slaService = new SLAService(incidentRepository, notificationService, policyRepository, servicePolicyRepository);
        payments = new ITService("Payment Gateway", "Card payments");
        ReflectionTestUtils.setField(payments, "id", 2L);

        SLAPolicy standardP1 = new SLAPolicy();
        standardP1.setPriority("P1"); standardP1.setResponseTimeMinutes(60); standardP1.setResolutionTimeMinutes(240); standardP1.setWarningTimeMinutes(30);
        when(policyRepository.findByPriorityAndActiveTrue("P1")).thenReturn(Optional.of(standardP1));

        ServiceSlaPolicy fast = new ServiceSlaPolicy(payments, "P1");
        fast.setResponseTimeMinutes(5); fast.setResolutionTimeMinutes(30); fast.setWarningTimeMinutes(10);
        when(servicePolicyRepository.findByServiceIdAndPriority(2L, "P1")).thenReturn(Optional.of(fast));
        when(servicePolicyRepository.findByServiceIdAndPriority(3L, "P1")).thenReturn(Optional.empty());
    }

    private Incident p1(ITService service) {
        Incident i = new Incident();
        i.setPriority("P1"); i.setAffectedService(service); i.setCreatedAt(LocalDateTime.now());
        return i;
    }

    private static long minutesFromNow(LocalDateTime t) {
        return Math.round(Duration.between(LocalDateTime.now(), t).toSeconds() / 60.0);
    }

    @Test
    void serviceSlaIsUsedForThatService() {
        Incident incident = p1(payments);
        slaService.calculateDeadline(incident);
        assertEquals(5, minutesFromNow(incident.getResponseDeadline()));
        assertEquals(30, minutesFromNow(incident.getSlaDeadline()));
        assertEquals("Payment Gateway P1 (service SLA)", incident.getAppliedSla());
    }

    @Test
    void otherServicesUseTheStandardSla() {
        ITService email = new ITService("Email", "Mail");
        ReflectionTestUtils.setField(email, "id", 3L);
        Incident incident = p1(email);
        slaService.calculateDeadline(incident);
        assertEquals(240, minutesFromNow(incident.getSlaDeadline()));
        assertEquals("Standard P1", incident.getAppliedSla());

        Incident noService = p1(null);
        slaService.calculateDeadline(noService);
        assertEquals("Standard P1", noService.getAppliedSla());
    }

    private SLAPolicy times(int response, int resolution, int warning) {
        SLAPolicy t = new SLAPolicy();
        t.setResponseTimeMinutes(response); t.setResolutionTimeMinutes(resolution); t.setWarningTimeMinutes(warning);
        return t;
    }

    @Test
    void serviceSlaIsSavedAfterChecks() {
        SLAPolicyService admin = new SLAPolicyService(policyRepository, servicePolicyRepository, itServiceRepository);
        when(itServiceRepository.findById(2L)).thenReturn(Optional.of(payments));
        when(servicePolicyRepository.findByServiceIdAndPriority(2L, "P2")).thenReturn(Optional.empty());
        when(servicePolicyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceSlaPolicy saved = admin.upsertServicePolicy(2L, "p2", times(15, 120, 30));
        assertEquals("P2", saved.getPriority());
        assertEquals("Payment Gateway", saved.getService().getName());
        assertEquals(120, saved.getResolutionTimeMinutes());
    }

    @Test
    void badServiceSlaIsRefused() {
        SLAPolicyService admin = new SLAPolicyService(policyRepository, servicePolicyRepository, itServiceRepository);
        when(itServiceRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> admin.upsertServicePolicy(2L, "P2", times(200, 120, 30)));
        assertThrows(IllegalArgumentException.class, () -> admin.upsertServicePolicy(2L, "P7", times(5, 30, 10)));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> admin.upsertServicePolicy(9L, "P1", times(5, 30, 10)));
        assertTrue(e.getMessage().contains("not found"));
        verify(servicePolicyRepository, never()).save(any());
    }
}
