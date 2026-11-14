package i_itsm.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "technicians")
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "technician_skills",
            joinColumns = @JoinColumn(name = "technician_id")
    )
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>();
    private int activeIncidentCount;
    private boolean active = true;

    // The login (users.username) this technician signs in with; null = not linked yet
    @Column(unique = true)
    private String username;

    // Only read from a request when an admin creates or resets the login; never stored here, never sent back
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    public Technician() {
    }

    public Technician(Long id, String name, Set<String> skills,
                      int activeIncidentCount) {
        this.id = id;
        this.name = name;
        this.skills = skills;
        this.activeIncidentCount = activeIncidentCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills;
    }

    public int getActiveIncidentCount() {
        return activeIncidentCount;
    }

    public void setActiveIncidentCount(int activeIncidentCount) {
        this.activeIncidentCount = activeIncidentCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
    this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
