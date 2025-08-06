package com.stormy.ai;

import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.SemanticNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages a collection of higher-level conceptual relationships (e.g., IS_A, PART_OF, CAUSES).
 * This knowledge can be used to enrich the semantic network during its construction.
 */
public class ConceptualKnowledgeBase { // <--- CORRECTED CLASS NAME HERE

    private List<ConceptRelation> conceptualRelations; // Static list of known conceptual relations
    private StringBuilder reasoningSummary; // For logging

    /**
     * Constructs a ConceptualKnowledgeBase.
     * @param reasoningSummary A StringBuilder to append reasoning logs.
     */
    public ConceptualKnowledgeBase(StringBuilder reasoningSummary) {
        this.conceptualRelations = new ArrayList<>();
        this.reasoningSummary = reasoningSummary;
        initializeDefaultConceptualKnowledgeBase(); // Populate with some default relations
    }

    /**
     * Initializes a set of default conceptual relations to expand the semantic network.
     * These represent factual or ontological knowledge.
     */
    private void initializeDefaultConceptualKnowledgeBase() {
        // IS_A relations
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("cat"), TextUtils.stem("animal"), ConceptRelation.RelationType.IS_A, 0.95));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("dog"), TextUtils.stem("animal"), ConceptRelation.RelationType.IS_A, 0.95));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("apple"), TextUtils.stem("fruit"), ConceptRelation.RelationType.IS_A, 0.9));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("car"), TextUtils.stem("vehicle"), ConceptRelation.RelationType.IS_A, 0.9));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("bird"), TextUtils.stem("animal"), ConceptRelation.RelationType.IS_A, 0.92));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("plane"), TextUtils.stem("vehicle"), ConceptRelation.RelationType.IS_A, 0.85));

        // PART_OF relations
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("wheel"), TextUtils.stem("car"), ConceptRelation.RelationType.PART_OF, 0.8));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("engine"), TextUtils.stem("car"), ConceptRelation.RelationType.PART_OF, 0.85));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("branch"), TextUtils.stem("tree"), ConceptRelation.RelationType.PART_OF, 0.7));

        // CAUSES relations (simplified)
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("rain"), TextUtils.stem("wet"), ConceptRelation.RelationType.CAUSES, 0.7));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("fire"), TextUtils.stem("heat"), ConceptRelation.RelationType.CAUSES, 0.8));

        // LOCATED_IN relations (simplified)
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("eiffel"), TextUtils.stem("paris"), ConceptRelation.RelationType.LOCATED_IN, 0.9)); // Stemmed "Eiffel" from "Eiffel Tower"
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("paris"), TextUtils.stem("france"), ConceptRelation.RelationType.LOCATED_IN, 0.95));

        // HAS_PROPERTY relations
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("sun"), TextUtils.stem("hot"), ConceptRelation.RelationType.HAS_PROPERTY, 0.85));
        conceptualRelations.add(new ConceptRelation(TextUtils.stem("ice"), TextUtils.stem("cold"), ConceptRelation.RelationType.HAS_PROPERTY, 0.9));
    }

    /**
     * Integrates conceptual relations from this knowledge base into the given semantic network.
     * This means adding the relations to relevant SemanticNodes within the network.
     * @param semanticNetwork The SemanticNetwork to integrate into.
     */
    public void integrateIntoNetwork(SemanticNetwork semanticNetwork) {
        reasoningSummary.append("\n--- Integrating Conceptual Knowledge ---\n");
        for (ConceptRelation relation : conceptualRelations) {
            SemanticNode source = semanticNetwork.getNode(relation.getSourceConcept());
            SemanticNode target = semanticNetwork.getNode(relation.getTargetConcept());

            // Add conceptual relation to nodes if they exist in the current context's network
            if (source != null) {
                source.addConceptualRelation(relation);
                // Optionally create an edge in the co-occurrence network too, or boost existing one
                if (target != null) {
                    // This is a new type of edge, but for simpler activation, we can just boost direct connection
                    semanticNetwork.addEdge(source, target, false); // Conceptual relations are generally not negated this way
                    semanticNetwork.addEdge(target, source, false);
                    reasoningSummary.append(" - Integrated relation: ").append(relation.toString()).append(" between '").append(source.getName()).append("' and '").append(target.getName()).append("'\n");
                } else {
                    reasoningSummary.append(" - Integrated relation: ").append(relation.toString()).append(" for source '").append(source.getName()).append("' (target '").append(relation.getTargetConcept()).append("' not in network)\n");
                }
            } else if (target != null) { // Also add to target if target is source in a reciprocal relationship sense
                target.addConceptualRelation(relation);
                reasoningSummary.append(" - Integrated relation: ").append(relation.toString()).append(" for target '").append(target.getName()).append("' (source '").append(relation.getSourceConcept()).append("' not in network)\n");
            } else {
                 reasoningSummary.append(" - Relation not integrated (neither source nor target concept in network): ").append(relation.toString()).append("\n");
            }
        }
    }


    /**
     * Adds a new conceptual relation to the knowledge base.
     * @param relation The ConceptRelation object to add.
     */
    public void addConceptualRelation(ConceptRelation relation) {
        if (relation != null) {
            // Check for exact duplicate relation (source, target, type)
            boolean exists = conceptualRelations.stream().anyMatch(r ->
                r.getSourceConcept().equals(relation.getSourceConcept()) &&
                r.getTargetConcept().equals(relation.getTargetConcept()) &&
                r.getType().equals(relation.getType())
            );
            if (!exists) {
                conceptualRelations.add(relation);
                reasoningSummary.append("\n[ConceptualKB] New conceptual relation added: ").append(relation.toString());
            } else {
                reasoningSummary.append("\n[ConceptualKB] Conceptual relation already exists, not added: ").append(relation.toString());
            }
        }
    }

    /**
     * Removes a conceptual relation from the knowledge base based on its source, target, and type.
     * @param sourceConcept The stemmed source concept of the relation.
     * @param targetConcept The stemmed target concept of the relation.
     * @param type The type of the relation.
     * @return true if the relation was removed, false otherwise.
     */
    public boolean removeConceptualRelationByDetails(String sourceConcept, String targetConcept, ConceptRelation.RelationType type) {
        if (sourceConcept == null || targetConcept == null || type == null ||
            sourceConcept.trim().isEmpty() || targetConcept.trim().isEmpty()) {
            return false;
        }
        final String stemmedSource = TextUtils.stem(sourceConcept);
        final String stemmedTarget = TextUtils.stem(targetConcept);

        boolean removed = conceptualRelations.removeIf(r ->
            r.getSourceConcept().equalsIgnoreCase(stemmedSource) &&
            r.getTargetConcept().equalsIgnoreCase(stemmedTarget) &&
            r.getType().equals(type)
        );
        if (removed) {
            reasoningSummary.append("\n[ConceptualKB] Conceptual relation removed: ").append(sourceConcept)
                            .append(" ").append(type.name()).append(" ").append(targetConcept);
        }
        return removed;
    }

    /**
     * Returns a copy of the current list of conceptual relations.
     * @return A List of ConceptRelation objects.
     */
    public List<ConceptRelation> getConceptualRelations() {
        return new ArrayList<>(conceptualRelations); // Return a copy
    }

    /**
     * Resets the conceptual knowledge base to its default state.
     */
    public void resetKnowledgeBaseToDefaults() {
        conceptualRelations.clear();
        initializeDefaultConceptualKnowledgeBase();
        reasoningSummary.append("\n[ConceptualKB] Conceptual knowledge base reset to defaults.\n");
    }
}
