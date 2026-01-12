package com.stormy.ai.models;

import java.util.HashSet;
import java.io.Serializable;
import java.util.Set;

/**
 * Represents a probabilistic rule.
 */
public class Rule implements Serializable {
    private static final long serialVersionUID = 1L;
    private Set<String> conditions;
    private String consequence;     // The stemmed keyword that is inferred if conditions are met
    private double confidence;      // The probability or confidence in this rule (0.0 to 1.0)
    private String description;     // A human-readable description of the rule

    /**
     * Constructs a new Rule.
     *
     * @param conditions  A set of stemmed words that act as conditions for the rule.
     * @param consequence The stemmed word that is the consequence of the rule.
     * @param confidence  The confidence score (0.0-1.0) of this rule.
     * @param description A descriptive string for the rule.
     */
    public Rule(Set<String> conditions, String consequence, double confidence, String description) {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Rule conditions cannot be null or empty.");
        }
        if (consequence == null || consequence.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule consequence cannot be null or empty.");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Rule confidence must be between 0.0 and 1.0.");
        }
        this.conditions = new HashSet<>(conditions);
        this.consequence = consequence.trim().toLowerCase(); // Ensure consequence is stemmed and lowercase
        this.confidence = confidence;
        this.description = description != null ? description : "Unnamed Rule";
    }

    // --- Getters ---
    public Set<String> getConditions() {
        return conditions;
    }

    public String getConsequence() {
        return consequence;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sets the confidence level of the rule.
     * @param confidence The new confidence value (between 0.0 and 1.0).
     */
    public void setConfidence(double confidence) {
        if (confidence < 0.0 || confidence > 1.0) {
            System.err.println("Warning: Attempted to set rule confidence out of bounds (0.0-1.0). Clamping value.");
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        } else {
            this.confidence = confidence;
        }
    }


    @Override
    public String toString() {
        return "Rule{Description='" + description + '\'' +
               ", Conditions=" + conditions +
               ", Consequence='" + consequence + '\'' +
               ", Confidence=" + String.format("%.2f", confidence) +
               '}';
    }
}
