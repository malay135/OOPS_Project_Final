package i_itsm.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import i_itsm.model.IncidentNote;

public interface IncidentNoteRepository extends JpaRepository<IncidentNote, Long> {
    List<IncidentNote> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
