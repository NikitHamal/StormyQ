package com.stormy.ai;

import android.content.Context;
import android.util.Log;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive NLP Pipeline integrating Apache OpenNLP, Stanford CoreNLP, and Apache Lucene
 * for advanced text processing, named entity recognition, dependency parsing, and search.
 */
public class NLPPipeline {
    private static final String TAG = "NLPPipeline";
    
    // OpenNLP components
    private SentenceDetectorME sentenceDetector;
    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    private NameFinderME personNameFinder;
    private NameFinderME locationNameFinder;
    private NameFinderME organizationNameFinder;
    private NameFinderME dateNameFinder;
    
    // Stanford CoreNLP pipeline
    private StanfordCoreNLP coreNLPPipeline;
    
    // Lucene components
    private Directory luceneDirectory;
    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;
    private QueryParser queryParser;
    private Analyzer analyzer;
    
    // Caches for performance
    private final Map<String, ProcessedText> textCache = new ConcurrentHashMap<>();
    private final Map<String, List<SearchResult>> searchCache = new ConcurrentHashMap<>();
    
    private Context context;
    private boolean isInitialized = false;

    public NLPPipeline(Context context) {
        this.context = context;
        this.analyzer = new StandardAnalyzer();
        this.luceneDirectory = new ByteBuffersDirectory();
    }

    /**
     * Initialize all NLP components with models
     */
    public boolean initialize() {
        try {
            initializeOpenNLP();
            initializeStanfordCoreNLP();
            initializeLucene();
            isInitialized = true;
            Log.i(TAG, "NLP Pipeline initialized successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize NLP Pipeline", e);
            return false;
        }
    }

    private void initializeOpenNLP() throws IOException {
        // Load OpenNLP models from assets
        sentenceDetector = new SentenceDetectorME(loadSentenceModel());
        tokenizer = new TokenizerME(loadTokenizerModel());
        posTagger = new POSTaggerME(loadPOSModel());
        
        // Load named entity recognition models
        personNameFinder = new NameFinderME(loadNERModel("person"));
        locationNameFinder = new NameFinderME(loadNERModel("location"));
        organizationNameFinder = new NameFinderME(loadNERModel("organization"));
        dateNameFinder = new NameFinderME(loadNERModel("date"));
    }

    private void initializeStanfordCoreNLP() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref");
        props.setProperty("coref.algorithm", "neural");
        props.setProperty("tokenize.language", "en");
        coreNLPPipeline = new StanfordCoreNLP(props);
    }

    private void initializeLucene() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new BM25Similarity());
        indexWriter = new IndexWriter(luceneDirectory, config);
        queryParser = new QueryParser("content", analyzer);
    }

    /**
     * Process text through the complete NLP pipeline
     */
    public ProcessedText processText(String text) {
        if (!isInitialized) {
            throw new IllegalStateException("NLP Pipeline not initialized");
        }

        // Check cache first
        String cacheKey = text.hashCode() + "";
        if (textCache.containsKey(cacheKey)) {
            return textCache.get(cacheKey);
        }

        ProcessedText result = new ProcessedText(text);
        
        try {
            // OpenNLP processing
            processWithOpenNLP(text, result);
            
            // Stanford CoreNLP processing
            processWithStanfordCoreNLP(text, result);
            
            // Cache the result
            textCache.put(cacheKey, result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing text", e);
        }

        return result;
    }

    private void processWithOpenNLP(String text, ProcessedText result) {
        // Sentence detection
        String[] sentences = sentenceDetector.sentDetect(text);
        result.setSentences(sentences);

        // Process each sentence
        for (String sentence : sentences) {
            // Tokenization
            String[] tokens = tokenizer.tokenize(sentence);
            
            // POS tagging
            String[] posTags = posTagger.tag(tokens);
            
            // Named Entity Recognition
            Map<String, List<String>> entities = extractNamedEntities(tokens);
            
            result.addSentenceAnalysis(sentence, tokens, posTags, entities);
        }
    }

    private void processWithStanfordCoreNLP(String text, ProcessedText result) {
        CoreDocument document = new CoreDocument(text);
        coreNLPPipeline.annotate(document);

        // Extract lemmas
        List<String> lemmas = new ArrayList<>();
        for (CoreLabel token : document.tokens()) {
            lemmas.add(token.lemma());
        }
        result.setLemmas(lemmas);

        // Extract dependency relations
        for (CoreMap sentence : document.annotation().get(CoreAnnotations.SentencesAnnotation.class)) {
            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
            result.addDependencyGraph(dependencies);
            
            // Extract parse tree
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            result.addParseTree(tree);
        }

        // Coreference resolution
        Map<Integer, CorefChain> corefChains = document.annotation().get(CorefCoreAnnotations.CorefChainAnnotation.class);
        if (corefChains != null) {
            result.setCoreferenceChains(corefChains);
        }
    }

    private Map<String, List<String>> extractNamedEntities(String[] tokens) {
        Map<String, List<String>> entities = new HashMap<>();
        
        // Extract different types of named entities
        extractEntityType(tokens, personNameFinder, "PERSON", entities);
        extractEntityType(tokens, locationNameFinder, "LOCATION", entities);
        extractEntityType(tokens, organizationNameFinder, "ORGANIZATION", entities);
        extractEntityType(tokens, dateNameFinder, "DATE", entities);
        
        return entities;
    }

    private void extractEntityType(String[] tokens, NameFinderME finder, String type, Map<String, List<String>> entities) {
        opennlp.tools.util.Span[] spans = finder.find(tokens);
        List<String> entityList = new ArrayList<>();
        
        for (opennlp.tools.util.Span span : spans) {
            StringBuilder entity = new StringBuilder();
            for (int i = span.getStart(); i < span.getEnd(); i++) {
                entity.append(tokens[i]).append(" ");
            }
            entityList.add(entity.toString().trim());
        }
        
        entities.put(type, entityList);
        finder.clearAdaptiveData();
    }

    /**
     * Index a document for search
     */
    public void indexDocument(String id, String title, String content) {
        if (!isInitialized) {
            throw new IllegalStateException("NLP Pipeline not initialized");
        }

        try {
            Document doc = new Document();
            doc.add(new TextField("id", id, Field.Store.YES));
            doc.add(new TextField("title", title, Field.Store.YES));
            doc.add(new TextField("content", content, Field.Store.YES));
            
            // Add processed content for better search
            ProcessedText processed = processText(content);
            doc.add(new TextField("lemmas", String.join(" ", processed.getLemmas()), Field.Store.NO));
            doc.add(new TextField("entities", flattenEntities(processed.getAllEntities()), Field.Store.NO));
            
            indexWriter.addDocument(doc);
            indexWriter.commit();
            
            // Update searcher
            updateSearcher();
            
        } catch (Exception e) {
            Log.e(TAG, "Error indexing document", e);
        }
    }

    /**
     * Search indexed documents
     */
    public List<SearchResult> search(String queryText, int maxResults) {
        if (!isInitialized) {
            throw new IllegalStateException("NLP Pipeline not initialized");
        }

        String cacheKey = queryText + "_" + maxResults;
        if (searchCache.containsKey(cacheKey)) {
            return searchCache.get(cacheKey);
        }

        List<SearchResult> results = new ArrayList<>();
        
        try {
            // Process query for better matching
            ProcessedText processedQuery = processText(queryText);
            String enhancedQuery = enhanceQuery(queryText, processedQuery);
            
            Query query = queryParser.parse(enhancedQuery);
            TopDocs topDocs = indexSearcher.search(query, maxResults);
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = indexSearcher.doc(scoreDoc.doc);
                SearchResult result = new SearchResult(
                    doc.get("id"),
                    doc.get("title"),
                    doc.get("content"),
                    scoreDoc.score
                );
                results.add(result);
            }
            
            searchCache.put(cacheKey, results);
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching documents", e);
        }

        return results;
    }

    private String enhanceQuery(String originalQuery, ProcessedText processed) {
        StringBuilder enhanced = new StringBuilder(originalQuery);
        
        // Add lemmas for better matching
        List<String> lemmas = processed.getLemmas();
        if (!lemmas.isEmpty()) {
            enhanced.append(" OR lemmas:(");
            enhanced.append(String.join(" ", lemmas));
            enhanced.append(")");
        }
        
        // Add entities for better matching
        Map<String, List<String>> entities = processed.getAllEntities();
        if (!entities.isEmpty()) {
            enhanced.append(" OR entities:(");
            enhanced.append(flattenEntities(entities));
            enhanced.append(")");
        }
        
        return enhanced.toString();
    }

    private String flattenEntities(Map<String, List<String>> entities) {
        StringBuilder flattened = new StringBuilder();
        for (List<String> entityList : entities.values()) {
            for (String entity : entityList) {
                flattened.append(entity).append(" ");
            }
        }
        return flattened.toString().trim();
    }

    private void updateSearcher() throws IOException {
        if (indexSearcher != null) {
            indexSearcher.getIndexReader().close();
        }
        DirectoryReader reader = DirectoryReader.open(luceneDirectory);
        indexSearcher = new IndexSearcher(reader);
        indexSearcher.setSimilarity(new BM25Similarity());
    }

    // Model loading methods (these would load from assets)
    private SentenceModel loadSentenceModel() throws IOException {
        InputStream modelIn = context.getAssets().open("models/en-sent.bin");
        return new SentenceModel(modelIn);
    }

    private TokenizerModel loadTokenizerModel() throws IOException {
        InputStream modelIn = context.getAssets().open("models/en-token.bin");
        return new TokenizerModel(modelIn);
    }

    private POSModel loadPOSModel() throws IOException {
        InputStream modelIn = context.getAssets().open("models/en-pos-maxent.bin");
        return new POSModel(modelIn);
    }

    private TokenNameFinderModel loadNERModel(String type) throws IOException {
        InputStream modelIn = context.getAssets().open("models/en-ner-" + type + ".bin");
        return new TokenNameFinderModel(modelIn);
    }

    /**
     * Get enhanced word similarity using lemmatization and entities
     */
    public double calculateEnhancedSimilarity(String word1, String word2) {
        if (!isInitialized) {
            return TextUtils.calculateSimilarity(word1, word2);
        }

        ProcessedText p1 = processText(word1);
        ProcessedText p2 = processText(word2);
        
        // Compare lemmas
        List<String> lemmas1 = p1.getLemmas();
        List<String> lemmas2 = p2.getLemmas();
        
        if (!lemmas1.isEmpty() && !lemmas2.isEmpty()) {
            String lemma1 = lemmas1.get(0);
            String lemma2 = lemmas2.get(0);
            if (lemma1.equals(lemma2)) {
                return 1.0;
            }
        }
        
        // Fallback to original similarity
        return TextUtils.calculateSimilarity(word1, word2);
    }

    /**
     * Extract noun phrases using parse trees
     */
    public List<String> extractNounPhrases(String text) {
        if (!isInitialized) {
            return new ArrayList<>();
        }

        ProcessedText processed = processText(text);
        List<String> nounPhrases = new ArrayList<>();
        
        for (Tree parseTree : processed.getParseTrees()) {
            extractNounPhrasesFromTree(parseTree, nounPhrases);
        }
        
        return nounPhrases;
    }

    private void extractNounPhrasesFromTree(Tree tree, List<String> nounPhrases) {
        if (tree.label().value().equals("NP")) {
            nounPhrases.add(tree.pennString().replaceAll("[()\\n]", " ").trim());
        }
        
        for (Tree child : tree.children()) {
            extractNounPhrasesFromTree(child, nounPhrases);
        }
    }

    /**
     * Determine if a word is a noun using POS tagging
     */
    public boolean isNounByPOS(String word) {
        if (!isInitialized) {
            return false;
        }

        ProcessedText processed = processText(word);
        List<String> posTags = processed.getAllPOSTags();
        
        for (String tag : posTags) {
            if (tag.startsWith("NN")) { // NN, NNS, NNP, NNPS
                return true;
            }
        }
        
        return false;
    }

    /**
     * Determine if a word is an adjective using POS tagging
     */
    public boolean isAdjectiveByPOS(String word) {
        if (!isInitialized) {
            return false;
        }

        ProcessedText processed = processText(word);
        List<String> posTags = processed.getAllPOSTags();
        
        for (String tag : posTags) {
            if (tag.startsWith("JJ")) { // JJ, JJR, JJS
                return true;
            }
        }
        
        return false;
    }

    public void shutdown() {
        try {
            if (indexWriter != null) {
                indexWriter.close();
            }
            if (indexSearcher != null) {
                indexSearcher.getIndexReader().close();
            }
            if (luceneDirectory != null) {
                luceneDirectory.close();
            }
            textCache.clear();
            searchCache.clear();
        } catch (IOException e) {
            Log.e(TAG, "Error shutting down NLP Pipeline", e);
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}