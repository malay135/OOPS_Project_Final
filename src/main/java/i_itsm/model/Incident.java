package i_itsm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.time.LocalDateTime;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Incident {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    @NotBlank(message = "Category is required")
    private String category;
    private String priority;
    private String status;
    @NotBlank(message = "Impact is required")
    private String impact;
    @NotBlank(message = "Urgency is required")
    private String urgency;
    @NotBlank(message = "Business criticality is required")
    private String businessCriticality;
    // How many people are affected. Used in the priority calculation (more people affected = higher priority).
    @Min(value = 1, message = "Number of affected users must be at least 1")
    @Max(value = 100000, message = "Number of affected users must be at most 100000")
    private Integer affectedUsers;
    // Which SLA was used for the deadlines, e.g. "Standard P2" or "Payment Gateway P1 (service SLA)"
    @Column(length = 150)
    private String appliedSla;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime slaDeadline;
    private LocalDateTime responseDeadline;
    private LocalDateTime firstResponseAt;
    private LocalDateTime acknowledgedAt; // when the assigned technician acknowledged the incident
    // New columns on an existing table need a default, or PostgreSQL refuses to add them when incidents already exist
    @Column(columnDefinition = "boolean default false not null")
    private boolean responseSlaViolated;
    private boolean slaViolated;
    @Column(columnDefinition = "boolean default false not null")
    private boolean slaWarningSent;
    private String slaRiskLevel;
    @Column(columnDefinition = "boolean default false not null")
    private boolean potentialDuplicate;
    private Long duplicateOfIncidentId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_by")
    private User reportedBy;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "affected_service_id")
    private ITService affectedService;
    @Size(max = 2000, message = "Resolution notes must not exceed 2000 characters")
    @Column(length = 2000)
    private String resolutionNotes;

    public Incident() {
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getBusinessCriticality() {
        return businessCriticality;
    }

    public void setBusinessCriticality(String businessCriticality) {
        this.businessCriticality = businessCriticality;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    

    public LocalDateTime getSlaDeadline() {
    	return slaDeadline;
    }

    public void setSlaDeadline(LocalDateTime slaDeadline) {
    	this.slaDeadline = slaDeadline;
    }

    public boolean isSlaViolated() {
    	return slaViolated;
    }

    public void setSlaViolated(boolean slaViolated) {
    	this.slaViolated = slaViolated;
    }

    public LocalDateTime getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(LocalDateTime responseDeadline) { this.responseDeadline = responseDeadline; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public LocalDateTime getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(LocalDateTime firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    public boolean isResponseSlaViolated() { return responseSlaViolated; }
    public void setResponseSlaViolated(boolean responseSlaViolated) { this.responseSlaViolated = responseSlaViolated; }
    public boolean isSlaWarningSent() { return slaWarningSent; }
    public void setSlaWarningSent(boolean slaWarningSent) { this.slaWarningSent = slaWarningSent; }
    public String getSlaRiskLevel() { return slaRiskLevel; }
    public void setSlaRiskLevel(String slaRiskLevel) { this.slaRiskLevel = slaRiskLevel; }
    public boolean isPotentialDuplicate() { return potentialDuplicate; }
    public void setPotentialDuplicate(boolean potentialDuplicate) { this.potentialDuplicate = potentialDuplicate; }
    public Long getDuplicateOfIncidentId() { return duplicateOfIncidentId; }
    public void setDuplicateOfIncidentId(Long duplicateOfIncidentId) { this.duplicateOfIncidentId = duplicateOfIncidentId; }

    public Technician getAssignedTechnician() {
    return assignedTechnician;
    }

    public void setAssignedTechnician(Technician assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public ITService getAffectedService() { return affectedService; }
    public void setAffectedService(ITService affectedService) { this.affectedService = affectedService; }

    public User getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(User reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Integer getAffectedUsers() { return affectedUsers; }
    public void setAffectedUsers(Integer affectedUsers) { this.affectedUsers = affectedUsers; }
    public String getAppliedSla() { return appliedSla; }
    public void setAppliedSla(String appliedSla) { this.appliedSla = appliedSla; }
}
