package com.stormy.ai;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.stormy.ai.models.TemporalInfo;
import com.stormy.ai.nlp.PorterStemmer;
import com.stormy.ai.nlp.SentimentAnalyzer;
import com.stormy.ai.nlp.TemporalProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet; 
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

    private static final PorterStemmer stemmer = new PorterStemmer();
    private static final SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();
    private static final TemporalProcessor temporalProcessor = new TemporalProcessor();

    // A basic set of common English stop words.
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "of", "in", "on", "at", "by", "for", "with",
            "from", "about", "as", "into", "through", "during", "before", "after", "above", "below",
            "to", "from", "up", "down", "out", "off", "over", "under", "again", "further", "then",
            "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each",
            "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"
    ));

    private static final Set<String> NEGATION_WORDS = new HashSet<>(Arrays.asList(
            "not", "no", "never", "n't", "none", "neither", "nor", "without", "hardly", "barely", "scarcely", "cannot"
    ));

    /**
     * Highlights a specified portion of a given text with a background color and makes it bold.
     */
    public static SpannableString highlightText(String fullText, int start, int end, int highlightColor, int textColor) {
        start = Math.max(0, start);
        end = Math.min(fullText.length(), end);
        SpannableString spannableString = new SpannableString(fullText);
        
        if (start < end) {
            spannableString.setSpan(new BackgroundColorSpan(highlightColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ForegroundColorSpan(textColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    public static String[] splitSentences(String text) {
        return text.split("(?<![A-Z][a-z]\\.)(?<=\\.|\\?|\\!)\\s+");
    }

    public static boolean isPotentialAnswer(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.length() >= 3 && text.length() <= 200 && text.split("\\s+").length <= 40;
    }

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b[\\p{L}0-9'-]+\\b");
        Matcher matcher = pattern.matcher(text.toLowerCase());
        while (matcher.find()) {
            String token = matcher.group();
            if (!token.equals("-") && !token.equals("'")) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public static String stem(String word) {
        return stemmer.stem(word);
    }

    public static boolean isStopWord(String word) {
        return STOP_WORDS.contains(word.toLowerCase());
    }

    public static boolean isNegationWord(String word) {
        return NEGATION_WORDS.contains(word.toLowerCase());
    }

    public static int getSentimentScore(String text) {
        return sentimentAnalyzer.getSentimentScore(text);
    }

    public static TemporalInfo extractTemporalInfo(String word) {
        return temporalProcessor.extractTemporalInfo(word);
    }

    // Helper method to get month index from month name
    private static int getMonthIndex(String monthName) {
        switch (monthName.toLowerCase()) {
            case "january": return Calendar.JANUARY;
            case "february": return Calendar.FEBRUARY;
            case "march": return Calendar.MARCH;
            case "april": return Calendar.APRIL;
            case "may": return Calendar.MAY;
            case "june": return Calendar.JUNE;
            case "july": return Calendar.JULY;
            case "august": return Calendar.AUGUST;
            case "september": return Calendar.SEPTEMBER;
            case "october": return Calendar.OCTOBER;
            case "november": return Calendar.NOVEMBER;
            case "december": return Calendar.DECEMBER;
            default: return -1;
        }
    }


    // --- Methods for modifying word sets (for training/customization) ---

    public static Set<String> getStopWords() {
        return STOP_WORDS;
    }

    public static void addStopWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            STOP_WORDS.add(word.trim().toLowerCase());
        }
    }

    public static void removeStopWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            STOP_WORDS.remove(word.trim().toLowerCase());
        }
    }

    public static Set<String> getNegationWords() {
        return NEGATION_WORDS;
    }

    public static void addNegationWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            NEGATION_WORDS.add(word.trim().toLowerCase());
        }
    }

    public static void removeNegationWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            NEGATION_WORDS.remove(word.trim().toLowerCase());
        }
    }

    public static Set<String> getPositiveWords() {
        return POSITIVE_WORDS;
    }

    public static void addPositiveWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            POSITIVE_WORDS.add(word.trim().toLowerCase());
        }
    }

    public static void removePositiveWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            POSITIVE_WORDS.remove(word.trim().toLowerCase());
        }
    }

    public static Set<String> getNegativeWords() {
        return NEGATIVE_WORDS;
    }

    public static void addNegativeWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            NEGATIVE_WORDS.add(word.trim().toLowerCase());
        }
    }

    public static void removeNegativeWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            NEGATIVE_WORDS.remove(word.trim().toLowerCase());
        }
    }
}