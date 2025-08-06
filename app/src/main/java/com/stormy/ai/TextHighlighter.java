package com.stormy.ai;

import android.graphics.Typeface; // Added import
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan; // Added import
import android.text.style.StyleSpan;
import com.stormy.ai.models.AnswerResult;

public class TextHighlighter {

    // Default amount of characters to include around the answer for a snippet
    private static final int DEFAULT_SNIPPET_PADDING = 100; // 100 characters before and after

    /**
     * Highlights a specified portion of a given text with a background color and makes it bold.
     * This method creates a SpannableString for Android's TextView.
     * Use this if you want to highlight the answer within the *entire* context.
     * @param fullText The complete text string.
     * @param result The AnswerResult containing start, end, and answer text.
     * @param highlightColor The color to use for the background highlight.
     * @param textColor The color to use for the foreground text.
     * @return A SpannableString with the specified range highlighted and bolded.
     */
    public SpannableString highlightAnswer(AnswerResult result, int highlightColor, int textColor) {
        if (!result.isValid()) {
            return new SpannableString(result.getHighlightedText()); // result.getHighlightedText() is the full context here
        }

        SpannableString spannable = new SpannableString(result.getHighlightedText());
        
        // Highlight background
        spannable.setSpan(
            new BackgroundColorSpan(highlightColor),
            result.getStartIndex(),
            result.getEndIndex(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        // Make text bold and change color
        spannable.setSpan(
            new StyleSpan(Typeface.BOLD),
            result.getStartIndex(),
            result.getEndIndex(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        spannable.setSpan(
            new ForegroundColorSpan(textColor),
            result.getStartIndex(),
            result.getEndIndex(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        return spannable;
    }

    /**
     * Extracts a snippet of text around the answer and then highlights the answer within that snippet.
     * This is useful for large contexts where only the relevant portion should be displayed.
     * @param fullContext The entire original text context.
     * @param result The AnswerResult containing start, end, and answer text within the full context.
     * @param highlightColor The color to use for the background highlight.
     * @param textColor The color to use for the foreground text.
     * @return A SpannableString containing the snippet with the answer highlighted.
     */
    public SpannableString highlightAnswerSnippet(String fullContext, AnswerResult result, int highlightColor, int textColor) {
        if (!result.isValid()) {
            return new SpannableString(result.getHighlightedText()); // Returns default "no answer found" text
        }

        int answerStartIndex = result.getStartIndex();
        int answerEndIndex = result.getEndIndex();

        // Calculate start and end for the snippet
        int snippetStart = Math.max(0, answerStartIndex - DEFAULT_SNIPPET_PADDING);
        int snippetEnd = Math.min(fullContext.length(), answerEndIndex + DEFAULT_SNIPPET_PADDING);

        // Adjust snippetStart to avoid cutting words in the middle, search for nearest whitespace
        if (snippetStart > 0 && Character.isLetterOrDigit(fullContext.charAt(snippetStart))) {
            int tempStart = fullContext.lastIndexOf(' ', snippetStart);
            if (tempStart != -1) {
                snippetStart = tempStart + 1;
            } else {
                snippetStart = 0; // If no space found, start from beginning
            }
        }

        // Adjust snippetEnd to avoid cutting words in the middle, search for nearest whitespace
        if (snippetEnd < fullContext.length() && Character.isLetterOrDigit(fullContext.charAt(snippetEnd - 1))) {
            int tempEnd = fullContext.indexOf(' ', snippetEnd);
            if (tempEnd != -1) {
                snippetEnd = tempEnd;
            } else {
                snippetEnd = fullContext.length(); // If no space found, end at end
            }
        }

        String snippetText = fullContext.substring(snippetStart, snippetEnd);

        // Calculate new start and end indices for the answer within the snippetText
        int newAnswerStartIndex = answerStartIndex - snippetStart;
        int newAnswerEndIndex = answerEndIndex - snippetStart;

        // Ensure new indices are within bounds of snippetText
        newAnswerStartIndex = Math.max(0, newAnswerStartIndex);
        newAnswerEndIndex = Math.min(snippetText.length(), newAnswerEndIndex);

        SpannableString spannable = new SpannableString(snippetText);

        // Highlight background
        if (newAnswerStartIndex < newAnswerEndIndex) {
            spannable.setSpan(
                new BackgroundColorSpan(highlightColor),
                newAnswerStartIndex,
                newAnswerEndIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            // Make text bold and change color
            spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                newAnswerStartIndex,
                newAnswerEndIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            spannable.setSpan(
                new ForegroundColorSpan(textColor),
                newAnswerStartIndex,
                newAnswerEndIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        return spannable;
    }
}
