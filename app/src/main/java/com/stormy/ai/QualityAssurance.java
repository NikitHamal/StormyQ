package com.stormy.ai;

import com.stormy.ai.models.AnswerResult;

public class QualityAssurance {

    private final AnswerValidator answerValidator;

    public QualityAssurance() {
        this.answerValidator = new AnswerValidator();
    }

    public boolean isAnswerGood(AnswerResult answer, String question, String context) {
        return answerValidator.isComplete(answer, question) &&
               answerValidator.isCoherent(answer) &&
               answerValidator.isFactuallyConsistent(answer, context) &&
               answerValidator.isRelevant(answer, question);
    }

    public String getImprovementSuggestion(AnswerResult answer, String question, String context) {
        if (!answerValidator.isComplete(answer, question)) {
            return "The answer may be incomplete. It seems to be missing some parts of the question.";
        }
        if (!answerValidator.isCoherent(answer)) {
            return "The answer may not be coherent. It seems to be missing a clear subject or action.";
        }
        if (!answerValidator.isFactuallyConsistent(answer, context)) {
            return "The answer may not be factually consistent with the context. Some of the information seems to be new.";
        }
        if (!answerValidator.isRelevant(answer, question)) {
            return "The answer may not be relevant to the question. It does not seem to contain any of the keywords from the question.";
        }
        return "The answer seems good!";
    }
}
