package i_itsm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "sla_policies")
public class SLAPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String priority;

    @Min(1)
    private int responseTimeMinutes;

    @Min(1)
    private int resolutionTimeMinutes;

    @Min(0)
    private int warningTimeMinutes;

    private boolean active = true;

    public SLAPolicy() {}
    // Only for code and tests. DISABLED stops the JSON reader from using it for request bodies: it would then expect
    // "resolutionTimeHours" and reject the Settings page's {responseTimeMinutes, resolutionTimeMinutes, ...} as "Bad Request".
    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    public SLAPolicy(String priority, int resolutionTimeHours) {
        this.priority = priority;
        this.responseTimeMinutes = Math.max(1, resolutionTimeHours * 60 / 4);
        this.resolutionTimeMinutes = resolutionTimeHours * 60;
        this.warningTimeMinutes = Math.max(15, this.resolutionTimeMinutes / 4);
    }
    public Long getId() { return id; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public int getResponseTimeMinutes() { return responseTimeMinutes; }
    public void setResponseTimeMinutes(int v) { responseTimeMinutes = v; }
    public int getResolutionTimeMinutes() { return resolutionTimeMinutes; }
    public void setResolutionTimeMinutes(int v) { resolutionTimeMinutes = v; }
    public int getWarningTimeMinutes() { return warningTimeMinutes; }
    public void setWarningTimeMinutes(int v) { warningTimeMinutes = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    // Kept for compatibility with existing code/tests.
    public int getResolutionTimeHours() { return resolutionTimeMinutes / 60; }
}
