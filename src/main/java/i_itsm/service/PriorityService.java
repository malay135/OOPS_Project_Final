package i_itsm.service;

import org.springframework.stereotype.Service;
import i_itsm.exception.InvalidIncidentException;

@Service
public class PriorityService {

    public String calculatePriority(
            String impact,
            String urgency,
            String businessCriticality) {

        if (impact == null
                || urgency == null
                || businessCriticality == null) {

            throw new InvalidIncidentException(
                    "Impact, urgency, and business criticality are required");
        }

        String basePriority = calculateBasePriority(impact, urgency);

        if (businessCriticality.equalsIgnoreCase("HIGH")) {
            return increasePriority(basePriority);
        }

        if (businessCriticality.equalsIgnoreCase("MEDIUM")
                || businessCriticality.equalsIgnoreCase("LOW")) {
            return basePriority;
        }

        throw new InvalidIncidentException(
                "Business criticality must be HIGH, MEDIUM, or LOW");
    }

    private String calculateBasePriority(
            String impact,
            String urgency) {

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

        throw new InvalidIncidentException(
                "Impact and urgency must be HIGH, MEDIUM, or LOW");
    }

    private String increasePriority(String priority) {

        switch (priority) {
            case "P4":
                return "P3";

            case "P3":
                return "P2";

            case "P2":
                return "P1";

            case "P1":
                return "P1";

            default:
                throw new InvalidIncidentException(
                        "Invalid priority: " + priority);
        }
    }
}