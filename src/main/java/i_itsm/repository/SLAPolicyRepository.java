package i_itsm.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import i_itsm.model.SLAPolicy;

public interface SLAPolicyRepository extends JpaRepository<SLAPolicy, Long> {
    Optional<SLAPolicy> findByPriorityAndActiveTrue(String priority);
    Optional<SLAPolicy> findByPriority(String priority);
}
