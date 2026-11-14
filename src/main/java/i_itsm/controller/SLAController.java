package i_itsm.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import i_itsm.model.Incident;
import i_itsm.service.IncidentService;
import i_itsm.service.SLAService;

@RestController
@RequestMapping("/api/sla")
public class SLAController {

    private final IncidentService incidentService;
    private final SLAService slaService;

    public SLAController(
            IncidentService incidentService,
            SLAService slaService) {
        this.incidentService = incidentService;
        this.slaService = slaService;
    }

    @GetMapping("/{incidentId}")
    public Map<String, Object> getSLAStatus(
            @PathVariable Long incidentId) {

        Incident incident =
                incidentService.getIncidentById(incidentId);

        boolean violated = slaService.isViolated(incident); // read-only: viewing changes nothing
  
        Map<String, Object> result = new java.util.HashMap<>();

        result.put("incidentId", incident.getId());
        result.put("priority", incident.getPriority());
        result.put("slaDeadline", incident.getSlaDeadline());
        result.put("slaViolated", violated);

        return result;
    }
}
