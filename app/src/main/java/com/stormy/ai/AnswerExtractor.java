package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import com.stormy.ai.models.SemanticNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.text.similarity.LevenshteinDistance;

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
     * Synthesizes answers from top candidates to cover all question aspects.
     * Uses LevenshteinDistance to merge similar/overlapping candidates.
     */
    public List<AnswerCandidate> synthesizeAndFormatAnswers(List<AnswerCandidate> candidates, String question) {
        List<AnswerCandidate> synthesized = new ArrayList<>();
        if (candidates.isEmpty()) return synthesized;
        LevenshteinDistance metric = new LevenshteinDistance();
        boolean[] used = new boolean[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            if (used[i]) continue;
            String base = candidates.get(i).getText();
            StringBuilder merged = new StringBuilder(base);
            used[i] = true;
            for (int j = i + 1; j < candidates.size(); j++) {
                if (used[j]) continue;
                String other = candidates.get(j).getText();
                int dist = metric.apply(base, other);
                int maxLen = Math.max(base.length(), other.length());
                double sim = (maxLen == 0) ? 1.0 : 1.0 - ((double) dist / maxLen);
                if (sim > 0.7) {
                    merged.append("; ").append(other);
                    used[j] = true;
                }
            }
            synthesized.add(new AnswerCandidate(merged.toString(), base, 0, 0));
        }
        for (AnswerCandidate candidate : synthesized) {
            if (!isComplete(candidate.getText(), question)) {
                candidate.setCompletenessScore(0.5);
            } else {
                candidate.setCompletenessScore(1.0);
            }
        }
        for (AnswerCandidate candidate : synthesized) {
            candidate = formatAnswer(candidate, question);
        }
        return synthesized;
    }

    /**
     * Checks if the answer covers all aspects of the question (who, what, when, where, why, how).
     */
    private boolean isComplete(String answer, String question) {
        String q = question.toLowerCase();
        if (q.contains("who") && !answer.matches(".*\\b[A-Z][a-z]+\\b.*")) return false;
        if (q.contains("when") && !answer.matches(".*\\d{4}.*|today|yesterday|tomorrow|month|year|week.*")) return false;
        if (q.contains("where") && !answer.matches(".*\\b(in|at|on|from|to) [A-Z][a-z]+.*")) return false;
        if (q.contains("why") && !answer.matches(".*because.*|due to.*|as a result.*")) return false;
        if (q.contains("how") && !answer.matches(".*by.*|using.*|with.*|through.*")) return false;
        return true;
    }

    /**
     * Formats the answer based on question type (list, fact, explanation).
     */
    private AnswerCandidate formatAnswer(AnswerCandidate candidate, String question) {
        String q = question.toLowerCase();
        String text = candidate.getText();
        if (q.startsWith("list") || q.contains("which of the following")) {
            // Format as bullet list
            String[] items = text.split("; ");
            StringBuilder sb = new StringBuilder();
            for (String item : items) {
                sb.append("- ").append(item.trim()).append("\n");
            }
            candidate = new AnswerCandidate(sb.toString().trim(), candidate.getSourceSentence(), candidate.getStartIndex(), candidate.getEndIndex());
        }
        // Add more formatting rules as needed
        return candidate;
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
     * Extracts key aspects from the question (who, what, when, where, why, how, numerical).
     */
    private List<String> extractAspects(String question) {
        List<String> aspects = new ArrayList<>();
        String q = question.toLowerCase();
        if (q.contains("who")) aspects.add("who");
        if (q.contains("what")) aspects.add("what");
        if (q.contains("when")) aspects.add("when");
        if (q.contains("where")) aspects.add("where");
        if (q.contains("why")) aspects.add("why");
        if (q.contains("how")) aspects.add("how");
        if (q.matches(".*\\d+.*|how many|how much|number|amount|percent|percentage|rate|date|year|age.*")) aspects.add("numerical");
        return aspects;
    }

    /**
     * Synthesizes a structured answer covering all aspects, with enhanced formatting and optional highlighting/confidence notes.
     */
    public String synthesizeStructuredAnswer(List<AnswerCandidate> candidates, String question, AdvancedScorer.ConfidenceResult confidenceResult) {
        List<String> aspects = extractAspects(question);
        StringBuilder answer = new StringBuilder();
        for (String aspect : aspects) {
            AnswerCandidate best = selectBestForAspect(candidates, aspect);
            if (best != null) {
                String label = aspect.substring(0, 1).toUpperCase() + aspect.substring(1) + ": ";
                String value = highlightAspect(best.getText(), aspect);
                answer.append(label).append(value).append("\n");
            }
        }
        if (answer.length() == 0 && !candidates.isEmpty()) {
            answer.append(candidates.get(0).getText());
        }
        // Add confidence/uncertainty note if needed
        if (confidenceResult != null && confidenceResult.confidence < 0.7) {
            answer.append("\n(Note: This answer is synthesized from uncertain or conflicting sources. Confidence: ")
                  .append(String.format("%.2f", confidenceResult.confidence)).append(")");
        }
        return answer.toString().trim();
    }

    /**
     * Selects the best candidate for a given aspect using LevenshteinDistance similarity.
     */
    private AnswerCandidate selectBestForAspect(List<AnswerCandidate> candidates, String aspect) {
        LevenshteinDistance metric = new LevenshteinDistance();
        double bestScore = 0.0;
        AnswerCandidate best = null;
        for (AnswerCandidate c : candidates) {
            double score = 0.0;
            String text = c.getText().toLowerCase();
            switch (aspect) {
                case "who":
                    if (text.matches(".*\\b[A-Z][a-z]+\\b.*")) score += 1.0;
                    break;
                case "when":
                    if (text.matches(".*\\d{4}.*|today|yesterday|tomorrow|month|year|week.*")) score += 1.0;
                    break;
                case "where":
                    if (text.matches(".*\\b(in|at|on|from|to) [A-Z][a-z]+.*")) score += 1.0;
                    break;
                case "why":
                    if (text.contains("because") || text.contains("due to") || text.contains("as a result")) score += 1.0;
                    break;
                case "how":
                    if (text.contains("by ") || text.contains("using ") || text.contains("with ") || text.contains("through ")) score += 1.0;
                    break;
                case "numerical":
                    if (text.matches(".*\\d+.*|percent|percentage|rate|amount|number|year|date|age.*")) score += 1.0;
                    break;
                default:
                    break;
            }
            int dist = metric.apply(text, aspect);
            int maxLen = Math.max(text.length(), aspect.length());
            double sim = (maxLen == 0) ? 1.0 : 1.0 - ((double) dist / maxLen);
            score += sim;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    /**
     * Highlights or emphasizes key answer parts for an aspect (e.g., bold numbers, dates, names).
     */
    private String highlightAspect(String text, String aspect) {
        switch (aspect) {
            case "when":
            case "numerical":
                return text.replaceAll("(\\d{4}|\\d+|percent|percentage|rate|amount|number|year|date|age)", "**$1**");
            case "who":
            case "where":
                return text.replaceAll("(\\b[A-Z][a-z]+\\b)", "**$1**");
            default:
                return text;
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
