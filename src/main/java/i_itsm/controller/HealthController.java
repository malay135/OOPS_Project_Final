package i_itsm.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Is the server awake?" check. No login needed.
 * The cloud host uses it to know the app has started, and the login page uses it
 * to wait while a sleeping free server wakes up.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
