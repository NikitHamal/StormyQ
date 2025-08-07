package com.stormy.ai.models; // Putting it in models package

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an edge (relationship) in the semantic network between two SemanticNodes.
 * It has a weight that determines the strength of the connection, influencing
 * how much activation is passed along this edge.
 */
public class SemanticEdge {
    private SemanticNode source; // The node from which the activation spreads
    private SemanticNode target; // The node to which the activation spreads
    private double weight; // The strength of this connection (e.g., based on co-occurrence frequency)
    private String type; // e.g., "is-a", "part-of", "causes", "enables", etc.
    private String label; // Human-readable label
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Constructor for a SemanticEdge.
     * @param source The source SemanticNode.
     * @param target The target SemanticNode.
     * @param weight The weight of the edge, indicating connection strength.
     */
    public SemanticEdge(SemanticNode source, SemanticNode target, double weight) {
        this(source, target, weight, "related-to", "", new HashMap<>());
    }
    public SemanticEdge(SemanticNode source, SemanticNode target, double weight, String type, String label, Map<String, Object> metadata) {
        this.source = source;
        this.target = target;
        this.weight = weight;
        this.type = type;
        this.label = label;
        if (metadata != null) this.metadata = metadata;
    }

    // --- Getters ---
    public SemanticNode getSource() {
        return source;
    }

    public SemanticNode getTarget() {
        return target;
    }

    public double getWeight() {
        return weight;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    // --- Setter for weight (for dynamic updates if needed) ---
    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Edge{" +
               "source=" + source.getName() +
               ", target=" + target.getName() +
               ", weight=" + String.format("%.2f", weight) +
               '}';
    }
}
