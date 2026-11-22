package i_itsm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import i_itsm.model.ServiceSlaPolicy;

public interface ServiceSlaPolicyRepository extends JpaRepository<ServiceSlaPolicy, Long> {
    Optional<ServiceSlaPolicy> findByServiceIdAndPriority(Long serviceId, String priority);
}
