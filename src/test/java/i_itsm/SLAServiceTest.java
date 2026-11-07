package i_itsm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import i_itsm.model.Incident;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;
import i_itsm.service.SLAService;

class SLAServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NotificationService notificationService;

    private SLAService slaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        slaService = new SLAService(
                incidentRepository,
                notificationService
        );
    }

    @Test
    void p1IncidentShouldGetFourHourSla() {

        Incident incident = new Incident();
        incident.setPriority("P1");

        LocalDateTime before = LocalDateTime.now();

        slaService.calculateDeadline(incident);

        LocalDateTime after = LocalDateTime.now();

        assertTrue(
                incident.getSlaDeadline().isAfter(before.plusHours(3))
        );

        assertTrue(
                incident.getSlaDeadline().isBefore(after.plusHours(5))
        );

        assertFalse(incident.isSlaViolated());
    }

    @Test
    void p2IncidentShouldGetEightHourSla() {

        Incident incident = new Incident();
        incident.setPriority("P2");

        LocalDateTime before = LocalDateTime.now();

        slaService.calculateDeadline(incident);

        LocalDateTime after = LocalDateTime.now();

        assertTrue(
                incident.getSlaDeadline().isAfter(before.plusHours(7))
        );

        assertTrue(
                incident.getSlaDeadline().isBefore(after.plusHours(9))
        );
    }

    @Test
    void p3IncidentShouldGetTwentyFourHourSla() {

        Incident incident = new Incident();
        incident.setPriority("P3");

        LocalDateTime before = LocalDateTime.now();

        slaService.calculateDeadline(incident);

        LocalDateTime after = LocalDateTime.now();

        assertTrue(
                incident.getSlaDeadline().isAfter(before.plusHours(23))
        );

        assertTrue(
                incident.getSlaDeadline().isBefore(after.plusHours(25))
        );
    }

    @Test
    void p4IncidentShouldGetSeventyTwoHourSla() {

        Incident incident = new Incident();
        incident.setPriority("P4");

        LocalDateTime before = LocalDateTime.now();

        slaService.calculateDeadline(incident);

        LocalDateTime after = LocalDateTime.now();

        assertTrue(
                incident.getSlaDeadline().isAfter(before.plusHours(71))
        );

        assertTrue(
                incident.getSlaDeadline().isBefore(after.plusHours(73))
        );
    }

    @Test
    void invalidPriorityShouldThrowException() {

        Incident incident = new Incident();
        incident.setPriority("P5");

        assertThrows(
                IllegalArgumentException.class,
                () -> slaService.calculateDeadline(incident)
        );
    }

    @Test
    void expiredSlaShouldBeMarkedViolated() {

        Incident incident = new Incident();
        incident.setId(100L);
        incident.setTitle("Test Incident");
        incident.setPriority("P3");
        incident.setSlaDeadline(
                LocalDateTime.now().minusHours(1)
        );
        incident.setResolvedAt(null);
        incident.setSlaViolated(false);

        boolean result = slaService.checkViolation(incident);

        assertTrue(result);
        assertTrue(incident.isSlaViolated());

        verify(incidentRepository, times(1))
                .save(incident);
    }

    @Test
    void nonExpiredSlaShouldNotBeMarkedViolated() {

        Incident incident = new Incident();
        incident.setSlaDeadline(
                LocalDateTime.now().plusHours(1)
        );
        incident.setResolvedAt(null);
        incident.setSlaViolated(false);

        boolean result = slaService.checkViolation(incident);

        assertFalse(result);
        assertFalse(incident.isSlaViolated());

        verify(incidentRepository, never())
                .save(any(Incident.class));
    }

    @Test
    void resolvedIncidentShouldNotBeMarkedViolated() {

        Incident incident = new Incident();
        incident.setSlaDeadline(
                LocalDateTime.now().minusHours(1)
        );
        incident.setResolvedAt(
                LocalDateTime.now().minusMinutes(30)
        );
        incident.setSlaViolated(false);

        boolean result = slaService.checkViolation(incident);

        assertFalse(result);
        assertFalse(incident.isSlaViolated());

        verify(incidentRepository, never())
                .save(any(Incident.class));
    }

    @Test
    void firstSlaViolationShouldSendTwoNotifications() {

        Incident incident = new Incident();
        incident.setId(101L);
        incident.setTitle("Network Failure");
        incident.setPriority("P1");
        incident.setSlaDeadline(
                LocalDateTime.now().minusHours(1)
        );
        incident.setResolvedAt(null);
        incident.setSlaViolated(false);

        slaService.checkViolation(incident);

        verify(notificationService, times(1))
                .sendNotification(
                        eq(101L),
                        eq("SLA breached for incident 'Network Failure' (Priority: P1)"),
                        eq("IN_APP")
                );

        verify(notificationService, times(1))
                .sendNotification(
                        eq(101L),
                        eq("SLA breached for incident 'Network Failure' (Priority: P1)"),
                        eq("EMAIL")
                );
    }

    @Test
    void previouslyViolatedSlaShouldNotSendDuplicateNotifications() {

        Incident incident = new Incident();
        incident.setId(102L);
        incident.setTitle("Server Failure");
        incident.setPriority("P1");
        incident.setSlaDeadline(
                LocalDateTime.now().minusHours(1)
        );
        incident.setResolvedAt(null);
        incident.setSlaViolated(true);

        slaService.checkViolation(incident);

        verify(notificationService, never())
                .sendNotification(
                        eq(102L),
                        any(String.class),
                        eq("IN_APP")
                );

        verify(notificationService, never())
                .sendNotification(
                        eq(102L),
                        any(String.class),
                        eq("EMAIL")
                );
    }
}