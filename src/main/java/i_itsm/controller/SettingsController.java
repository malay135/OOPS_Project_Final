package i_itsm.controller;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import i_itsm.service.SystemSettingService;
import i_itsm.strategy.AssignmentPolicy;

/** Administrator settings (admin only, except reading the workload limit; see SecurityConfig). */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SystemSettingService settings;

    public SettingsController(SystemSettingService settings) {
        this.settings = settings;
    }

    // {"strategy": "SKILL_THEN_WORKLOAD", "options": [{"value": ..., "label": ..., "description": ...}, ...]}
    @GetMapping("/assignment-strategy")
    public Map<String, Object> getAssignmentStrategy() {
        return describe(settings.getAssignmentPolicy());
    }

    // Body: {"strategy": "LEAST_LOADED"}
    @PutMapping("/assignment-strategy")
    public Map<String, Object> setAssignmentStrategy(@RequestBody Map<String, String> body) {
        return describe(settings.setAssignmentPolicy(body.get("strategy")));
    }

    // {"maxActiveIncidents": 10}. Technicians can read it too (their page shows "workload 3 / 10").
    @GetMapping("/workload-limit")
    public Map<String, Integer> getWorkloadLimit() {
        return Map.of("maxActiveIncidents", settings.getMaxActiveIncidents());
    }

    // Body: {"maxActiveIncidents": 8}
    @PutMapping("/workload-limit")
    public Map<String, Integer> setWorkloadLimit(@RequestBody Map<String, Object> body) {
        Object value = body.get("maxActiveIncidents");
        Integer limit;
        try {
            limit = value instanceof Number n ? Integer.valueOf(n.intValue()) : value == null ? null : Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The workload limit must be a whole number");
        }
        return Map.of("maxActiveIncidents", settings.setMaxActiveIncidents(limit));
    }

    private Map<String, Object> describe(AssignmentPolicy current) {
        List<Map<String, String>> options = Arrays.stream(AssignmentPolicy.values())
                .map(p -> Map.of("value", p.name(), "label", p.getLabel(), "description", p.getDescription()))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strategy", current.name());
        result.put("options", options);
        return result;
    }
}
