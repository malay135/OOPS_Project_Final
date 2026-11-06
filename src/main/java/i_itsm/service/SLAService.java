
package i_itsm.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import i_itsm.model.Incident;
import i_itsm.model.SLAPolicy;
import i_itsm.repository.IncidentRepository;
import i_itsm.notification.NotificationService;

@Service
public class SLAService {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    public SLAService(
            IncidentRepository incidentRepository,
            NotificationService notificationService) {

        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
    }

    private final Map<String, SLAPolicy> policies = Map.of(
            "P1", new SLAPolicy("P1", 4),
            "P2", new SLAPolicy("P2", 8),
            "P3", new SLAPolicy("P3", 24),
            "P4", new SLAPolicy("P4", 72)
    );

    public void calculateDeadline(Incident incident) {

        SLAPolicy policy = policies.get(incident.getPriority());

        if (policy == null) {
            throw new IllegalArgumentException(
                    "No SLA policy for priority: "
                            + incident.getPriority());
        }

        incident.setSlaDeadline(
                LocalDateTime.now().plusHours(
                        policy.getResolutionTimeHours()));

        incident.setSlaViolated(false);
    }

    public boolean checkViolation(Incident incident) {

        boolean previouslyViolated = incident.isSlaViolated();

        boolean violated =
                incident.getResolvedAt() == null
                && incident.getSlaDeadline() != null
                && LocalDateTime.now().isAfter(
                        incident.getSlaDeadline());

        incident.setSlaViolated(violated);

        if (violated) {

            incidentRepository.save(incident);

            if (!previouslyViolated) {

                String message =
                        "SLA breached for incident '"
                        + incident.getTitle()
                        + "' (Priority: "
                        + incident.getPriority() + ")";

                notificationService.sendNotification(
                        incident.getId(),
                        message,
                        "IN_APP");

                notificationService.sendNotification(
                        incident.getId(),
                        message,
                        "EMAIL");
            }
        }

        return violated;
    }
}
