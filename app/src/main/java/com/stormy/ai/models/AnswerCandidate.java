package com.stormy.ai.models;

/**
 * Represents a potential answer candidate with its associated scores.
 */
public class AnswerCandidate {

    private String text;
    private String sourceSentence;
    private int startIndex;
    private int endIndex;

    private double semanticScore;
    private double completenessScore;
    private double relevanceScore;
    private double lengthScore;
    private double negationScore;
    private double temporalScore;
    private double finalScore;

    /**
     * Constructs a new AnswerCandidate.
     * @param text The text of the answer candidate.
     * @param sourceSentence The sentence from which the candidate was extracted.
     * @param startIndex The start index of the candidate in the original context.
     * @param endIndex The end index of the candidate in the original context.
     */
    public AnswerCandidate(String text, String sourceSentence, int startIndex, int endIndex) {
        this.text = text;
        this.sourceSentence = sourceSentence;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public String getText() {
        return text;
    }

    public String getSourceSentence() {
        return sourceSentence;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public double getSemanticScore() {
        return semanticScore;
    }

    public void setSemanticScore(double semanticScore) {
        this.semanticScore = semanticScore;
    }

    public double getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(double completenessScore) {
        this.completenessScore = completenessScore;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public double getLengthScore() {
        return lengthScore;
    }

    public void setLengthScore(double lengthScore) {
        this.lengthScore = lengthScore;
    }

    public double getNegationScore() {
        return negationScore;
    }

    public void setNegationScore(double negationScore) {
        this.negationScore = negationScore;
    }

    public double getTemporalScore() {
        return temporalScore;
    }

    public void setTemporalScore(double temporalScore) {
        this.temporalScore = temporalScore;
    }

    public double getFinalScore() {
        return finalScore;
    }

    /**
     * Calculates the final score of the answer candidate based on the weighted sum of its individual scores.
     * @param semanticWeight The weight for the semantic score.
     * @param completenessWeight The weight for the completeness score.
     * @param relevanceWeight The weight for the relevance score.
     * @param lengthWeight The weight for the length score.
     * @param negationWeight The weight for the negation score.
     * @param temporalWeight The weight for the temporal score.
     */
    public void calculateFinalScore(double semanticWeight, double completenessWeight, double relevanceWeight, double lengthWeight, double negationWeight, double temporalWeight) {
        this.finalScore = (semanticScore * semanticWeight) +
                          (completenessScore * completenessWeight) +
                          (relevanceScore * relevanceWeight) +
                          (lengthScore * lengthWeight) +
                          (negationScore * negationWeight) +
                          (temporalScore * temporalWeight);
    }
}
