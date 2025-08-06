package com.stormy.ai.models;

/**
 * Represents temporal information associated with a SemanticNode or a concept.
 * This is a simplified representation for demonstration, allowing a 'start time'
 * and 'end time' (which could be years, specific dates, or relative markers).
 * In a real-world scenario, this would be much more sophisticated (e.g., ISO 8601,
 * temporal relations like 'before', 'after', 'during').
 */
public class TemporalInfo {
    private String rawTemporalExpression; // The original temporal phrase (e.g., "last year", "in 1990")
    private Long startTimeMillis;         // Numeric representation of start time (e.g., milliseconds since epoch, or a year)
    private Long endTimeMillis;           // Numeric representation of end time

    /**
     * Constructor for TemporalInfo.
     *
     * @param rawTemporalExpression The raw text expression (e.g., "yesterday", "July 4, 1776").
     * @param startTimeMillis A numerical representation of the start time (e.g., timestamp, year). Can be null.
     * @param endTimeMillis A numerical representation of the end time. Can be null.
     */
    public TemporalInfo(String rawTemporalExpression, Long startTimeMillis, Long endTimeMillis) {
        this.rawTemporalExpression = rawTemporalExpression;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
    }

    // --- Getters ---
    public String getRawTemporalExpression() {
        return rawTemporalExpression;
    }

    public Long getStartTimeMillis() {
        return startTimeMillis;
    }

    public Long getEndTimeMillis() {
        return endTimeMillis;
    }

    /**
     * Checks if this temporal info represents a point in time (start and end are the same or very close).
     * @return true if it's a point in time, false otherwise.
     */
    public boolean isPointInTime() {
        return startTimeMillis != null && endTimeMillis != null && startTimeMillis.equals(endTimeMillis);
    }

    /**
     * Checks if this temporal info indicates a duration.
     * @return true if it's a duration, false otherwise.
     */
    public boolean isDuration() {
        return startTimeMillis != null && endTimeMillis != null && !startTimeMillis.equals(endTimeMillis);
    }

    @Override
    public String toString() {
        return "TemporalInfo{" +
               "expression='" + rawTemporalExpression + '\'' +
               ", start=" + (startTimeMillis != null ? startTimeMillis : "N/A") +
               ", end=" + (endTimeMillis != null ? endTimeMillis : "N/A") +
               '}';
    }
}
