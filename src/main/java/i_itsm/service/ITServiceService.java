package i_itsm.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import i_itsm.model.ITService;
import i_itsm.repository.ITServiceRepository;

@Service
public class ITServiceService {
    private final ITServiceRepository repository;
    public ITServiceService(ITServiceRepository repository) { this.repository = repository; }
    public List<ITService> getAll() { return repository.findAll(); }
    public List<ITService> getActive() { return repository.findByActiveTrueOrderByNameAsc(); }
    public ITService get(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("IT service not found: " + id)); }
    @Transactional
    public ITService create(ITService service) {
        if (repository.findByNameIgnoreCase(service.getName()).isPresent()) throw new IllegalArgumentException("An IT service with that name already exists");
        service.setActive(true);
        return repository.save(service);
    }
    @Transactional
    public ITService update(Long id, ITService incoming) {
        ITService s = get(id);
        if (incoming.getName() != null && !incoming.getName().isBlank()) s.setName(incoming.getName().trim());
        s.setDescription(incoming.getDescription());
        s.setActive(incoming.isActive());
        return repository.save(s);
    }
}
