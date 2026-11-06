package i_itsm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import i_itsm.model.Technician;

public interface TechnicianRepository
        extends JpaRepository<Technician, Long> {
        List<Technician> findByActiveTrue();
}
