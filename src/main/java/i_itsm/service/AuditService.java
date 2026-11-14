package i_itsm.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import i_itsm.model.AuditRecord;
import i_itsm.repository.AuditRecordRepository;

@Service
public class AuditService {

    private final AuditRecordRepository auditRecordRepository;

    public AuditService(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    public AuditRecord recordAction(Long incidentId, String action, String details) {
        String performedBy = getCurrentUsername();
        AuditRecord record = new AuditRecord(incidentId, action, details, performedBy);
        return auditRecordRepository.save(record);
    }

    public AuditRecord recordAction(Long incidentId, String action, String details, String performedBy) {
        AuditRecord record = new AuditRecord(incidentId, action, details, performedBy);
        return auditRecordRepository.save(record);
    }

    public List<AuditRecord> getIncidentHistory(Long incidentId) {
        return auditRecordRepository.findByIncidentIdOrderByTimestampDesc(incidentId);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
