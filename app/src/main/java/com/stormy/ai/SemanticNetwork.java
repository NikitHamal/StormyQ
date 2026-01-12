package com.stormy.ai;

import com.stormy.ai.models.SemanticEdge;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimized semantic network construction and management.
 */
public class SemanticNetwork {

    private final Map<String, SemanticNode> nodes = new HashMap<>();
    private final Map<SemanticNode, List<SemanticEdge>> adjacencyList = new HashMap<>();

    private double decayRate;
    private static final int WINDOW_SIZE = 5;

    public SemanticNetwork(double initialDecayRate) {
        this.decayRate = initialDecayRate;
    }

    public void buildNetwork(String context) {
        nodes.clear();
        adjacencyList.clear();

        List<String> rawTokens = TextUtils.tokenize(context);
        int size = rawTokens.size();
        
        // Pre-process nodes and negation
        SemanticNode[] tokenNodes = new SemanticNode[size];
        int[] negationScope = new int[size];

        for (int i = 0; i < size; i++) {
            String raw = rawTokens.get(i);
            String stem = TextUtils.stem(raw);
            
            SemanticNode node = nodes.computeIfAbsent(stem, k -> new SemanticNode(k, decayRate));
            tokenNodes[i] = node;

            if (TextUtils.isNegationWord(raw)) {
                for (int k = i + 1; k < Math.min(size, i + 4); k++) {
                    if (rawTokens.get(k).matches("[.,;!?]")) break;
                    negationScope[k] = 3;
                }
            }
            
            TemporalInfo temp = TextUtils.extractTemporalInfo(raw);
            if (temp != null) node.setTemporalInfo(temp);
        }

        // Build edges
        for (int i = 0; i < size; i++) {
            SemanticNode source = tokenNodes[i];
            if (negationScope[i] > 0) source.setNegated(true);

            for (int j = i + 1; j < Math.min(i + WINDOW_SIZE, size); j++) {
                SemanticNode target = tokenNodes[j];
                if (source.equals(target)) continue;

                boolean negated = source.isNegated() || target.isNegated();
                addEdgeInternal(source, target, negated);
                addEdgeInternal(target, source, negated);
            }
        }
    }

    private void addEdgeInternal(SemanticNode source, SemanticNode target, boolean negated) {
        List<SemanticEdge> edges = adjacencyList.computeIfAbsent(source, k -> new ArrayList<>());
        for (SemanticEdge edge : edges) {
            if (edge.getTarget().equals(target)) {
                edge.setWeight(edge.getWeight() + (negated ? 0.03 : 0.1));
                return;
            }
        }
        edges.add(new SemanticEdge(source, target, negated ? 0.2 : 0.5));
    }

    public SemanticNode getNode(String name) { return nodes.get(name); }
    public List<SemanticNode> getAllNodes() { return new ArrayList<>(nodes.values()); }
    public List<SemanticEdge> getEdgesFromNode(SemanticNode node) { return adjacencyList.get(node); }
    public int getNodeCount() { return nodes.size(); }

    public void resetActivations() {
        for (SemanticNode node : nodes.values()) {
            node.setActivation(0.0);
            node.setActivatedThisCycle(false);
        }
    }

    public void setDecayRate(double decayRate) {
        this.decayRate = decayRate;
        for (SemanticNode node : nodes.values()) node.setDecayRate(decayRate);
    }
}