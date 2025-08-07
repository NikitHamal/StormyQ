package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import com.stormy.ai.models.SemanticNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Responsible for extracting and ranking potential answer candidates from the context.
 */
public class AnswerExtractor {

    private final StringBuilder reasoningSummary; // For logging
    private double activationThreshold; // Needs to know the global activation threshold

    private final AdvancedScorer advancedScorer;

    /**
     * Constructs an AnswerExtractor.
     * @param reasoningSummary A StringBuilder to append reasoning logs.
     * @param initialActivationThreshold The initial activation threshold.
     * @param sentimentBoostFactor The factor used for sentiment boosting/penalizing (currently unused).
     */
    public AnswerExtractor(StringBuilder reasoningSummary, double initialActivationThreshold, double sentimentBoostFactor) {
        this.reasoningSummary = reasoningSummary;
        this.activationThreshold = initialActivationThreshold;
        this.advancedScorer = new AdvancedScorer();
    }

    /**
     * Extracts a list of ranked answer candidates based on the new multi-dimensional scoring.
     * @param context The full original text context.
     * @param question The original question.
     * @param semanticNetwork The SemanticNetwork containing activated nodes.
     * @return A list of ranked AnswerCandidate objects.
     */
    public List<AnswerCandidate> extractRankedAnswers(String context, String question, SemanticNetwork semanticNetwork) {
        List<AnswerCandidate> candidates = new ArrayList<>();
        List<String> sentences = Arrays.asList(TextUtils.splitSentences(context));

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) continue;

            // Add the full sentence as a candidate
            int sentenceStartIndex = context.indexOf(trimmedSentence);
            if (sentenceStartIndex != -1) {
                candidates.add(new AnswerCandidate(trimmedSentence, trimmedSentence, sentenceStartIndex, sentenceStartIndex + trimmedSentence.length()));
            }

            // Extract noun phrases as candidates
            List<String> nounPhrases = extractNounPhrases(trimmedSentence);
            for (String phrase : nounPhrases) {
                int phraseStartIndex = trimmedSentence.indexOf(phrase);
                if (phraseStartIndex != -1) {
                    candidates.add(new AnswerCandidate(phrase, trimmedSentence, sentenceStartIndex + phraseStartIndex, sentenceStartIndex + phraseStartIndex + phrase.length()));
                }
            }
        }

        // Score each candidate
        for (AnswerCandidate candidate : candidates) {
            advancedScorer.scoreCandidate(candidate, question, semanticNetwork);
        }

        // Rank candidates by final score
        candidates.sort(Comparator.comparingDouble(AnswerCandidate::getFinalScore).reversed());

        // Synthesize answers from the top candidates
        List<AnswerCandidate> synthesizedCandidates = synthesizeAnswers(candidates, context);
        candidates.addAll(synthesizedCandidates);

        // Re-rank candidates after synthesis
        for (AnswerCandidate candidate : candidates) {
            advancedScorer.scoreCandidate(candidate, question, semanticNetwork);
        }
        candidates.sort(Comparator.comparingDouble(AnswerCandidate::getFinalScore).reversed());

        return candidates;
    }

    /**
     * Extracts noun phrases from a sentence using the OpenNLP library.
     * @param sentence The sentence to extract noun phrases from.
     * @return A list of noun phrases.
     */
    private List<String> extractNounPhrases(String sentence) {
        // This method requires the OpenNLP models to be loaded.
        // For now, we will return an empty list.
        return new ArrayList<>();
    }

    /**
     * A simple heuristic to identify nouns based on common endings.
     * @param word The word to check.
     * @return True if the word is likely a noun, false otherwise.
     */
    private boolean isNoun(String word) {
        // This is not a comprehensive list and should be expanded.
        String lowerCaseWord = word.toLowerCase();
        return lowerCaseWord.endsWith("tion") || lowerCaseWord.endsWith("sion") || lowerCaseWord.endsWith("ment") ||
               lowerCaseWord.endsWith("ness") || lowerCaseWord.endsWith("ity") || lowerCaseWord.endsWith("er") ||
               lowerCaseWord.endsWith("or") || lowerCaseWord.endsWith("ist") || lowerCaseWord.endsWith("ism");
    }

    /**
     * A simple heuristic to identify adjectives based on common endings.
     * @param word The word to check.
     * @return True if the word is likely an adjective, false otherwise.
     */
    private boolean isAdjective(String word) {
        // This is not a comprehensive list and should be expanded.
        String lowerCaseWord = word.toLowerCase();
        return lowerCaseWord.endsWith("able") || lowerCaseWord.endsWith("ible") || lowerCaseWord.endsWith("al") ||
               lowerCaseWord.endsWith("ful") || lowerCaseWord.endsWith("less") || lowerCaseWord.endsWith("ous") ||
               lowerCaseWord.endsWith("ive") || lowerCaseWord.endsWith("ic");
    }

    private List<AnswerCandidate> synthesizeAnswers(List<AnswerCandidate> candidates, String context) {
        List<AnswerCandidate> synthesizedCandidates = new ArrayList<>();
        if (candidates.size() < 2) {
            return synthesizedCandidates;
        }

        AnswerCandidate c1 = candidates.get(0);
        AnswerCandidate c2 = candidates.get(1);

        if (!c1.getSourceSentence().equals(c2.getSourceSentence())) {
            String synthesizedText = c1.getText() + " " + c2.getText();
            if (synthesizedText.length() < 200) { // Avoid overly long synthesized answers
                int startIndex = Math.min(c1.getStartIndex(), c2.getStartIndex());
                int endIndex = Math.max(c1.getEndIndex(), c2.getEndIndex());
                synthesizedCandidates.add(new AnswerCandidate(synthesizedText, c1.getSourceSentence() + " " + c2.getSourceSentence(), startIndex, endIndex));
            }
        }
        return synthesizedCandidates;
    }

    public String extractNumericalAnswer(String answer) {
        // This is a placeholder for a more sophisticated numerical answer extraction logic.
        // For now, we'll just return the first number we find.
        List<String> tokens = TextUtils.tokenize(answer);
        for (String token : tokens) {
            if (isNumeric(token)) {
                return token;
            }
        }
        return null;
    }

    public String extractFactualAnswer(String answer) {
        // This is a placeholder for a more sophisticated factual answer extraction logic.
        // For now, we'll just return the first capitalized word we find.
        List<String> tokens = TextUtils.tokenize(answer);
        for (String token : tokens) {
            if (Character.isUpperCase(token.charAt(0))) {
                return token;
            }
        }
        return null;
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Sets the activation threshold used for answer extraction.
     * @param activationThreshold The new activation threshold.
     */
    public void setActivationThreshold(double activationThreshold) {
        this.activationThreshold = activationThreshold;
    }
}
