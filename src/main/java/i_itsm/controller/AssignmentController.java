package i_itsm.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import i_itsm.model.DelegationRequest;
import i_itsm.model.Technician;
import i_itsm.service.AssignmentService;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
    private final AssignmentService assignmentService;
    public AssignmentController(AssignmentService assignmentService) { this.assignmentService = assignmentService; }

    /** Administrator override: POST /api/assignments/override with {"incidentId":1,"technicianId":2}. */
    @PostMapping("/override")
    public Technician overrideAssignment(@RequestBody Map<String, Long> body) {
        return assignmentService.overrideAssignment(body.get("incidentId"), body.get("technicianId"));
    }

    /** Legacy endpoint retained for compatibility; it now invokes the automatic policy only. */
    @PostMapping
    public Technician assignIncident(@RequestParam Long incidentId,
            @RequestParam(defaultValue = "SKILL_THEN_WORKLOAD") String strategy) {
        return assignmentService.assignIncident(incidentId, strategy);
    }

    /** Assigned technician asks to delegate: POST /api/assignments/{incidentId}/delegate with {"technicianId":2}. Needs admin approval. */
    @PostMapping("/{incidentId}/delegate")
    public DelegationRequest delegate(@PathVariable Long incidentId, @RequestBody Map<String, Long> body) {
        return assignmentService.requestDelegation(incidentId, body.get("technicianId"));
    }

    /** Admin: GET /api/assignments/delegations?status=PENDING (or ALL). */
    @GetMapping("/delegations")
    public List<DelegationRequest> delegations(@RequestParam(required = false) String status) {
        return assignmentService.listDelegations(status);
    }

    /** Technician: my own delegation requests and their status. */
    @GetMapping("/delegations/mine")
    public List<DelegationRequest> myDelegations() {
        return assignmentService.myDelegations();
    }

    @PostMapping("/delegations/{id}/approve")
    public DelegationRequest approve(@PathVariable Long id) {
        return assignmentService.approveDelegation(id);
    }

    @PostMapping("/delegations/{id}/reject")
    public DelegationRequest reject(@PathVariable Long id) {
        return assignmentService.rejectDelegation(id);
    }
}
