package i_itsm.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import i_itsm.model.IncidentNote;
import i_itsm.service.IncidentNoteService;

@RestController
@RequestMapping("/api/incidents/{incidentId}/notes")
public class IncidentNoteController {
    private final IncidentNoteService service;
    public IncidentNoteController(IncidentNoteService service){this.service=service;}
    @GetMapping public List<IncidentNote> list(@PathVariable Long incidentId){return service.list(incidentId);}
    @PostMapping public IncidentNote add(@PathVariable Long incidentId,@RequestBody Map<String,String> body){return service.add(incidentId,body.get("type"),body.get("message"));}
}
