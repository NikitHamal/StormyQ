package com.stormy.ai;

import com.stormy.ai.models.AnswerCandidate;
import com.stormy.ai.models.AnswerResult;
import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.DynamicMemoryBuffer;
import com.stormy.ai.models.Rule;
import com.stormy.ai.models.SemanticNode;
import com.stormy.ai.models.TemporalInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main QnA Processor, acting as an orchestrator for modular components.
 * It manages the overall flow, adaptive parameters, and integration of knowledge.
 */
public class QnAProcessor {

    // Singleton instance
    private static QnAProcessor instance;

    // Core Parameters
    private static final double INITIAL_ACTIVATION = 0.8;
    private double decayRate = 0.1;
    private double activationThreshold = 0.05;
    private static final int MAX_SPREADING_ITERATIONS = 15;
    private static final double NEGATION_EFFECT = 0.7;
    private static final double CONCEPT_RELATION_BOOST = 0.2;
    private static final double SENTIMENT_BOOST_FACTOR = 0.1;

    // Self-monitoring parameters
    private static final int CONFIDENCE_HISTORY_SIZE = 5;
    private final LinkedList<Double> recentConfidences;

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.3;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;

    // Modular Components
    private SemanticNetwork semanticNetwork;
    private final SpreadingActivator spreadingActivator;
    private final RuleEngine ruleEngine;
    private final ConceptualKnowledgeBase conceptualKnowledgeBase;
    private final AnswerExtractor answerExtractor;

    private final DynamicMemoryBuffer memoryBuffer;
    private final StringBuilder reasoningSummary;

    // For network caching and optimization
    private String lastProcessedContext = "";
    private boolean isNetworkBuilt = false;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private QnAProcessor() {
        this.reasoningSummary = new StringBuilder();
        this.semanticNetwork = new SemanticNetwork(this.decayRate);
        this.spreadingActivator = new SpreadingActivator(
                this.semanticNetwork,
                this.reasoningSummary,
                INITIAL_ACTIVATION,
                this.activationThreshold,
                MAX_SPREADING_ITERATIONS,
                NEGATION_EFFECT,
                CONCEPT_RELATION_BOOST,
                SENTIMENT_BOOST_FACTOR
        );
        this.ruleEngine = new RuleEngine(this.reasoningSummary, this.activationThreshold);
        this.conceptualKnowledgeBase = new ConceptualKnowledgeBase(this.reasoningSummary);
        this.answerExtractor = new AnswerExtractor(this.reasoningSummary, this.activationThreshold, SENTIMENT_BOOST_FACTOR);

        this.memoryBuffer = new DynamicMemoryBuffer();
        this.recentConfidences = new LinkedList<>();
    }

    /**
     * Returns the singleton instance of QnAProcessor.
     * @return The single instance of QnAProcessor.
     */
    public static synchronized QnAProcessor getInstance() {
        if (instance == null) {
            instance = new QnAProcessor();
        }
        return instance;
    }

    /**
     * Finds an answer to the question within the given context.
     * @param context The text in which to find the answer.
     * @param question The question to answer.
     * @return An AnswerResult object containing the answer, its position, and confidence.
     */
    public AnswerResult findAnswer(String context, String question) {
        reasoningSummary.setLength(0);
        reasoningSummary.append("--- Reasoning Process Summary ---\n");

        // 1. Build or reuse the semantic network from the context
        buildOrReuseNetwork(context);

        // 2. Process the question
        List<String> questionKeywords = processQuestion(question);
        if (questionKeywords.isEmpty()) {
            reasoningSummary.append("No relevant question keywords found in context.\n");
            return new AnswerResult("", context, -1, -1, 0.0);
        }

        // 3. Perform spreading activation
        int questionSentiment = TextUtils.getSentimentScore(question);
        spreadingActivator.activate(questionKeywords, questionSentiment);

        // 4. Evaluate probabilistic rules
        ruleEngine.evaluateRules(semanticNetwork);

        // 5. Extract and rank answers
        List<AnswerCandidate> rankedCandidates = answerExtractor.extractRankedAnswers(context, question, semanticNetwork);

        // 6. Select the best answer
        AnswerResult currentAnswer = selectBestAnswer(rankedCandidates, context);

        // 7. Perform meta-cognition
        performMetaCognition(context, question, currentAnswer);

        return currentAnswer;
    }

    /**
     * Builds or reuses the semantic network from the context.
     * @param context The context to build the network from.
     */
    private void buildOrReuseNetwork(String context) {
        if (!isNetworkBuilt || !context.equals(lastProcessedContext)) {
            reasoningSummary.append("Context changed or network not built. Rebuilding semantic network...\n");
            semanticNetwork.buildNetwork(context);
            conceptualKnowledgeBase.integrateIntoNetwork(semanticNetwork);
            lastProcessedContext = context;
            isNetworkBuilt = true;
            reasoningSummary.append("\n--- Semantic Network Built ---\n");
            reasoningSummary.append("Nodes in network: ").append(semanticNetwork.getNodeCount()).append("\n");
        } else {
            reasoningSummary.append("Using existing semantic network for context. Nodes: ").append(semanticNetwork.getNodeCount()).append("\n");
            semanticNetwork.resetActivations();
        }
    }

    /**
     * Processes the question to extract keywords.
     * @param question The question to process.
     * @return A list of stemmed keywords.
     */
    private List<String> processQuestion(String question) {
        List<String> questionKeywords = TextUtils.tokenize(question).stream()
                .map(TextUtils::stem)
                .collect(Collectors.toList());
        questionKeywords.removeIf(kw -> semanticNetwork.getNode(kw) == null || TextUtils.isStopWord(kw));
        reasoningSummary.append("Stemmed Question Keywords: ").append(questionKeywords).append("\n");
        return questionKeywords;
    }

    /**
     * Selects the best answer from a list of ranked candidates.
     * @param rankedCandidates The list of ranked answer candidates.
     * @param context The original context.
     * @return The best AnswerResult.
     */
    private AnswerResult selectBestAnswer(List<AnswerCandidate> rankedCandidates, String context) {
        if (rankedCandidates.isEmpty()) {
            return new AnswerResult("", context, -1, -1, 0.0);
        }
        AnswerCandidate bestCandidate = rankedCandidates.get(0);
        return new AnswerResult(bestCandidate.getText(), bestCandidate.getSourceSentence(),
                bestCandidate.getStartIndex(), bestCandidate.getEndIndex(),
                bestCandidate.getFinalScore());
    }

    /**
     * Performs meta-cognition, including self-monitoring and adaptive processing.
     * @param context The context.
     * @param question The question.
     * @param currentAnswer The current answer.
     */
    private void performMetaCognition(String context, String question, AnswerResult currentAnswer) {
        if (recentConfidences.size() >= CONFIDENCE_HISTORY_SIZE) {
            recentConfidences.removeFirst();
        }
        recentConfidences.addLast(currentAnswer.getConfidence());
        adjustProcessingParameters();
        considerGeneratingRuleFeedback(context, question, currentAnswer);
        if (currentAnswer.isValid()) {
            memoryBuffer.addResult(currentAnswer);
            reasoningSummary.append("[Memory Buffer] Answer added to memory.\n");
        } else {
            reasoningSummary.append("[Memory Buffer] Answer not valid enough to be added to memory.\n");
        }
    }

    /**
     * Adjusts processing parameters based on recent performance.
     */
    private void adjustProcessingParameters() {
        if (recentConfidences.size() < CONFIDENCE_HISTORY_SIZE) {
            reasoningSummary.append("\n[Meta-Cognition] Not enough data for adaptive parameter adjustment (need ").append(CONFIDENCE_HISTORY_SIZE)
                    .append(", have ").append(recentConfidences.size()).append(")\n");
            return;
        }
        double averageConfidence = recentConfidences.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        reasoningSummary.append("\n[Meta-Cognition] Average recent confidence: ").append(String.format("%.2f", averageConfidence)).append("\n");
        if (averageConfidence < LOW_CONFIDENCE_THRESHOLD) {
            this.activationThreshold = Math.min(0.2, this.activationThreshold + 0.01);
            this.decayRate = Math.max(0.05, this.decayRate - 0.005);
            reasoningSummary.append(" - Low confidence detected. Adjusting parameters:\n")
                    .append("   - activationThreshold: ").append(String.format("%.2f", this.activationThreshold))
                    .append("\n   - decayRate: ").append(String.format("%.2f", this.decayRate)).append("\n");
        } else if (averageConfidence > HIGH_CONFIDENCE_THRESHOLD) {
            this.activationThreshold = Math.max(0.05, this.activationThreshold - 0.005);
            this.decayRate = Math.min(0.2, this.decayRate + 0.005);
            reasoningSummary.append(" - High confidence detected. Adjusting parameters:\n")
                    .append("   - activationThreshold: ").append(String.format("%.2f", this.activationThreshold))
                    .append("\n   - decayRate: ").append(String.format("%.2f", this.decayRate)).append("\n");
        } else {
            reasoningSummary.append(" - Confidence stable. No parameter adjustment.\n");
        }
        semanticNetwork.setDecayRate(this.decayRate);
        spreadingActivator.setActivationThreshold(this.activationThreshold);
        ruleEngine.setActivationThreshold(this.activationThreshold);
        answerExtractor.setActivationThreshold(this.activationThreshold);
    }

    /**
     * Considers generating new rules based on high-confidence answers.
     * @param context The context.
     * @param question The question.
     * @param answerResult The answer result.
     */
    private void considerGeneratingRuleFeedback(String context, String question, AnswerResult answerResult) {
        reasoningSummary.append("\n[Meta-Cognition] Considering feedback for rule generation...\n");
        if (answerResult.getConfidence() >= HIGH_CONFIDENCE_THRESHOLD) {
            reasoningSummary.append("  High confidence answer detected. Analyzing for new rule opportunities.\n");
            List<String> activatedAnswerKeywords = TextUtils.tokenize(answerResult.getAnswer()).stream()
                    .map(TextUtils::stem)
                    .filter(s -> !s.isEmpty() && semanticNetwork.getNode(s) != null && semanticNetwork.getNode(s).getActivation() >= activationThreshold)
                    .collect(Collectors.toList());
            List<String> questionKeywords = TextUtils.tokenize(question).stream()
                    .map(TextUtils::stem)
                    .filter(s -> !s.isEmpty() && semanticNetwork.getNode(s) != null && semanticNetwork.getNode(s).getActivation() >= activationThreshold)
                    .collect(Collectors.toList());
            if (!questionKeywords.isEmpty() && !activatedAnswerKeywords.isEmpty()) {
                List<String> validQuestionConditions = questionKeywords.stream()
                        .filter(s -> !TextUtils.isStopWord(s) && !s.isEmpty())
                        .collect(Collectors.toList());
                List<String> validAnswerConsequences = activatedAnswerKeywords.stream()
                        .filter(s -> !TextUtils.isStopWord(s) && !s.isEmpty())
                        .collect(Collectors.toList());
                if (validQuestionConditions.isEmpty() || validAnswerConsequences.isEmpty()) {
                    reasoningSummary.append("  Skipping rule generation: No valid question or answer keywords after filtering.\n");
                    return;
                }
                for (String qk : validQuestionConditions) {
                    for (String ak : validAnswerConsequences) {
                        if (qk.equals(ak)) {
                            reasoningSummary.append("  Skipping rule generation: Question keyword '").append(qk).append("' is same as answer keyword.\n");
                            continue;
                        }
                        Set<String> conditions = new HashSet<>();
                        conditions.add(qk);
                        if (conditions.isEmpty()) {
                            reasoningSummary.append("  Warning: Conditions set is unexpectedly empty for qk='").append(qk).append("'. Skipping rule creation.\n");
                            continue;
                        }
                        Rule potentialRule = new Rule(conditions, ak, answerResult.getConfidence() * 0.9,
                                "Inference: '" + qk + "' often leads to '" + ak + "'");
                        boolean ruleExists = ruleEngine.getRules().stream().anyMatch(r ->
                                r.getConsequence().equals(potentialRule.getConsequence()) &&
                                        r.getConditions().equals(potentialRule.getConditions()));
                        if (!ruleExists) {
                            ruleEngine.addRule(potentialRule);
                            reasoningSummary.append("  Proposed new rule: ").append(potentialRule.getDescription()).append("\n");
                            return;
                        }
                    }
                }
            } else {
                reasoningSummary.append("  Skipping rule generation: No activated question or answer keywords for rule formation.\n");
            }
        } else {
            reasoningSummary.append("  Confidence too low for rule generation consideration.\n");
        }
    }

    public DynamicMemoryBuffer getMemoryBuffer() {
        return memoryBuffer;
    }

    public String getReasoningSummary() {
        return reasoningSummary.toString();
    }

    public double getDecayRate() {
        return decayRate;
    }

    public void setDecayRate(double decayRate) {
        if (decayRate >= 0.0 && decayRate <= 1.0) {
            this.decayRate = decayRate;
            semanticNetwork.setDecayRate(decayRate);
            isNetworkBuilt = false;
        } else {
            System.err.println("Warning: Decay rate must be between 0.0 and 1.0.");
        }
    }

    public double getActivationThreshold() {
        return activationThreshold;
    }

    public void setActivationThreshold(double activationThreshold) {
        if (activationThreshold >= 0.0 && activationThreshold <= 1.0) {
            this.activationThreshold = activationThreshold;
            spreadingActivator.setActivationThreshold(activationThreshold);
            ruleEngine.setActivationThreshold(activationThreshold);
            answerExtractor.setActivationThreshold(activationThreshold);
            isNetworkBuilt = false;
        } else {
            System.err.println("Warning: Activation threshold must be between 0.0 and 1.0.");
        }
    }

    public List<Rule> getRules() {
        return ruleEngine.getRules();
    }

    public void addRule(Rule rule) {
        ruleEngine.addRule(rule);
    }

    public boolean removeRuleByDescription(String description) {
        return ruleEngine.removeRuleByDescription(description);
    }

    public List<ConceptRelation> getConceptualKnowledgeBase() {
        return conceptualKnowledgeBase.getConceptualRelations();
    }

    public void addConceptualRelation(ConceptRelation relation) {
        conceptualKnowledgeBase.addConceptualRelation(relation);
    }

    public boolean removeConceptualRelationByDetails(String sourceConcept, String targetConcept, ConceptRelation.RelationType type) {
        return conceptualKnowledgeBase.removeConceptualRelationByDetails(sourceConcept, targetConcept, type);
    }

    public void resetKnowledgeBaseToDefaults() {
        ruleEngine.resetRulesToDefaults();
        conceptualKnowledgeBase.resetKnowledgeBaseToDefaults();
        reasoningSummary.append("\n[Meta-Cognition] Knowledge base reset to defaults.\n");
        isNetworkBuilt = false;
    }

    public void resetAdaptiveParametersAndMemory() {
        this.decayRate = 0.1;
        this.activationThreshold = 0.05;
        this.recentConfidences.clear();
        this.memoryBuffer.clear();
        semanticNetwork.setDecayRate(this.decayRate);
        spreadingActivator.setActivationThreshold(this.activationThreshold);
        ruleEngine.setActivationThreshold(this.activationThreshold);
        answerExtractor.setActivationThreshold(this.activationThreshold);
        reasoningSummary.append("\n[Meta-Cognition] Adaptive parameters and memory reset.\n");
        isNetworkBuilt = false;
    }

    public void resetProcessorState() {
        semanticNetwork = new SpreadingActivator(
                this.semanticNetwork,
                this.reasoningSummary,
                INITIAL_ACTIVATION,
                this.activationThreshold,
                MAX_SPREADING_ITERATIONS,
                NEGATION_EFFECT,
                CONCEPT_RELATION_BOOST,
                SENTIMENT_BOOST_FACTOR
        );
        isNetworkBuilt = false;
        lastProcessedContext = "";
        reasoningSummary.setLength(0);
        reasoningSummary.append("QnA Processor state reset (network and context cache).\n");
    }
}
