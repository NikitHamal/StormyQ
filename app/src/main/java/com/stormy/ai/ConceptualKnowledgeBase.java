package com.stormy.ai;

import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.SemanticNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages higher-level conceptual relationships.
 */
public class ConceptualKnowledgeBase {

    private final List<ConceptRelation> relations = new ArrayList<>();
    private final StringBuilder reasoning;

    public ConceptualKnowledgeBase(StringBuilder reasoning) {
        this.reasoning = reasoning;
        initDefaults();
    }

    private void initDefaults() {
        relations.add(new ConceptRelation(TextUtils.stem("cat"), TextUtils.stem("animal"), ConceptRelation.RelationType.IS_A, 0.95));
        relations.add(new ConceptRelation(TextUtils.stem("dog"), TextUtils.stem("animal"), ConceptRelation.RelationType.IS_A, 0.95));
        relations.add(new ConceptRelation(TextUtils.stem("apple"), TextUtils.stem("fruit"), ConceptRelation.RelationType.IS_A, 0.9));
        relations.add(new ConceptRelation(TextUtils.stem("car"), TextUtils.stem("vehicle"), ConceptRelation.RelationType.IS_A, 0.9));
        relations.add(new ConceptRelation(TextUtils.stem("wheel"), TextUtils.stem("car"), ConceptRelation.RelationType.PART_OF, 0.8));
        relations.add(new ConceptRelation(TextUtils.stem("rain"), TextUtils.stem("wet"), ConceptRelation.RelationType.CAUSES, 0.7));
    }

    public void integrateIntoNetwork(SemanticNetwork network) {
        reasoning.append("\n--- Integrating Conceptual Knowledge ---\n");
        for (ConceptRelation rel : relations) {
            SemanticNode source = network.getNode(rel.getSourceConcept());
            SemanticNode target = network.getNode(rel.getTargetConcept());

            if (source != null) {
                source.addConceptualRelation(rel);
                if (target != null) {
                    network.addEdge(source, target, false);
                }
            } else if (target != null) {
                target.addConceptualRelation(rel);
            }
        }
    }

    public void addConceptualRelation(ConceptRelation rel) {
        if (rel == null) return;
        relations.add(rel);
    }

    public List<ConceptRelation> getConceptualRelations() { return new ArrayList<>(relations); }
    public void resetKnowledgeBaseToDefaults() {
        relations.clear();
        initDefaults();
    }
}