package com.stormy.ai;

import com.stormy.ai.models.Rule;
import com.stormy.ai.models.SemanticNode;

import java.util.ArrayList;
import java.util.Arrays;   // <--- ADDED THIS IMPORT
import java.util.HashSet;  // <--- ADDED THIS IMPORT
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages a collection of probabilistic rules and applies them to a semantic network.
 * Rules consist of conditions (activated semantic nodes) and a consequence (another semantic node)
 * with a confidence score.
 */
public class RuleEngine {

    private List<Rule> rules; // The collection of probabilistic rules
    private StringBuilder reasoningSummary; // For logging the rule evaluation process
    private double activationThreshold; // Needs to know the global activation threshold

    /**
     * Constructs a RuleEngine.
     * @param reasoningSummary A StringBuilder to append reasoning logs.
     * @param initialActivationThreshold The initial activation threshold from QnAProcessor.
     */
    public RuleEngine(StringBuilder reasoningSummary, double initialActivationThreshold) {
        this.rules = new ArrayList<>();
        this.reasoningSummary = reasoningSummary;
        this.activationThreshold = initialActivationThreshold;
        initializeDefaultRules(); // Populate with some default rules
    }

    /**
     * Initializes a set of default probabilistic rules for the system.
     * These rules represent basic inferential knowledge.
     */
    private void initializeDefaultRules() {
        // Example Rule: If something is "fast" AND "vehicle", it implies "speed"
        Set<String> conditions1 = new HashSet<>(Arrays.asList(TextUtils.stem("fast"), TextUtils.stem("vehicle")));
        rules.add(new Rule(conditions1, TextUtils.stem("speed"), 0.8, "Fast vehicle implies speed"));

        // Example Rule: If someone "built" something in a "year", it implies "creation"
        Set<String> conditions2 = new HashSet<>(Arrays.asList(TextUtils.stem("built"), TextUtils.stem("year")));
        rules.add(new Rule(conditions2, TextUtils.stem("create"), 0.75, "Built in year implies creation"));

        // Example Rule: If something is "large" and "animal", it implies "big"
        Set<String> conditions3 = new HashSet<>(Arrays.asList(TextUtils.stem("large"), TextUtils.stem("animal")));
        rules.add(new Rule(conditions3, TextUtils.stem("big"), 0.9, "Large animal implies big"));

        // Example Rule: If "water" and "cold", implies "ice"
        Set<String> conditions4 = new HashSet<>(Arrays.asList(TextUtils.stem("water"), TextUtils.stem("cold")));
        rules.add(new Rule(conditions4, TextUtils.stem("ice"), 0.6, "Cold water implies ice"));

        // Example Rule: If good and service, implies economy
        Set<String> conditions5 = new HashSet<>(Arrays.asList(TextUtils.stem("good"), TextUtils.stem("service")));
        rules.add(new Rule(conditions5, TextUtils.stem("economy"), 0.7, "Good and service imply economy"));
    }


    /**
     * Evaluates probabilistic rules based on the current activation state of the network.
     * If rule conditions are met, the consequence node's activation is boosted.
     * @param semanticNetwork The current SemanticNetwork instance.
     */
    public void evaluateRules(SemanticNetwork semanticNetwork) {
        reasoningSummary.append("\n--- Rule Evaluation ---\n");
        boolean ruleFired = false;
        for (Rule rule : rules) {
            boolean conditionsMet = true;
            double minConditionActivation = 1.0; // Smallest activation among conditions

            for (String conditionStem : rule.getConditions()) {
                SemanticNode conditionNode = semanticNetwork.getNode(conditionStem);
                if (conditionNode == null || conditionNode.getActivation() < activationThreshold) {
                    conditionsMet = false;
                    break;
                }
                minConditionActivation = Math.min(minConditionActivation, conditionNode.getActivation());
            }

            if (conditionsMet) {
                SemanticNode consequenceNode = semanticNetwork.getNode(rule.getConsequence());
                if (consequenceNode != null) {
                    // Boost consequence activation based on rule confidence and condition activation
                    double boostAmount = minConditionActivation * rule.getConfidence();
                    consequenceNode.increaseActivation(boostAmount);
                    reasoningSummary.append(" - Rule Fired: '").append(rule.getDescription())
                                    .append("' -> Boosted '").append(consequenceNode.getName())
                                    .append("' by ").append(String.format("%.2f", boostAmount))
                                    .append(" (New Act: ").append(String.format("%.2f", consequenceNode.getActivation())).append(")\n");
                    ruleFired = true;
                }
            }
        }
        if (!ruleFired) {
            reasoningSummary.append(" - No rules fired in this cycle.\n");
        }
    }

    /**
     * Adds a new rule to the engine's collection.
     * @param rule The Rule object to add.
     */
    public void addRule(Rule rule) {
        if (rule != null) {
            // Check for exact duplicate conditions and consequence
            boolean exists = rules.stream().anyMatch(r ->
                r.getConditions().equals(rule.getConditions()) && r.getConsequence().equals(rule.getConsequence())
            );
            if (!exists) {
                rules.add(rule);
                reasoningSummary.append("\n[RuleEngine] New rule added: ").append(rule.getDescription());
            } else {
                reasoningSummary.append("\n[RuleEngine] Rule already exists, not added: ").append(rule.getDescription());
            }
        }
    }

    /**
     * Removes a rule from the engine's collection based on its description.
     * @param description The description of the rule to remove.
     * @return true if the rule was removed, false otherwise.
     */
    public boolean removeRuleByDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return false;
        }
        boolean removed = rules.removeIf(r -> r.getDescription().equalsIgnoreCase(description.trim()));
        if (removed) {
            reasoningSummary.append("\n[RuleEngine] Rule removed: ").append(description);
        }
        return removed;
    }

    /**
     * Returns a copy of the current list of rules.
     * @return A List of Rule objects.
     */
    public List<Rule> getRules() {
        return new ArrayList<>(rules); // Return a copy to prevent external modification
    }

    /**
     * Resets the rules to their default state.
     */
    public void resetRulesToDefaults() {
        rules.clear();
        initializeDefaultRules();
        reasoningSummary.append("\n[RuleEngine] Rules reset to defaults.\n");
    }

    /**
     * Sets the activation threshold used for evaluating rule conditions.
     * @param activationThreshold The new activation threshold.
     */
    public void setActivationThreshold(double activationThreshold) {
        this.activationThreshold = activationThreshold;
    }
}
