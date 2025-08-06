package com.stormy.ai.models;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive model to hold all NLP processing results from multiple libraries.
 * Contains results from Apache OpenNLP, Stanford CoreNLP, and Apache Lucene processing.
 */
public class ProcessedText {
    private final String originalText;
    
    // OpenNLP results
    private String[] sentences;
    private List<SentenceAnalysis> sentenceAnalyses;
    
    // Stanford CoreNLP results
    private List<String> lemmas;
    private List<SemanticGraph> dependencyGraphs;
    private List<Tree> parseTrees;
    private Map<Integer, CorefChain> coreferenceChains;
    
    // Combined results
    private Map<String, List<String>> allEntities;
    private List<String> allPOSTags;
    private List<String> allTokens;

    public ProcessedText(String originalText) {
        this.originalText = originalText;
        this.sentenceAnalyses = new ArrayList<>();
        this.lemmas = new ArrayList<>();
        this.dependencyGraphs = new ArrayList<>();
        this.parseTrees = new ArrayList<>();
        this.allEntities = new HashMap<>();
        this.allPOSTags = new ArrayList<>();
        this.allTokens = new ArrayList<>();
    }

    // OpenNLP results
    public void setSentences(String[] sentences) {
        this.sentences = sentences;
    }

    public String[] getSentences() {
        return sentences;
    }

    public void addSentenceAnalysis(String sentence, String[] tokens, String[] posTags, Map<String, List<String>> entities) {
        SentenceAnalysis analysis = new SentenceAnalysis(sentence, tokens, posTags, entities);
        sentenceAnalyses.add(analysis);
        
        // Aggregate data
        for (String token : tokens) {
            allTokens.add(token);
        }
        for (String tag : posTags) {
            allPOSTags.add(tag);
        }
        for (Map.Entry<String, List<String>> entry : entities.entrySet()) {
            allEntities.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }
    }

    public List<SentenceAnalysis> getSentenceAnalyses() {
        return sentenceAnalyses;
    }

    // Stanford CoreNLP results
    public void setLemmas(List<String> lemmas) {
        this.lemmas = lemmas;
    }

    public List<String> getLemmas() {
        return lemmas;
    }

    public void addDependencyGraph(SemanticGraph graph) {
        dependencyGraphs.add(graph);
    }

    public List<SemanticGraph> getDependencyGraphs() {
        return dependencyGraphs;
    }

    public void addParseTree(Tree tree) {
        parseTrees.add(tree);
    }

    public List<Tree> getParseTrees() {
        return parseTrees;
    }

    public void setCoreferenceChains(Map<Integer, CorefChain> chains) {
        this.coreferenceChains = chains;
    }

    public Map<Integer, CorefChain> getCoreferenceChains() {
        return coreferenceChains;
    }

    // Combined accessors
    public Map<String, List<String>> getAllEntities() {
        return allEntities;
    }

    public List<String> getAllPOSTags() {
        return allPOSTags;
    }

    public List<String> getAllTokens() {
        return allTokens;
    }

    public String getOriginalText() {
        return originalText;
    }

    /**
     * Get all named entities of a specific type
     */
    public List<String> getEntitiesByType(String type) {
        return allEntities.getOrDefault(type, new ArrayList<>());
    }

    /**
     * Check if text contains entities of a specific type
     */
    public boolean hasEntitiesOfType(String type) {
        List<String> entities = allEntities.get(type);
        return entities != null && !entities.isEmpty();
    }

    /**
     * Get the first dependency graph (for simple cases)
     */
    public SemanticGraph getPrimaryDependencyGraph() {
        return dependencyGraphs.isEmpty() ? null : dependencyGraphs.get(0);
    }

    /**
     * Get the first parse tree (for simple cases)
     */
    public Tree getPrimaryParseTree() {
        return parseTrees.isEmpty() ? null : parseTrees.get(0);
    }

    /**
     * Inner class to hold sentence-level analysis
     */
    public static class SentenceAnalysis {
        private final String sentence;
        private final String[] tokens;
        private final String[] posTags;
        private final Map<String, List<String>> entities;

        public SentenceAnalysis(String sentence, String[] tokens, String[] posTags, Map<String, List<String>> entities) {
            this.sentence = sentence;
            this.tokens = tokens;
            this.posTags = posTags;
            this.entities = entities;
        }

        public String getSentence() {
            return sentence;
        }

        public String[] getTokens() {
            return tokens;
        }

        public String[] getPosTags() {
            return posTags;
        }

        public Map<String, List<String>> getEntities() {
            return entities;
        }

        /**
         * Get tokens with a specific POS tag
         */
        public List<String> getTokensByPOS(String posPrefix) {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < tokens.length && i < posTags.length; i++) {
                if (posTags[i].startsWith(posPrefix)) {
                    result.add(tokens[i]);
                }
            }
            return result;
        }

        /**
         * Get all nouns in this sentence
         */
        public List<String> getNouns() {
            return getTokensByPOS("NN");
        }

        /**
         * Get all verbs in this sentence
         */
        public List<String> getVerbs() {
            return getTokensByPOS("VB");
        }

        /**
         * Get all adjectives in this sentence
         */
        public List<String> getAdjectives() {
            return getTokensByPOS("JJ");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProcessedText{\n");
        sb.append("  originalText='").append(originalText).append("'\n");
        sb.append("  sentences=").append(sentences != null ? sentences.length : 0).append("\n");
        sb.append("  lemmas=").append(lemmas.size()).append("\n");
        sb.append("  entities=").append(allEntities.size()).append(" types\n");
        sb.append("  dependencyGraphs=").append(dependencyGraphs.size()).append("\n");
        sb.append("  parseTrees=").append(parseTrees.size()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}