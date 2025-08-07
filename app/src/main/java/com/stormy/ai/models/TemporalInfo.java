package com.stormy.ai.models;

/**
 * Represents temporal information associated with a SemanticNode or a concept.
 * This is a simplified representation for demonstration, allowing a 'start time'
 * and 'end time' (which could be years, specific dates, or relative markers).
 * In a real-world scenario, this would be much more sophisticated (e.g., ISO 8601,
 * temporal relations like 'before', 'after', 'during').
 */
public class TemporalInfo {
    private String rawTemporalExpression;
    private Long startTimeMillis;
    private Long endTimeMillis;
    private ExpressionType expressionType;
    private boolean isAmbiguous;

    public enum ExpressionType {
        DATE,
        DURATION,
        SEQUENCE,
        UNKNOWN
    }

    public TemporalInfo(String rawTemporalExpression, Long startTimeMillis, Long endTimeMillis, ExpressionType expressionType, boolean isAmbiguous) {
        this.rawTemporalExpression = rawTemporalExpression;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.expressionType = expressionType;
        this.isAmbiguous = isAmbiguous;
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

    public ExpressionType getExpressionType() {
        return expressionType;
    }

    public boolean isAmbiguous() {
        return isAmbiguous;
    }

    @Override
    public String toString() {
        return "TemporalInfo{" +
                "expression='" + rawTemporalExpression + '\'' +
                ", start=" + (startTimeMillis != null ? startTimeMillis : "N/A") +
                ", end=" + (endTimeMillis != null ? endTimeMillis : "N/A") +
                ", type=" + expressionType +
                ", ambiguous=" + isAmbiguous +
                '}';
    }
}
