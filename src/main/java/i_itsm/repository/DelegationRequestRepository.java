package i_itsm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import i_itsm.model.DelegationRequest;

public interface DelegationRequestRepository extends JpaRepository<DelegationRequest, Long> {

    boolean existsByIncidentIdAndStatus(Long incidentId, String status);

    List<DelegationRequest> findByIncidentIdAndStatus(Long incidentId, String status);

    List<DelegationRequest> findByStatusOrderByRequestedAtDesc(String status);

    List<DelegationRequest> findAllByOrderByRequestedAtDesc();

    List<DelegationRequest> findByFromTechnicianIdOrderByRequestedAtDesc(Long technicianId);
}
