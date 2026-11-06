package i_itsm.service;

import java.util.List;

import org.springframework.stereotype.Service;

import i_itsm.model.AuditRecord;
import i_itsm.repository.AuditRecordRepository;

@Service
public class AuditService {

    private final AuditRecordRepository auditRecordRepository;

    public AuditService(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    public AuditRecord recordAction(
            Long incidentId, String action, String details) {

        AuditRecord record = new AuditRecord(
                incidentId, action, details);

        return auditRecordRepository.save(record);
    }

    public List<AuditRecord> getIncidentHistory(Long incidentId) {
        return auditRecordRepository
                .findByIncidentIdOrderByTimestampDesc(incidentId);
    }
}
