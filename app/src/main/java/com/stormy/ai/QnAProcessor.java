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

import com.stormy.ai.SemanticNetwork;
import com.stormy.ai.SpreadingActivator;
import com.stormy.ai.RuleEngine;
import com.stormy.ai.ConceptualKnowledgeBase;
import com.stormy.ai.AnswerExtractor;


/**
 * The main QnA Processor, now acting as an orchestrator for modular components.
 * It remains a Singleton and manages the overall flow, adaptive parameters,
 * and integration of knowledge.
 */
public class QnAProcessor {

    // Singleton instance
    private static QnAProcessor instance;

    // Core Parameters (now mostly managed and set by QnAProcessor, but passed to modules)
    private static final double INITIAL_ACTIVATION = 0.8;
    private double decayRate = 0.1;
    private double activationThreshold = 0.05;
    private static final int MAX_SPREADING_ITERATIONS = 15;
    private static final double NEGATION_EFFECT = 0.7;
    private static final double CONCEPT_RELATION_BOOST = 0.2;
    private static final double SENTIMENT_BOOST_FACTOR = 0.1;

    // Self-monitoring parameters
    private static final int CONFIDENCE_HISTORY_SIZE = 5;
    private LinkedList<Double> recentConfidences;

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.3;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;

    // Modular Components
    private SemanticNetwork semanticNetwork;
    private SpreadingActivator spreadingActivator;
    private RuleEngine ruleEngine;
    private ConceptualKnowledgeBase conceptualKnowledgeBase;
    private AnswerExtractor answerExtractor;

    private DynamicMemoryBuffer memoryBuffer;
    private StringBuilder reasoningSummary; // Centralized for logging from all modules

    // For network caching and optimization
    private String lastProcessedContext = "";
    private boolean isNetworkBuilt = false; // Flag to indicate if the network is currently valid

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private QnAProcessor() {
        this.reasoningSummary = new StringBuilder(); // Initialize first, as other modules depend on it
        this.semanticNetwork = new SemanticNetwork(this.decayRate); // Pass initial decay rate
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
     * Finds an answer to the question within the given context using a semantic network
     * and spreading activation, incorporating temporal reasoning, rule evaluation,
     * sentiment analysis, and meta-cognition for self-monitoring and adaptive processing.
     * @param context The text in which to find the answer.
     * @param question The question to answer.
     * @return An AnswerResult object containing the answer, its position, and confidence.
     */
    public AnswerResult findAnswer(String context, String question) {
        reasoningSummary.setLength(0); // Clear summary for new query
        reasoningSummary.append("--- Reasoning Process Summary ---\n"); // Add header back for each new query

        // 1. Build/Reuse Semantic Network from the context
        if (!isNetworkBuilt || !context.equals(lastProcessedContext)) {
            reasoningSummary.append("Context changed or network not built. Rebuilding semantic network...\n");
            semanticNetwork.buildNetwork(context);
            // Integrate conceptual knowledge into the newly built network
            conceptualKnowledgeBase.integrateIntoNetwork(semanticNetwork);
            lastProcessedContext = context;
            isNetworkBuilt = true;
            reasoningSummary.append("\n--- Semantic Network Built ---\n");
            reasoningSummary.append("Nodes in network: ").append(semanticNetwork.getNodeCount()).append("\n");
        } else {
            reasoningSummary.append("Using existing semantic network for context. Nodes: ").append(semanticNetwork.getNodeCount()).append("\n");
            semanticNetwork.resetActivations(); // Only reset activations for a new query on the same network
        }

        // 2. Extract keywords from the question and stem them
        List<String> questionKeywords = TextUtils.tokenize(question).stream()
                                                 .map(TextUtils::stem)
                                                 .collect(Collectors.toList());

        // Store original, non-stemmed, lowercased keywords from the question
        List<String> originalQuestionWords = TextUtils.tokenize(question).stream()
                                                      .map(String::toLowerCase)
                                                      .collect(Collectors.toList());

        // Calculate question sentiment
        int questionSentiment = TextUtils.getSentimentScore(question);

        // Remove keywords not present in the network (context) or stop words
        questionKeywords.removeIf(kw -> semanticNetwork.getNode(kw) == null || TextUtils.isStopWord(kw));

        if (questionKeywords.isEmpty()) {
            reasoningSummary.append("No relevant question keywords found in context.\n");
            return new AnswerResult("", context, -1, -1, 0.0); // No relevant keywords
        }
        reasoningSummary.append("Stemmed Question Keywords: ").append(questionKeywords).append("\n");

        // 3. Perform Spreading Activation
        spreadingActivator.activate(questionKeywords, questionSentiment);

        // 4. Evaluate Probabilistic Rules based on activated network
        ruleEngine.evaluateRules(semanticNetwork);

        // 5. Extract and Rank Answers
        List<AnswerCandidate> rankedCandidates = answerExtractor.extractRankedAnswers(context, question, semanticNetwork);

        AnswerResult currentAnswer;
        if (rankedCandidates.isEmpty()) {
            currentAnswer = new AnswerResult("", context, -1, -1, 0.0);
        } else {
            AnswerCandidate bestCandidate = rankedCandidates.get(0);
            currentAnswer = new AnswerResult(bestCandidate.getText(), bestCandidate.getSourceSentence(),
                                             bestCandidate.getStartIndex(), bestCandidate.getEndIndex(),
                                             bestCandidate.getFinalScore());
        }


        // --- Meta-Cognition: Self-monitoring and Adaptive Processing ---
        // Add the confidence of the current answer to the history
        if (recentConfidences.size() >= CONFIDENCE_HISTORY_SIZE) {
            recentConfidences.removeFirst(); // Remove oldest
        }
        recentConfidences.addLast(currentAnswer.getConfidence());

        // Adjust processing parameters based on recent performance
        adjustProcessingParameters();
        
        // Consider generating new rules based on high-confidence answers
        considerGeneratingRuleFeedback(context, question, currentAnswer); // This method still needs access to internal state for now

        if (currentAnswer.isValid()) {
            memoryBuffer.addResult(currentAnswer);
            reasoningSummary.append("[Memory Buffer] Answer added to memory.\n");
        } else {
            reasoningSummary.append("[Memory Buffer] Answer not valid enough to be added to memory.\n");
        }

        return currentAnswer;
    }

    /**
     * Adjusts processing parameters (e.g., activation threshold, decay rate) based on recent performance.
     * This is a simple adaptive control mechanism.
     */
    private void adjustProcessingParameters() {
        if (recentConfidences.size() < CONFIDENCE_HISTORY_SIZE) {
            reasoningSummary.append("\n[Meta-Cognition] Not enough data for adaptive parameter adjustment (need ").append(CONFIDENCE_HISTORY_SIZE)
                            .append(", have ").append(recentConfidences.size()).append(")\n");
            return; // Not enough data to adjust
        }

        double averageConfidence = recentConfidences.stream()
                                                    .mapToDouble(Double::doubleValue)
                                                    .average()
                                                    .orElse(0.0);

        reasoningSummary.append("\n[Meta-Cognition] Average recent confidence: ").append(String.format("%.2f", averageConfidence)).append("\n");

        // If average confidence is consistently low, make the system "more cautious"
        if (averageConfidence < LOW_CONFIDENCE_THRESHOLD) {
            this.activationThreshold = Math.min(0.2, this.activationThreshold + 0.01);
            this.decayRate = Math.max(0.05, this.decayRate - 0.005);
            reasoningSummary.append(" - Low confidence detected. Adjusting parameters:\n")
                            .append("   - activationThreshold: ").append(String.format("%.2f", this.activationThreshold))
                            .append("\n   - decayRate: ").append(String.format("%.2f", this.decayRate)).append("\n");
        } else if (averageConfidence > HIGH_CONFIDENCE_THRESHOLD) { // If confidence is high, make it "less cautious"
            this.activationThreshold = Math.max(0.05, this.activationThreshold - 0.005);
            this.decayRate = Math.min(0.2, this.decayRate + 0.005);
             reasoningSummary.append(" - High confidence detected. Adjusting parameters:\n")
                            .append("   - activationThreshold: ").append(String.format("%.2f", this.activationThreshold))
                            .append("\n   - decayRate: ").append(String.format("%.2f", this.decayRate)).append("\n");
        } else {
             reasoningSummary.append(" - Confidence stable. No parameter adjustment.\n");
        }
        // Update dependent modules with new parameters
        semanticNetwork.setDecayRate(this.decayRate);
        spreadingActivator.setActivationThreshold(this.activationThreshold);
        ruleEngine.setActivationThreshold(this.activationThreshold);
        answerExtractor.setActivationThreshold(this.activationThreshold);
    }

    /**
     * Simulates a feedback loop for rule generation. This part could be further
     * modularized into a separate "LearningAgent" if complexity grows.
     * @param context The context that led to the high confidence answer.
     * @param question The question asked.
     * @param answerResult The high confidence answer result.
     */
    private void considerGeneratingRuleFeedback(String context, String question, AnswerResult answerResult) {
        reasoningSummary.append("\n[Meta-Cognition] Considering feedback for rule generation...\n");

        if (answerResult.getConfidence() >= HIGH_CONFIDENCE_THRESHOLD) {
            reasoningSummary.append("  High confidence answer detected. Analyzing for new rule opportunities.\n");

            // Activated keywords from the *answer itself* that are also in the network and activated
            List<String> activatedAnswerKeywords = TextUtils.tokenize(answerResult.getAnswer()).stream()
                    .map(TextUtils::stem)
                    .filter(s -> !s.isEmpty() && semanticNetwork.getNode(s) != null && semanticNetwork.getNode(s).getActivation() >= activationThreshold)
                    .collect(Collectors.toList());

            // Keywords from the *question* that are also in the network and activated
            List<String> questionKeywords = TextUtils.tokenize(question).stream()
                    .map(TextUtils::stem)
                    .filter(s -> !s.isEmpty() && semanticNetwork.getNode(s) != null && semanticNetwork.getNode(s).getActivation() >= activationThreshold)
                    .collect(Collectors.toList());

            if (!questionKeywords.isEmpty() && !activatedAnswerKeywords.isEmpty()) {
                // Filter out stop words (again, defensively, though previous filtering should handle most cases)
                List<String> validQuestionConditions = questionKeywords.stream()
                        .filter(s -> !TextUtils.isStopWord(s) && !s.isEmpty()) // Ensure conditions are not empty strings
                        .collect(Collectors.toList());

                List<String> validAnswerConsequences = activatedAnswerKeywords.stream()
                        .filter(s -> !TextUtils.isStopWord(s) && !s.isEmpty()) // Ensure consequences are not empty strings
                        .collect(Collectors.toList());

                // Crucial Check: Ensure we have valid, non-empty lists for rule generation
                if (validQuestionConditions.isEmpty() || validAnswerConsequences.isEmpty()) {
                    reasoningSummary.append("  Skipping rule generation: No valid question or answer keywords after filtering.\n");
                    return; // Exit if no valid keywords to form rules
                }

                for (String qk : validQuestionConditions) {
                    for (String ak : validAnswerConsequences) {
                        if (qk.equals(ak)) {
                            reasoningSummary.append("  Skipping rule generation: Question keyword '").append(qk).append("' is same as answer keyword.\n");
                            continue; // Avoid self-referential rules
                        }

                        Set<String> conditions = new HashSet<>();
                        conditions.add(qk);

                        // Double-check: ensure the conditions set is not empty *after* adding the keyword
                        // This should ideally never be empty if qk is valid and non-empty.
                        if (conditions.isEmpty()) {
                             reasoningSummary.append("  Warning: Conditions set is unexpectedly empty for qk='").append(qk).append("'. Skipping rule creation.\n");
                             continue; // This should ideally not happen if qk is valid and non-empty
                        }

                        Rule potentialRule = new Rule(conditions, ak, answerResult.getConfidence() * 0.9,
                                "Inference: '" + qk + "' often leads to '" + ak + "'");

                        boolean ruleExists = ruleEngine.getRules().stream().anyMatch(r ->
                                r.getConsequence().equals(potentialRule.getConsequence()) &&
                                r.getConditions().equals(potentialRule.getConditions()));

                        if (!ruleExists) {
                            ruleEngine.addRule(potentialRule);
                            reasoningSummary.append("  Proposed new rule: ").append(potentialRule.getDescription()).append("\n");
                            return; // Add one rule at a time for simplicity
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


    /**
     * Provides access to the dynamic memory buffer.
     * @return The DynamicMemoryBuffer instance.
     */
    public DynamicMemoryBuffer getMemoryBuffer() {
        return memoryBuffer;
    }

    /**
     * Provides the text-based summary of the reasoning process.
     * This can be used for debugging or displaying to the user.
     * @return A String containing the reasoning summary.
     */
    public String getReasoningSummary() {
        return reasoningSummary.toString();
    }

    // --- Methods for external training/monitoring (delegated to modules) ---

    public double getDecayRate() {
        return decayRate;
    }

    public void setDecayRate(double decayRate) {
        if (decayRate >= 0.0 && decayRate <= 1.0) {
            this.decayRate = decayRate;
            semanticNetwork.setDecayRate(decayRate); // Update underlying network
            isNetworkBuilt = false; // Invalidate network if decay rate changes
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
            isNetworkBuilt = false; // Invalidate network if threshold changes
        } else {
            System.err.println("Warning: Activation threshold must be between 0.0 and 1.0.");
        }
    }

    /**
     * Returns a copy of the current list of rules (delegated to RuleEngine).
     * @return A List of Rule objects.
     */
    public List<Rule> getRules() {
        return ruleEngine.getRules();
    }

    /**
     * Adds a rule (delegated to RuleEngine).
     * @param rule The rule to add.
     */
    public void addRule(Rule rule) {
        ruleEngine.addRule(rule);
    }

    /**
     * Removes a rule by description (delegated to RuleEngine).
     * @param description The description of the rule to remove.
     * @return True if removed, false otherwise.
     */
    public boolean removeRuleByDescription(String description) {
        return ruleEngine.removeRuleByDescription(description);
    }

    /**
     * Returns a copy of the current list of conceptual relations (delegated to ConceptualKnowledgeBase).
     * @return A List of ConceptRelation objects.
     */
    public List<ConceptRelation> getConceptualKnowledgeBase() {
        return conceptualKnowledgeBase.getConceptualRelations();
    }

    /**
     * Adds a conceptual relation (delegated to ConceptualKnowledgeBase).
     * @param relation The relation to add.
     */
    public void addConceptualRelation(ConceptRelation relation) {
        conceptualKnowledgeBase.addConceptualRelation(relation);
    }

    /**
     * Removes a conceptual relation by details (delegated to ConceptualKnowledgeBase).
     * @param sourceConcept The source concept.
     * @param targetConcept The target concept.
     * @param type The relation type.
     * @return True if removed, false otherwise.
     */
    public boolean removeConceptualRelationByDetails(String sourceConcept, String targetConcept, ConceptRelation.RelationType type) {
        return conceptualKnowledgeBase.removeConceptualRelationByDetails(sourceConcept, targetConcept, type);
    }

    /**
     * Resets the entire knowledge base (rules and conceptual relations) to their default states.
     * Use with caution.
     */
    public void resetKnowledgeBaseToDefaults() {
        ruleEngine.resetRulesToDefaults();
        conceptualKnowledgeBase.resetKnowledgeBaseToDefaults();
        reasoningSummary.append("\n[Meta-Cognition] Knowledge base reset to defaults.\n");
        isNetworkBuilt = false; // Invalidate current network as knowledge base changed
    }

    /**
     * Clears all adaptive parameters and memory, effectively resetting the AI's learning state.
     */
    public void resetAdaptiveParametersAndMemory() {
        this.decayRate = 0.1;
        this.activationThreshold = 0.05;
        this.recentConfidences.clear();
        this.memoryBuffer.clear();
        // Update dependent modules
        semanticNetwork.setDecayRate(this.decayRate);
        spreadingActivator.setActivationThreshold(this.activationThreshold);
        ruleEngine.setActivationThreshold(this.activationThreshold);
        answerExtractor.setActivationThreshold(this.activationThreshold);

        reasoningSummary.append("\n[Meta-Cognition] Adaptive parameters and memory reset.\n");
        isNetworkBuilt = false; // Invalidate current network as adaptive parameters changed
    }

    /**
     * Resets the internal state of the QnAProcessor, effectively clearing the semantic network
     * and invalidating the cached context. This should be called when a new document is loaded
     * or a fresh start is desired for context processing.
     */
    public void resetProcessorState() {
        // Clear the semantic network instance and force new one
        semanticNetwork = new SemanticNetwork(this.decayRate);
        // Re-initialize spreading activator with new network instance
        spreadingActivator = new SpreadingActivator(
                this.semanticNetwork,
                this.reasoningSummary,
                INITIAL_ACTIVATION,
                this.activationThreshold,
                MAX_SPREADING_ITERATIONS,
                NEGATION_EFFECT,
                CONCEPT_RELATION_BOOST,
                SENTIMENT_BOOST_FACTOR
        );
        // Re-integrate conceptual knowledge into the new network next time buildNetwork is called
        isNetworkBuilt = false;
        lastProcessedContext = "";
        reasoningSummary.setLength(0);
        reasoningSummary.append("QnA Processor state reset (network and context cache).\n");
    }
}
