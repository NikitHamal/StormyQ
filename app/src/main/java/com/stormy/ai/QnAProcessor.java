package com.stormy.ai;

import android.content.Context;
import com.stormy.ai.models.AnswerResult;
import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.DynamicMemoryBuffer;
import com.stormy.ai.models.Rule;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Orchestrates the QnA process, managing modular components and AI state.
 */
public class QnAProcessor {

    private static QnAProcessor instance;

    // Parameters
    private double decayRate = 0.1;
    private double activationThreshold = 0.05;
    
    // Components
    private SemanticNetwork semanticNetwork;
    private SpreadingActivator spreadingActivator;
    private RuleEngine ruleEngine;
    private ConceptualKnowledgeBase conceptualKnowledgeBase;
    private AnswerExtractor answerExtractor;
    private LearningEngine learningEngine;
    private DynamicMemoryBuffer memoryBuffer;
    private KnowledgeStorage storage;
    
    private final StringBuilder reasoningSummary = new StringBuilder();
    private final LinkedList<Double> recentConfidences = new LinkedList<>();

    private String lastContext = "";
    private boolean isNetworkBuilt = false;

    private QnAProcessor() {
    }

    public void initialize(Context context) {
        this.storage = new KnowledgeStorage(context);
        initComponents();
        loadKnowledge();
    }

    private void initComponents() {
        this.semanticNetwork = new SemanticNetwork(decayRate);
        this.spreadingActivator = new SpreadingActivator(semanticNetwork, reasoningSummary, 0.8, activationThreshold, 15, 0.7, 0.2, 0.1);
        this.ruleEngine = new RuleEngine(reasoningSummary, activationThreshold);
        this.conceptualKnowledgeBase = new ConceptualKnowledgeBase(reasoningSummary);
        this.answerExtractor = new AnswerExtractor(reasoningSummary, activationThreshold, 0.1);
        this.learningEngine = new LearningEngine(reasoningSummary);
        this.memoryBuffer = new DynamicMemoryBuffer();
    }

    private void loadKnowledge() {
        List<Rule> savedRules = storage.loadRules();
        for (Rule r : savedRules) ruleEngine.addRule(r);

        List<ConceptRelation> savedRelations = storage.loadConceptualRelations();
        for (ConceptRelation cr : savedRelations) conceptualKnowledgeBase.addConceptualRelation(cr);
    }

    public static synchronized QnAProcessor getInstance() {
        if (instance == null) instance = new QnAProcessor();
        return instance;
    }

    public AnswerResult findAnswer(String context, String question) {
        reasoningSummary.setLength(0);
        reasoningSummary.append("--- Reasoning Summary ---");

        if (!isNetworkBuilt || !context.equals(lastContext)) {
            rebuildNetwork(context);
        } else {
            semanticNetwork.resetActivations();
        }

        List<String> qKeywords = TextUtils.tokenize(question).stream()
                .map(TextUtils::stem)
                .filter(kw -> semanticNetwork.getNode(kw) != null && !TextUtils.isStopWord(kw))
                .collect(Collectors.toList());

        if (qKeywords.isEmpty()) {
            return new AnswerResult("", context, -1, -1, 0.0);
        }

        int qSentiment = TextUtils.getSentimentScore(question);
        spreadingActivator.activate(qKeywords, qSentiment);
        ruleEngine.evaluateRules(semanticNetwork);

        AnswerResult result = answerExtractor.extractAnswer(context, qKeywords, qSentiment, semanticNetwork, TextUtils.tokenize(question));

        updateMetaCognition(context, question, result);

        return result;
    }

    private void rebuildNetwork(String context) {
        semanticNetwork.buildNetwork(context);
        conceptualKnowledgeBase.integrateIntoNetwork(semanticNetwork);
        lastContext = context;
        isNetworkBuilt = true;
    }

    private void updateMetaCognition(String context, String question, AnswerResult result) {
        if (recentConfidences.size() >= 5) recentConfidences.removeFirst();
        recentConfidences.addLast(result.getConfidence());

        double[] params = learningEngine.adjustParameters(recentConfidences, activationThreshold, decayRate);
        setActivationThreshold(params[0]);
        setDecayRate(params[1]);

        learningEngine.considerGeneratingRule(context, question, result, semanticNetwork, ruleEngine, activationThreshold);

        if (storage != null) {
            storage.saveRules(ruleEngine.getRules());
            storage.saveConceptualRelations(conceptualKnowledgeBase.getConceptualRelations());
        }

        if (result.isValid()) memoryBuffer.addResult(result);
    }

    public void resetProcessorState() {
        isNetworkBuilt = false;
        lastContext = "";
        initComponents();
    }

    // Delegation & Setters
    public void setDecayRate(double rate) {
        this.decayRate = rate;
        semanticNetwork.setDecayRate(rate);
    }

    public void setActivationThreshold(double threshold) {
        this.activationThreshold = threshold;
        spreadingActivator.setActivationThreshold(threshold);
        ruleEngine.setActivationThreshold(threshold);
        answerExtractor.setActivationThreshold(threshold);
    }

    public String getReasoningSummary() { return reasoningSummary.toString(); }
    public List<Rule> getRules() { return ruleEngine.getRules(); }
    public void addRule(Rule rule) { ruleEngine.addRule(rule); }
    public List<ConceptRelation> getConceptualKnowledgeBase() { return conceptualKnowledgeBase.getConceptualRelations(); }
}