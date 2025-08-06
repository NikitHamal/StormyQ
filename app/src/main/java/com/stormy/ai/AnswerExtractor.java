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

        return candidates;
    }

    /**
     * Extracts noun phrases from a sentence using a rule-based approach.
     * @param sentence The sentence to extract noun phrases from.
     * @return A list of noun phrases.
     */
    private List<String> extractNounPhrases(String sentence) {
        List<String> phrases = new ArrayList<>();
        // A more robust regex to split by conjunctions and prepositions
        String[] chunks = sentence.split("\\b(and|or|but|of|in|on|at|for|to|with|by)\\b");
        for (String chunk : chunks) {
            String trimmedChunk = chunk.trim();
            if (trimmedChunk.isEmpty()) {
                continue;
            }
            // A simple heuristic to check if the chunk is a noun phrase
            if (isNounPhrase(trimmedChunk)) {
                phrases.add(trimmedChunk);
            }
        }
        return phrases;
    }

    /**
     * A simple heuristic to identify noun phrases.
     * @param chunk The chunk of text to check.
     * @return True if the chunk is likely a noun phrase, false otherwise.
     */
    private boolean isNounPhrase(String chunk) {
        // A chunk is considered a noun phrase if it contains at least one noun or adjective.
        // This is a placeholder for a more sophisticated POS tagging and NP chunking logic.
        List<String> tokens = TextUtils.tokenize(chunk);
        for (String token : tokens) {
            if (isNoun(token) || isAdjective(token)) {
                return true;
            }
        }
        return false;
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

    /**
     * Sets the activation threshold used for answer extraction.
     * @param activationThreshold The new activation threshold.
     */
    public void setActivationThreshold(double activationThreshold) {
        this.activationThreshold = activationThreshold;
    }
}
