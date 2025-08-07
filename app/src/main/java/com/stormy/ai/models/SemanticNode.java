package com.stormy.ai.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // For Objects.hash and Objects.equals

/**
 * Represents a node in the semantic network. Each node corresponds to a unique word or concept.
 * It stores an activation level, which changes during the spreading activation process.
 * Enhanced to include temporal information, a flag for negation context,
 * and a list of conceptual relations this node participates in.
 */
public class SemanticNode {
    private String name; // The word/concept this node represents (e.g., "cat", "run", "animal")
    private double activation; // The current activation level of this node
    private double decayRate; // Rate at which activation decays per iteration
    private boolean activatedThisCycle; // Flag to prevent re-activating in the same cycle of spreading activation
    private TemporalInfo temporalInfo; // Temporal information associated with this node (e.g., "in 1990")
    private boolean isNegated; // Flag indicating if this node is in a negated context (e.g., "NOT good")
    private List<ConceptRelation> conceptualRelations; // Higher-level relations this node is involved in
    private List<TemporalInfo> temporalInfos = new ArrayList<>();

    /**
     * Constructor for a SemanticNode.
     * @param name The name (word/concept) of the node.
     * @param decayRate The rate at which the node's activation decays per step.
     */
    public SemanticNode(String name, double decayRate) {
        this.name = name;
        this.activation = 0.0;
        this.decayRate = decayRate; // Initialize with the network's decay rate
        this.activatedThisCycle = false;
        this.isNegated = false; // Default to not negated
        this.conceptualRelations = new ArrayList<>(); // Initialize empty list
    }

    // --- Getters ---
    public String getName() {
        return name;
    }

    public double getActivation() {
        return activation;
    }

    public boolean isActivatedThisCycle() {
        return activatedThisCycle;
    }

    public TemporalInfo getTemporalInfo() {
        return temporalInfos.isEmpty() ? null : temporalInfos.get(0);
    }

    public boolean isNegated() {
        return isNegated;
    }

    public List<ConceptRelation> getConceptualRelations() {
        return new ArrayList<>(conceptualRelations); // Return a copy to prevent external modification
    }

    public List<TemporalInfo> getTemporalInfos() {
        return temporalInfos;
    }

    // --- Setters / Modifiers ---

    public void setActivation(double activation) {
        this.activation = activation;
    }

    public void increaseActivation(double amount) {
        this.activation += amount;
        // Optionally cap activation at 1.0 to prevent runaway values
        if (this.activation > 1.0) {
            this.activation = 1.0;
        }
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
        if (temporalInfo != null) {
            if (temporalInfos.isEmpty()) temporalInfos.add(temporalInfo);
            else temporalInfos.set(0, temporalInfo);
        }
    }

    public void addTemporalInfo(TemporalInfo t) {
        if (t != null) temporalInfos.add(t);
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
