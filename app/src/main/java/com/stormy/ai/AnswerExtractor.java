package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import com.stormy.ai.models.AnswerResult;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for extracting the most relevant answer from the original context
 * based on the activated nodes in the semantic network, taking into account
 * temporal information, sentiment, and providing a snippet.
 */
public class AnswerExtractor {

    private StringBuilder reasoningSummary; // For logging
    private double activationThreshold; // Needs to know the global activation threshold

    // Sentiment boost factor from QnAProcessor/SpreadingActivator
    private double sentimentBoostFactor;
    private AdvancedScorer advancedScorer;

    /**
     * Constructs an AnswerExtractor.
     * @param reasoningSummary A StringBuilder to append reasoning logs.
     * @param initialActivationThreshold The initial activation threshold.
     * @param sentimentBoostFactor The factor used for sentiment boosting/penalizing.
     */
    public AnswerExtractor(StringBuilder reasoningSummary, double initialActivationThreshold, double sentimentBoostFactor) {
        this.reasoningSummary = reasoningSummary;
        this.activationThreshold = initialActivationThreshold;
        this.sentimentBoostFactor = sentimentBoostFactor;
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

            // Simple approach: treat the whole sentence as a candidate
            int startIndex = context.indexOf(trimmedSentence);
            if (startIndex != -1) {
                candidates.add(new AnswerCandidate(trimmedSentence, trimmedSentence, startIndex, startIndex + trimmedSentence.length()));
            }

            // More advanced: generate candidates from phrases within the sentence
            // For now, we'll stick to sentence-level candidates for simplicity
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
     * Extracts the best answer based on highly activated nodes and temporal considerations.
     * It first identifies the best sentence, then refines it to the most activated phrase.
     * @param context The full original text context.
     * @param questionKeywords Stemmed keywords from the question (for relevance checking).
     * @param questionSentiment The sentiment score of the question.
     * @param semanticNetwork The SemanticNetwork containing activated nodes.
     * @param originalQuestionWords Original (non-stemmed, lowercased) words from the question.
     * @return An AnswerResult object with the extracted answer, its indices, and confidence.
     */
    public AnswerResult extractAnswer(String context, List<String> questionKeywords, int questionSentiment, SemanticNetwork semanticNetwork, List<String> originalQuestionWords) {
        List<String> sentences = Arrays.asList(TextUtils.splitSentences(context));
        double maxSentenceActivation = 0.0;
        String bestSentence = "";
        int bestSentenceStartIndex = -1; // Store the start index of the best sentence
        int bestSentenceEndIndex = -1;   // Store the end index of the best sentence

        // Extract temporal information from the question for temporal reasoning
        TemporalInfo questionTemporalInfo = null;
        // Also identify if the question is specifically asking for time/date
        boolean questionAsksForTime = false;
        for (String qk : TextUtils.tokenize(questionKeywords.stream().collect(Collectors.joining(" ")))) {
            TemporalInfo temp = TextUtils.extractTemporalInfo(qk);
            if (temp != null) {
                questionTemporalInfo = temp;
                reasoningSummary.append("\nQuestion Temporal Info Detected: ").append(temp.getRawTemporalExpression()).append("\n");
            }
            // Check for explicit temporal query words in the original question words
            // Make sure these keywords are stemmed if you are comparing against stemmed `qk`
            if (originalQuestionWords.contains("when") || originalQuestionWords.contains("year") ||
                originalQuestionWords.contains("date") || originalQuestionWords.contains("time")) {
                questionAsksForTime = true;
            }
        }
        reasoningSummary.append("Question asks for time/date: ").append(questionAsksForTime).append("\n");


        reasoningSummary.append("\n--- Answer Candidate Evaluation (Sentence Level) ---\n");
        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) continue;

            List<String> sentenceTokens = TextUtils.tokenize(trimmedSentence);
            double currentSentenceActivation = 0.0;
            int activatedWordCount = 0;
            boolean sentenceContainsNegation = false;
            TemporalInfo sentenceTemporalInfo = null;
            int sentenceSentiment = TextUtils.getSentimentScore(trimmedSentence);


            for (String token : sentenceTokens) {
                String stemmedToken = TextUtils.stem(token);
                SemanticNode node = semanticNetwork.getNode(stemmedToken);
                if (node != null) {
                    if (node.getActivation() >= activationThreshold) {
                        currentSentenceActivation += node.getActivation();
                        activatedWordCount++;
                    }
                    if (node.isNegated()) {
                        sentenceContainsNegation = true;
                    }
                    if (node.getTemporalInfo() != null) {
                        sentenceTemporalInfo = node.getTemporalInfo();
                    }
                }
            }

            // Calculate score for the sentence
            double sentenceScore = (activatedWordCount > 0) ? currentSentenceActivation / activatedWordCount : 0.0;

            reasoningSummary.append(" - Sentence: \"").append(trimmedSentence.substring(0, Math.min(trimmedSentence.length(), 50))).append("...\"\n");
            reasoningSummary.append("   Base Score: ").append(String.format("%.2f", sentenceScore));

            // Apply temporal reasoning boost/penalty
            if (questionTemporalInfo != null && sentenceTemporalInfo != null) {
                // If both are point in time and match
                if (questionTemporalInfo.isPointInTime() && sentenceTemporalInfo.isPointInTime() &&
                    questionTemporalInfo.getStartTimeMillis().equals(sentenceTemporalInfo.getStartTimeMillis())) {
                    sentenceScore *= 1.2; // Boost if temporal info matches
                    reasoningSummary.append(" (Temporal Match +20%)");
                }
                // If both are durations and overlap
                else if (questionTemporalInfo.isDuration() && sentenceTemporalInfo.isDuration() &&
                         doTimeRangesOverlap(questionTemporalInfo, sentenceTemporalInfo)) {
                    sentenceScore *= 1.1; // Smaller boost for duration overlap
                    reasoningSummary.append(" (Temporal Overlap +10%)");
                }
                // Penalty if temporal info present but no match/overlap
                else if ((questionTemporalInfo.getStartTimeMillis() != null || questionTemporalInfo.getEndTimeMillis() != null) &&
                         (sentenceTemporalInfo.getStartTimeMillis() != null || sentenceTemporalInfo.getEndTimeMillis() != null)) {
                    sentenceScore *= 0.8; // Minor penalty if temporal info doesn't match but is present
                    reasoningSummary.append(" (Temporal Mismatch -20%)");
                }
            }


            // Apply negation handling penalty if the sentence contains negation and the question implies a positive answer
            // or vice versa (e.g., "who is not here?" vs "who is here?")
            boolean questionImpliesNegation = questionKeywords.stream().anyMatch(TextUtils::isNegationWord);
            if (sentenceContainsNegation != questionImpliesNegation) { // If one is negated and other isn't
                sentenceScore *= 0.5; // Penalty for negation mismatch
                reasoningSummary.append(" (Negation Mismatch Penalty -50%)");
            }

            // Apply sentiment boost/penalty for the sentence
            if (questionSentiment != 0 && sentenceSentiment != 0) {
                if ((questionSentiment > 0 && sentenceSentiment > 0) || (questionSentiment < 0 && sentenceSentiment < 0)) {
                    sentenceScore *= (1.0 + sentimentBoostFactor); // Boost if sentiment matches
                    reasoningSummary.append(" (Sentiment Match Boost +").append(sentimentBoostFactor*100).append("%)");
                } else {
                    sentenceScore *= (1.0 - sentimentBoostFactor); // Penalty if sentiment mismatches
                    reasoningSummary.append(" (Sentiment Mismatch Penalty -").append(sentimentBoostFactor*100).append("%)");
                }
            }


            reasoningSummary.append(" -> Final Sentence Score: ").append(String.format("%.2f", sentenceScore)).append("\n");


            if (sentenceScore > maxSentenceActivation) {
                maxSentenceActivation = sentenceScore;
                bestSentence = trimmedSentence;
                bestSentenceStartIndex = context.indexOf(trimmedSentence);
                bestSentenceEndIndex = bestSentenceStartIndex + trimmedSentence.length();
                reasoningSummary.append("   (NEW BEST SENTENCE CANDIDATE)\n");
            }
        }

        // --- Enhanced Answer Extraction: Find most activated phrase within the best sentence ---
        String finalAnswer = "";
        int finalStartIndex = -1;
        int finalEndIndex = -1;

        if (!bestSentence.isEmpty() && maxSentenceActivation >= 0.15) { // Only refine if a good sentence found
            List<String> bestSentenceRawTokens = TextUtils.tokenize(bestSentence);
            double maxPhraseScore = 0.0;
            String bestPhrase = "";
            int bestPhraseLocalStartIndex = -1;
            int bestPhraseLocalEndIndex = -1;

            // Iterate through all possible sub-phrases (contiguous sequences of tokens)
            for (int i = 0; i < bestSentenceRawTokens.size(); i++) {
                for (int j = i; j < bestSentenceRawTokens.size(); j++) {
                    List<String> currentPhraseTokens = bestSentenceRawTokens.subList(i, j + 1);
                    double currentPhraseActivationSum = 0.0;
                    int activatedWordCountInPhrase = 0;
                    int contiguousActivatedWords = 0; // Track contiguous activated words

                    // Check sentiment alignment of the phrase
                    int phraseSentiment = TextUtils.getSentimentScore(String.join(" ", currentPhraseTokens));
                    double sentimentMatchFactor = 1.0;
                    if (questionSentiment != 0 && phraseSentiment != 0) {
                        if ((questionSentiment > 0 && phraseSentiment > 0) || (questionSentiment < 0 && phraseSentiment < 0)) {
                            sentimentMatchFactor = (1.0 + sentimentBoostFactor * 0.5); // Smaller boost for phrase sentiment
                        } else {
                            sentimentMatchFactor = (1.0 - sentimentBoostFactor * 0.5); // Smaller penalty
                        }
                    }

                    // Calculate sum of activations for words in the current phrase
                    for (int k = 0; k < currentPhraseTokens.size(); k++) {
                        String token = currentPhraseTokens.get(k);
                        String stemmedToken = TextUtils.stem(token);
                        SemanticNode node = semanticNetwork.getNode(stemmedToken);
                        if (node != null && node.getActivation() >= activationThreshold) {
                            currentPhraseActivationSum += node.getActivation();
                            activatedWordCountInPhrase++;
                            if (k > 0) { // Check contiguity with previous token
                                String prevStemmedToken = TextUtils.stem(currentPhraseTokens.get(k-1));
                                SemanticNode prevNode = semanticNetwork.getNode(prevStemmedToken);
                                if (prevNode != null && prevNode.getActivation() >= activationThreshold) {
                                    contiguousActivatedWords++; // Count contiguity
                                }
                            }
                        }
                    }

                    // Calculate phrase score: activation sum * density / (1 + log(length)) * sentiment_factor
                    // Density: how many words in the phrase are activated
                    double phraseDensity = (currentPhraseTokens.isEmpty() || activatedWordCountInPhrase == 0) ? 0.0 :
                                           (double) activatedWordCountInPhrase / currentPhraseTokens.size();

                    // Logarithmic length penalty
                    double lengthPenaltyFactor = (currentPhraseTokens.size() > 1) ? Math.log(currentPhraseTokens.size()) : 0.0;

                    double currentPhraseScore = 0.0;
                    if (phraseDensity > 0) { // Only calculate score if there's at least one activated word
                        currentPhraseScore = currentPhraseActivationSum * phraseDensity * sentimentMatchFactor / (1.0 + lengthPenaltyFactor);
                    }

                    // Add a small boost for phrase cohesion
                    if (activatedWordCountInPhrase > 1) {
                         currentPhraseScore *= (1.0 + (double) contiguousActivatedWords / (activatedWordCountInPhrase - 1) * 0.05); // Up to 5% boost
                    }


                    // Boost if the phrase contains any of the original question words (non-stemmed)
                    boolean containsOriginalQuestionWord = currentPhraseTokens.stream()
                        .map(String::toLowerCase)
                        .anyMatch(originalQuestionWords::contains);

                    if (containsOriginalQuestionWord) {
                        currentPhraseScore *= 1.1; // Small boost for direct relevance
                        reasoningSummary.append("     Phrase '").append(String.join(" ", currentPhraseTokens))
                                        .append("' boosted (Original Q-word Match +10%)\n");
                    }

                    // Temporal consistency check for phrase
                    TemporalInfo phraseTemporalInfo = null;
                    // Attempt to extract temporal info from the *entire* current phrase
                    phraseTemporalInfo = TextUtils.extractTemporalInfo(String.join(" ", currentPhraseTokens));

                    if (questionTemporalInfo != null && phraseTemporalInfo != null) {
                        if (!doTimeRangesOverlap(questionTemporalInfo, phraseTemporalInfo)) {
                            currentPhraseScore *= 0.5; // Significant penalty if phrase's temporal info strongly mismatches query's
                            reasoningSummary.append("     Phrase '").append(String.join(" ", currentPhraseTokens))
                                            .append("' penalized (Temporal Mismatch -50%)\n");
                        }
                    }

                    // >>> NEW & CRUCIAL: Strong boost for temporal entities if question asks for time/date <<<
                    if (questionAsksForTime && phraseTemporalInfo != null && !currentPhraseTokens.isEmpty()) {
                        // Prioritize single-word or very short temporal expressions directly matching query type
                        // We're adding a flat score to make them highly competitive
                        double temporal_boost_score = 0.0;
                        if (currentPhraseTokens.size() == 1) { // e.g., "1775"
                            temporal_boost_score = 0.5; // Very strong flat boost for single temporal token
                        } else if (currentPhraseTokens.size() <= 3 && TextUtils.isTemporalKeyword(currentPhraseTokens.get(0).toLowerCase())) { // e.g., "April 1775"
                            temporal_boost_score = 0.3; // Strong flat boost for short temporal phrases
                        } else if (currentPhraseTokens.size() <= 5) { // Any other short temporal phrase
                            temporal_boost_score = 0.1; // Moderate flat boost
                        }
                        currentPhraseScore += temporal_boost_score;
                        reasoningSummary.append("     Phrase '").append(String.join(" ", currentPhraseTokens))
                                        .append("' received TEMPORAL ANSWER BOOST (+").append(String.format("%.2f", temporal_boost_score*100)).append("%)\n");
                    }


                    // Ensure the phrase contains at least one activated keyword from the question or highly activated node
                    boolean containsRelevantActivatedWord = currentPhraseTokens.stream()
                        .anyMatch(token -> {
                            String stemmed = TextUtils.stem(token);
                            return questionKeywords.contains(stemmed) ||
                                   (semanticNetwork.getNode(stemmed) != null && semanticNetwork.getNode(stemmed).getActivation() >= activationThreshold);
                        });

                    if (currentPhraseScore > maxPhraseScore && containsRelevantActivatedWord) {
                        maxPhraseScore = currentPhraseScore;
                        bestPhrase = String.join(" ", currentPhraseTokens);
                        // Recalculate accurate indices in original sentence
                        int tempStart = bestSentence.indexOf(bestPhrase); // Use indexOf with the joined phrase
                        if (tempStart != -1) {
                            bestPhraseLocalStartIndex = tempStart;
                            bestPhraseLocalEndIndex = tempStart + bestPhrase.length();
                        }
                    }
                }
            }

            if (!bestPhrase.isEmpty() && bestPhraseLocalStartIndex != -1) {
                finalAnswer = bestPhrase;
                // Adjust global indices based on best sentence's start index
                finalStartIndex = bestSentenceStartIndex + bestPhraseLocalStartIndex;
                finalEndIndex = bestSentenceStartIndex + bestPhraseLocalEndIndex;
                // Ensure indices are within bounds
                finalStartIndex = Math.max(0, finalStartIndex);
                finalEndIndex = Math.min(context.length(), finalEndIndex);

                reasoningSummary.append("\nAnswer Extracted from Best Sentence:\n");
                reasoningSummary.append(" - Selected Phrase: \"").append(finalAnswer).append("\"\n");
                reasoningSummary.append(" - Phrase Score: ").append(String.format("%.2f", maxPhraseScore)).append("\n");
                reasoningSummary.append(" - Original Context Start Index: ").append(finalStartIndex).append("\n");
                reasoningSummary.append(" - Original Context End Index: ").append(finalEndIndex).append("\n");

                // Adjust confidence based on the precision of the extracted answer vs full sentence
                // Combine maxPhraseScore and maxSentenceActivation for final confidence
                // Give more weight to the final maxPhraseScore for a more precise highlight
                maxSentenceActivation = (maxPhraseScore * 0.8 + maxSentenceActivation * 0.2);
                // Ensure confidence doesn't exceed 1.0 or go below 0.0
                maxSentenceActivation = Math.max(0.0, Math.min(1.0, maxSentenceActivation));

            } else {
                // Fallback to full sentence if no good precise phrase could be extracted
                finalAnswer = bestSentence;
                finalStartIndex = bestSentenceStartIndex;
                finalEndIndex = bestSentenceEndIndex;
                reasoningSummary.append("\nNo precise phrase found. Falling back to best sentence as answer.\n");
            }
        } else {
            // If the best sentence score was too low, or no best sentence, then no answer
            finalAnswer = "";
            finalStartIndex = -1;
            finalEndIndex = -1;
            maxSentenceActivation = 0.0;
            reasoningSummary.append("\nFinal answer confidence too low (").append(String.format("%.2f", maxSentenceActivation))
                            .append("). No valid answer found.\n");
        }

        // IMPORTANT: If finalAnswer is empty or invalid, ensure startIndex/endIndex are -1 or 0,0
        if (finalAnswer.isEmpty() || finalStartIndex == -1 || finalEndIndex == -1) {
             finalStartIndex = -1;
             finalEndIndex = -1;
             maxSentenceActivation = 0.0; // Reset confidence if no valid answer was found
             reasoningSummary.append("No valid final answer could be determined after phrase extraction.\n");
        }


        return new AnswerResult(finalAnswer, context, finalStartIndex, finalEndIndex, maxSentenceActivation);
    }

    /**
     * Helper method to check if two temporal ranges overlap.
     * @param info1 First TemporalInfo object.
     * @param info2 Second TemporalInfo object.
     * @return True if they overlap, false otherwise.
     */
    private boolean doTimeRangesOverlap(TemporalInfo info1, TemporalInfo info2) {
        if (info1.getStartTimeMillis() == null || info1.getEndTimeMillis() == null ||
            info2.getStartTimeMillis() == null || info2.getEndTimeMillis() == null) {
            return false; // Cannot determine overlap if times are null
        }

        long start1 = info1.getStartTimeMillis();
        long end1 = info1.getEndTimeMillis();
        long start2 = info2.getStartTimeMillis();
        long end2 = info2.getEndTimeMillis();

        // Check for overlap: (Start1 <= End2) AND (Start2 <= End1)
        return start1 <= end2 && start2 <= end1;
    }

    /**
     * Sets the activation threshold used for answer extraction.
     * @param activationThreshold The new activation threshold.
     */
    public void setActivationThreshold(double activationThreshold) {
        this.activationThreshold = activationThreshold;
    }

    /**
     * Sets the sentiment boost factor used during answer extraction.
     * @param sentimentBoostFactor The new sentiment boost factor.
     */
    public void setSentimentBoostFactor(double sentimentBoostFactor) {
        this.sentimentBoostFactor = sentimentBoostFactor;
    }
}
