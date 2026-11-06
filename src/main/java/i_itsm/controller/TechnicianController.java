package i_itsm.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import i_itsm.model.Technician;
import i_itsm.service.TechnicianService;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

    private final TechnicianService technicianService;

    public TechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @PostMapping
    public Technician createTechnician(
            @RequestBody Technician technician) {
        return technicianService.createTechnician(technician);
    }

    @GetMapping
    public List<Technician> getAllTechnicians() {
        return technicianService.getAllTechnicians();
    }

    @GetMapping("/{id}")
    public Technician getTechnicianById(@PathVariable Long id) {
        return technicianService.getTechnicianById(id);
    }

    @PutMapping("/{id}")
    public Technician updateTechnician(
            @PathVariable Long id,
            @RequestBody Technician technician) {
        return technicianService.updateTechnician(id, technician);
    }
}
