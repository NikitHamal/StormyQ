package com.stormy.ai;

import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.SemanticEdge;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Optimized spreading activation implementation.
 */
public class SpreadingActivator {

    private final SemanticNetwork network;
    private final StringBuilder reasoning;

    private double initialActivation;
    private double activationThreshold;
    private int maxIterations;
    private double negationEffect;
    private double conceptBoost;
    private double sentimentBoost;

    public SpreadingActivator(SemanticNetwork network, StringBuilder reasoning,
                              double initialActivation, double threshold, int maxIterations,
                              double negationEffect, double conceptBoost, double sentimentBoost) {
        this.network = network;
        this.reasoning = reasoning;
        this.initialActivation = initialActivation;
        this.activationThreshold = threshold;
        this.maxIterations = maxIterations;
        this.negationEffect = negationEffect;
        this.conceptBoost = conceptBoost;
        this.sentimentBoost = sentimentBoost;
    }

    public void activate(List<String> seeds, int qSentiment) {
        network.resetActivations();
        Queue<SemanticNode> queue = new LinkedList<>();

        for (String seed : seeds) {
            SemanticNode node = network.getNode(seed);
            if (node != null) {
                node.setActivation(initialActivation * (node.isNegated() ? (1.0 - negationEffect) : 1.0));
                node.setActivatedThisCycle(true);
                queue.offer(node);
            }
        }

        int iter = 0;
        while (!queue.isEmpty() && iter < maxIterations) {
            int levelSize = queue.size();
            List<SemanticNode> nextLevel = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                SemanticNode current = queue.poll();
                if (current == null || current.getActivation() < activationThreshold) continue;

                current.applyDecay();
                spreadToNeighbors(current, nextLevel, qSentiment);
                spreadViaRelations(current, nextLevel, qSentiment);
            }

            for (SemanticNode n : nextLevel) {
                n.setActivatedThisCycle(true);
                queue.offer(n);
            }
            
            // Reset flags for next iteration
            for (SemanticNode n : network.getAllNodes()) n.setActivatedThisCycle(false);
            iter++;
        }
    }

    private void spreadToNeighbors(SemanticNode source, List<SemanticNode> next, int qSentiment) {
        List<SemanticEdge> edges = network.getEdgesFromNode(source);
        if (edges == null) return;

        for (SemanticEdge edge : edges) {
            SemanticNode target = edge.getTarget();
            double amount = source.getActivation() * edge.getWeight();

            if (target.isNegated()) amount *= (1.0 - negationEffect);
            
            int tSentiment = TextUtils.getSentimentScore(target.getName());
            if (qSentiment != 0 && tSentiment != 0) {
                if (Integer.signum(qSentiment) == Integer.signum(tSentiment)) amount *= (1.0 + sentimentBoost);
                else amount *= (1.0 - sentimentBoost);
            }

            if (amount >= activationThreshold) {
                target.increaseActivation(amount);
                if (!target.isActivatedThisCycle()) next.add(target);
            }
        }
    }

    private void spreadViaRelations(SemanticNode source, List<SemanticNode> next, int qSentiment) {
        for (ConceptRelation rel : source.getConceptualRelations()) {
            String targetName = rel.getSourceConcept().equals(source.getName()) ? rel.getTargetConcept() : rel.getSourceConcept();
            SemanticNode target = network.getNode(targetName);
            
            if (target != null && !target.equals(source)) {
                double amount = source.getActivation() * rel.getStrength() * conceptBoost;
                
                if (amount >= activationThreshold) {
                    target.increaseActivation(amount);
                    if (!target.isActivatedThisCycle()) next.add(target);
                }
            }
        }
    }

    public void setActivationThreshold(double threshold) { this.activationThreshold = threshold; }
}