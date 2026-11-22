package i_itsm.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import i_itsm.model.IncidentCategory;
import i_itsm.repository.IncidentCategoryRepository;

@Service
public class IncidentCategoryService {
    private final IncidentCategoryRepository repository;
    public IncidentCategoryService(IncidentCategoryRepository repository) { this.repository=repository; }
    public List<IncidentCategory> getAll() { return repository.findAll(); }
    public List<IncidentCategory> getActive() { return repository.findByActiveTrueOrderByNameAsc(); }
    @Transactional public IncidentCategory create(IncidentCategory c) {
        if (c.getName()==null || c.getName().isBlank()) throw new IllegalArgumentException("Category name is required");
        if (repository.findByNameIgnoreCase(c.getName().trim()).isPresent()) throw new IllegalArgumentException("Category already exists");
        c.setName(c.getName().trim().toUpperCase()); c.setActive(true); return repository.save(c);
    }
    @Transactional public IncidentCategory update(Long id, IncidentCategory incoming) {
        IncidentCategory c=repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        if (incoming.getName()!=null && !incoming.getName().isBlank()) c.setName(incoming.getName().trim().toUpperCase());
        c.setDescription(incoming.getDescription()); c.setActive(incoming.isActive()); return repository.save(c);
    }
}
