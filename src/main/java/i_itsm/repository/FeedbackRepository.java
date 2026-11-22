package i_itsm.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import i_itsm.model.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findByIncidentId(Long incidentId);
    Optional<Feedback> findByIncidentIdAndUserId(Long incidentId, Long userId);
}
