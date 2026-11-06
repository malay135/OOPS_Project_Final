package i_itsm.controller;

import org.springframework.web.bind.annotation.*;

import i_itsm.model.Technician;
import i_itsm.service.AssignmentService;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public Technician assignIncident(
            @RequestParam Long incidentId,
            @RequestParam String strategy) {

        return assignmentService.assignIncident(
                incidentId, strategy);
    }
}
