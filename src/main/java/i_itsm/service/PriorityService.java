package i_itsm.service;

import org.springframework.stereotype.Service;
import i_itsm.exception.InvalidIncidentException;

/**
 * Works out the priority (P1 = critical ... P4 = low).
 *
 * 1. Impact and urgency give the base priority (table below).
 * 2. HIGH business criticality raises it one level.
 * 3. The number of affected users raises it further:
 *      1-9 people: no change, 10-99 people: one level, 100 or more: two levels.
 * It never goes above P1.
 */
@Service
public class PriorityService {

    static final int MANY_USERS = 10;
    static final int VERY_MANY_USERS = 100;

    /** Without a number of affected users (counted as one person). */
    public String calculatePriority(
            String impact,
            String urgency,
            String businessCriticality) {
        return calculatePriority(impact, urgency, businessCriticality, null);
    }

    public String calculatePriority(
            String impact,
            String urgency,
            String businessCriticality,
            Integer affectedUsers) {
        String priority = priorityFromFactors(impact, urgency, businessCriticality);
        int people = affectedUsers == null ? 1 : affectedUsers;
        if (people < 1) throw new InvalidIncidentException("Number of affected users must be at least 1");
        if (people >= MANY_USERS) priority = increasePriority(priority);
        if (people >= VERY_MANY_USERS) priority = increasePriority(priority);
        return priority;
    }

    private String priorityFromFactors(
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