package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo;
import java.util.List;
import java.util.stream.Collectors;

public class AdvancedScorer {

    public void scoreCandidate(AnswerCandidate candidate, String question, SemanticNetwork semanticNetwork) {
        candidate.setSemanticScore(calculateSemanticProximity(candidate, question, semanticNetwork));
        candidate.setCompletenessScore(calculateCompleteness(candidate, question, semanticNetwork));
        candidate.setRelevanceScore(calculateContextualRelevance(candidate, semanticNetwork));
        candidate.setLengthScore(calculateLengthAppropriateness(candidate, question));
        candidate.setNegationScore(calculateNegationScore(candidate, question));
        candidate.setTemporalScore(calculateTemporalScore(candidate, question));

        // Define weights for each score component
        double semanticWeight = 0.25;
        double completenessWeight = 0.2;
        double relevanceWeight = 0.2;
        double lengthWeight = 0.1;
        double negationWeight = 0.15;
        double temporalWeight = 0.1;

        candidate.calculateFinalScore(semanticWeight, completenessWeight, relevanceWeight, lengthWeight, negationWeight, temporalWeight);

        System.out.println("Scored candidate: " + candidate.getText());
        System.out.println("  Semantic Score: " + candidate.getSemanticScore());
        System.out.println("  Completeness Score: " + candidate.getCompletenessScore());
        System.out.println("  Relevance Score: " + candidate.getRelevanceScore());
        System.out.println("  Length Score: " + candidate.getLengthScore());
        System.out.println("  Negation Score: " + candidate.getNegationScore());
        System.out.println("  Temporal Score: " + candidate.getTemporalScore());
        System.out.println("  Final Score: " + candidate.getFinalScore());
    }

    private double calculateSemanticProximity(AnswerCandidate candidate, String question, SemanticNetwork semanticNetwork) {
        List<String> questionKeywords = TextUtils.tokenize(question).stream()
                .map(TextUtils::stem)
                .filter(kw -> semanticNetwork.getNode(kw) != null)
                .collect(Collectors.toList());

        if (questionKeywords.isEmpty()) {
            return 0.0;
        }

        List<String> candidateTokens = TextUtils.tokenize(candidate.getText()).stream()
                .map(TextUtils::stem)
                .collect(Collectors.toList());

        long matchCount = questionKeywords.stream().filter(candidateTokens::contains).count();
        return (double) matchCount / questionKeywords.size();
    }

    private double calculateCompleteness(AnswerCandidate candidate, String question, SemanticNetwork semanticNetwork) {
        if (question.toLowerCase().contains(" and ") || question.toLowerCase().startsWith("what are")) {
            List<String> questionKeywords = TextUtils.tokenize(question).stream()
                    .map(TextUtils::stem)
                    .filter(kw -> semanticNetwork.getNode(kw) != null && semanticNetwork.getNode(kw).getActivation() > 0)
                    .collect(Collectors.toList());

            if (questionKeywords.size() < 2) {
                return 1.0; // Not a complex question
            }

            List<String> candidateTokens = TextUtils.tokenize(candidate.getText()).stream()
                    .map(TextUtils::stem)
                    .collect(Collectors.toList());

            long conceptsCovered = questionKeywords.stream().filter(candidateTokens::contains).count();
            return (double) conceptsCovered / questionKeywords.size();
        }
        return 1.0;
    }

    private double calculateContextualRelevance(AnswerCandidate candidate, SemanticNetwork semanticNetwork) {
        List<String> candidateTokens = TextUtils.tokenize(candidate.getText());
        if (candidateTokens.isEmpty()) {
            return 0.0;
        }
        double totalActivation = 0;
        int activatedTokenCount = 0;
        for (String token : candidateTokens) {
            String stemmedToken = TextUtils.stem(token);
            SemanticNode node = semanticNetwork.getNode(stemmedToken);
            if (node != null && node.getActivation() > 0) {
                totalActivation += node.getActivation();
                activatedTokenCount++;
            }
        }
        return activatedTokenCount > 0 ? totalActivation / activatedTokenCount : 0;
    }

    private double calculateLengthAppropriateness(AnswerCandidate candidate, String question) {
        int length = candidate.getText().length();
        int idealLength = 100; // Can be adjusted
        if (question.toLowerCase().contains("what is the definition of")) {
            idealLength = 150;
        }

        double diff = Math.abs(length - idealLength);
        return Math.max(0.0, 1.0 - (diff / idealLength));
    }

    private double calculateNegationScore(AnswerCandidate candidate, String question) {
        boolean questionHasNegation = TextUtils.tokenize(question).stream().anyMatch(TextUtils::isNegationWord);
        boolean candidateHasNegation = TextUtils.tokenize(candidate.getText()).stream().anyMatch(TextUtils::isNegationWord);

        return questionHasNegation == candidateHasNegation ? 1.0 : 0.1;
    }

    private double calculateTemporalScore(AnswerCandidate candidate, String question) {
        TemporalInfo questionTemporalInfo = TextUtils.extractTemporalInfo(question);
        TemporalInfo candidateTemporalInfo = TextUtils.extractTemporalInfo(candidate.getText());

        if (questionTemporalInfo == null) {
            return 1.0; // No temporal aspect in the question
        }
        if (candidateTemporalInfo == null) {
            return 0.5; // Question is temporal, but answer is not
        }

        if (questionTemporalInfo.isPointInTime() && candidateTemporalInfo.isPointInTime()) {
            return questionTemporalInfo.getStartTimeMillis().equals(candidateTemporalInfo.getStartTimeMillis()) ? 1.0 : 0.2;
        }
        // Basic duration overlap check
        if (questionTemporalInfo.isDuration() && candidateTemporalInfo.isDuration()) {
            long q_start = questionTemporalInfo.getStartTimeMillis();
            long q_end = questionTemporalInfo.getEndTimeMillis();
            long c_start = candidateTemporalInfo.getStartTimeMillis();
            long c_end = candidateTemporalInfo.getEndTimeMillis();
            if (q_start < c_end && c_start < q_end) {
                return 1.0;
            }
            return 0.2;
        }

        return 0.5; // Mismatch in temporal types
    }
}
