package i_itsm.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import i_itsm.model.AuditRecord;
import i_itsm.service.AuditService;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/{incidentId}")
    public List<AuditRecord> getIncidentHistory(
            @PathVariable Long incidentId) {
        return auditService.getIncidentHistory(incidentId);
    }
}
