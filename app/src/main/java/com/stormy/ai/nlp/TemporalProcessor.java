package com.stormy.ai.nlp;

import com.stormy.ai.models.TemporalInfo;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and processes temporal information from text.
 */
public class TemporalProcessor {

    /**
     * Attempts to extract temporal information from a word or short phrase.
     */
    public TemporalInfo extractTemporalInfo(String input) {
        if (input == null || input.isEmpty()) return null;
        String word = input.toLowerCase(Locale.ENGLISH).trim();

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH);
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);

        cal.set(currentYear, currentMonth, currentDay, 0, 0, 0);
        long startOfDay = cal.getTimeInMillis();
        cal.set(currentYear, currentMonth, currentDay, 23, 59, 59);
        long endOfDay = cal.getTimeInMillis();

        switch (word) {
            case "today":
                return new TemporalInfo("today", startOfDay, endOfDay);
            case "yesterday":
                return new TemporalInfo("yesterday", startOfDay - 86400000L, endOfDay - 86400000L);
            case "tomorrow":
                return new TemporalInfo("tomorrow", startOfDay + 86400000L, endOfDay + 86400000L);
            case "now":
                return new TemporalInfo("now", System.currentTimeMillis(), System.currentTimeMillis());
        }

        // Year matching
        if (word.matches("\\d{4}")) {
            try {
                int year = Integer.parseInt(word);
                cal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
                long start = cal.getTimeInMillis();
                cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
                long end = cal.getTimeInMillis();
                return new TemporalInfo(word, start, end);
            } catch (NumberFormatException ignored) {}
        }

        // Specific date format: Month Day, Year
        Pattern datePattern = Pattern.compile("([a-z]+)\\s+(\\d{1,2}),?\\s*(\\d{4})");
        Matcher matcher = datePattern.matcher(word);
        if (matcher.find()) {
            try {
                int month = getMonthIndex(matcher.group(1));
                int day = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                if (month != -1) {
                    cal.set(year, month, day, 0, 0, 0);
                    long start = cal.getTimeInMillis();
                    cal.set(year, month, day, 23, 59, 59);
                    long end = cal.getTimeInMillis();
                    return new TemporalInfo(word, start, end);
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    private int getMonthIndex(String monthName) {
        switch (monthName) {
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
}
