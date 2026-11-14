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

import i_itsm.model.Incident;
import i_itsm.service.IncidentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
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
    

    @PutMapping("/{id}/status")
    public Incident updateStatus(
    		@PathVariable Long id,
    		@RequestParam String status) {
    	return incidentService.updateStatus(id, status);
    }

    @GetMapping("/mine")
    public List<Incident> getMyIncidents() {
        return incidentService.getMyIncidents();
    }
}
