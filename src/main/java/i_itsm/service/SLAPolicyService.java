package i_itsm.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import i_itsm.model.ITService;
import i_itsm.model.SLAPolicy;
import i_itsm.model.ServiceSlaPolicy;
import i_itsm.repository.ITServiceRepository;
import i_itsm.repository.SLAPolicyRepository;
import i_itsm.repository.ServiceSlaPolicyRepository;

/** Admin settings for SLAs: the standard policy per priority, and optional service-specific SLAs. */
@Service
public class SLAPolicyService {
    private static final Set<String> PRIORITIES = Set.of("P1", "P2", "P3", "P4");

    private final SLAPolicyRepository repository;
    private final ServiceSlaPolicyRepository serviceRepository;
    private final ITServiceRepository itServiceRepository;

    // Used by unit tests that only need the standard policies
    public SLAPolicyService(SLAPolicyRepository repository) {
        this(repository, null, null);
    }

    @Autowired
    public SLAPolicyService(SLAPolicyRepository repository, ServiceSlaPolicyRepository serviceRepository,
            ITServiceRepository itServiceRepository) {
        this.repository = repository;
        this.serviceRepository = serviceRepository;
        this.itServiceRepository = itServiceRepository;
    }

    public List<SLAPolicy> getAll() { return repository.findAll(); }

    /** P1..P4 only; response >= 1 and not longer than resolution; warning before the deadline. */
    private static String checkTimes(String priority, int response, int resolution, int warning) {
        String p = priority == null ? "" : priority.trim().toUpperCase();
        if (!PRIORITIES.contains(p)) throw new IllegalArgumentException("Priority must be P1, P2, P3 or P4");
        if (response < 1 || resolution < 1) throw new IllegalArgumentException("Response and resolution times must be at least 1 minute");
        if (response > resolution) throw new IllegalArgumentException("Response time cannot be longer than resolution time");
        if (warning < 0 || warning >= resolution) throw new IllegalArgumentException("Warning time must be 0 or more and less than the resolution time");
        return p;
    }

    @Transactional
    public SLAPolicy upsert(String priority, SLAPolicy incoming) {
        String p = checkTimes(priority, incoming.getResponseTimeMinutes(), incoming.getResolutionTimeMinutes(), incoming.getWarningTimeMinutes());
        SLAPolicy s = repository.findByPriority(p).orElseGet(SLAPolicy::new);
        s.setPriority(p);
        s.setResponseTimeMinutes(incoming.getResponseTimeMinutes());
        s.setResolutionTimeMinutes(incoming.getResolutionTimeMinutes());
        s.setWarningTimeMinutes(incoming.getWarningTimeMinutes());
        s.setActive(incoming.isActive());
        return repository.save(s);
    }

    public SLAPolicy getActive(String priority) {
        return repository.findByPriorityAndActiveTrue(priority).orElseThrow(() -> new IllegalArgumentException("No active SLA policy for priority: " + priority));
    }

    // ---- Service-specific SLAs ----

    /** All service SLAs, sorted by service name and priority. */
    public List<ServiceSlaPolicy> getServicePolicies() {
        return serviceRepository.findAll().stream()
                .sorted(Comparator.comparing((ServiceSlaPolicy s) -> s.getService().getName().toLowerCase())
                        .thenComparing(ServiceSlaPolicy::getPriority))
                .toList();
    }

    /** Add or change the SLA of one service for one priority. */
    @Transactional
    public ServiceSlaPolicy upsertServicePolicy(Long serviceId, String priority, SLAPolicy times) {
        String p = checkTimes(priority, times.getResponseTimeMinutes(), times.getResolutionTimeMinutes(), times.getWarningTimeMinutes());
        ITService service = itServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("IT service not found: " + serviceId));
        ServiceSlaPolicy policy = serviceRepository.findByServiceIdAndPriority(serviceId, p)
                .orElseGet(() -> new ServiceSlaPolicy(service, p));
        policy.setResponseTimeMinutes(times.getResponseTimeMinutes());
        policy.setResolutionTimeMinutes(times.getResolutionTimeMinutes());
        policy.setWarningTimeMinutes(times.getWarningTimeMinutes());
        return serviceRepository.save(policy);
    }

    /** Remove a service SLA: that service goes back to the standard policy for this priority. */
    @Transactional
    public void deleteServicePolicy(Long serviceId, String priority) {
        String p = priority == null ? "" : priority.trim().toUpperCase();
        ServiceSlaPolicy policy = serviceRepository.findByServiceIdAndPriority(serviceId, p)
                .orElseThrow(() -> new IllegalArgumentException("No service SLA for this service and priority"));
        serviceRepository.delete(policy);
    }
}
