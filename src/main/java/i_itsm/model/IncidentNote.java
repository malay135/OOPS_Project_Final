package i_itsm.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "incident_notes")
public class IncidentNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @NotBlank
    @Column(nullable = false)
    private String type;

    @NotBlank
    @Size(max = 3000)
    @Column(length = 3000, nullable = false)
    private String message;

    private LocalDateTime createdAt;

    public IncidentNote() { this.createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
