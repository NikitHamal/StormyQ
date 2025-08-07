package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import java.util.List;
import com.stormy.ai.models.AnswerResult;

public class AnswerValidator {

    public boolean isComplete(AnswerResult answer, String question) {
        List<String> questionPhrases = extractQuestionPhrases(question);
        if (questionPhrases.size() <= 1) {
            return true; // Not a complex question
        }
        for (String phrase : questionPhrases) {
            if (!answer.getAnswer().toLowerCase().contains(phrase)) {
                return false;
            }
        }
        return true;
    }

    private List<String> extractQuestionPhrases(String question) {
        // This method requires the OpenNLP models to be loaded.
        // For now, we will return an empty list.
        return new ArrayList<>();
    }

    public boolean isCoherent(AnswerResult answer) {
        // This method requires the OpenNLP models to be loaded.
        // For now, we will return true.
        return true;
    }

    public boolean isFactuallyConsistent(AnswerResult answer, String context) {
        // This method requires the OpenNLP models to be loaded.
        // For now, we will return true.
        return true;
    }

    public boolean isRelevant(AnswerResult answer, String question) {
        // This method requires the OpenNLP models to be loaded.
        // For now, we will return true.
        return true;
    }
}
