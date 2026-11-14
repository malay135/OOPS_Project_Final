package i_itsm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import i_itsm.model.Incident;
import i_itsm.model.User;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

@Query("""
    SELECT i FROM Incident i LEFT JOIN i.assignedTechnician t
    WHERE (:incidentId IS NULL OR i.id = :incidentId)
    AND (:priority IS NULL OR i.priority = :priority)
    AND (:status IS NULL OR i.status = :status)
    AND (:category IS NULL OR i.category = :category)
    AND (:technicianId IS NULL OR
         t.id = :technicianId)
    AND (:title = '' OR
         LOWER(i.title) LIKE LOWER(CONCAT('%', :title, '%')))
    """)
List<Incident> searchIncidents(
        @Param("incidentId") Long incidentId,
        @Param("priority") String priority,
        @Param("status") String status,
        @Param("category") String category,
        @Param("technicianId") Long technicianId,
        @Param("title") String title
);

List<Incident> findByReportedByOrderByCreatedAtDesc(User reportedBy);

// Open incidents whose deadline has passed and that are not flagged yet
@Query("SELECT i FROM Incident i WHERE i.resolvedAt IS NULL AND i.slaViolated = false AND i.slaDeadline < :now")
List<Incident> findNewlyOverdue(@Param("now") LocalDateTime now);

// Sets only the flag (never overwrites a status change made at the same moment); returns 1 if it was set
@Modifying
@Query("UPDATE Incident i SET i.slaViolated = true WHERE i.id = :id AND i.slaViolated = false AND i.resolvedAt IS NULL")
int markSlaViolated(@Param("id") Long id);

}
