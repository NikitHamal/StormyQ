package com.stormy.ai;

import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.SemanticEdge;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the core semantic network structure: nodes and their connections (edges).
 * Responsible for building the network from raw text, adding edges, and resetting node activations.
 */
public class SemanticNetwork {

    private Map<String, SemanticNode> nodes; // Map of stemmed word -> SemanticNode
    private Map<SemanticNode, List<SemanticEdge>> adjacencyList; // Adjacency list for edges

    // Parameters, some of which might come from QnAProcessor's adaptive logic
    private double decayRate; // Decay rate for nodes in this network
    private static final int CO_OCCURRENCE_WINDOW_SIZE = 5; // Words within this window form an edge
    private static final double NEGATION_EFFECT = 0.7; // Factor by which activation is reduced due to negation
    private static final int NEGATION_SCOPE_WORDS = 3; // How many words negation effect spans

    public SemanticNetwork(double initialDecayRate) {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.decayRate = initialDecayRate; // Initialize with a decay rate
    }

    /**
     * Sets the decay rate for all nodes in the network.
     * @param decayRate The new decay rate.
     */
    public void setDecayRate(double decayRate) {
        this.decayRate = decayRate;
        // Update existing nodes with the new decay rate
        for (SemanticNode node : nodes.values()) {
            node.setDecayRate(decayRate);
        }
    }

    /**
     * Retrieves a SemanticNode by its stemmed name.
     * @param name The stemmed name of the node.
     * @return The SemanticNode if found, null otherwise.
     */
    public SemanticNode getNode(String name) {
        return nodes.get(name);
    }

    /**
     * Returns a collection of all SemanticNodes in the network.
     * @return A List of SemanticNode objects.
     */
    public List<SemanticNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    /**
     * Returns the adjacency list for a given node, representing its outbound edges.
     * @param node The SemanticNode.
     * @return A List of SemanticEdge objects connected from the given node.
     */
    public List<SemanticEdge> getEdgesFromNode(SemanticNode node) {
        return adjacencyList.get(node);
    }

    /**
     * Builds the semantic network from the provided context text.
     * Each unique stemmed word becomes a node. Co-occurring words within a window
     * create/strengthen edges. Also identifies temporal info and negation.
     * @param context The text to build the network from.
     */
    public void buildNetwork(String context) {
        // Clear existing network for new context
        nodes.clear();
        adjacencyList.clear();

        List<String> rawTokens = TextUtils.tokenize(context);
        List<String> stemmedTokens = new ArrayList<>();
        // Track negation context: for how many words forward the negation applies
        List<Integer> negationScopeRemaining = new ArrayList<>(rawTokens.size());
        for (int i = 0; i < rawTokens.size(); i++) {
            negationScopeRemaining.add(0); // Initialize with no active negation scope
        }

        // First pass: Stem words, identify negation scope and temporal info
        for (int i = 0; i < rawTokens.size(); i++) {
            String rawToken = rawTokens.get(i);
            String stemmedToken = TextUtils.stem(rawToken);
            stemmedTokens.add(stemmedToken);

            // Determine negation scope from this token
            if (TextUtils.isNegationWord(rawToken)) {
                // Set negation scope for upcoming words
                for (int k = i + 1; k < Math.min(rawTokens.size(), i + 1 + NEGATION_SCOPE_WORDS); k++) {
                    // Stop negation scope at punctuation marks, but allow it to pass over stop words
                    if (rawTokens.get(k).matches("[.,;!?]")) {
                        break; // Punctuation ends negation scope
                    }
                    negationScopeRemaining.set(k, NEGATION_SCOPE_WORDS); // Mark subsequent words
                }
            }
            
            // If previous word initiated negation, decrement scope
            if (i > 0 && negationScopeRemaining.get(i-1) > 0) {
                negationScopeRemaining.set(i, negationScopeRemaining.get(i-1) - 1);
            }


            // Create nodes for each unique stemmed word
            if (!nodes.containsKey(stemmedToken)) {
                SemanticNode newNode = new SemanticNode(stemmedToken, decayRate); // Use network's decayRate
                nodes.put(stemmedToken, newNode);
                adjacencyList.put(newNode, new ArrayList<>());
            }

            // Assign temporal info if available
            TemporalInfo tempInfo = TextUtils.extractTemporalInfo(rawToken);
            if (tempInfo != null) {
                nodes.get(stemmedToken).setTemporalInfo(tempInfo);
            }
        }

        // Second pass: Create edges and apply initial negation status to nodes
        for (int i = 0; i < stemmedTokens.size(); i++) {
            SemanticNode sourceNode = nodes.get(stemmedTokens.get(i));
            if (sourceNode == null) continue;

            // Apply negation status to the node based on `negationScopeRemaining`
            // A node is negated if it's within the scope of a negation word
            if (negationScopeRemaining.get(i) > 0 || TextUtils.isNegationWord(rawTokens.get(i))) {
                sourceNode.setNegated(true);
            }

            for (int j = i + 1; j < Math.min(i + CO_OCCURRENCE_WINDOW_SIZE, stemmedTokens.size()); j++) {
                SemanticNode targetNode = nodes.get(stemmedTokens.get(j));
                if (targetNode == null || sourceNode.equals(targetNode)) continue;

                // An edge is negated if either of the connected nodes is negated
                boolean isEdgeNegated = sourceNode.isNegated() || targetNode.isNegated();

                // Add or strengthen edge (bidirectional)
                addEdge(sourceNode, targetNode, isEdgeNegated);
                addEdge(targetNode, sourceNode, isEdgeNegated); // Make it bidirectional
            }
        }
    }

    /**
     * Adds an edge between two nodes or strengthens an existing one.
     * The weight of the edge can be influenced by negation.
     * @param source The source node.
     * @param target The target node.
     * @param isNegated True if the edge is formed under a negation context.
     */
    public void addEdge(SemanticNode source, SemanticNode target, boolean isNegated) {
        boolean edgeExists = false;
        if (!adjacencyList.containsKey(source)) {
            adjacencyList.put(source, new ArrayList<>());
        }
        for (SemanticEdge edge : adjacencyList.get(source)) {
            if (edge.getTarget().equals(target)) {
                // Strengthen existing edge
                double newWeight = edge.getWeight() + 0.1;
                // Apply negation effect more directly here: if the edge is negated, its base strength is lower
                if (isNegated) {
                    newWeight *= (1.0 - NEGATION_EFFECT); // Reduce weight due to negation
                }
                edge.setWeight(newWeight);
                edgeExists = true;
                break;
            }
        }
        if (!edgeExists) {
            // Initial weight based on simple co-occurrence. Can be refined.
            double initialWeight = 0.5;
            if (isNegated) {
                initialWeight *= (1.0 - NEGATION_EFFECT); // Reduce initial weight
            }
            adjacencyList.get(source).add(new SemanticEdge(source, target, initialWeight));
        }
    }

    /**
     * Resets all node activations and the 'activatedThisCycle' flag for a new spreading activation run.
     */
    public void resetActivations() {
        for (SemanticNode node : nodes.values()) {
            node.setActivation(0.0);
            node.setActivatedThisCycle(false);
            // Do not reset isNegated or TemporalInfo as they are part of the network structure
        }
    }

    /**
     * Returns the number of nodes in the network.
     * @return The size of the nodes map.
     */
    public int getNodeCount() {
        return nodes.size();
    }
}
