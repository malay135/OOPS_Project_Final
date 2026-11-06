package i_itsm.service;

import java.util.List;

import org.springframework.stereotype.Service;

import i_itsm.model.Technician;
import i_itsm.repository.TechnicianRepository;

@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public TechnicianService(
            TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    public Technician createTechnician(Technician technician) {
        technician.setId(null);
        return technicianRepository.save(technician);
    }

    public List<Technician> getAllTechnicians() {
        return technicianRepository.findAll();
    }

    public Technician getTechnicianById(Long id) {
        return technicianRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Technician not found: " + id));
    }

    public Technician updateTechnician(
            Long id, Technician updatedTechnician) {

        Technician existing = getTechnicianById(id);

        existing.setName(updatedTechnician.getName());
        existing.setSkills(updatedTechnician.getSkills());
        existing.setActiveIncidentCount(
                updatedTechnician.getActiveIncidentCount());
        existing.setActive(updatedTechnician.isActive());

        return technicianRepository.save(existing);
    }
}
