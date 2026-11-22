package i_itsm.strategy;

/**
 * The assignment strategies an administrator can choose on the Settings page.
 * Each value names one AssignmentStrategy implementation (Strategy pattern).
 */
public enum AssignmentPolicy {

    SKILL_THEN_WORKLOAD("Skill first, then workload",
            "A technician with the incident's category as a skill (least busy first). If nobody has the skill, the least busy technician."),
    SKILL_ONLY("Skill only",
            "Only technicians with the matching skill. If nobody has it, the incident stays unassigned for an admin to assign."),
    LEAST_LOADED("Least workload",
            "The technician with the fewest active incidents, whatever their skills.");

    private final String label;
    private final String description;

    AssignmentPolicy(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }

    /** "least_loaded" -> LEAST_LOADED. Unknown names are refused with a clear message. */
    public static AssignmentPolicy from(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Choose an assignment strategy");
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown assignment strategy: " + name);
        }
    }
}
