package i_itsm.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "incident_feedback", uniqueConstraints = @UniqueConstraint(columnNames = {"incident_id", "user_id"}))
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Min(1) @Max(5)
    private int rating;

    @Size(max = 2000)
    @Column(length = 2000)
    private String comment;

    private LocalDateTime createdAt;

    public Feedback() { this.createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    @JsonIgnore
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    @JsonIgnore
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
