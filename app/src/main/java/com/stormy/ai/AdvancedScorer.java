package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Calculates the multi-dimensional scores for an AnswerCandidate.
 */
public class AdvancedScorer {

    /**
     * Scores an answer candidate based on various dimensions.
     * @param candidate The answer candidate to score.
     * @param question The question being answered.
     * @param semanticNetwork The semantic network representing the context.
     */
    public void scoreCandidate(AnswerCandidate candidate, String question, SemanticNetwork semanticNetwork) {
        candidate.setSemanticScore(calculateSemanticProximity(candidate, question, semanticNetwork));
        candidate.setCompletenessScore(calculateCompleteness(candidate, question, semanticNetwork));
        candidate.setRelevanceScore(calculateContextualRelevance(candidate, semanticNetwork));
        candidate.setLengthScore(calculateLengthAppropriateness(candidate, question));
        candidate.setNegationScore(calculateNegationScore(candidate, question));
        candidate.setTemporalScore(calculateTemporalScore(candidate, question));

        // Define weights for each score component
        double semanticWeight = 0.35;
        double completenessWeight = 0.25;
        double relevanceWeight = 0.15;
        double lengthWeight = 0.05;
        double negationWeight = 0.1;
        double temporalWeight = 0.1;

        candidate.calculateFinalScore(semanticWeight, completenessWeight, relevanceWeight, lengthWeight, negationWeight, temporalWeight);
    }

    /**
     * Calculates the semantic proximity of the candidate to the question.
     * @param candidate The answer candidate.
     * @param question The question.
     * @param semanticNetwork The semantic network.
     * @return The semantic proximity score.
     */
    private double calculateSemanticProximity(AnswerCandidate candidate, String question, SemanticNetwork semanticNetwork) {
        List<String> questionKeywords = TextUtils.tokenize(question).stream()
                .map(TextUtils::stem)
                .filter(kw -> semanticNetwork.getNode(kw) != null && semanticNetwork.getNode(kw).getActivation() > 0)
                .collect(Collectors.toList());

        if (questionKeywords.isEmpty()) {
            return 0.0;
        }

        List<String> candidateTokens = TextUtils.tokenize(candidate.getText()).stream()
                .map(TextUtils::stem)
                .collect(Collectors.toList());

        double totalActivationOfMatches = 0;
        double totalQuestionActivation = 0;

        for (String qk : questionKeywords) {
            SemanticNode node = semanticNetwork.getNode(qk);
            if (node != null) {
                totalQuestionActivation += node.getActivation();
                if (candidateTokens.contains(qk)) {
                    totalActivationOfMatches += node.getActivation();
                }
            }
        }

        return totalQuestionActivation > 0 ? totalActivationOfMatches / totalQuestionActivation : 0.0;
    }

    /**
     * Calculates the completeness of the answer.
     * @param candidate The answer candidate.
     * @param question The question.
     * @param semanticNetwork The semantic network.
     * @return The completeness score.
     */
    private double calculateCompleteness(AnswerCandidate candidate, String question, SemanticNetwork semanticNetwork) {
        List<String> questionKeywords = TextUtils.tokenize(question).stream()
                .map(TextUtils::stem)
                .filter(kw -> semanticNetwork.getNode(kw) != null && semanticNetwork.getNode(kw).getActivation() > 0.1) // Higher threshold for important keywords
                .collect(Collectors.toList());

        if (questionKeywords.size() <= 1) {
            return 1.0; // Not a complex question
        }

        List<String> candidateTokens = TextUtils.tokenize(candidate.getText()).stream()
                .map(TextUtils::stem)
                .collect(Collectors.toList());

        long conceptsCovered = questionKeywords.stream().distinct().filter(candidateTokens::contains).count();
        double completeness = (double) conceptsCovered / questionKeywords.stream().distinct().count();

        // Boost if the question implies a list and the answer provides a list-like structure
        if (question.toLowerCase().startsWith("what are") && (candidate.getText().contains(",") || candidate.getText().contains(" and "))) {
            completeness = Math.min(1.0, completeness * 1.2);
        }

        return completeness;
    }

    /**
     * Calculates the contextual relevance of the answer.
     * @param candidate The answer candidate.
     * @param semanticNetwork The semantic network.
     * @return The contextual relevance score.
     */
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

    /**
     * Calculates the appropriateness of the answer's length.
     * @param candidate The answer candidate.
     * @param question The question.
     * @return The length appropriateness score.
     */
    private double calculateLengthAppropriateness(AnswerCandidate candidate, String question) {
        int length = candidate.getText().length();
        int idealLength;

        if (question.toLowerCase().startsWith("who is") || question.toLowerCase().startsWith("what is the name")) {
            idealLength = 25;
        } else if (question.toLowerCase().contains("definition")) {
            idealLength = 150;
        } else if (question.toLowerCase().startsWith("what are")) {
            idealLength = 120;
        } else {
            idealLength = 80;
        }

        // Gaussian-like function for scoring length
        double score = Math.exp(-Math.pow(length - idealLength, 2) / (2 * Math.pow(idealLength * 0.5, 2)));
        return score;
    }

    /**
     * Calculates the negation score of the answer.
     * @param candidate The answer candidate.
     * @param question The question.
     * @return The negation score.
     */
    private double calculateNegationScore(AnswerCandidate candidate, String question) {
        boolean questionHasNegation = TextUtils.tokenize(question).stream().anyMatch(TextUtils::isNegationWord);
        boolean candidateHasNegation = TextUtils.tokenize(candidate.getText()).stream().anyMatch(TextUtils::isNegationWord);

        return questionHasNegation == candidateHasNegation ? 1.0 : 0.1;
    }

    /**
     * Calculates the temporal score of the answer.
     * @param candidate The answer candidate.
     * @param question The question.
     * @return The temporal score.
     */
    private double calculateTemporalScore(AnswerCandidate candidate, String question) {
        TemporalInfo questionTemporalInfo = TextUtils.extractTemporalInfo(question);
        TemporalInfo candidateTemporalInfo = TextUtils.extractTemporalInfo(candidate.getText());

        if (questionTemporalInfo == null) {
            return 1.0; // No temporal aspect in the question, so no penalty.
        }
        if (candidateTemporalInfo == null) {
            return 0.4; // Question is temporal, but answer is not.
        }

        // Exact match for points in time
        if (questionTemporalInfo.isPointInTime() && candidateTemporalInfo.isPointInTime()) {
            return questionTemporalInfo.getStartTimeMillis().equals(candidateTemporalInfo.getStartTimeMillis()) ? 1.0 : 0.1;
        }

        // Overlap for durations
        if (questionTemporalInfo.isDuration() && candidateTemporalInfo.isDuration()) {
            long q_start = questionTemporalInfo.getStartTimeMillis();
            long q_end = questionTemporalInfo.getEndTimeMillis();
            long c_start = candidateTemporalInfo.getStartTimeMillis();
            long c_end = candidateTemporalInfo.getEndTimeMillis();
            if (q_start < c_end && c_start < q_end) {
                // Calculate overlap percentage for a more granular score
                long overlap = Math.max(0, Math.min(q_end, c_end) - Math.max(q_start, c_start));
                long total_duration = Math.max(q_end, c_end) - Math.min(q_start, c_start);
                return total_duration > 0 ? (double) overlap / total_duration : 0.0;
            }
            return 0.1;
        }

        // One is a point and the other is a duration
        if (questionTemporalInfo.isPointInTime() && candidateTemporalInfo.isDuration()) {
            return (questionTemporalInfo.getStartTimeMillis() >= candidateTemporalInfo.getStartTimeMillis() &&
                    questionTemporalInfo.getStartTimeMillis() <= candidateTemporalInfo.getEndTimeMillis()) ? 0.9 : 0.1;
        }
        if (questionTemporalInfo.isDuration() && candidateTemporalInfo.isPointInTime()) {
            return (candidateTemporalInfo.getStartTimeMillis() >= questionTemporalInfo.getStartTimeMillis() &&
                    candidateTemporalInfo.getStartTimeMillis() <= questionTemporalInfo.getEndTimeMillis()) ? 0.9 : 0.1;
        }

        return 0.3; // Mismatch in temporal types or other cases
    }
}
