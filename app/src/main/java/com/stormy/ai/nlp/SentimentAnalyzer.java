package com.stormy.ai.nlp;

import com.stormy.ai.TextUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles sentiment analysis with improved keyword dictionaries and negation awareness.
 */
public class SentimentAnalyzer {

    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "good", "great", "excellent", "wonderful", "amazing", "happy", "joy", "positive", "beautiful", "love",
            "best", "fantastic", "awesome", "perfect", "strong", "success", "benefit", "advantage", "clear", "bright",
            "brilliant", "outstanding", "superb", "terrific", "valuable", "worthy", "winner", "satisfied", "pleased",
            "grateful", "inspired", "delighted", "cheerful", "optimistic", "efficient", "reliable", "secure", "safe"
    ));

    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
            "bad", "terrible", "horrible", "awful", "sad", "unhappy", "negative", "ugly", "hate",
            "worst", "disaster", "poor", "weak", "failure", "problem", "issue", "difficult", "dark", "trouble",
            "nasty", "dreadful", "miserable", "annoying", "disgusting", "useless", "broken", "angry", "fear", "scary",
            "dangerous", "harmful", "toxic", "vulnerable", "risky", "unstable", "shame", "guilt", "pain", "hurt"
    ));

    /**
     * Analyzes sentiment of a text, returning a score where > 0 is positive, < 0 is negative.
     * @param text The input text.
     * @return Sentiment score.
     */
    public int getSentimentScore(String text) {
        if (text == null || text.isEmpty()) return 0;

        List<String> tokens = TextUtils.tokenize(text);
        int score = 0;
        int negationScope = 0;

        for (String token : tokens) {
            if (TextUtils.isNegationWord(token)) {
                negationScope = 3; // Negation affects next 3 words
                continue;
            }

            int wordSentiment = 0;
            if (POSITIVE_WORDS.contains(token)) {
                wordSentiment = 1;
            } else if (NEGATIVE_WORDS.contains(token)) {
                wordSentiment = -1;
            }

            if (wordSentiment != 0) {
                if (negationScope > 0) {
                    score -= wordSentiment; // Invert sentiment
                } else {
                    score += wordSentiment;
                }
            }

            if (negationScope > 0) negationScope--;
        }
        return score;
    }
}
