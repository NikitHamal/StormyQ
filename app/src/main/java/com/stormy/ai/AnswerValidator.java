package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import java.util.List;
import com.stormy.ai.models.AnswerResult;

public class AnswerValidator {

    public boolean isComplete(AnswerResult answer, String question) {
        if (question.toLowerCase().contains(" and ")) {
            String[] parts = question.toLowerCase().split(" and ");
            if (parts.length > 1) {
                String firstPart = parts[0];
                String secondPart = parts[1];
                // This is a simple heuristic. A more advanced implementation would use dependency parsing.
                return answer.getAnswer().toLowerCase().contains(firstPart.substring(firstPart.indexOf(" ") + 1).trim()) &&
                       answer.getAnswer().toLowerCase().contains(secondPart.substring(0, secondPart.lastIndexOf(" ")).trim());
            }
        }
        return true;
    }

    public boolean isCoherent(AnswerResult answer) {
        // A simple heuristic to check for coherence.
        // A more advanced implementation would use a language model.
        List<String> tokens = TextUtils.tokenize(answer.getAnswer());
        if (tokens.size() < 3) {
            return false;
        }
        boolean hasNoun = false;
        boolean hasVerb = false;
        for (String token : tokens) {
            if (isNoun(token)) {
                hasNoun = true;
            }
            if (isVerb(token)) {
                hasVerb = true;
            }
        }
        return hasNoun && hasVerb;
    }

    public boolean isFactuallyConsistent(AnswerResult answer, String context) {
        // A simple heuristic to check for factual consistency.
        // A more advanced implementation would use natural language inference.
        List<String> answerTokens = TextUtils.tokenize(answer.getAnswer());
        List<String> contextTokens = TextUtils.tokenize(context);
        for (String token : answerTokens) {
            if (!contextTokens.contains(token)) {
                return false;
            }
        }
        return true;
    }

    public boolean isRelevant(AnswerResult answer, String question) {
        // A simple heuristic to check for relevance.
        // A more advanced implementation would use a semantic similarity model.
        List<String> questionTokens = TextUtils.tokenize(question);
        List<String> answerTokens = TextUtils.tokenize(answer.getAnswer());
        for (String qToken : questionTokens) {
            if (answerTokens.contains(qToken)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNoun(String word) {
        // A simple heuristic based on a predefined list of common noun endings.
        // This is not a comprehensive list and should be expanded.
        String lowerCaseWord = word.toLowerCase();
        return lowerCaseWord.endsWith("tion") || lowerCaseWord.endsWith("sion") || lowerCaseWord.endsWith("ment") ||
               lowerCaseWord.endsWith("ness") || lowerCaseWord.endsWith("ity") || lowerCaseWord.endsWith("er") ||
               lowerCaseWord.endsWith("or") || lowerCaseWord.endsWith("ist") || lowerCaseWord.endsWith("ism");
    }

    private boolean isVerb(String word) {
        // A simple heuristic based on a predefined list of common verb endings.
        // This is not a comprehensive list and should be expanded.
        String lowerCaseWord = word.toLowerCase();
        return lowerCaseWord.endsWith("ed") || lowerCaseWord.endsWith("ing") || lowerCaseWord.endsWith("ize") ||
               lowerCaseWord.endsWith("ate") || lowerCaseWord.endsWith("ify");
    }
}
