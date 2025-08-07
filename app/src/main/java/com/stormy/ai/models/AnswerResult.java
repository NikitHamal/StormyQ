package com.stormy.ai.models;

public class AnswerResult {
    private String answer;
    private String highlightedText;
    private int startIndex;
    private int endIndex;
    private double confidence; // Stores the confidence score directly

    public AnswerResult(String answer, String highlightedText, int startIndex, int endIndex, double confidence) {
        this.answer = answer;
        this.highlightedText = highlightedText;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.confidence = confidence;
    }

    // Getters
    public String getAnswer() { return answer; }
    public String getHighlightedText() { return highlightedText; }
    public int getStartIndex() { return startIndex; }
    public int getEndIndex() { return endIndex; }
    public double getConfidence() { return confidence; }

    /**
     * Determines if the answer is considered valid based on its content and confidence.
     * The confidence threshold is now defined here.
     * @return true if the answer is valid, false otherwise.
     */
    public boolean isValid() {
        // Answer is valid if it's not null/empty and confidence is above a reasonable threshold
        return answer != null && !answer.isEmpty() && confidence >= 0.15; // Adjusted threshold for semantic network
    }

    /**
     * Provides a reasoning score, potentially combining initial confidence
     * with any boosts from rule applications or other reasoning layers.
     * For now, it returns the confidence, but could be extended.
     * @return The reasoning score of the answer.
     */
    public double getReasoningScore() {
        // In future, this could be a more complex aggregation:
        // return this.confidence * (1 + rule_boost_factor) * (1 - negation_penalty);
        return this.confidence;
    }

    /**
     * Allows setting the confidence dynamically, for instance, if meta-cognition
     * processes decide to adjust it based on further analysis.
     * @param confidence The new confidence value.
     */
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String formatAnswer(String question) {
        if (question.toLowerCase().startsWith("what are") || question.toLowerCase().startsWith("list")) {
            // Simple list formatting
            String[] items = this.answer.split(",| and ");
            StringBuilder formattedAnswer = new StringBuilder();
            for (String item : items) {
                formattedAnswer.append("- ").append(item.trim()).append("\n");
            }
            return formattedAnswer.toString();
        }
        return this.answer;
    }
}
