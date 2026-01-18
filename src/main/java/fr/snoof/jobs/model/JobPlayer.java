package fr.snoof.jobs.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JobPlayer {
    private UUID uuid;
    private String name;
    private Map<JobType, JobData> jobs;
    private Set<JobType> joinedJobs;
    private long lastSeen;

    public JobPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.jobs = new EnumMap<>(JobType.class);
        this.joinedJobs = EnumSet.noneOf(JobType.class);
        this.lastSeen = System.currentTimeMillis();

        // Initialize all jobs at level 1
        for (JobType type : JobType.values()) {
            jobs.put(type, new JobData());
        }
    }

    // For JSON deserialization
    public JobPlayer() {
        this.jobs = new EnumMap<>(JobType.class);
        this.joinedJobs = EnumSet.noneOf(JobType.class);
    }

    // ========== Job Join/Leave System ==========

    public boolean hasJoinedJob(JobType type) {
        return joinedJobs != null && joinedJobs.contains(type);
    }

    public boolean joinJob(JobType type) {
        if (joinedJobs == null) {
            joinedJobs = EnumSet.noneOf(JobType.class);
        }
        return joinedJobs.add(type);
    }

    public boolean leaveJob(JobType type) {
        if (joinedJobs == null)
            return false;
        return joinedJobs.remove(type);
    }

    public Set<JobType> getJoinedJobs() {
        if (joinedJobs == null) {
            joinedJobs = EnumSet.noneOf(JobType.class);
        }
        return joinedJobs;
    }

    public void setJoinedJobs(Set<JobType> joinedJobs) {
        this.joinedJobs = joinedJobs != null ? EnumSet.copyOf(joinedJobs) : EnumSet.noneOf(JobType.class);
    }

    public int getJoinedJobCount() {
        return joinedJobs != null ? joinedJobs.size() : 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<JobType, JobData> getJobs() {
        return jobs;
    }

    public void setJobs(Map<JobType, JobData> jobs) {
        this.jobs = jobs;
    }

    public JobData getJobData(JobType type) {
        if (jobs == null) {
            jobs = new EnumMap<>(JobType.class);
        }
        return jobs.computeIfAbsent(type, k -> new JobData());
    }

    public int getLevel(JobType type) {
        return getJobData(type).getLevel();
    }

    public long getExperience(JobType type) {
        return getJobData(type).getExperience();
    }

    public long getTotalExperience(JobType type) {
        return getJobData(type).getTotalExperience();
    }

    public void addExperience(JobType type, long amount) {
        getJobData(type).addExperience(amount);
    }

    public void setLevel(JobType type, int level) {
        getJobData(type).setLevel(level);
    }

    public void setExperience(JobType type, long experience) {
        getJobData(type).setExperience(experience);
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    public int getTotalLevel() {
        int total = 0;
        for (JobData data : jobs.values()) {
            total += data.getLevel();
        }
        return total;
    }

    public void resetJob(JobType type) {
        jobs.put(type, new JobData());
    }

    public void resetAllJobs() {
        for (JobType type : JobType.values()) {
            jobs.put(type, new JobData());
        }
    }
}
