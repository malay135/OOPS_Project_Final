package i_itsm.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import i_itsm.dto.ResolutionSuggestion;
import i_itsm.model.Incident;
import i_itsm.service.IncidentService;
import i_itsm.service.ResolutionSuggestionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    private final ResolutionSuggestionService suggestionService;

    public IncidentController(IncidentService incidentService, ResolutionSuggestionService suggestionService) {
        this.incidentService = incidentService;
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public List<Incident> getAllIncidents() {
        return incidentService.getAllIncidents();
    }

    @GetMapping("/search")
    public List<Incident> searchIncidents(
            @RequestParam(required = false) Long incidentId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(defaultValue = "") String title) {

        return incidentService.searchIncidents(
                incidentId,
                priority,
                status,
                category,
                technicianId,
                title
        );
    }

    @PostMapping
    public Incident createIncident(@Valid @RequestBody Incident incident) {
        return incidentService.createIncident(incident);
    }
    


    @PutMapping("/{id}")
    public Incident updateIncident(@PathVariable Long id, @RequestBody java.util.Map<String,Object> body) {
        return incidentService.updateIncident(id,
                (String) body.get("title"),
                (String) body.get("description"),
                (String) body.get("category"),
                (String) body.get("impact"),
                (String) body.get("urgency"),
                (String) body.get("businessCriticality"),
                body.get("affectedServiceId") == null ? null : ((Number) body.get("affectedServiceId")).longValue(),
                toInteger(body.get("affectedUsers")));
    }

    // The number can arrive as 25 or "25"; anything else is a clear 400 error
    private static Integer toInteger(Object value) {
        if (value == null || "".equals(value)) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Number of affected users must be a whole number");
        }
    }

    @PutMapping("/{id}/status")
    public Incident updateStatus(
    		@PathVariable Long id,
    		@RequestParam String status,
    		@RequestParam(required = false) String notes) {
    	return incidentService.updateStatus(id, status, notes);
    }

    // The assigned technician acknowledges a new incident (it stays OPEN; the time is recorded)
    @PostMapping("/{id}/acknowledge")
    public Incident acknowledge(@PathVariable Long id) {
        return incidentService.acknowledge(id);
    }

    // How similar finished incidents were fixed (technicians and admins only)
    @GetMapping("/{id}/suggestions")
    public List<ResolutionSuggestion> suggestions(@PathVariable Long id) {
        return suggestionService.suggestFor(id);
    }

    @GetMapping("/mine")
    public List<Incident> getMyIncidents() {
        return incidentService.getMyIncidents();
    }
}
