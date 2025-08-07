package com.stormy.ai;

import java.util.*;

public class QuestionAnalyzer {
    public enum QuestionType {
        FACTUAL, ANALYTICAL, COMPARATIVE, CAUSAL, LIST, YESNO, NUMERICAL, DEFINITION, CONDITIONAL, AMBIGUOUS, UNKNOWN
    }

    /**
     * Classifies the question type using rules and keywords.
     */
    public static QuestionType classifyType(String question) {
        String q = question.trim().toLowerCase();
        if (q.startsWith("who") || q.startsWith("what") || q.startsWith("where") || q.startsWith("when")) return QuestionType.FACTUAL;
        if (q.startsWith("why")) return QuestionType.CAUSAL;
        if (q.startsWith("how many") || q.startsWith("how much") || q.contains("number") || q.contains("amount") || q.contains("percent")) return QuestionType.NUMERICAL;
        if (q.startsWith("how") && !q.startsWith("how many") && !q.startsWith("how much")) return QuestionType.ANALYTICAL;
        if (q.startsWith("is ") || q.startsWith("are ") || q.startsWith("was ") || q.startsWith("were ") || q.startsWith("do ") || q.startsWith("does ") || q.startsWith("did ")) return QuestionType.YESNO;
        if (q.startsWith("list") || q.contains("which of the following")) return QuestionType.LIST;
        if (q.contains("compare") || q.contains("difference between") || q.contains("vs.")) return QuestionType.COMPARATIVE;
        if (q.contains("define") || q.contains("definition of")) return QuestionType.DEFINITION;
        if (q.contains("if ") && q.contains(" then ")) return QuestionType.CONDITIONAL;
        if (isAmbiguous(question)) return QuestionType.AMBIGUOUS;
        return QuestionType.UNKNOWN;
    }

    /**
     * Extracts the focus of the question: subject, object, time, location, reason.
     * Uses simple heuristics and keyword patterns.
     */
    public static Map<String, String> extractFocus(String question) {
        Map<String, String> focus = new HashMap<>();
        String q = question.toLowerCase();
        // Subject
        if (q.startsWith("who") || q.startsWith("what")) focus.put("subject", "?");
        // Object
        if (q.contains("about ")) focus.put("object", q.substring(q.indexOf("about ") + 6).split(" ")[0]);
        // Time
        if (q.contains("when") || q.contains("date") || q.contains("year")) focus.put("time", "?");
        // Location
        if (q.contains("where") || q.contains("place") || q.contains("location")) focus.put("location", "?");
        // Reason
        if (q.contains("why") || q.contains("because") || q.contains("due to")) focus.put("reason", "?");
        return focus;
    }

    /**
     * Detects implicit assumptions in the question.
     */
    public static List<String> detectAssumptions(String question) {
        List<String> assumptions = new ArrayList<>();
        String q = question.toLowerCase();
        if (q.contains("why did") && !q.contains("if")) {
            String assumed = q.replaceAll(".*why did ([a-z ]+)[?]?.*", "$1");
            assumptions.add("Assumes that '" + assumed.trim() + "' happened.");
        }
        if (q.contains("how come")) assumptions.add("Assumes something unexpected occurred.");
        if (q.contains("since when")) assumptions.add("Assumes the event is ongoing.");
        return assumptions;
    }

    /**
     * Detects ambiguity in the question (multiple possible interpretations).
     */
    public static boolean isAmbiguous(String question) {
        String q = question.toLowerCase();
        // Heuristic: multiple question words, vague pronouns, or "or" in question
        int qwords = 0;
        for (String w : new String[]{"who","what","when","where","why","how"}) if (q.contains(w)) qwords++;
        if (qwords > 1) return true;
        if (q.contains(" or ")) return true;
        if (q.contains("thing") || q.contains("stuff") || q.contains("something")) return true;
        return false;
    }

    /**
     * Splits compound/conditional questions into sub-questions.
     */
    public static List<String> splitCompound(String question) {
        List<String> subs = new ArrayList<>();
        if (question.contains("? ")) {
            for (String part : question.split("\\? ")) {
                if (!part.trim().endsWith("?")) part = part.trim() + "?";
                subs.add(part.trim());
            }
        } else if (question.contains(" and ")) {
            for (String part : question.split(" and ")) subs.add(part.trim() + (part.trim().endsWith("?") ? "" : "?"));
        } else {
            subs.add(question.trim());
        }
        return subs;
    }
}