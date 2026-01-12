package com.stormy.ai;

import com.stormy.ai.models.AnswerResult;
import com.stormy.ai.models.Rule;
import com.stormy.ai.models.SemanticNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the learning aspects of the AI: adaptive parameters and rule generation.
 */
public class LearningEngine {

    private final StringBuilder reasoningSummary;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;

    public LearningEngine(StringBuilder reasoningSummary) {
        this.reasoningSummary = reasoningSummary;
    }

    public void considerGeneratingRule(String context, String question, AnswerResult answer, 
                                     SemanticNetwork network, RuleEngine ruleEngine, 
                                     double activationThreshold) {
        
        if (answer.getConfidence() < HIGH_CONFIDENCE_THRESHOLD) return;

        reasoningSummary.append("\n[Learning] High confidence answer detected. Generating rules...\n");

        List<String> answerKws = TextUtils.tokenize(answer.getAnswer()).stream()
                .map(TextUtils::stem)
                .filter(s -> network.getNode(s) != null && network.getNode(s).getActivation() >= activationThreshold)
                .collect(Collectors.toList());

        List<String> questionKws = TextUtils.tokenize(question).stream()
                .map(TextUtils::stem)
                .filter(s -> network.getNode(s) != null && network.getNode(s).getActivation() >= activationThreshold)
                .collect(Collectors.toList());

        if (questionKws.isEmpty() || answerKws.isEmpty()) return;

        for (String qk : questionKws) {
            if (TextUtils.isStopWord(qk)) continue;
            for (String ak : answerKws) {
                if (qk.equals(ak) || TextUtils.isStopWord(ak)) continue;

                Set<String> conditions = new HashSet<>();
                conditions.add(qk);
                
                Rule newRule = new Rule(conditions, ak, answer.getConfidence() * 0.9,
                        "Learned: '" + qk + "' correlates with '" + ak + "'");

                ruleEngine.addRule(newRule);
            }
        }
    }

    public double[] adjustParameters(List<Double> confidenceHistory, double currentThreshold, double currentDecay) {
        if (confidenceHistory.size() < 5) return new double[]{currentThreshold, currentDecay};

        double avg = confidenceHistory.stream().mapToDouble(d -> d).average().orElse(0.5);
        double newThreshold = currentThreshold;
        double newDecay = currentDecay;

        if (avg < 0.3) {
            newThreshold = Math.min(0.2, currentThreshold + 0.01);
            newDecay = Math.max(0.05, currentDecay - 0.005);
        } else if (avg > 0.8) {
            newThreshold = Math.max(0.05, currentThreshold - 0.005);
            newDecay = Math.min(0.2, currentDecay + 0.005);
        }

        return new double[]{newThreshold, newDecay};
    }
}
