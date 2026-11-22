package i_itsm.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * An SLA for one IT service and one priority, for example "Payment Gateway, P1: respond in 5 min, fix in 30 min".
 * It replaces the standard SLA policy (SLAPolicy) for incidents that affect this service.
 * Services without one use the standard policies.
 */
@Entity
@Table(name = "service_sla_policies", uniqueConstraints = @UniqueConstraint(columnNames = { "service_id", "priority" }))
public class ServiceSlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ITService service;

    private String priority;
    private int responseTimeMinutes;
    private int resolutionTimeMinutes;
    private int warningTimeMinutes;

    public ServiceSlaPolicy() {
    }

    public ServiceSlaPolicy(ITService service, String priority) {
        this.service = service;
        this.priority = priority;
    }

    /** The same times as a (not saved) SLAPolicy, so the SLA calculation can treat both kinds alike. */
    public SLAPolicy toPolicy() {
        SLAPolicy policy = new SLAPolicy();
        policy.setPriority(priority);
        policy.setResponseTimeMinutes(responseTimeMinutes);
        policy.setResolutionTimeMinutes(resolutionTimeMinutes);
        policy.setWarningTimeMinutes(warningTimeMinutes);
        policy.setActive(true);
        return policy;
    }

    public Long getId() { return id; }
    public ITService getService() { return service; }
    public void setService(ITService service) { this.service = service; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public int getResponseTimeMinutes() { return responseTimeMinutes; }
    public void setResponseTimeMinutes(int v) { responseTimeMinutes = v; }
    public int getResolutionTimeMinutes() { return resolutionTimeMinutes; }
    public void setResolutionTimeMinutes(int v) { resolutionTimeMinutes = v; }
    public int getWarningTimeMinutes() { return warningTimeMinutes; }
    public void setWarningTimeMinutes(int v) { warningTimeMinutes = v; }
}
