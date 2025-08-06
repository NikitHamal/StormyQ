package com.stormy.ai.models;

import java.util.Objects;

/**
 * Represents a higher-level conceptual relationship between two entities (SemanticNodes).
 * This can be used to model ontological knowledge or factual relationships beyond simple co-occurrence.
 * Examples: IS_A (cat IS_A animal), HAS_PART (car HAS_PART wheel), CAUSES (rain CAUSES wetness).
 */
public class ConceptRelation {
    public enum RelationType {
        IS_A,       // A is a type of B (e.g., "cat IS_A animal")
        PART_OF,    // A is a part of B (e.g., "wheel PART_OF car")
        CAUSES,     // A causes B (e.g., "rain CAUSES wetness")
        LOCATED_IN, // A is located in B (e.g., "Eiffel Tower LOCATED_IN Paris")
        HAS_PROPERTY, // A has property B (e.g., "sun HAS_PROPERTY hot")
        // Add more relation types as needed
        UNKNOWN
    }

    private String sourceConcept; // The stemmed name of the source concept (e.g., "cat")
    private String targetConcept; // The stemmed name of the target concept (e.g., "animal")
    private RelationType type;    // The type of relationship
    private double strength;      // Confidence or strength of this relationship (0.0 to 1.0)

    /**
     * Constructs a new ConceptRelation.
     * @param sourceConcept The stemmed source concept.
     * @param targetConcept The stemmed target concept.
     * @param type The type of relationship.
     * @param strength The strength/confidence of the relationship (0.0-1.0).
     */
    public ConceptRelation(String sourceConcept, String targetConcept, RelationType type, double strength) {
        if (sourceConcept == null || sourceConcept.trim().isEmpty() ||
            targetConcept == null || targetConcept.trim().isEmpty()) {
            throw new IllegalArgumentException("Source and target concepts cannot be null or empty.");
        }
        if (type == null) {
            throw new IllegalArgumentException("RelationType cannot be null.");
        }
        if (strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("Strength must be between 0.0 and 1.0.");
        }
        this.sourceConcept = sourceConcept.trim().toLowerCase();
        this.targetConcept = targetConcept.trim().toLowerCase();
        this.type = type;
        this.strength = strength;
    }

    // --- Getters ---
    public String getSourceConcept() {
        return sourceConcept;
    }

    public String getTargetConcept() {
        return targetConcept;
    }

    public RelationType getType() {
        return type;
    }

    public double getStrength() {
        return strength;
    }

    // --- Setter for strength (if adaptive learning of relation strength is desired) ---
    public void setStrength(double strength) {
        this.strength = strength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConceptRelation that = (ConceptRelation) o;
        return Double.compare(that.strength, strength) == 0 &&
               Objects.equals(sourceConcept, that.sourceConcept) &&
               Objects.equals(targetConcept, that.targetConcept) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceConcept, targetConcept, type, strength);
    }

    @Override
    public String toString() {
        return "Relation{" +
               sourceConcept + " " + type.name() + " " + targetConcept +
               ", strength=" + String.format("%.2f", strength) +
               '}';
    }
}
