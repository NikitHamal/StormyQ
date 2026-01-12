package com.stormy.ai;

import com.stormy.ai.models.AnswerResult;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Optimally extracts the most relevant answer from the context.
 */
public class AnswerExtractor {

    private final StringBuilder reasoningSummary;
    private double activationThreshold;
    private double sentimentBoostFactor;

    public AnswerExtractor(StringBuilder reasoningSummary, double initialActivationThreshold, double sentimentBoostFactor) {
        this.reasoningSummary = reasoningSummary;
        this.activationThreshold = initialActivationThreshold;
        this.sentimentBoostFactor = sentimentBoostFactor;
    }

    public AnswerResult extractAnswer(String context, List<String> questionKeywords, int questionSentiment, SemanticNetwork semanticNetwork, List<String> originalQuestionWords) {
        List<String> sentences = Arrays.asList(TextUtils.splitSentences(context));
        double maxSentenceScore = 0.0;
        String bestSentence = "";
        int bestSentenceStartIndex = -1;

        TemporalInfo questionTempInfo = null;
        boolean isTimeQuery = originalQuestionWords.contains("when") || originalQuestionWords.contains("year") || 
                             originalQuestionWords.contains("date") || originalQuestionWords.contains("time");

        for (String kw : questionKeywords) {
            TemporalInfo temp = TextUtils.extractTemporalInfo(kw);
            if (temp != null) {
                questionTempInfo = temp;
                break;
            }
        }

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            double score = evaluateSentence(trimmed, semanticNetwork, questionTempInfo, questionSentiment, questionKeywords);
            
            if (score > maxSentenceScore) {
                maxSentenceScore = score;
                bestSentence = trimmed;
                bestSentenceStartIndex = context.indexOf(trimmed);
            }
        }

        if (bestSentence.isEmpty() || maxSentenceScore < 0.1) {
            return new AnswerResult("", context, -1, -1, 0.0);
        }

        return refineAnswer(bestSentence, bestSentenceStartIndex, context, semanticNetwork, questionTempInfo, questionSentiment, questionKeywords, originalQuestionWords, isTimeQuery);
    }

    private double evaluateSentence(String sentence, SemanticNetwork network, TemporalInfo qTemp, int qSentiment, List<String> qKeywords) {
        List<String> tokens = TextUtils.tokenize(sentence);
        double totalActivation = 0;
        int activeCount = 0;
        boolean hasNegation = false;
        TemporalInfo sTemp = null;

        for (String token : tokens) {
            SemanticNode node = network.getNode(TextUtils.stem(token));
            if (node != null) {
                if (node.getActivation() >= activationThreshold) {
                    totalActivation += node.getActivation();
                    activeCount++;
                }
                if (node.isNegated()) hasNegation = true;
                if (node.getTemporalInfo() != null) sTemp = node.getTemporalInfo();
            }
        }

        double score = (activeCount > 0) ? totalActivation / tokens.size() : 0;
        
        // Boosts/Penalties
        if (qTemp != null && sTemp != null) {
            if (qTemp.getStartTimeMillis().equals(sTemp.getStartTimeMillis())) score *= 1.3;
        }

        boolean qNegated = qKeywords.stream().anyMatch(TextUtils::isNegationWord);
        if (hasNegation != qNegated) score *= 0.6;

        int sSentiment = TextUtils.getSentimentScore(sentence);
        if (qSentiment != 0 && sSentiment != 0) {
            if (Integer.signum(qSentiment) == Integer.signum(sSentiment)) score *= (1.0 + sentimentBoostFactor);
            else score *= (1.0 - sentimentBoostFactor);
        }

        return score;
    }

    private AnswerResult refineAnswer(String sentence, int sentenceStart, String context, SemanticNetwork network, 
                                     TemporalInfo qTemp, int qSentiment, List<String> qKeywords, 
                                     List<String> origQWords, boolean isTimeQuery) {
        List<String> tokens = TextUtils.tokenize(sentence);
        double maxPhraseScore = 0;
        String bestPhrase = sentence;
        int bestStart = 0;

        // Optimization: Limit phrase length and only start from active tokens
        for (int i = 0; i < tokens.size(); i++) {
            String startToken = tokens.get(i);
            SemanticNode startNode = network.getNode(TextUtils.stem(startToken));
            if (startNode == null || startNode.getActivation() < activationThreshold) continue;

            for (int j = i; j < Math.min(i + 15, tokens.size()); j++) {
                List<String> sub = tokens.subList(i, j + 1);
                String phrase = String.join(" ", sub);
                double score = evaluatePhrase(sub, network, qTemp, qSentiment, qKeywords, origQWords, isTimeQuery);

                if (score > maxPhraseScore) {
                    maxPhraseScore = score;
                    bestPhrase = phrase;
                    bestStart = sentence.indexOf(phrase);
                }
            }
        }

        int finalStart = sentenceStart + bestStart;
        return new AnswerResult(bestPhrase, context, finalStart, finalStart + bestPhrase.length(), Math.min(1.0, maxPhraseScore));
    }

    private double evaluatePhrase(List<String> tokens, SemanticNetwork network, TemporalInfo qTemp, int qSentiment, 
                                 List<String> qKeywords, List<String> origQWords, boolean isTimeQuery) {
        double sum = 0;
        int active = 0;
        for (String t : tokens) {
            SemanticNode n = network.getNode(TextUtils.stem(t));
            if (n != null && n.getActivation() >= activationThreshold) {
                sum += n.getActivation();
                active++;
            }
        }

        double density = (double) active / tokens.size();
        double score = sum * density / (1.0 + Math.log(tokens.size()));

        if (isTimeQuery) {
            TemporalInfo pTemp = TextUtils.extractTemporalInfo(String.join(" ", tokens));
            if (pTemp != null) score += 0.5;
        }

        return score;
    }

    public void setActivationThreshold(double activationThreshold) { this.activationThreshold = activationThreshold; }
    public void setSentimentBoostFactor(double sentimentBoostFactor) { this.sentimentBoostFactor = sentimentBoostFactor; }
}