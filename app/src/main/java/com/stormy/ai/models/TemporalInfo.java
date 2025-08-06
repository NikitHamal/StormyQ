package com.stormy.ai.models;

/**
 * Production-ready temporal information representation supporting ISO 8601 dates,
 * relative temporal expressions, and temporal relations for sophisticated time-aware reasoning.
 * Integrates with NLP pipeline for automatic temporal entity extraction and normalization.
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

    /**
     * Checks if this temporal info represents a relative temporal expression.
     * @return true if the expression is relative (e.g., "yesterday", "next month")
     */
    public boolean isRelative() {
        if (rawTemporalExpression == null) return false;
        String lower = rawTemporalExpression.toLowerCase();
        return lower.contains("ago") || lower.contains("last") || lower.contains("next") ||
               lower.contains("yesterday") || lower.contains("tomorrow") || lower.contains("now") ||
               lower.contains("recent") || lower.contains("current") || lower.contains("future");
    }

    /**
     * Checks if this temporal info represents an absolute temporal expression.
     * @return true if the expression is absolute (e.g., "1990", "July 4, 1776")
     */
    public boolean isAbsolute() {
        return !isRelative() && (startTimeMillis != null || endTimeMillis != null);
    }

    /**
     * Get the duration in milliseconds if this represents a duration.
     * @return duration in milliseconds, or null if not a duration
     */
    public Long getDurationMillis() {
        if (isDuration() && startTimeMillis != null && endTimeMillis != null) {
            return Math.abs(endTimeMillis - startTimeMillis);
        }
        return null;
    }

    /**
     * Checks if this temporal info overlaps with another temporal info.
     * @param other The other temporal info to compare with
     * @return true if there's temporal overlap
     */
    public boolean overlapsWith(TemporalInfo other) {
        if (other == null || !isValid() || !other.isValid()) {
            return false;
        }

        long thisStart = startTimeMillis != null ? startTimeMillis : endTimeMillis;
        long thisEnd = endTimeMillis != null ? endTimeMillis : startTimeMillis;
        long otherStart = other.startTimeMillis != null ? other.startTimeMillis : other.endTimeMillis;
        long otherEnd = other.endTimeMillis != null ? other.endTimeMillis : other.startTimeMillis;

        return thisStart <= otherEnd && otherStart <= thisEnd;
    }

    /**
     * Checks if this temporal info comes before another temporal info.
     * @param other The other temporal info to compare with
     * @return true if this comes before the other
     */
    public boolean isBefore(TemporalInfo other) {
        if (other == null || !isValid() || !other.isValid()) {
            return false;
        }

        long thisTime = endTimeMillis != null ? endTimeMillis : startTimeMillis;
        long otherTime = other.startTimeMillis != null ? other.startTimeMillis : other.endTimeMillis;

        return thisTime < otherTime;
    }

    /**
     * Checks if this temporal info comes after another temporal info.
     * @param other The other temporal info to compare with
     * @return true if this comes after the other
     */
    public boolean isAfter(TemporalInfo other) {
        if (other == null || !isValid() || !other.isValid()) {
            return false;
        }

        long thisTime = startTimeMillis != null ? startTimeMillis : endTimeMillis;
        long otherTime = other.endTimeMillis != null ? other.endTimeMillis : other.startTimeMillis;

        return thisTime > otherTime;
    }

    /**
     * Checks if this temporal info is during another temporal info.
     * @param other The other temporal info to compare with
     * @return true if this is contained within the other
     */
    public boolean isDuring(TemporalInfo other) {
        if (other == null || !isValid() || !other.isValid()) {
            return false;
        }

        if (!other.isDuration()) {
            return false;
        }

        long thisStart = startTimeMillis != null ? startTimeMillis : endTimeMillis;
        long thisEnd = endTimeMillis != null ? endTimeMillis : startTimeMillis;

        return thisStart >= other.startTimeMillis && thisEnd <= other.endTimeMillis;
    }

    /**
     * Checks if this temporal info has valid time information.
     * @return true if at least one time value is present
     */
    public boolean isValid() {
        return startTimeMillis != null || endTimeMillis != null;
    }

    /**
     * Get the temporal granularity (year, month, day, etc.) based on the expression.
     * @return temporal granularity as a string
     */
    public String getGranularity() {
        if (rawTemporalExpression == null) return "unknown";
        
        String lower = rawTemporalExpression.toLowerCase();
        if (lower.contains("century") || lower.contains("decade")) return "decade";
        if (lower.contains("year") || lower.matches(".*\\b\\d{4}\\b.*")) return "year";
        if (lower.contains("month") || lower.contains("january") || lower.contains("february") ||
            lower.contains("march") || lower.contains("april") || lower.contains("may") ||
            lower.contains("june") || lower.contains("july") || lower.contains("august") ||
            lower.contains("september") || lower.contains("october") || lower.contains("november") ||
            lower.contains("december")) return "month";
        if (lower.contains("week") || lower.contains("monday") || lower.contains("tuesday") ||
            lower.contains("wednesday") || lower.contains("thursday") || lower.contains("friday") ||
            lower.contains("saturday") || lower.contains("sunday")) return "week";
        if (lower.contains("day") || lower.contains("yesterday") || lower.contains("tomorrow") ||
            lower.contains("today")) return "day";
        if (lower.contains("hour") || lower.contains("morning") || lower.contains("afternoon") ||
            lower.contains("evening") || lower.contains("night")) return "hour";
        if (lower.contains("minute") || lower.contains("second")) return "minute";
        
        return "day"; // default granularity
    }

    /**
     * Create a normalized ISO 8601 representation if possible.
     * @return ISO 8601 string or null if cannot be normalized
     */
    public String toISO8601() {
        if (startTimeMillis == null) return null;
        
        try {
            java.text.SimpleDateFormat iso8601Format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            iso8601Format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return iso8601Format.format(new java.util.Date(startTimeMillis));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TemporalInfo{");
        sb.append("expression='").append(rawTemporalExpression).append('\'');
        sb.append(", start=").append(startTimeMillis != null ? startTimeMillis : "N/A");
        sb.append(", end=").append(endTimeMillis != null ? endTimeMillis : "N/A");
        sb.append(", granularity=").append(getGranularity());
        sb.append(", type=").append(isRelative() ? "relative" : (isAbsolute() ? "absolute" : "unknown"));
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TemporalInfo that = (TemporalInfo) o;
        
        if (startTimeMillis != null ? !startTimeMillis.equals(that.startTimeMillis) : that.startTimeMillis != null)
            return false;
        if (endTimeMillis != null ? !endTimeMillis.equals(that.endTimeMillis) : that.endTimeMillis != null)
            return false;
        return rawTemporalExpression != null ? rawTemporalExpression.equals(that.rawTemporalExpression) : that.rawTemporalExpression == null;
    }

    @Override
    public int hashCode() {
        int result = rawTemporalExpression != null ? rawTemporalExpression.hashCode() : 0;
        result = 31 * result + (startTimeMillis != null ? startTimeMillis.hashCode() : 0);
        result = 31 * result + (endTimeMillis != null ? endTimeMillis.hashCode() : 0);
        return result;
    }
}
