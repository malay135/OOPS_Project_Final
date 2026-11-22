package i_itsm.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import i_itsm.model.Incident;
import i_itsm.model.SLAPolicy;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.SLAPolicyRepository;
import i_itsm.repository.ServiceSlaPolicyRepository;

@Service
public class SLAService {
    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final SLAPolicyRepository policyRepository;
    private final ServiceSlaPolicyRepository servicePolicyRepository; // service-specific SLAs (null in some unit tests)

    // Compatibility constructor for existing unit tests.
    public SLAService(IncidentRepository incidentRepository, NotificationService notificationService) {
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
        this.policyRepository = null;
        this.servicePolicyRepository = null;
    }

    public SLAService(IncidentRepository incidentRepository, NotificationService notificationService, SLAPolicyRepository policyRepository) {
        this(incidentRepository, notificationService, policyRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SLAService(IncidentRepository incidentRepository, NotificationService notificationService, SLAPolicyRepository policyRepository,
                      ServiceSlaPolicyRepository servicePolicyRepository) {
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
        this.policyRepository = policyRepository;
        this.servicePolicyRepository = servicePolicyRepository;
    }

    /** The service's own SLA for this priority, if the admin set one; otherwise null. */
    private SLAPolicy servicePolicyFor(Incident incident) {
        if (servicePolicyRepository == null || incident.getAffectedService() == null || incident.getAffectedService().getId() == null) return null;
        return servicePolicyRepository.findByServiceIdAndPriority(incident.getAffectedService().getId(), incident.getPriority())
                .map(s -> s.toPolicy()).orElse(null);
    }

    /** The SLA that applies to this incident: its service's SLA first, then the standard policy for its priority. */
    private SLAPolicy policyFor(Incident incident) {
        SLAPolicy servicePolicy = servicePolicyFor(incident);
        return servicePolicy != null ? servicePolicy : policyFor(incident.getPriority());
    }

    /** Text shown on the incident page, for example "Payment Gateway P1 (service SLA)" or "Standard P3". */
    private String describe(Incident incident, boolean serviceSla) {
        return serviceSla ? incident.getAffectedService().getName() + " " + incident.getPriority() + " (service SLA)"
                : "Standard " + incident.getPriority();
    }

    private SLAPolicy policyFor(String priority) {
        if (policyRepository != null) return policyRepository.findByPriorityAndActiveTrue(priority).orElseThrow(() -> new IllegalArgumentException("No active SLA policy for priority: " + priority));
        return switch (priority) {
            case "P1" -> new SLAPolicy("P1", 4);
            case "P2" -> new SLAPolicy("P2", 8);
            case "P3" -> new SLAPolicy("P3", 24);
            case "P4" -> new SLAPolicy("P4", 72);
            default -> throw new IllegalArgumentException("No SLA policy for priority: " + priority);
        };
    }

    public void calculateDeadline(Incident incident) {
        SLAPolicy servicePolicy = servicePolicyFor(incident);
        SLAPolicy policy = servicePolicy != null ? servicePolicy : policyFor(incident.getPriority());
        incident.setAppliedSla(describe(incident, servicePolicy != null));
        LocalDateTime now = LocalDateTime.now();
        incident.setResponseDeadline(now.plusMinutes(policy.getResponseTimeMinutes()));
        incident.setSlaDeadline(now.plusMinutes(policy.getResolutionTimeMinutes()));
        incident.setResponseSlaViolated(false);
        incident.setSlaViolated(false);
        incident.setSlaWarningSent(false);
        incident.setSlaRiskLevel("LOW");
    }

    public boolean checkViolation(Incident incident) {
        boolean previouslyViolated = incident.isSlaViolated();
        boolean violated = incident.getResolvedAt() == null && incident.getSlaDeadline() != null && LocalDateTime.now().isAfter(incident.getSlaDeadline());
        incident.setSlaViolated(violated);
        checkResponseViolation(incident);
        updateRisk(incident);
        if (violated) {
            incidentRepository.save(incident);
            if (!previouslyViolated) {
                String message = "SLA breached for incident '" + incident.getTitle() + "' (Priority: " + incident.getPriority() + ")";
                notificationService.sendNotification(incident.getId(), message, "IN_APP");
                notificationService.sendNotification(incident.getId(), message, "EMAIL");
            }
        }
        return violated;
    }

    private void checkResponseViolation(Incident incident) {
        boolean violated = incident.getFirstResponseAt() == null && incident.getResponseDeadline() != null && LocalDateTime.now().isAfter(incident.getResponseDeadline()) && incident.getResolvedAt() == null;
        incident.setResponseSlaViolated(violated);
    }

    private void updateRisk(Incident incident) {
        if (incident.getResolvedAt() != null || incident.getSlaDeadline() == null) { incident.setSlaRiskLevel("LOW"); return; }
        long total = Math.max(1, java.time.Duration.between(incident.getCreatedAt(), incident.getSlaDeadline()).toMinutes());
        long elapsed = Math.max(0, java.time.Duration.between(incident.getCreatedAt(), LocalDateTime.now()).toMinutes());
        double pct = (double) elapsed / total;
        incident.setSlaRiskLevel(pct >= 0.75 ? "HIGH" : pct >= 0.50 ? "MEDIUM" : "LOW");
    }

    public boolean isViolated(Incident incident) {
        boolean overdueNow = incident.getResolvedAt() == null && incident.getSlaDeadline() != null && LocalDateTime.now().isAfter(incident.getSlaDeadline());
        return incident.isSlaViolated() || overdueNow;
    }

    // First run 30 s after start-up, so the default SLA policies are saved before the first check
    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    @Transactional
    public void flagOverdueIncidents() {
        LocalDateTime now = LocalDateTime.now();
        List<Incident> active = incidentRepository.findAll().stream().filter(i -> i.getResolvedAt() == null && i.getSlaDeadline() != null).toList();
        for (Incident incident : active) {
          // One bad incident (for example a priority whose SLA policy was switched off) must not stop the check for all the others
          try {
            checkResponseViolation(incident);
            updateRisk(incident);
            maybeWarn(incident, now);
            if (incident.getSlaDeadline() != null && now.isAfter(incident.getSlaDeadline()) && !incident.isSlaViolated()) {
                incident.setSlaViolated(true);
                String message = "SLA breached for incident '" + incident.getTitle() + "' (Priority: " + incident.getPriority() + ")";
                incidentRepository.save(incident);
                notificationService.sendNotification(incident.getId(), message, "IN_APP");
                notificationService.sendNotification(incident.getId(), message, "EMAIL");
            } else {
                incidentRepository.save(incident);
            }
          } catch (IllegalArgumentException e) {
            System.out.println("SLA check skipped for incident #" + incident.getId() + ": " + e.getMessage());
          }
        }
    }

    private void maybeWarn(Incident incident, LocalDateTime now) {
        int warningMinutes = policyRepository == null ? Math.max(15, policyFor(incident.getPriority()).getWarningTimeMinutes()) : policyFor(incident).getWarningTimeMinutes();
        if (!incident.isSlaWarningSent() && incident.getSlaDeadline() != null && now.isBefore(incident.getSlaDeadline()) && !now.plusMinutes(warningMinutes).isBefore(incident.getSlaDeadline())) {
            String message = "SLA deadline approaching for incident '" + incident.getTitle() + "' (Priority: " + incident.getPriority() + ")";
            notificationService.sendNotification(incident.getId(), message, "IN_APP");
            notificationService.sendNotification(incident.getId(), message, "EMAIL");
            incident.setSlaWarningSent(true);
        }
    }
}
