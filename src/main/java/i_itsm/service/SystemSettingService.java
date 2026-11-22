package i_itsm.service;

import org.springframework.stereotype.Service;

import i_itsm.model.SystemSetting;
import i_itsm.repository.SystemSettingRepository;
import i_itsm.strategy.AssignmentPolicy;

/** Reads and changes application-wide settings: the assignment strategy and the technician workload limit. */
@Service
public class SystemSettingService {

    static final String ASSIGNMENT_STRATEGY = "ASSIGNMENT_STRATEGY";
    static final String MAX_ACTIVE_INCIDENTS = "MAX_ACTIVE_INCIDENTS";
    public static final int DEFAULT_MAX_ACTIVE_INCIDENTS = 10;
    public static final int HIGHEST_ALLOWED_LIMIT = 100;

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    /** The strategy chosen by the admin; skill-then-workload when nothing (or something unknown) is saved. */
    public AssignmentPolicy getAssignmentPolicy() {
        return repository.findById(ASSIGNMENT_STRATEGY)
                .map(SystemSetting::getValue)
                .map(value -> {
                    try { return AssignmentPolicy.from(value); } catch (IllegalArgumentException e) { return null; }
                })
                .orElse(AssignmentPolicy.SKILL_THEN_WORKLOAD);
    }

    public AssignmentPolicy setAssignmentPolicy(String name) {
        AssignmentPolicy policy = AssignmentPolicy.from(name);
        repository.save(new SystemSetting(ASSIGNMENT_STRATEGY, policy.name()));
        return policy;
    }

    /** How many active (not resolved or closed) incidents one technician may have. Default 10. */
    public int getMaxActiveIncidents() {
        return repository.findById(MAX_ACTIVE_INCIDENTS)
                .map(SystemSetting::getValue)
                .map(value -> {
                    try { return Integer.valueOf(value.trim()); } catch (NumberFormatException e) { return null; }
                })
                .filter(n -> n >= 1)
                .orElse(DEFAULT_MAX_ACTIVE_INCIDENTS);
    }

    public int setMaxActiveIncidents(Integer limit) {
        if (limit == null || limit < 1 || limit > HIGHEST_ALLOWED_LIMIT)
            throw new IllegalArgumentException("The workload limit must be between 1 and " + HIGHEST_ALLOWED_LIMIT);
        repository.save(new SystemSetting(MAX_ACTIVE_INCIDENTS, String.valueOf(limit)));
        return limit;
    }
}
