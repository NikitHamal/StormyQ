package com.stormy.ai;

import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.SemanticEdge;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo; // Added import

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set; // For initial activators

/**
 * Implements the spreading activation algorithm over a given SemanticNetwork.
 * It manages activation propagation, decay, and applies various boosts/penalties
 * based on contextual factors like negation and sentiment.
 */
public class SpreadingActivator {

    private SemanticNetwork semanticNetwork;
    private StringBuilder reasoningSummary; // For logging the activation process

    // Parameters for spreading activation (can be made adaptive/configurable)
    private double initialActivation;
    private double activationThreshold;
    private int maxSpreadingIterations;
    private double negationEffect;
    private double conceptRelationBoost;
    private double sentimentBoostFactor;

    /**
     * Constructs a SpreadingActivator.
     * @param semanticNetwork The semantic network to perform activation on.
     * @param reasoningSummary A StringBuilder to append reasoning logs.
     * @param initialActivation Initial activation value for query keywords.
     * @param activationThreshold Minimum activation to spread.
     * @param maxSpreadingIterations Maximum iterations for activation spread.
     * @param negationEffect Factor to reduce activation due to negation.
     * @param conceptRelationBoost Boost for activation via conceptual relations.
     * @param sentimentBoostFactor Boost for matching sentiment.
     */
    public SpreadingActivator(SemanticNetwork semanticNetwork, StringBuilder reasoningSummary,
                              double initialActivation, double activationThreshold, int maxSpreadingIterations,
                              double negationEffect, double conceptRelationBoost, double sentimentBoostFactor) {
        this.semanticNetwork = semanticNetwork;
        this.reasoningSummary = reasoningSummary;
        this.initialActivation = initialActivation;
        this.activationThreshold = activationThreshold;
        this.maxSpreadingIterations = maxSpreadingIterations;
        this.negationEffect = negationEffect;
        this.conceptRelationBoost = conceptRelationBoost;
        this.sentimentBoostFactor = sentimentBoostFactor;
    }

    /**
     * Performs spreading activation from initial query keywords.
     * Activation spreads through the network, decreasing with distance and decay.
     * Negation reduces activation. Conceptual relations also influence spread.
     * @param initialActivators A list of stemmed words to initially activate.
     * @param questionSentiment The sentiment score of the question.
     */
    public void activate(List<String> initialActivators, int questionSentiment) {
        semanticNetwork.resetActivations(); // Reset all node activations before a new run
        reasoningSummary.append("\n--- Spreading Activation Process ---\n");
        reasoningSummary.append("Initial Activators: ").append(initialActivators).append("\n");
        reasoningSummary.append("Question Sentiment: ").append(questionSentiment).append("\n");

        Queue<SemanticNode> activationQueue = new LinkedList<>();

        // Extract temporal information from initial activators (assuming question context for now)
        List<TemporalInfo> queryTemporalIntervals = new ArrayList<>();
        for (String activator : initialActivators) {
            List<TextUtils.TemporalInfoResult> tempResults = TextUtils.extractAllTemporalInfo(activator);
            for (TextUtils.TemporalInfoResult t : tempResults) {
                queryTemporalIntervals.add(t.getTemporalInfo());
                reasoningSummary.append("Query Temporal Info Detected (from initial activators): ").append(t.getTemporalInfo().getRawTemporalExpression()).append("\n");
            }
        }


        // Initialize activation for query keywords
        for (String activator : initialActivators) {
            SemanticNode node = semanticNetwork.getNode(activator);
            if (node != null) {
                // If the initial activator itself is in a negated context, its initial activation might be lower
                node.setActivation(initialActivation * (node.isNegated() ? (1.0 - negationEffect) : 1.0));
                node.setActivatedThisCycle(true); // Mark as activated in this cycle
                activationQueue.offer(node);
                reasoningSummary.append(" - Activated '").append(node.getName()).append("' with initial score: ")
                                .append(String.format("%.2f", node.getActivation())).append("\n");
            }
        }

        int iterations = 0;
        while (!activationQueue.isEmpty() && iterations < maxSpreadingIterations) {
            int currentQueueSize = activationQueue.size();
            List<SemanticNode> nextCycleActivations = new ArrayList<>(); // Nodes to process in next iteration
            reasoningSummary.append("\n  --- Iteration ").append(iterations + 1).append(" ---\n");

            for (int i = 0; i < currentQueueSize; i++) {
                SemanticNode currentNode = activationQueue.poll();

                if (currentNode == null || currentNode.getActivation() < activationThreshold) {
                    continue; // Skip if node is null or activation is too low
                }

                reasoningSummary.append("    Spreading from '").append(currentNode.getName())
                                .append("' (Activation: ").append(String.format("%.2f", currentNode.getActivation())).append("):\n");

                // Apply decay to the current node *after* it has spread its activation
                currentNode.applyDecay();

                // Spread activation to neighbors via co-occurrence edges
                List<SemanticEdge> edges = semanticNetwork.getEdgesFromNode(currentNode);
                if (edges != null) {
                    for (SemanticEdge edge : edges) {
                        SemanticNode targetNode = edge.getTarget();
                        double activationToSpread = currentNode.getActivation() * edge.getWeight();

                        // Apply negation effect if the target node or the edge implies negation
                        if (targetNode.isNegated()) { // Node is explicitly marked as negated
                             activationToSpread *= (1.0 - negationEffect);
                             reasoningSummary.append("      -> Negation effect applied to '").append(targetNode.getName()).append("'\n");
                        } else if (edge.getWeight() < 0.5 * (1.0 - negationEffect)) { // Heuristic: very low edge weight due to strong negation on connection
                             activationToSpread *= (1.0 - negationEffect * 0.5); // Less severe than direct node negation
                             reasoningSummary.append("      -> Indirect negation effect applied to '").append(targetNode.getName()).append("'\n");
                        }

                        // Apply sentiment boost/penalty for target node
                        int targetNodeSentiment = TextUtils.getSentimentScore(targetNode.getName());
                        if (questionSentiment != 0 && targetNodeSentiment != 0) {
                            if ((questionSentiment > 0 && targetNodeSentiment > 0) || (questionSentiment < 0 && targetNodeSentiment < 0)) {
                                activationToSpread *= (1.0 + sentimentBoostFactor); // Boost if sentiment matches
                                reasoningSummary.append("      -> Sentiment Match Boost for '").append(targetNode.getName()).append("'\n");
                            } else {
                                activationToSpread *= (1.0 - sentimentBoostFactor); // Penalty if sentiment mismatches
                                reasoningSummary.append("      -> Sentiment Mismatch Penalty for '").append(targetNode.getName()).append("'\n");
                            }
                        }

                        // Temporal relevance boost for target node
                        if (!queryTemporalIntervals.isEmpty() && targetNode.getTemporalInfos() != null && !targetNode.getTemporalInfos().isEmpty()) {
                            boolean anyMatch = false;
                            for (TemporalInfo q : queryTemporalIntervals) {
                                for (TemporalInfo t : targetNode.getTemporalInfos()) {
                                    String rel = TemporalReasoner.getTemporalRelation(t, q);
                                    if (rel.equals("during") || rel.equals("equal") || rel.equals("contains") || rel.equals("overlaps")) {
                                        activationToSpread *= (1.0 + conceptRelationBoost * 0.5); // Use a portion of conceptual boost
                                        reasoningSummary.append("      -> Temporal Match/Overlap Boost for '").append(targetNode.getName()).append("' (relation: ").append(rel).append(")\n");
                                        anyMatch = true;
                                        break;
                                    }
                                }
                                if (anyMatch) break;
                            }
                        }


                        if (activationToSpread >= activationThreshold) {
                            double oldActivation = targetNode.getActivation();
                            targetNode.increaseActivation(activationToSpread);

                            // Only add to next cycle if activation significantly increased and not processed yet
                            if (targetNode.getActivation() > oldActivation + activationThreshold / 2 && !targetNode.isActivatedThisCycle()) {
                                nextCycleActivations.add(targetNode);
                                targetNode.setActivatedThisCycle(true); // Mark for current cycle
                                reasoningSummary.append("      -> Activated '").append(targetNode.getName())
                                                .append("' via co-occurrence (new act: ").append(String.format("%.2f", targetNode.getActivation())).append(")\n");
                            }
                        }
                    }
                }

                // Spread activation via conceptual relations associated with the current node
                for (ConceptRelation relation : currentNode.getConceptualRelations()) {
                    SemanticNode relatedNode = null;
                    // Check if current node is the source of the relation
                    if (relation.getSourceConcept().equals(currentNode.getName())) {
                        relatedNode = semanticNetwork.getNode(relation.getTargetConcept());
                    }
                    // Check if current node is the target of the relation (for reciprocal reasoning)
                    else if (relation.getTargetConcept().equals(currentNode.getName())) {
                        relatedNode = semanticNetwork.getNode(relation.getSourceConcept());
                    }

                    if (relatedNode != null && !relatedNode.equals(currentNode)) {
                        // Use relation strength to determine activation from relation
                        double activationFromRelation = currentNode.getActivation() * relation.getStrength() * conceptRelationBoost;

                        // Apply sentiment boost/penalty for related node
                        int relatedNodeSentiment = TextUtils.getSentimentScore(relatedNode.getName());
                        if (questionSentiment != 0 && relatedNodeSentiment != 0) {
                            if ((questionSentiment > 0 && relatedNodeSentiment > 0) || (questionSentiment < 0 && relatedNodeSentiment < 0)) {
                                activationFromRelation *= (1.0 + sentimentBoostFactor);
                                reasoningSummary.append("      -> Sentiment Match Boost for '").append(relatedNode.getName()).append("'\n");
                            } else {
                                activationFromRelation *= (1.0 - sentimentBoostFactor);
                                reasoningSummary.append("      -> Sentiment Mismatch Penalty for '").append(relatedNode.getName()).append("'\n");
                            }
                        }

                        // Temporal relevance boost for related node
                        if (queryTemporalIntervals.isEmpty() && relatedNode.getTemporalInfos() != null && !relatedNode.getTemporalInfos().isEmpty()) {
                            boolean anyMatch = false;
                            for (TemporalInfo q : queryTemporalIntervals) {
                                for (TemporalInfo t : relatedNode.getTemporalInfos()) {
                                    String rel = TemporalReasoner.getTemporalRelation(t, q);
                                    if (rel.equals("during") || rel.equals("equal") || rel.equals("contains") || rel.equals("overlaps")) {
                                        activationFromRelation *= (1.0 + conceptRelationBoost * 0.5);
                                        reasoningSummary.append("      -> Temporal Match/Overlap Boost for '").append(relatedNode.getName()).append("' (relation: ").append(rel).append(")\n");
                                        anyMatch = true;
                                        break;
                                    }
                                }
                                if (anyMatch) break;
                            }
                        }

                        if (activationFromRelation >= activationThreshold) {
                            double oldActivation = relatedNode.getActivation();
                            relatedNode.increaseActivation(activationFromRelation);
                             if (relatedNode.getActivation() > oldActivation + activationThreshold / 2 && !relatedNode.isActivatedThisCycle()) {
                                nextCycleActivations.add(relatedNode);
                                relatedNode.setActivatedThisCycle(true);
                                reasoningSummary.append("      -> Activated '").append(relatedNode.getName())
                                                .append("' via ").append(relation.getType().name()).append(" relation (new act: ")
                                                .append(String.format("%.2f", relatedNode.getActivation())).append(")\n");
                            }
                        }
                    }
                }
            }
            // Add nodes activated in this cycle to the queue for the next iteration
            for (SemanticNode node : nextCycleActivations) {
                activationQueue.offer(node);
            }

            iterations++;
            // Reset activatedThisCycle for nodes that were processed in this iteration so they can be re-activated
            // in subsequent iterations if their activation is sufficiently boosted again.
            for (SemanticNode node : semanticNetwork.getAllNodes()) { // Iterate through all nodes to reset flags
                node.setActivatedThisCycle(false);
            }
        }

        // Log final activated nodes for summary
        reasoningSummary.append("\n--- Top Activated Nodes After Spreading Activation ---\n");
        semanticNetwork.getAllNodes().stream()
             .filter(node -> node.getActivation() >= activationThreshold)
             .sorted(java.util.Comparator.comparing(SemanticNode::getActivation).reversed())
             .limit(10)
             .forEach(node -> reasoningSummary.append(" - ").append(node.getName())
                                              .append(": ").append(String.format("%.2f", node.getActivation()))
                                              .append(node.isNegated() ? " (Negated)" : "")
                                              .append(node.getTemporalInfo() != null ? " (Temporal: " + node.getTemporalInfo().getRawTemporalExpression() + ")" : "")
                                              .append("\n"));
    }

    /**
     * Helper method to check if two temporal ranges overlap.
     * @param info1 First TemporalInfo object.
     * @param info2 Second TemporalInfo object.
     * @return True if they overlap, false otherwise.
     */
    private boolean doTimeRangesOverlap(TemporalInfo info1, TemporalInfo info2) {
        if (info1.getStartTimeMillis() == null || info1.getEndTimeMillis() == null ||
            info2.getStartTimeMillis() == null || info2.getEndTimeMillis() == null) {
            return false; // Cannot determine overlap if times are null
        }

        long start1 = info1.getStartTimeMillis();
        long end1 = info1.getEndTimeMillis();
        long start2 = info2.getStartTimeMillis();
        long end2 = info2.getEndTimeMillis();

        // Check for overlap: (Start1 <= End2) AND (Start2 <= End1)
        return start1 <= end2 && start2 <= end1;
    }


    // --- Getters and Setters for parameters (if adjustable from outside) ---
    public double getInitialActivation() {
        return initialActivation;
    }

    public void setInitialActivation(double initialActivation) {
        this.initialActivation = initialActivation;
    }

    public double getActivationThreshold() {
        return activationThreshold;
    }

    public void setActivationThreshold(double activationThreshold) {
        this.activationThreshold = activationThreshold;
    }

    public int getMaxSpreadingIterations() {
        return maxSpreadingIterations;
    }

    public void setMaxSpreadingIterations(int maxSpreadingIterations) {
        // Corrected typo here:
        this.maxSpreadingIterations = maxSpreadingIterations;
    }

    public double getNegationEffect() {
        return negationEffect;
    }

    public void setNegationEffect(double negationEffect) {
        this.negationEffect = negationEffect;
    }

    public double getConceptRelationBoost() {
        return conceptRelationBoost;
    }

    public void setConceptRelationBoost(double conceptRelationBoost) {
        this.conceptRelationBoost = conceptRelationBoost;
    }

    public double getSentimentBoostFactor() {
        return sentimentBoostFactor;
    }

    public void setSentimentBoostFactor(double sentimentBoostFactor) {
        this.sentimentBoostFactor = sentimentBoostFactor;
    }
}
