package i_itsm.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A technician's request to hand an incident to another technician. Needs administrator approval. */
@Entity
@Table(name = "delegation_requests")
public class DelegationRequest {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long incidentId;
    private String incidentTitle;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_technician_id")
    private Technician fromTechnician;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_technician_id")
    private Technician toTechnician;
    private String status = PENDING;
    private String requestedBy;
    private LocalDateTime requestedAt = LocalDateTime.now();
    private String decidedBy;
    private LocalDateTime decidedAt;

    public DelegationRequest() {
    }

    public DelegationRequest(Long incidentId, String incidentTitle, Technician from, Technician to, String requestedBy) {
        this.incidentId = incidentId;
        this.incidentTitle = incidentTitle;
        this.fromTechnician = from;
        this.toTechnician = to;
        this.requestedBy = requestedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public String getIncidentTitle() { return incidentTitle; }
    public void setIncidentTitle(String incidentTitle) { this.incidentTitle = incidentTitle; }
    public Technician getFromTechnician() { return fromTechnician; }
    public void setFromTechnician(Technician fromTechnician) { this.fromTechnician = fromTechnician; }
    public Technician getToTechnician() { return toTechnician; }
    public void setToTechnician(Technician toTechnician) { this.toTechnician = toTechnician; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
