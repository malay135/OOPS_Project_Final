package i_itsm.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import i_itsm.model.IncidentCategory;

public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {
    Optional<IncidentCategory> findByNameIgnoreCase(String name);
    List<IncidentCategory> findByActiveTrueOrderByNameAsc();
}
