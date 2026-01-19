package fr.snoof.jobs.model;

public class JobData {
    private int level;
    private long experience;
    private long totalExperience;
    private long totalEarnings;
    private int actionsCount;
    private long lastActionTime;

    public JobData() {
        this.level = 1;
        this.experience = 0;
        this.totalExperience = 0;
        this.totalEarnings = 0;
        this.actionsCount = 0;
        this.lastActionTime = 0;
    }

    public JobData(int level, long experience, long totalExperience) {
        this.level = level;
        this.experience = experience;
        this.totalExperience = totalExperience;
        this.totalEarnings = 0;
        this.actionsCount = 0;
        this.lastActionTime = 0;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    public long getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(long totalExperience) {
        this.totalExperience = totalExperience;
    }

    public void addExperience(long amount) {
        this.experience += amount;
        this.totalExperience += amount;
    }

    public void resetExperience() {
        this.experience = 0;
    }

    public long getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(long totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public void addEarnings(long amount) {
        this.totalEarnings += amount;
    }

    public int getActionsCount() {
        return actionsCount;
    }

    public void setActionsCount(int actionsCount) {
        this.actionsCount = actionsCount;
    }

    public void incrementActions() {
        this.actionsCount++;
    }

    public long getLastActionTime() {
        return lastActionTime;
    }

    public void setLastActionTime(long lastActionTime) {
        this.lastActionTime = lastActionTime;
    }

    public void updateLastAction() {
        this.lastActionTime = System.currentTimeMillis();
    }
}
