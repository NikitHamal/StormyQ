package com.stormy.ai;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.stormy.ai.models.TemporalInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet; // Changed from Collections.unmodifiableSet to mutable HashSet
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

    // A basic set of common English stop words. Now mutable for training.
    private static Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "of", "in", "on", "at", "by", "for", "with",
            "from", "about", "as", "into", "through", "during", "before", "after", "above", "below",
            "to", "from", "up", "down", "out", "off", "over", "under", "again", "further", "then",
            "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each",
            "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"
    ));

    // A list of common negation words that will influence reasoning. Now mutable for training.
    private static Set<String> NEGATION_WORDS = new HashSet<>(Arrays.asList(
            "not", "no", "never", "n't", "none", "neither", "nor", "without", "hardly", "barely", "scarcely", "cannot"
    ));

    // A basic set of temporal keywords. Now mutable for training.
    private static Set<String> TEMPORAL_KEYWORDS = new HashSet<>(Arrays.asList(
            "today", "yesterday", "tomorrow", "now", "then", "ago", "later", "before", "after",
            "morning", "afternoon", "evening", "night", "day", "week", "month", "year", "decade", "century",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "january", "february", "march", "april", "may", "june", "july", "august", "september",
            "october", "november", "december",
            "current", "past", "future", "present", "recent", "old", "new", "long", "short",
            "early", "late", "since", "until", "during", "when", "while", "annual", "daily", "hourly"
    ));

    // Simple sets for sentiment analysis. Now mutable for training.
    private static Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "good", "great", "excellent", "wonderful", "amazing", "happy", "joy", "positive", "beautiful", "love",
            "best", "fantastic", "awesome", "perfect", "strong", "success", "benefit", "advantage", "clear", "bright"
    ));

    private static Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
            "bad", "terrible", "horrible", "awful", "sad", "unhappy", "negative", "ugly", "hate",
            "worst", "disaster", "poor", "weak", "failure", "problem", "issue", "difficult", "dark", "trouble"
    ));


    /**
     * Highlights a specified portion of a given text with a background color and makes it bold.
     * This method creates a SpannableString for Android's TextView.
     * @param fullText The complete text string.
     * @param start The starting index of the text to highlight.
     * @param end The ending index of the text to highlight (exclusive).
     * @param highlightColor The color to use for the background highlight.
     * @param textColor The color to use for the foreground text.
     * @return A SpannableString with the specified range highlighted and bolded.
     */
    public static SpannableString highlightText(String fullText, int start, int end, int highlightColor, int textColor) {
        // Ensure start and end indices are within the bounds of the fullText
        start = Math.max(0, start);
        end = Math.min(fullText.length(), end);

        SpannableString spannableString = new SpannableString(fullText);
        
        // Apply background color span if the range is valid
        if (start < end) {
            spannableString.setSpan(
                new BackgroundColorSpan(highlightColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE // Does not expand if text is added at start/end
            );
            
            // Apply bold style span if the range is valid
            spannableString.setSpan(
                new StyleSpan(Typeface.BOLD), // Use Typeface.BOLD for bold style
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // Apply foreground color span
            spannableString.setSpan(
                new ForegroundColorSpan(textColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        return spannableString;
    }

    /**
     * Splits a given text into sentences. This method uses an improved regex
     * to handle common abbreviations and ensures correct sentence termination.
     * @param text The input text to be split.
     * @return An array of strings, where each string is a sentence.
     */
    public static String[] splitSentences(String text) {
        // Regex to split sentences:
        // (?<![A-Z][a-z]\.) - Negative lookbehind: prevents splitting after abbreviations like "Mr." or "Dr."
        // (?<=\.|\?|\!)    - Positive lookbehind: ensures split occurs after ., ?, or !
        // \s+              - Matches one or more whitespace characters
        return text.split("(?<![A-Z][a-z]\\.)(?<=\\.|\\?|\\!)\\s+");
    }

    /**
     * Checks if a given text is a potential answer based on length and word count.
     * This helps filter out overly short, overly long, or irrelevant snippets.
     * @param text The text to evaluate.
     * @return true if the text meets the criteria for a potential answer, false otherwise.
     */
    public static boolean isPotentialAnswer(String text) {
        if (text == null || text.isEmpty()) return false;
        // Answer should be at least 3 characters and not excessively long (e.g., up to 200 chars)
        if (text.length() < 3 || text.length() > 200) return false;
        // Answer should not be excessively verbose (e.g., max 40 words)
        if (text.split(" ").length > 40) return false;
        return true;
    }

    /**
     * Performs basic tokenization by splitting text into words,
     * removing non-alphanumeric characters (except spaces within words for now),
     * and converting to lowercase.
     * @param text The input text.
     * @return A list of processed tokens (words).
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        // Pattern to match words (alphanumeric sequences and internal hyphens/apostrophes)
        // \b ensures word boundaries
        Pattern pattern = Pattern.compile("\\b[\\p{L}0-9'-]+\\b"); // Supports Unicode letters, numbers, hyphen, apostrophe
        Matcher matcher = pattern.matcher(text.toLowerCase());
        while (matcher.find()) {
            String token = matcher.group();
            // Optional: Filter out single hyphens or apostrophes if they appear as tokens
            if (token.equals("-") || token.equals("'")) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    /**
     * Applies a very basic stemming algorithm to a word.
     * This is a simple rule-based approach and not a full linguistic stemmer.
     * Removes common English suffixes.
     * @param word The word to stem.
     * @return The stemmed word.
     */
    public static String stem(String word) {
        word = word.toLowerCase(); // Work with lowercase

        // Rule 1: Plural forms (sses, ies, ss, s)
        if (word.endsWith("sses")) return word.substring(0, word.length() - 2); // classes -> class
        if (word.endsWith("ies")) return word.substring(0, word.length() - 3) + "y"; // flies -> fly, parties -> party
        if (word.endsWith("s") && word.length() > 2 && !word.endsWith("ss")) return word.substring(0, word.length() - 1); // cats -> cat

        // Rule 2: -ed, -ing suffixes
        if (word.endsWith("eed")) { // e.g., "agreed" -> "agree"
            if (word.length() > 4 && hasVowel(word.substring(0, word.length() - 3))) {
                return word.substring(0, word.length() - 1);
            }
        }
        if (word.endsWith("ed")) {
            String temp = word.substring(0, word.length() - 2);
            if (temp.endsWith("at") || temp.endsWith("bl") || temp.endsWith("iz")) return temp + "e";
            if (hasVowel(temp)) {
                // Handle double consonants: "felled" -> "fel", "hopped" -> "hop"
                if (temp.length() >= 2 && temp.charAt(temp.length()-1) == temp.charAt(temp.length()-2) &&
                    !("lsz".indexOf(temp.charAt(temp.length()-1)) >= 0)) { // Don't remove for l, s, z
                    return temp.substring(0, temp.length() - 1);
                }
                return temp;
            }
        }
        if (word.endsWith("ing")) {
            String temp = word.substring(0, word.length() - 3);
            if (temp.endsWith("e") && !hasVowel(temp.substring(0, temp.length()-1))) return temp; // drive -> driv
            if (hasVowel(temp)) {
                 // Handle double consonants: "running" -> "run"
                if (temp.length() >= 2 && temp.charAt(temp.length()-1) == temp.charAt(temp.length()-2) &&
                    !("lsz".indexOf(temp.charAt(temp.length()-1)) >= 0)) {
                    return temp.substring(0, temp.length() - 1);
                }
                return temp;
            }
        }

        // Rule 3: -y to -i (e.g., "happy" -> "happi", "lovely" -> "loveli")
        if (word.endsWith("y") && word.length() > 2 && !hasVowel(word.substring(word.length() - 2, word.length() - 1))) {
            return word.substring(0, word.length() - 1) + "i";
        }

        // Rule 4: Common noun/adjective suffixes
        if (word.endsWith("tional")) return word.substring(0, word.length() - 2); // conditional -> condit
        if (word.endsWith("enci")) return word.substring(0, word.length() - 1) + "e"; // urgency -> urgenc
        if (word.endsWith("anci")) return word.substring(0, word.length() - 1) + "e"; // tenancy -> tenanc
        if (word.endsWith("izer")) return word.substring(0, word.length() - 1) + "ise"; // satirizer -> satirise
        if (word.endsWith("bli")) return word.substring(0, word.length() - 1) + "ble"; // possibly -> possibl
        if (word.endsWith("alli")) return word.substring(0, word.length() - 2) + "al"; // finally -> fin
        if (word.endsWith("entli")) return word.substring(0, word.length() - 2) + "ent"; // frequently -> frequent
        if (word.endsWith("eli")) return word.substring(0, word.length() - 2) + "e"; // likely -> lik
        if (word.endsWith("ousli")) return word.substring(0, word.length() - 2) + "ous"; // furiously -> furious
        if (word.endsWith("ization")) return word.substring(0, word.length() - 5) + "ize"; // modernization -> modernize
        if (word.endsWith("ation")) return word.substring(0, word.length() - 3) + "ate"; // relation -> relate
        if (word.endsWith("ator")) return word.substring(0, word.length() - 2) + "ate"; // operator -> operate
        if (word.endsWith("alism")) return word.substring(0, word.length() - 3); // feminism -> femin
        if (word.endsWith("iviti")) return word.substring(0, word.length() - 3) + "ive"; // sensitivity -> sensitive
        if (word.endsWith("aliti")) return word.substring(0, word.length() - 3) + "al"; // generality -> general
        if (word.endsWith("biliti")) return word.substring(0, word.length() - 5) + "ble"; // disability -> disable
        if (word.endsWith("logi")) return word.substring(0, word.length() - 1) + "log"; // biology -> biolog

        // Rule 5: Remove -e if it follows a consonant-vowel-consonant sequence and is not a "special" case
        // e.g., "hope" -> "hop", "love" -> "lov" (but not "move" -> "mov" or "like" -> "lik")
        if (word.endsWith("e") && word.length() > 3) {
            String stem = word.substring(0, word.length() - 1);
            if (countVowelGroups(stem) > 1 || (countVowelGroups(stem) == 1 && endsWithCVC(stem) && !endsWithLsz(stem))) {
                 return stem;
            }
        }


        return word; // Return original if no rule applies or rules not applicable
    }

    /**
     * Helper for stemming: Checks if a string contains at least one vowel.
     */
    private static boolean hasVowel(String s) {
        return s.matches(".*[aeiouAEIOU].*");
    }

    /**
     * Helper for stemming: Counts vowel groups (sequences of vowels) in a string.
     * Used for Porter Stemmer-like 'measure' (m).
     */
    private static int countVowelGroups(String s) {
        int m = 0;
        boolean inVowelGroup = false;
        for (char c : s.toCharArray()) {
            boolean isVowel = "aeiou".indexOf(c) != -1;
            if (isVowel && !inVowelGroup) {
                inVowelGroup = true;
            } else if (!isVowel && inVowelGroup) {
                m++;
                inVowelGroup = false;
            }
        }
        if (inVowelGroup) m++; // If ends with a vowel group
        return m;
    }

    /**
     * Helper for stemming: Checks if a string ends with a CVC (consonant-vowel-consonant) pattern,
     * where the last consonant is not W, X, or Y.
     */
    private static boolean endsWithCVC(String s) {
        int len = s.length();
        if (len < 3) return false;
        char c1 = s.charAt(len - 3);
        char v = s.charAt(len - 2);
        char c2 = s.charAt(len - 1);

        return !isVowelChar(c1) && isVowelChar(v) && !isVowelChar(c2) &&
               !("wxy".indexOf(c2) >= 0); // c2 is not w, x, or y
    }

    /**
     * Helper for stemming: Checks if a string ends with l, s, or z.
     */
    private static boolean endsWithLsz(String s) {
        if (s.isEmpty()) return false;
        char lastChar = s.charAt(s.length() - 1);
        return "lsz".indexOf(lastChar) >= 0;
    }

    /**
     * Helper for stemming: Checks if a character is a vowel.
     */
    private static boolean isVowelChar(char c) {
        return "aeiou".indexOf(c) != -1;
    }


    /**
     * Checks if a given word is a stop word.
     * @param word The word to check.
     * @return true if it's a stop word, false otherwise.
     */
    public static boolean isStopWord(String word) {
        return STOP_WORDS.contains(word.toLowerCase());
    }

    /**
     * Checks if a given word is a negation word.
     * @param word The word to check.
     * @return true if it's a negation word, false otherwise.
     */
    public static boolean isNegationWord(String word) {
        return NEGATION_WORDS.contains(word.toLowerCase());
    }

    /**
     * Checks if a given word is a temporal keyword.
     * @param word The word to check.
     * @return true if it's a temporal keyword, false otherwise.
     */
    public static boolean isTemporalKeyword(String word) {
        return TEMPORAL_KEYWORDS.contains(word.toLowerCase());
    }

    /**
     * Performs a very basic sentiment analysis on a given text.
     * It counts positive and negative words and returns a score.
     * Score > 0 indicates positive sentiment.
     * Score < 0 indicates negative sentiment.
     * Score = 0 indicates neutral sentiment.
     *
     * @param text The input text for sentiment analysis.
     * @return An integer representing the sentiment score.
     */
    public static int getSentimentScore(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        List<String> tokens = tokenize(text);
        int score = 0;
        boolean isInNegation = false;

        for (String token : tokens) {
            // Check for negation words
            if (isNegationWord(token)) {
                isInNegation = true;
                continue; // Don't count negation word itself towards sentiment
            }

            // Apply sentiment based on word and negation context
            if (POSITIVE_WORDS.contains(token)) {
                score += (isInNegation ? -1 : 1);
            } else if (NEGATIVE_WORDS.contains(token)) {
                score += (isInNegation ? 1 : -1);
            }

            // Reset negation after a word (simple model; more complex would manage scope)
            isInNegation = false;
        }
        return score;
    }


    /**
     * Very basic temporal expression extraction.
     * This method attempts to assign a numerical 'time' to simple temporal keywords.
     * In a real application, this would involve a robust NLP date/time parser.
     * For now, it just returns a `TemporalInfo` with a raw expression and dummy timestamps.
     *
     * @param word The temporal keyword found.
     * @return A TemporalInfo object or null if not a recognized simple temporal keyword.
     */
    public static TemporalInfo extractTemporalInfo(String word) {
        long currentTime = System.currentTimeMillis(); // Use current time as a reference
        word = word.toLowerCase();

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH); // 0-indexed
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);

        cal.set(currentYear, currentMonth, currentDay, 0, 0, 0);
        long startOfDay = cal.getTimeInMillis();
        cal.set(currentYear, currentMonth, currentDay, 23, 59, 59);
        long endOfDay = cal.getTimeInMillis();

        switch (word) {
            case "today":
                return new TemporalInfo("today", startOfDay, endOfDay, TemporalInfo.ExpressionType.DATE, false);
            case "yesterday":
                return new TemporalInfo("yesterday", startOfDay - (24 * 60 * 60 * 1000L), endOfDay - (24 * 60 * 60 * 1000L), TemporalInfo.ExpressionType.DATE, false);
            case "tomorrow":
                return new TemporalInfo("tomorrow", startOfDay + (24 * 60 * 60 * 1000L), endOfDay + (24 * 60 * 60 * 1000L), TemporalInfo.ExpressionType.DATE, false);
            case "now":
                return new TemporalInfo("now", currentTime, currentTime, TemporalInfo.ExpressionType.DATE, false);
            case "last week":
                return new TemporalInfo("last week", startOfDay - (7 * 24 * 60 * 60 * 1000L), endOfDay - (7 * 24 * 60 * 60 * 1000L), TemporalInfo.ExpressionType.DURATION, false);
            case "next week":
                return new TemporalInfo("next week", startOfDay + (7 * 24 * 60 * 60 * 1000L), endOfDay + (7 * 24 * 60 * 60 * 1000L), TemporalInfo.ExpressionType.DURATION, false);
            case "last month":
                cal.add(Calendar.MONTH, -1);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                long lastMonthStart = cal.getTimeInMillis();
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                long lastMonthEnd = cal.getTimeInMillis();
                return new TemporalInfo("last month", lastMonthStart, lastMonthEnd, TemporalInfo.ExpressionType.DURATION, false);
            case "next month":
                cal.add(Calendar.MONTH, 1);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                long nextMonthStart = cal.getTimeInMillis();
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                long nextMonthEnd = cal.getTimeInMillis();
                return new TemporalInfo("next month", nextMonthStart, nextMonthEnd, TemporalInfo.ExpressionType.DURATION, false);
            case "last year":
                cal.set(currentYear - 1, Calendar.JANUARY, 1, 0, 0, 0);
                long lastYearStart = cal.getTimeInMillis();
                cal.set(currentYear - 1, Calendar.DECEMBER, 31, 23, 59, 59);
                long lastYearEnd = cal.getTimeInMillis();
                return new TemporalInfo("last year", lastYearStart, lastYearEnd, TemporalInfo.ExpressionType.DURATION, false);
            case "next year":
                cal.set(currentYear + 1, Calendar.JANUARY, 1, 0, 0, 0);
                long nextYearStart = cal.getTimeInMillis();
                cal.set(currentYear + 1, Calendar.DECEMBER, 31, 23, 59, 59);
                long nextYearEnd = cal.getTimeInMillis();
                return new TemporalInfo("next year", nextYearStart, nextYearEnd, TemporalInfo.ExpressionType.DURATION, false);
            case "two years ago":
                cal.set(currentYear - 2, Calendar.JANUARY, 1, 0, 0, 0);
                long twoYearsAgoStart = cal.getTimeInMillis();
                cal.set(currentYear - 2, Calendar.DECEMBER, 31, 23, 59, 59);
                long twoYearsAgoEnd = cal.getTimeInMillis();
                return new TemporalInfo("two years ago", twoYearsAgoStart, twoYearsAgoEnd, TemporalInfo.ExpressionType.DURATION, false);
            case "in the past": // Broad temporal marker
            case "historically":
                return new TemporalInfo(word, null, currentTime, TemporalInfo.ExpressionType.DURATION, true); // Ends now, started indefinitely in past
            case "in the future": // Broad temporal marker
            case "soon":
                return new TemporalInfo(word, currentTime, null, TemporalInfo.ExpressionType.DURATION, true); // Starts now, ends indefinitely in future

            // Add months
            case "january": case "february": case "march": case "april": case "may": case "june":
            case "july": case "august": case "september": case "october": case "november": case "december":
                try {
                    Calendar monthCal = Calendar.getInstance();
                    monthCal.set(currentYear, getMonthIndex(word), 1, 0, 0, 0);
                    long monthStart = monthCal.getTimeInMillis();
                    monthCal.set(Calendar.DAY_OF_MONTH, monthCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    long monthEnd = monthCal.getTimeInMillis();
                    return new TemporalInfo(word, monthStart, monthEnd, TemporalInfo.ExpressionType.DURATION, false);
                } catch (Exception e) { /* Fall through */ }


            default:
                // For year numbers (e.g., "1990")
                if (word.matches("\\d{4}")) {
                    try {
                        int year = Integer.parseInt(word);
                        Calendar yearCal = Calendar.getInstance();
                        yearCal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
                        long yearStart = yearCal.getTimeInMillis();
                        yearCal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
                        long yearEnd = yearCal.getTimeInMillis();
                        return new TemporalInfo(word, yearStart, yearEnd, TemporalInfo.ExpressionType.DURATION, false);
                    } catch (NumberFormatException e) {
                        // Not a valid year number, continue
                    }
                }
                // For specific dates like "July 4, 1776" (simplified, needs full parser for robustness)
                Pattern datePattern = Pattern.compile("([A-Za-z]+)\\s+(\\d{1,2}),\\s*(\\d{4})");
                Matcher dateMatcher = datePattern.matcher(word);
                if (dateMatcher.find()) {
                    try {
                        int month = getMonthIndex(dateMatcher.group(1));
                        int day = Integer.parseInt(dateMatcher.group(2));
                        int year = Integer.parseInt(dateMatcher.group(3));
                        
                        Calendar specificDateCal = Calendar.getInstance();
                        specificDateCal.set(year, month, day, 0, 0, 0);
                        long dateStart = specificDateCal.getTimeInMillis();
                        specificDateCal.set(year, month, day, 23, 59, 59);
                        long dateEnd = specificDateCal.getTimeInMillis();
                        return new TemporalInfo(word, dateStart, dateEnd, TemporalInfo.ExpressionType.DATE, false);
                    } catch (Exception e) { /* Fall through */ }
                }
                return null;
        }
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

