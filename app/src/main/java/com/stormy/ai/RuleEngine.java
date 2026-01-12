package com.stormy.ai;

import com.stormy.ai.models.Rule;
import com.stormy.ai.models.SemanticNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages and evaluates probabilistic rules.
 */
public class RuleEngine {

    private final List<Rule> rules = new ArrayList<>();
    private final StringBuilder reasoning;
    private double activationThreshold;

    public RuleEngine(StringBuilder reasoning, double threshold) {
        this.reasoning = reasoning;
        this.activationThreshold = threshold;
        initializeDefaultRules();
    }

    private void initializeDefaultRules() {
        addRule(new Rule(new HashSet<>(Arrays.asList(TextUtils.stem("fast"), TextUtils.stem("vehicle"))), 
                TextUtils.stem("speed"), 0.8, "Fast vehicle implies speed"));
        addRule(new Rule(new HashSet<>(Arrays.asList(TextUtils.stem("built"), TextUtils.stem("year"))), 
                TextUtils.stem("create"), 0.75, "Built in year implies creation"));
        addRule(new Rule(new HashSet<>(Arrays.asList(TextUtils.stem("water"), TextUtils.stem("cold"))), 
                TextUtils.stem("ice"), 0.6, "Cold water implies ice"));
    }

    public void evaluateRules(SemanticNetwork network) {
        reasoning.append("\n--- Rule Evaluation ---\n");
        for (Rule rule : rules) {
            double minAct = Double.MAX_VALUE;
            boolean possible = true;

            for (String cond : rule.getConditions()) {
                SemanticNode node = network.getNode(cond);
                if (node == null || node.getActivation() < activationThreshold) {
                    possible = false;
                    break;
                }
                minAct = Math.min(minAct, node.getActivation());
            }

            if (possible) {
                SemanticNode target = network.getNode(rule.getConsequence());
                if (target != null) {
                    double boost = minAct * rule.getConfidence();
                    target.increaseActivation(boost);
                    reasoning.append(" - Fired: ").append(rule.getDescription()).append("\n");
                }
            }
        }
    }

    public void addRule(Rule rule) {
        if (rule == null) return;
        for (Rule r : rules) {
            if (r.getConditions().equals(rule.getConditions()) && r.getConsequence().equals(rule.getConsequence())) return;
        }
        rules.add(rule);
    }

    public List<Rule> getRules() { return new ArrayList<>(rules); }
    public void setActivationThreshold(double threshold) { this.activationThreshold = threshold; }
    public void resetRulesToDefaults() {
        rules.clear();
        initializeDefaultRules();
    }
}