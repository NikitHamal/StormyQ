package com.stormy.ai.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Enhanced SemanticNode with serialization and syntactic roles.
 */
public class SemanticNode implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String name;
    private transient double activation; 
    private double decayRate;
    private transient boolean activatedThisCycle;
    private TemporalInfo temporalInfo;
    private boolean isNegated;
    private final List<ConceptRelation> conceptualRelations;
    private String primaryRole; // Syntactic role: SUBJECT, VERB, OBJECT, etc.

    public SemanticNode(String name, double decayRate) {
        this.name = name;
        this.activation = 0.0;
        this.decayRate = decayRate;
        this.conceptualRelations = new ArrayList<>();
    }

    public String getName() { return name; }
    public double getActivation() { return activation; }
    public boolean isActivatedThisCycle() { return activatedThisCycle; }
    public TemporalInfo getTemporalInfo() { return temporalInfo; }
    public boolean isNegated() { return isNegated; }
    public List<ConceptRelation> getConceptualRelations() { return new ArrayList<>(conceptualRelations); }
    public String getPrimaryRole() { return primaryRole; }

    public void setPrimaryRole(String role) { this.primaryRole = role; }
    public void setActivation(double activation) { this.activation = activation; }
    
    public void increaseActivation(double amount) {
        this.activation = Math.min(1.0, this.activation + amount);
    }

    public void applyDecay() {
        this.activation -= decayRate;
        if (this.activation < 0.0) { // Ensure activation doesn't go below zero
            this.activation = 0.0;
        }
    }

    /**
     * Sets the decay rate for this specific node.
     * @param decayRate The new decay rate.
     */
    public void setDecayRate(double decayRate) { // <--- ADDED THIS METHOD
        this.decayRate = decayRate;
    }

    public void setActivatedThisCycle(boolean activatedThisCycle) {
        this.activatedThisCycle = activatedThisCycle;
    }

    /**
     * Associates temporal information with this node.
     * @param temporalInfo The TemporalInfo object to associate.
     */
    public void setTemporalInfo(TemporalInfo temporalInfo) {
        this.temporalInfo = temporalInfo;
    }

    /**
     * Sets the negation status of this node.
     * @param negated True if the node is in a negated context, false otherwise.
     */
    public void setNegated(boolean negated) {
        isNegated = negated;
    }

    /**
     * Adds a conceptual relation involving this node.
     * @param relation The ConceptRelation to add.
     */
    public void addConceptualRelation(ConceptRelation relation) {
        // Only add if not already present to avoid duplicates
        if (relation != null && !conceptualRelations.contains(relation)) {
            this.conceptualRelations.add(relation);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemanticNode that = (SemanticNode) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Node{" +
               "name='" + name + '\'' +
               ", activation=" + String.format("%.2f", activation) +
               ", isNegated=" + isNegated +
               (temporalInfo != null ? ", temporal=" + temporalInfo.getRawTemporalExpression() : "") +
               ", relations=" + conceptualRelations.size() + // Show count of relations
               '}';
    }
}
