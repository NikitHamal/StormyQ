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
     * Production-ready noun phrase identification using NLP pipeline.
     * Uses Stanford CoreNLP's parse trees to identify actual noun phrases.
     * @param chunk The chunk of text to check.
     * @return True if the chunk is a noun phrase, false otherwise.
     */
    private boolean isNounPhrase(String chunk) {
        if (chunk == null || chunk.trim().isEmpty()) {
            return false;
        }

        // Get NLP pipeline from QnAProcessor if available
        NLPPipeline nlpPipeline = QnAProcessor.getInstance().getNLPPipeline();
        if (nlpPipeline != null && nlpPipeline.isInitialized()) {
            List<String> nounPhrases = nlpPipeline.extractNounPhrases(chunk);
            return !nounPhrases.isEmpty();
        }

        // Fallback to enhanced heuristic using POS patterns
        List<String> tokens = TextUtils.tokenize(chunk);
        if (tokens.isEmpty()) {
            return false;
        }

        // Check for noun phrase patterns: DT? JJ* NN+
        // (optional determiner, optional adjectives, required nouns)
        boolean hasNoun = false;
        boolean hasValidPattern = true;
        
        for (String token : tokens) {
            if (isNoun(token)) {
                hasNoun = true;
            } else if (!isAdjective(token) && !isDeterminer(token) && !isPreposition(token)) {
                // If it's not a noun, adjective, determiner, or preposition, it's probably not a valid NP
                hasValidPattern = false;
                break;
            }
        }

        return hasNoun && hasValidPattern;
    }

    /**
     * Production-ready noun identification using POS tagging.
     * Uses NLP pipeline when available, falls back to enhanced heuristics.
     * @param word The word to check.
     * @return True if the word is a noun, false otherwise.
     */
    private boolean isNoun(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        // Use NLP pipeline for accurate POS tagging
        NLPPipeline nlpPipeline = QnAProcessor.getInstance().getNLPPipeline();
        if (nlpPipeline != null && nlpPipeline.isInitialized()) {
            return nlpPipeline.isNounByPOS(word);
        }

        // Enhanced heuristic fallback with more patterns
        String lowerWord = word.toLowerCase();
        
        // Common noun suffixes
        String[] nounSuffixes = {
            "tion", "sion", "ness", "ment", "ship", "hood", "ism", "ist", 
            "er", "or", "ar", "age", "ity", "ty", "ance", "ence", "ure",
            "dom", "ward", "ful", "ling", "let", "ette", "ese", "ster"
        };
        
        for (String suffix : nounSuffixes) {
            if (lowerWord.endsWith(suffix)) {
                return true;
            }
        }

        // Check if it's a proper noun (capitalized)
        if (Character.isUpperCase(word.charAt(0)) && word.length() > 1) {
            return true;
        }

        return false;
    }

    /**
     * Production-ready adjective identification using POS tagging.
     * Uses NLP pipeline when available, falls back to enhanced heuristics.
     * @param word The word to check.
     * @return True if the word is an adjective, false otherwise.
     */
    private boolean isAdjective(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        // Use NLP pipeline for accurate POS tagging
        NLPPipeline nlpPipeline = QnAProcessor.getInstance().getNLPPipeline();
        if (nlpPipeline != null && nlpPipeline.isInitialized()) {
            return nlpPipeline.isAdjectiveByPOS(word);
        }

        // Enhanced heuristic fallback with more patterns
        String lowerWord = word.toLowerCase();
        
        // Common adjective suffixes
        String[] adjectiveSuffixes = {
            "able", "ible", "ful", "less", "ous", "ious", "eous", "ive", 
            "ative", "itive", "al", "ial", "ic", "tic", "ed", "ing", 
            "ly", "y", "ary", "ory", "some", "like", "ward", "wise"
        };
        
        for (String suffix : adjectiveSuffixes) {
            if (lowerWord.endsWith(suffix)) {
                return true;
            }
        }

        // Comparative and superlative forms
        if (lowerWord.endsWith("er") || lowerWord.endsWith("est")) {
            return true;
        }

        return false;
    }

    /**
     * Check if a word is a determiner (the, a, an, this, that, etc.)
     */
    private boolean isDeterminer(String word) {
        String lowerWord = word.toLowerCase();
        return "the".equals(lowerWord) || "a".equals(lowerWord) || "an".equals(lowerWord) ||
               "this".equals(lowerWord) || "that".equals(lowerWord) || "these".equals(lowerWord) ||
               "those".equals(lowerWord) || "my".equals(lowerWord) || "your".equals(lowerWord) ||
               "his".equals(lowerWord) || "her".equals(lowerWord) || "its".equals(lowerWord) ||
               "our".equals(lowerWord) || "their".equals(lowerWord);
    }

    /**
     * Check if a word is a common preposition
     */
    private boolean isPreposition(String word) {
        String lowerWord = word.toLowerCase();
        return "of".equals(lowerWord) || "in".equals(lowerWord) || "on".equals(lowerWord) ||
               "at".equals(lowerWord) || "by".equals(lowerWord) || "for".equals(lowerWord) ||
               "with".equals(lowerWord) || "to".equals(lowerWord) || "from".equals(lowerWord) ||
               "about".equals(lowerWord) || "into".equals(lowerWord) || "through".equals(lowerWord);
    }

    /**
     * Sets the activation threshold used for answer extraction.
     * @param activationThreshold The new activation threshold.
     */
    public void setActivationThreshold(double activationThreshold) {
        this.activationThreshold = activationThreshold;
    }
}
