package fr.snoof.jobs.model;

public record JobsEventData(String action, String jobId, int page) {
}
