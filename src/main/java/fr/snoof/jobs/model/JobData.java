package fr.snoof.jobs.model;

public class JobData {
    private int level;
    private long experience;
    private long totalExperience;

    public JobData() {
        this.level = 1;
        this.experience = 0;
        this.totalExperience = 0;
    }

    public JobData(int level, long experience, long totalExperience) {
        this.level = level;
        this.experience = experience;
        this.totalExperience = totalExperience;
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
}
