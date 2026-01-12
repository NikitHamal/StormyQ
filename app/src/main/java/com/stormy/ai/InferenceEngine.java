package com.stormy.ai;

import java.util.List;

/**
 * A Bayesian-inspired inference engine for calculating answer probability.
 */
public class InferenceEngine {

    /**
     * Calculates the posterior probability P(Answer | Evidence)
     * using a simplified Bayesian update.
     * 
     * P(A|E) = (P(E|A) * P(A)) / P(E)
     */
    public double calculateProbability(double prior, Evidence evidence) {
        double likelihood = 1.0;

        // P(Evidence | Answer is Correct)
        if (evidence.keywordMatch) likelihood *= 0.8;
        else likelihood *= 0.2;

        if (evidence.roleMatch) likelihood *= 0.9;
        if (evidence.temporalMatch) likelihood *= 0.85;
        if (evidence.sentimentMatch) likelihood *= 0.7;

        // Simplified posterior calculation
        double posterior = prior * likelihood;
        
        // Normalize to [0, 1]
        return Math.min(1.0, posterior / (posterior + (1 - prior) * (1 - likelihood)));
    }

    public static class Evidence {
        public boolean keywordMatch;
        public boolean roleMatch;
        public boolean temporalMatch;
        public boolean sentimentMatch;

        public Evidence(boolean k, boolean r, boolean t, boolean s) {
            this.keywordMatch = k;
            this.roleMatch = r;
            this.temporalMatch = t;
            this.sentimentMatch = s;
        }
    }
}
