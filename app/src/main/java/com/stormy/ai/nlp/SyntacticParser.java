package com.stormy.ai.nlp;

import com.stormy.ai.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A lightweight syntactic parser to extract Subject-Verb-Object (SVO) relationships.
 */
public class SyntacticParser {

    private static final Set<String> COMMON_VERBS = new HashSet<>(Arrays.asList(
            "is", "are", "was", "were", "has", "have", "had", "do", "does", "did",
            "built", "created", "discovered", "invented", "wrote", "painted", "defeated",
            "won", "lost", "born", "died", "lives", "works", "located", "found"
    ));

    public static class SVOTriplet {
        public String subject;
        public String verb;
        public String object;

        public SVOTriplet(String s, String v, String o) {
            this.subject = s;
            this.verb = v;
            this.object = o;
        }

        @Override
        public String toString() {
            return String.format("[%s] --(%s)--> [%s]", subject, verb, object);
        }
    }

    /**
     * Extracts SVO triplets from a sentence.
     */
    public List<SVOTriplet> parse(String sentence) {
        List<SVOTriplet> triplets = new ArrayList<>();
        List<String> tokens = TextUtils.tokenize(sentence);
        if (tokens.size() < 3) return triplets;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (isVerb(token, i, tokens)) {
                String subject = extractSubject(i, tokens);
                String object = extractObject(i, tokens);
                if (!subject.isEmpty() && !object.isEmpty()) {
                    triplets.add(new SVOTriplet(subject, token, object));
                }
            }
        }
        return triplets;
    }

    private boolean isVerb(String token, int index, List<String> tokens) {
        if (COMMON_VERBS.contains(token)) return true;
        // Basic heuristic: words ending in -ed, -ing, -s (if not a noun)
        if (token.endsWith("ed") || token.endsWith("ing")) return true;
        // In English, verbs often follow subjects (nouns)
        return false;
    }

    private String extractSubject(int verbIndex, List<String> tokens) {
        // Look back for the nearest non-stopword
        for (int i = verbIndex - 1; i >= 0; i--) {
            String token = tokens.get(i);
            if (!TextUtils.isStopWord(token)) return TextUtils.stem(token);
        }
        return "";
    }

    private String extractObject(int verbIndex, List<String> tokens) {
        // Look forward for the nearest non-stopword
        for (int i = verbIndex + 1; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (!TextUtils.isStopWord(token)) return TextUtils.stem(token);
        }
        return "";
    }
}
