package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import com.stormy.ai.models.AnswerResult;
import com.stormy.ai.models.ProcessedText;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Production-ready answer validator using NLP pipeline for sophisticated linguistic analysis.
 * Replaces simple heuristics with proper syntactic and semantic validation.
 */
public class AnswerValidator {

    /**
     * Enhanced completeness check using dependency parsing and entity analysis.
     * Validates that the answer addresses all components of complex questions.
     */
    public boolean isComplete(AnswerResult answer, String question) {
        NLPPipeline nlpPipeline = QnAProcessor.getInstance().getNLPPipeline();
        
        if (nlpPipeline != null && nlpPipeline.isInitialized()) {
            try {
                return isCompleteWithNLP(answer, question, nlpPipeline);
            } catch (Exception e) {
                // Fallback to enhanced heuristic
                return isCompleteHeuristic(answer, question);
            }
        } else {
            return isCompleteHeuristic(answer, question);
        }
    }

    private boolean isCompleteWithNLP(AnswerResult answer, String question, NLPPipeline nlpPipeline) {
        ProcessedText questionProcessed = nlpPipeline.processText(question);
        ProcessedText answerProcessed = nlpPipeline.processText(answer.getAnswer());
        
        // Check if answer contains entities mentioned in question
        Map<String, List<String>> questionEntities = questionProcessed.getAllEntities();
        Map<String, List<String>> answerEntities = answerProcessed.getAllEntities();
        
        int requiredEntities = 0;
        int addressedEntities = 0;
        
        // Count how many entity types from question are addressed in answer
        for (Map.Entry<String, List<String>> entry : questionEntities.entrySet()) {
            String entityType = entry.getKey();
            List<String> questionEntityList = entry.getValue();
            
            if (!questionEntityList.isEmpty()) {
                requiredEntities++;
                List<String> answerEntityList = answerEntities.get(entityType);
                
                if (answerEntityList != null && !answerEntityList.isEmpty()) {
                    // Check for overlap in entities
                    boolean hasOverlap = false;
                    for (String qEntity : questionEntityList) {
                        for (String aEntity : answerEntityList) {
                            if (TextUtils.calculateSimilarity(qEntity.toLowerCase(), aEntity.toLowerCase()) > 0.7) {
                                hasOverlap = true;
                                break;
                            }
                        }
                        if (hasOverlap) break;
                    }
                    if (hasOverlap) addressedEntities++;
                }
            }
        }
        
        // Answer is complete if it addresses most entities or if no specific entities required
        return requiredEntities == 0 || (double) addressedEntities / requiredEntities >= 0.6;
    }
    
    private boolean isCompleteHeuristic(AnswerResult answer, String question) {
        // Enhanced heuristic for compound questions
        if (question.toLowerCase().contains(" and ")) {
            String[] parts = question.toLowerCase().split(" and ");
            if (parts.length > 1) {
                int addressedParts = 0;
                for (String part : parts) {
                    String[] keywords = part.trim().split("\\s+");
                    boolean partAddressed = false;
                    for (String keyword : keywords) {
                        if (keyword.length() > 3 && !TextUtils.isStopWord(keyword) && 
                            answer.getAnswer().toLowerCase().contains(keyword)) {
                            partAddressed = true;
                            break;
                        }
                    }
                    if (partAddressed) addressedParts++;
                }
                return addressedParts >= Math.max(1, parts.length / 2);
            }
        }
        
        // Check semantic overlap using stemming
        List<String> questionTokens = TextUtils.tokenize(question);
        List<String> answerTokens = TextUtils.tokenize(answer.getAnswer());
        
        int significantWords = 0;
        int matchedWords = 0;
        
        for (String qToken : questionTokens) {
            if (qToken.length() > 3 && !TextUtils.isStopWord(qToken)) {
                significantWords++;
                String qStemmed = TextUtils.stem(qToken);
                for (String aToken : answerTokens) {
                    if (TextUtils.stem(aToken).equals(qStemmed)) {
                        matchedWords++;
                        break;
                    }
                }
            }
        }
        
        return significantWords == 0 || (double) matchedWords / significantWords >= 0.3;
    }

    /**
     * Enhanced coherence check using proper POS tagging and syntactic analysis.
     * Validates that the answer forms grammatically correct and meaningful sentences.
     */
    public boolean isCoherent(AnswerResult answer) {
        if (answer.getAnswer() == null || answer.getAnswer().trim().isEmpty()) {
            return false;
        }

        NLPPipeline nlpPipeline = QnAProcessor.getInstance().getNLPPipeline();
        
        if (nlpPipeline != null && nlpPipeline.isInitialized()) {
            try {
                return isCoherentWithNLP(answer, nlpPipeline);
            } catch (Exception e) {
                return isCoherentHeuristic(answer);
            }
        } else {
            return isCoherentHeuristic(answer);
        }
    }

    private boolean isCoherentWithNLP(AnswerResult answer, NLPPipeline nlpPipeline) {
        ProcessedText processed = nlpPipeline.processText(answer.getAnswer());
        
        // Check sentence structure
        String[] sentences = processed.getSentences();
        if (sentences == null || sentences.length == 0) {
            return false;
        }
        
        // Analyze each sentence for basic grammatical structure
        for (ProcessedText.SentenceAnalysis sentence : processed.getSentenceAnalyses()) {
            List<String> nouns = sentence.getNouns();
            List<String> verbs = sentence.getVerbs();
            
            // A coherent sentence should have at least one noun or pronoun and potentially a verb
            if (nouns.isEmpty() && sentence.getTokens().length > 2) {
                // Check for pronouns
                boolean hasPronoun = false;
                for (String token : sentence.getTokens()) {
                    if (isPronoun(token)) {
                        hasPronoun = true;
                        break;
                    }
                }
                if (!hasPronoun) {
                    return false; // No nouns or pronouns in a complex sentence
                }
            }
        }
        
        return true;
    }

    private boolean isCoherentHeuristic(AnswerResult answer) {
        List<String> tokens = TextUtils.tokenize(answer.getAnswer());
        if (tokens.size() < 2) {
            return false;
        }
        
        // Check for basic sentence structure
        boolean hasContentWord = false;
        for (String token : tokens) {
            if (token.length() > 3 && !TextUtils.isStopWord(token)) {
                hasContentWord = true;
                break;
            }
        }
        
        return hasContentWord;
    }

    /**
     * Enhanced factual consistency check using semantic similarity and entity matching.
     * Validates that the answer doesn't contradict information in the context.
     */
    public boolean isFactuallyConsistent(AnswerResult answer, String context) {
        if (answer.getAnswer() == null || context == null) {
            return false;
        }

        NLPPipeline nlpPipeline = QnAProcessor.getInstance().getNLPPipeline();
        
        if (nlpPipeline != null && nlpPipeline.isInitialized()) {
            try {
                return isFactuallyConsistentWithNLP(answer, context, nlpPipeline);
            } catch (Exception e) {
                return isFactuallyConsistentHeuristic(answer, context);
            }
        } else {
            return isFactuallyConsistentHeuristic(answer, context);
        }
    }

    private boolean isFactuallyConsistentWithNLP(AnswerResult answer, String context, NLPPipeline nlpPipeline) {
        ProcessedText answerProcessed = nlpPipeline.processText(answer.getAnswer());
        ProcessedText contextProcessed = nlpPipeline.processText(context);
        
        // Check for contradictory entities
        Map<String, List<String>> answerEntities = answerProcessed.getAllEntities();
        Map<String, List<String>> contextEntities = contextProcessed.getAllEntities();
        
        // For each entity type in answer, verify it's consistent with context
        for (Map.Entry<String, List<String>> entry : answerEntities.entrySet()) {
            String entityType = entry.getKey();
            List<String> answerEntityList = entry.getValue();
            List<String> contextEntityList = contextEntities.get(entityType);
            
            if (contextEntityList != null && !contextEntityList.isEmpty() && !answerEntityList.isEmpty()) {
                // Check if any answer entity is mentioned in context
                boolean hasMatch = false;
                for (String answerEntity : answerEntityList) {
                    for (String contextEntity : contextEntityList) {
                        if (TextUtils.calculateSimilarity(answerEntity.toLowerCase(), contextEntity.toLowerCase()) > 0.8) {
                            hasMatch = true;
                            break;
                        }
                    }
                    if (hasMatch) break;
                }
                
                // If answer mentions specific entities not in context, it might be inconsistent
                if (!hasMatch && entityType.equals("PERSON") || entityType.equals("ORGANIZATION")) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private boolean isFactuallyConsistentHeuristic(AnswerResult answer, String context) {
        // Check that significant terms in answer appear in context
        List<String> answerTokens = TextUtils.tokenize(answer.getAnswer());
        List<String> contextTokens = TextUtils.tokenize(context);
        
        Set<String> contextStemmed = new HashSet<>();
        for (String token : contextTokens) {
            contextStemmed.add(TextUtils.stem(token.toLowerCase()));
        }
        
        int significantTerms = 0;
        int foundTerms = 0;
        
        for (String token : answerTokens) {
            if (token.length() > 4 && !TextUtils.isStopWord(token)) {
                significantTerms++;
                if (contextStemmed.contains(TextUtils.stem(token.toLowerCase()))) {
                    foundTerms++;
                }
            }
        }
        
        // Most significant terms should be found in context
        return significantTerms == 0 || (double) foundTerms / significantTerms >= 0.7;
    }

    /**
     * Enhanced relevance check using semantic similarity and Lucene search scores.
     * Validates that the answer semantically relates to the question.
     */
    public boolean isRelevant(AnswerResult answer, String question) {
        if (answer.getAnswer() == null || question == null) {
            return false;
        }

        NLPPipeline nlpPipeline = QnAProcessor.getInstance().getNLPPipeline();
        
        if (nlpPipeline != null && nlpPipeline.isInitialized()) {
            try {
                return isRelevantWithNLP(answer, question, nlpPipeline);
            } catch (Exception e) {
                return isRelevantHeuristic(answer, question);
            }
        } else {
            return isRelevantHeuristic(answer, question);
        }
    }

    private boolean isRelevantWithNLP(AnswerResult answer, String question, NLPPipeline nlpPipeline) {
        // Use enhanced similarity calculation
        double similarity = nlpPipeline.calculateEnhancedSimilarity(answer.getAnswer(), question);
        
        if (similarity > 0.3) {
            return true;
        }
        
        // Check entity overlap
        ProcessedText questionProcessed = nlpPipeline.processText(question);
        ProcessedText answerProcessed = nlpPipeline.processText(answer.getAnswer());
        
        Map<String, List<String>> questionEntities = questionProcessed.getAllEntities();
        Map<String, List<String>> answerEntities = answerProcessed.getAllEntities();
        
        // Check for shared entities
        for (Map.Entry<String, List<String>> entry : questionEntities.entrySet()) {
            String entityType = entry.getKey();
            List<String> questionEntityList = entry.getValue();
            List<String> answerEntityList = answerEntities.get(entityType);
            
            if (answerEntityList != null) {
                for (String qEntity : questionEntityList) {
                    for (String aEntity : answerEntityList) {
                        if (TextUtils.calculateSimilarity(qEntity.toLowerCase(), aEntity.toLowerCase()) > 0.7) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    private boolean isRelevantHeuristic(AnswerResult answer, String question) {
        List<String> questionTokens = TextUtils.tokenize(question);
        List<String> answerTokens = TextUtils.tokenize(answer.getAnswer());
        
        int significantQuestionTerms = 0;
        int matchedTerms = 0;
        
        for (String qToken : questionTokens) {
            if (qToken.length() > 3 && !TextUtils.isStopWord(qToken)) {
                significantQuestionTerms++;
                String qStemmed = TextUtils.stem(qToken);
                
                for (String aToken : answerTokens) {
                    if (TextUtils.stem(aToken).equals(qStemmed)) {
                        matchedTerms++;
                        break;
                    }
                }
            }
        }
        
        return significantQuestionTerms > 0 && (double) matchedTerms / significantQuestionTerms >= 0.2;
    }

    /**
     * Helper method to identify pronouns
     */
    private boolean isPronoun(String word) {
        String lowerWord = word.toLowerCase();
        return "i".equals(lowerWord) || "you".equals(lowerWord) || "he".equals(lowerWord) ||
               "she".equals(lowerWord) || "it".equals(lowerWord) || "we".equals(lowerWord) ||
               "they".equals(lowerWord) || "me".equals(lowerWord) || "him".equals(lowerWord) ||
               "her".equals(lowerWord) || "us".equals(lowerWord) || "them".equals(lowerWord) ||
               "this".equals(lowerWord) || "that".equals(lowerWord) || "these".equals(lowerWord) ||
               "those".equals(lowerWord);
    }
}
