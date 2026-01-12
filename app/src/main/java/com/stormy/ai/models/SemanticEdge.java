package com.stormy.ai.models;

import java.io.Serializable;

/**
 * Enhanced SemanticEdge with serialization and relation typing.
 */
public class SemanticEdge implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EdgeType { CO_OCCURRENCE, SUBJECT, OBJECT, ACTION }

    private final SemanticNode source;
    private final SemanticNode target;
    private double weight;
    private EdgeType type = EdgeType.CO_OCCURRENCE;

    public SemanticEdge(SemanticNode source, SemanticNode target, double weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    public SemanticNode getSource() { return source; }
    public SemanticNode getTarget() { return target; }
    public double getWeight() { return weight; }
    public EdgeType getType() { return type; }
    public void setType(EdgeType type) { this.type = type; }
    public void setWeight(double weight) { this.weight = weight; }

    @Override
    public String toString() {
        return "Edge{" +
               "source=" + source.getName() +
               ", target=" + target.getName() +
               ", weight=" + String.format("%.2f", weight) +
               '}';
    }
}
