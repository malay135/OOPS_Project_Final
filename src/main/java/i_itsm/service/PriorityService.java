package i_itsm.service;

import org.springframework.stereotype.Service;

@Service
public class PriorityService {

    public String calculatePriority(String impact, String urgency) {

        if (impact == null || urgency == null) {
            throw new IllegalArgumentException(
                    "Impact and urgency are required");
        }

        if (impact.equalsIgnoreCase("HIGH")) {
            if (urgency.equalsIgnoreCase("HIGH")) {
                return "P1";
            }
            if (urgency.equalsIgnoreCase("MEDIUM")) {
                return "P2";
            }
            if (urgency.equalsIgnoreCase("LOW")) {
                return "P3";
            }
        }

        if (impact.equalsIgnoreCase("MEDIUM")) {
            if (urgency.equalsIgnoreCase("HIGH")) {
                return "P2";
            }
            if (urgency.equalsIgnoreCase("MEDIUM")) {
                return "P3";
            }
            if (urgency.equalsIgnoreCase("LOW")) {
                return "P4";
            }
        }

        if (impact.equalsIgnoreCase("LOW")) {
            if (urgency.equalsIgnoreCase("HIGH")) {
                return "P3";
            }
            if (urgency.equalsIgnoreCase("MEDIUM")
                    || urgency.equalsIgnoreCase("LOW")) {
                return "P4";
            }
        }

        throw new IllegalArgumentException(
                "Impact and urgency must be HIGH, MEDIUM, or LOW");
    }
}
