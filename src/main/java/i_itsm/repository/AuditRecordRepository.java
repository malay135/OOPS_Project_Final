package i_itsm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import i_itsm.model.AuditRecord;

public interface AuditRecordRepository
        extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByIncidentIdOrderByTimestampDesc(Long incidentId);
}
