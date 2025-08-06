package com.stormy.ai.models;

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

    public void calculateFinalScore(double semanticWeight, double completenessWeight, double relevanceWeight, double lengthWeight, double negationWeight, double temporalWeight) {
        this.finalScore = (semanticScore * semanticWeight) +
                          (completenessScore * completenessWeight) +
                          (relevanceScore * relevanceWeight) +
                          (lengthScore * lengthWeight) +
                          (negationScore * negationWeight) +
                          (temporalScore * temporalWeight);
    }
}
