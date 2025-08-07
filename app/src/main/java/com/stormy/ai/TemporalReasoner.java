package com.stormy.ai;

import com.stormy.ai.models.TemporalInfo;
import java.util.List;

public class TemporalReasoner {
    /**
     * Determines the temporal relationship between two intervals.
     * Returns: "before", "after", "during", "overlaps", "meets", "starts", "finishes", or "none".
     */
    public static String getTemporalRelation(TemporalInfo a, TemporalInfo b) {
        if (a == null || b == null || !a.isValid() || !b.isValid()) return "none";
        long aStart = a.getStartTimeMillis();
        long aEnd = a.getEndTimeMillis();
        long bStart = b.getStartTimeMillis();
        long bEnd = b.getEndTimeMillis();
        if (aEnd < bStart) return "before";
        if (aStart > bEnd) return "after";
        if (aStart == bStart && aEnd == bEnd) return "equal";
        if (aStart >= bStart && aEnd <= bEnd) return "during";
        if (aStart <= bStart && aEnd >= bEnd) return "contains";
        if (aEnd > bStart && aStart < bStart && aEnd < bEnd) return "overlaps";
        if (aEnd == bStart) return "meets";
        if (aStart == bStart && aEnd < bEnd) return "starts";
        if (aEnd == bEnd && aStart > bStart) return "finishes";
        return "none";
    }

    /**
     * Given a question and answer, validate if the answer is temporally plausible.
     * Returns a result with explanation and confidence.
     */
    public static TemporalValidationResult validateTemporalAnswer(String question, String answer) {
        List<TextUtils.TemporalInfoResult> qTimes = TextUtils.extractAllTemporalInfo(question);
        List<TextUtils.TemporalInfoResult> aTimes = TextUtils.extractAllTemporalInfo(answer);
        if (qTimes.isEmpty() || aTimes.isEmpty()) {
            return new TemporalValidationResult(true, 0.5, "No clear temporal info in question or answer.");
        }
        for (TextUtils.TemporalInfoResult q : qTimes) {
            for (TextUtils.TemporalInfoResult a : aTimes) {
                String rel = getTemporalRelation(a.getTemporalInfo(), q.getTemporalInfo());
                if (rel.equals("during") || rel.equals("equal") || rel.equals("contains")) {
                    return new TemporalValidationResult(true, Math.min(q.getConfidence(), a.getConfidence()), "Answer is temporally valid (" + rel + ").");
                }
                if (rel.equals("before") || rel.equals("after") || rel.equals("overlaps")) {
                    return new TemporalValidationResult(false, 0.3, "Answer is temporally mismatched (" + rel + ").");
                }
            }
        }
        return new TemporalValidationResult(false, 0.2, "No matching temporal scope found.");
    }

    /**
     * Result for temporal answer validation.
     */
    public static class TemporalValidationResult {
        public final boolean valid;
        public final double confidence;
        public final String explanation;
        public TemporalValidationResult(boolean valid, double confidence, String explanation) {
            this.valid = valid;
            this.confidence = confidence;
            this.explanation = explanation;
        }
    }
}