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
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

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
            List<TextUtils.TemporalInfoResult> tempInfos = TextUtils.extractAllTemporalInfo(rawToken);
            if (!tempInfos.isEmpty()) {
                for (TextUtils.TemporalInfoResult t : tempInfos) {
                    nodes.get(stemmedToken).addTemporalInfo(t.getTemporalInfo());
                }
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

    /**
     * Adds a new node to the network or updates an existing one with a new weight.
     * @param nodeName The name of the node to add or update.
     * @param weight The weight/activation to assign to the node.
     */
    public void addOrUpdateNode(String nodeName, double weight) {
        String stemmedName = TextUtils.stem(nodeName);
        
        if (!nodes.containsKey(stemmedName)) {
            // Create new node
            SemanticNode newNode = new SemanticNode(stemmedName, decayRate);
            newNode.setActivation(weight);
            nodes.put(stemmedName, newNode);
            adjacencyList.put(newNode, new ArrayList<>());
        } else {
            // Update existing node's activation
            SemanticNode existingNode = nodes.get(stemmedName);
            existingNode.setActivation(Math.max(existingNode.getActivation(), weight));
        }
    }

    /**
     * Adds an is-a (hypernym/hyponym) relationship between two nodes.
     */
    public void addIsARelationship(String child, String parent) {
        SemanticNode childNode = nodes.get(TextUtils.stem(child));
        SemanticNode parentNode = nodes.get(TextUtils.stem(parent));
        if (childNode != null && parentNode != null) {
            childNode.addParent(parentNode);
            parentNode.addChild(childNode);
        }
    }

    /**
     * Adds a part-of (meronym/holonym) relationship between two nodes.
     */
    public void addPartOfRelationship(String part, String whole) {
        SemanticNode partNode = nodes.get(TextUtils.stem(part));
        SemanticNode wholeNode = nodes.get(TextUtils.stem(whole));
        if (partNode != null && wholeNode != null) {
            partNode.addWhole(wholeNode);
            wholeNode.addPart(partNode);
        }
    }

    /**
     * Adds a fuzzy edge using SimMetrics similarity.
     */
    public void addFuzzyEdge(String source, String target, double minSimilarity) {
        StringMetric metric = StringMetrics.levenshtein();
        double sim = metric.compare(source, target);
        if (sim >= minSimilarity) {
            SemanticNode sourceNode = nodes.get(TextUtils.stem(source));
            SemanticNode targetNode = nodes.get(TextUtils.stem(target));
            if (sourceNode != null && targetNode != null) {
                addEdge(sourceNode, targetNode, false);
            }
        }
    }

    /**
     * Strengthens edges dynamically based on co-activation statistics.
     * (Call this after a spreading activation run.)
     */
    public void strengthenEdgesByCoactivation(List<String> activatedNodes) {
        for (int i = 0; i < activatedNodes.size(); i++) {
            for (int j = i + 1; j < activatedNodes.size(); j++) {
                SemanticNode n1 = nodes.get(TextUtils.stem(activatedNodes.get(i)));
                SemanticNode n2 = nodes.get(TextUtils.stem(activatedNodes.get(j)));
                if (n1 != null && n2 != null) {
                    addEdge(n1, n2, false);
                    addEdge(n2, n1, false);
                }
            }
        }
    }

    /**
     * Imports external knowledge as is-a or part-of relationships.
     * Accepts a list of triples: [type, source, target].
     * type = "is-a" or "part-of"
     */
    public void importExternalKnowledge(List<String[]> triples) {
        for (String[] triple : triples) {
            if (triple.length != 3) continue;
            String type = triple[0];
            String source = triple[1];
            String target = triple[2];
            if (type.equals("is-a")) addIsARelationship(source, target);
            if (type.equals("part-of")) addPartOfRelationship(source, target);
        }
    }

    /**
     * Extracts a subgraph containing only nodes and edges relevant to the given keywords.
     */
    public SemanticNetwork extractSubgraph(List<String> keywords) {
        SemanticNetwork subgraph = new SemanticNetwork(this.decayRate);
        for (String keyword : keywords) {
            String stem = TextUtils.stem(keyword);
            SemanticNode node = nodes.get(stem);
            if (node != null) {
                subgraph.nodes.put(stem, node);
                List<SemanticEdge> edges = adjacencyList.get(node);
                if (edges != null) subgraph.adjacencyList.put(node, new ArrayList<>(edges));
            }
        }
        return subgraph;
    }

    /**
     * Explains the reasoning path from source to target node (if exists).
     * Returns a list of node names and edge types traversed.
     */
    public List<String> explainPath(String source, String target) {
        List<String> path = new ArrayList<>();
        String src = TextUtils.stem(source);
        String tgt = TextUtils.stem(target);
        SemanticNode start = nodes.get(src);
        SemanticNode end = nodes.get(tgt);
        if (start == null || end == null) return path;
        // Simple BFS for path
        Map<SemanticNode, SemanticNode> prev = new HashMap<>();
        Queue<SemanticNode> queue = new LinkedList<>();
        Set<SemanticNode> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            SemanticNode curr = queue.poll();
            if (curr.equals(end)) break;
            List<SemanticEdge> edges = adjacencyList.get(curr);
            if (edges == null) continue;
            for (SemanticEdge edge : edges) {
                SemanticNode next = edge.getTarget();
                if (!visited.contains(next)) {
                    prev.put(next, curr);
                    queue.add(next);
                    visited.add(next);
                }
            }
        }
        // Reconstruct path
        if (!prev.containsKey(end)) return path;
        List<SemanticNode> nodePath = new ArrayList<>();
        for (SemanticNode at = end; at != null; at = prev.get(at)) nodePath.add(at);
        Collections.reverse(nodePath);
        for (int i = 0; i < nodePath.size() - 1; i++) {
            SemanticNode n1 = nodePath.get(i);
            SemanticNode n2 = nodePath.get(i + 1);
            List<SemanticEdge> edges = adjacencyList.get(n1);
            String edgeType = "";
            if (edges != null) {
                for (SemanticEdge edge : edges) {
                    if (edge.getTarget().equals(n2)) {
                        edgeType = edge.getType();
                        break;
                    }
                }
            }
            path.add(n1.getName() + " -[" + edgeType + "]-> " + n2.getName());
        }
        return path;
    }

    /**
     * Computes similarity between two nodes using their embeddings (if available).
     * Returns cosine similarity, or -1 if not available.
     */
    public double embeddingSimilarity(String nodeA, String nodeB) {
        SemanticNode a = nodes.get(TextUtils.stem(nodeA));
        SemanticNode b = nodes.get(TextUtils.stem(nodeB));
        if (a == null || b == null || a.getEmbedding() == null || b.getEmbedding() == null) return -1.0;
        float[] va = a.getEmbedding();
        float[] vb = b.getEmbedding();
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < va.length; i++) {
            dot += va[i] * vb[i];
            normA += va[i] * va[i];
            normB += vb[i] * vb[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
