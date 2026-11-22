package i_itsm.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import i_itsm.model.ITService;

public interface ITServiceRepository extends JpaRepository<ITService, Long> {
    Optional<ITService> findByNameIgnoreCase(String name);
    List<ITService> findByActiveTrueOrderByNameAsc();
}
