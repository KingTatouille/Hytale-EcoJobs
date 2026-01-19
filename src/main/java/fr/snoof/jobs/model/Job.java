package fr.snoof.jobs.model;

/**
 * Represents a Job definition with all its properties for the UI.
 */
public class Job {
    private final JobType type;
    private final String icon;
    private final int requiredLevel;
    private final double baseSalary;
    private final double xpPerAction;
    private final boolean enabled;
    private final boolean requirePermission;

    public Job(JobType type, String icon, int requiredLevel, double baseSalary, double xpPerAction) {
        this.type = type;
        this.icon = icon;
        this.requiredLevel = requiredLevel;
        this.baseSalary = baseSalary;
        this.xpPerAction = xpPerAction;
        this.enabled = true;
        this.requirePermission = false; // Default for now
    }

    public String getId() {
        return type.name();
    }

    public String getName() {
        return type.getDisplayName();
    }

    public String getDescription() {
        return type.getDescription();
    }

    public String getIcon() {
        return icon;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public double getXpPerAction() {
        return xpPerAction;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequirePermission() {
        return requirePermission;
    }

    public JobType getType() {
        return type;
    }
}
