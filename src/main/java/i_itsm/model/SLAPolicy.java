package i_itsm.model;

public class SLAPolicy {

    private String priority;
    private int resolutionTimeHours;

    public SLAPolicy() {
    }

    public SLAPolicy(String priority, int resolutionTimeHours) {
        this.priority = priority;
        this.resolutionTimeHours = resolutionTimeHours;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getResolutionTimeHours() {
        return resolutionTimeHours;
    }

    public void setResolutionTimeHours(int resolutionTimeHours) {
        this.resolutionTimeHours = resolutionTimeHours;
    }
}
