package com.stormy.ai;

import android.content.Context;
import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.Rule;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages persistence for the Global Brain.
 */
public class KnowledgeStorage {

    private static final String RULES_FILE = "ai_rules.dat";
    private static final String RELATIONS_FILE = "ai_relations.dat";
    private final Context context;

    public KnowledgeStorage(Context context) {
        this.context = context;
    }

    public void saveRules(List<Rule> rules) {
        saveToFile(RULES_FILE, new ArrayList<>(rules));
    }

    @SuppressWarnings("unchecked")
    public List<Rule> loadRules() {
        return (List<Rule>) loadFromFile(RULES_FILE);
    }

    public void saveConceptualRelations(List<ConceptRelation> relations) {
        saveToFile(RELATIONS_FILE, new ArrayList<>(relations));
    }

    @SuppressWarnings("unchecked")
    public List<ConceptRelation> loadConceptualRelations() {
        return (List<ConceptRelation>) loadFromFile(RELATIONS_FILE);
    }

    private void saveToFile(String filename, Object data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE))) {
            oos.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Object loadFromFile(String filename) {
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) return new ArrayList<>();

        try (ObjectInputStream ois = new ObjectInputStream(context.openFileInput(filename))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
