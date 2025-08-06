package com.stormy.ai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.stormy.ai.TextUtils; // Correct import for your custom TextUtils class
import com.stormy.ai.models.ConceptRelation;
import com.stormy.ai.models.Rule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.lang.String; // Explicitly import String for join if needed, or rely on implicit import

public class TrainActivity extends AppCompatActivity {

    private QnAProcessor qnaProcessor;

    private static final int PICK_FILE_REQUEST_CODE = 1;

    // UI elements for AI Parameters
    private EditText decayRateEditText;
    private EditText activationThresholdEditText;
    private Button saveParametersButton;

    // UI elements for Word Lists
    private EditText stopWordsEditText;
    private Button updateStopWordsButton;
    private EditText negationWordsEditText;
    private Button updateNegationWordsButton;
    private EditText positiveWordsEditText;
    private Button updatePositiveWordsButton;
    private EditText negativeWordsEditText;
    private Button updateNegativeWordsButton;

    // UI elements for Rules
    private EditText newRuleConditionsEditText;
    private EditText newRuleConsequenceEditText;
    private EditText newRuleConfidenceEditText;
    private EditText newRuleDescriptionEditText;
    private Button addRuleButton;
    private TextView currentRulesTextView;
    private Button refreshRulesButton;
    private EditText removeRuleDescriptionEditText;
    private Button removeRuleButton;

    // UI elements for Conceptual Relations
    private EditText newRelationSourceEditText;
    private EditText newRelationTargetEditText;
    private Spinner newRelationTypeSpinner;
    private EditText newRelationStrengthEditText;
    private Button addRelationButton;
    private TextView currentRelationsTextView;
    private Button refreshRelationsButton;
    private EditText removeRelationSourceEditText;
    private EditText removeRelationTargetEditText;
    private Spinner removeRelationTypeSpinner;
    private Button removeRelationButton;

    // UI elements for Reset Options
    private Button resetKnowledgeBaseButton;
    private Button resetAdaptiveParametersButton;

    // UI elements for JSON Import
    private Button importJsonButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.train);

        qnaProcessor = QnAProcessor.getInstance();

        initViews();
        loadCurrentAIData();
        setupListeners();
    }

    private void initViews() {
        decayRateEditText = findViewById(R.id.decayRateEditText);
        activationThresholdEditText = findViewById(R.id.activationThresholdEditText);
        saveParametersButton = findViewById(R.id.saveParametersButton);

        stopWordsEditText = findViewById(R.id.stopWordsEditText);
        updateStopWordsButton = findViewById(R.id.updateStopWordsButton);
        negationWordsEditText = findViewById(R.id.negationWordsEditText);
        updateNegationWordsButton = findViewById(R.id.updateNegationWordsButton);
        positiveWordsEditText = findViewById(R.id.positiveWordsEditText);
        updatePositiveWordsButton = findViewById(R.id.updatePositiveWordsButton);
        negativeWordsEditText = findViewById(R.id.negativeWordsEditText);
        updateNegativeWordsButton = findViewById(R.id.updateNegativeWordsButton);

        newRuleConditionsEditText = findViewById(R.id.newRuleConditionsEditText);
        newRuleConsequenceEditText = findViewById(R.id.newRuleConsequenceEditText);
        newRuleConfidenceEditText = findViewById(R.id.newRuleConfidenceEditText);
        newRuleDescriptionEditText = findViewById(R.id.newRuleDescriptionEditText);
        addRuleButton = findViewById(R.id.addRuleButton);
        currentRulesTextView = findViewById(R.id.currentRulesTextView);
        refreshRulesButton = findViewById(R.id.refreshRulesButton);
        removeRuleDescriptionEditText = findViewById(R.id.removeRuleDescriptionEditText);
        removeRuleButton = findViewById(R.id.removeRuleButton);

        newRelationSourceEditText = findViewById(R.id.newRelationSourceEditText);
        newRelationTargetEditText = findViewById(R.id.newRelationTargetEditText);
        newRelationTypeSpinner = findViewById(R.id.newRelationTypeSpinner);
        newRelationStrengthEditText = findViewById(R.id.newRelationStrengthEditText);
        addRelationButton = findViewById(R.id.addRelationButton);
        currentRelationsTextView = findViewById(R.id.currentRelationsTextView);
        refreshRelationsButton = findViewById(R.id.refreshRelationsButton);
        removeRelationSourceEditText = findViewById(R.id.removeRelationSourceEditText);
        removeRelationTargetEditText = findViewById(R.id.removeRelationTargetEditText);
        removeRelationTypeSpinner = findViewById(R.id.removeRelationTypeSpinner);
        removeRelationButton = findViewById(R.id.removeRelationButton);

        resetKnowledgeBaseButton = findViewById(R.id.resetKnowledgeBaseButton);
        resetAdaptiveParametersButton = findViewById(R.id.resetAdaptiveParametersButton);

        importJsonButton = findViewById(R.id.importJsonButton);

        ArrayAdapter<ConceptRelation.RelationType> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ConceptRelation.RelationType.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        newRelationTypeSpinner.setAdapter(adapter);
        removeRelationTypeSpinner.setAdapter(adapter);
    }

    private void loadCurrentAIData() {
        decayRateEditText.setText(String.valueOf(qnaProcessor.getDecayRate()));
        activationThresholdEditText.setText(String.valueOf(qnaProcessor.getActivationThreshold()));

        stopWordsEditText.setText(String.join(", ", TextUtils.getStopWords()));
        negationWordsEditText.setText(String.join(", ", TextUtils.getNegationWords()));
        positiveWordsEditText.setText(String.join(", ", TextUtils.getPositiveWords()));
        negativeWordsEditText.setText(String.join(", ", TextUtils.getNegativeWords()));

        displayCurrentRules();
        displayCurrentRelations();
    }

    private void setupListeners() {
        saveParametersButton.setOnClickListener(v -> saveAIParameters());

        updateStopWordsButton.setOnClickListener(v -> updateWordList(
                stopWordsEditText, TextUtils.getStopWords(), TextUtils::addStopWord, TextUtils::removeStopWord));
        updateNegationWordsButton.setOnClickListener(v -> updateWordList(
                negationWordsEditText, TextUtils.getNegationWords(), TextUtils::addNegationWord, TextUtils::removeNegationWord));
        updatePositiveWordsButton.setOnClickListener(v -> updateWordList(
                positiveWordsEditText, TextUtils.getPositiveWords(), TextUtils::addPositiveWord, TextUtils::removePositiveWord));
        updateNegativeWordsButton.setOnClickListener(v -> updateWordList(
                negativeWordsEditText, TextUtils.getNegativeWords(), TextUtils::addNegativeWord, TextUtils::removeNegativeWord));

        addRuleButton.setOnClickListener(v -> addNewRule());
        refreshRulesButton.setOnClickListener(v -> displayCurrentRules());
        removeRuleButton.setOnClickListener(v -> removeRule());

        addRelationButton.setOnClickListener(v -> addNewRelation());
        refreshRelationsButton.setOnClickListener(v -> displayCurrentRelations());
        removeRelationButton.setOnClickListener(v -> removeRelation());

        resetKnowledgeBaseButton.setOnClickListener(v -> resetKnowledgeBase());
        resetAdaptiveParametersButton.setOnClickListener(v -> resetAdaptiveParameters());

        importJsonButton.setOnClickListener(v -> openFilePicker());
    }

    private void saveAIParameters() {
        try {
            double decayRate = Double.parseDouble(decayRateEditText.getText().toString());
            double activationThreshold = Double.parseDouble(activationThresholdEditText.getText().toString());

            qnaProcessor.setDecayRate(decayRate);
            qnaProcessor.setActivationThreshold(activationThreshold);
            Toast.makeText(this, "AI parameters updated successfully!", Toast.LENGTH_SHORT).show();
            // This change affects the network, so invalidate it for next QnA cycle
            qnaProcessor.resetProcessorState();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format for parameters. Please use decimals.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateWordList(EditText editText, Set<String> currentWords,
                                java.util.function.Consumer<String> addFunction,
                                java.util.function.Consumer<String> removeFunction) {
        String input = editText.getText().toString().trim();
        Set<String> newWords = new HashSet<>();
        if (!input.isEmpty()) {
            newWords = Arrays.stream(input.split(","))
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }

        Set<String> wordsToRemove = new HashSet<>(currentWords);
        wordsToRemove.removeAll(newWords);

        Set<String> wordsToAdd = new HashSet<>(newWords);
        wordsToAdd.removeAll(currentWords);

        for (String word : wordsToRemove) {
            removeFunction.accept(word);
        }
        for (String word : wordsToAdd) {
            addFunction.accept(word);
        }

        Toast.makeText(this, "Word list updated. Added " + wordsToAdd.size() + ", Removed " + wordsToRemove.size(), Toast.LENGTH_SHORT).show();
        editText.setText(String.join(", ", currentWords));
        // Changes to word lists can affect network building, so invalidate
        qnaProcessor.resetProcessorState();
    }


    private void addNewRule() {
        String conditionsStr = newRuleConditionsEditText.getText().toString().trim();
        String consequence = newRuleConsequenceEditText.getText().toString().trim();
        String confidenceStr = newRuleConfidenceEditText.getText().toString().trim();
        String description = newRuleDescriptionEditText.getText().toString().trim();

        if (conditionsStr.isEmpty() || consequence.isEmpty() || confidenceStr.isEmpty()) {
            Toast.makeText(this, "Conditions, Consequence, and Confidence are required for a new rule.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Set<String> conditions = Arrays.stream(conditionsStr.split(","))
                    .map(s -> TextUtils.stem(s.trim()))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            double confidence = Double.parseDouble(confidenceStr);

            if (conditions.isEmpty()) {
                Toast.makeText(this, "Please provide at least one condition for the rule.", Toast.LENGTH_LONG).show();
                return;
            }
            if (confidence < 0.0 || confidence > 1.0) {
                Toast.makeText(this, "Confidence must be between 0.0 and 1.0.", Toast.LENGTH_LONG).show();
                return;
            }

            Rule newRule = new Rule(conditions, TextUtils.stem(consequence), confidence, description);
            qnaProcessor.addRule(newRule);
            Toast.makeText(this, "Rule added: " + newRule.getDescription(), Toast.LENGTH_SHORT).show();
            clearRuleInputs();
            displayCurrentRules();
            // Rules change can affect reasoning, invalidate processor state
            qnaProcessor.resetProcessorState();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid confidence format. Please use a decimal (e.g., 0.75).", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Error adding rule: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void removeRule() {
        String description = removeRuleDescriptionEditText.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter the description of the rule to remove.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean removed = qnaProcessor.removeRuleByDescription(description);
        if (removed) {
            Toast.makeText(this, "Rule '" + description + "' removed successfully.", Toast.LENGTH_SHORT).show();
            removeRuleDescriptionEditText.setText("");
            displayCurrentRules();
            qnaProcessor.resetProcessorState(); // Invalidate state as rules changed
        } else {
            Toast.makeText(this, "Rule with description '" + description + "' not found.", Toast.LENGTH_LONG).show();
        }
    }


    private void clearRuleInputs() {
        newRuleConditionsEditText.setText("");
        newRuleConsequenceEditText.setText("");
        newRuleConfidenceEditText.setText("");
        newRuleDescriptionEditText.setText("");
    }

    private void displayCurrentRules() {
        List<Rule> rules = qnaProcessor.getRules();
        if (rules.isEmpty()) {
            currentRulesTextView.setText("No rules currently defined.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            sb.append(i + 1).append(". ");
            sb.append("IF (").append(String.join(", ", rule.getConditions())).append(") ");
            sb.append("THEN (").append(rule.getConsequence()).append(") ");
            sb.append("[Conf: ").append(String.format("%.2f", rule.getConfidence())).append("] ");
            if (!rule.getDescription().isEmpty() && !rule.getDescription().equals("Unnamed Rule")) {
                sb.append(" (").append(rule.getDescription()).append(")");
            }
            sb.append("\n");
        }
        currentRulesTextView.setText(sb.toString());
    }

    private void addNewRelation() {
        String source = newRelationSourceEditText.getText().toString().trim();
        String target = newRelationTargetEditText.getText().toString().trim();
        ConceptRelation.RelationType type = (ConceptRelation.RelationType) newRelationTypeSpinner.getSelectedItem();
        String strengthStr = newRelationStrengthEditText.getText().toString().trim();

        if (source.isEmpty() || target.isEmpty() || strengthStr.isEmpty() || type == null) {
            Toast.makeText(this, "Source, Target, Type, and Strength are required for a new relation.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            double strength = Double.parseDouble(strengthStr);
            if (strength < 0.0 || strength > 1.0) {
                Toast.makeText(this, "Strength must be between 0.0 and 1.0.", Toast.LENGTH_LONG).show();
                return;
            }

            ConceptRelation newRelation = new ConceptRelation(TextUtils.stem(source), TextUtils.stem(target), type, strength);
            qnaProcessor.addConceptualRelation(newRelation);
            Toast.makeText(this, "Relation added: " + newRelation.toString(), Toast.LENGTH_SHORT).show();
            clearRelationInputs();
            displayCurrentRelations();
            // Relations change can affect network building, invalidate processor state
            qnaProcessor.resetProcessorState();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid strength format. Please use a decimal (e.g., 0.9).", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Error adding relation: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void removeRelation() {
        String source = removeRelationSourceEditText.getText().toString().trim();
        String target = removeRelationTargetEditText.getText().toString().trim();
        ConceptRelation.RelationType type = (ConceptRelation.RelationType) removeRelationTypeSpinner.getSelectedItem();

        if (source.isEmpty() || target.isEmpty() || type == null) {
            Toast.makeText(this, "Source, Target, and Type are required to remove a relation.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean removed = qnaProcessor.removeConceptualRelationByDetails(source, target, type);
        if (removed) {
            Toast.makeText(this, "Relation removed successfully.", Toast.LENGTH_SHORT).show();
            removeRelationSourceEditText.setText("");
            removeRelationTargetEditText.setText("");
            removeRelationTypeSpinner.setSelection(0);
            displayCurrentRelations();
            qnaProcessor.resetProcessorState(); // Invalidate state as relations changed
        } else {
            Toast.makeText(this, "Relation not found.", Toast.LENGTH_LONG).show();
        }
    }

    private void clearRelationInputs() {
        newRelationSourceEditText.setText("");
        newRelationTargetEditText.setText("");
        newRelationTypeSpinner.setSelection(0);
        newRelationStrengthEditText.setText("");
    }

    private void displayCurrentRelations() {
        List<ConceptRelation> relations = qnaProcessor.getConceptualKnowledgeBase();
        if (relations.isEmpty()) {
            currentRelationsTextView.setText("No conceptual relations currently defined.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < relations.size(); i++) {
            ConceptRelation relation = relations.get(i);
            sb.append(i + 1).append(". ");
            sb.append(relation.getSourceConcept()).append(" ")
              .append(relation.getType().name()).append(" ")
              .append(relation.getTargetConcept()).append(" ");
            sb.append("[Strength: ").append(String.format("%.2f", relation.getStrength())).append("]");
            sb.append("\n");
        }
        currentRelationsTextView.setText(sb.toString());
    }

    private void resetKnowledgeBase() {
        qnaProcessor.resetKnowledgeBaseToDefaults();
        Toast.makeText(this, "Knowledge Base reset to defaults!", Toast.LENGTH_SHORT).show();
        loadCurrentAIData(); // Reload all data as it might have changed
        qnaProcessor.resetProcessorState(); // Force network rebuild on next query
    }

    private void resetAdaptiveParameters() {
        qnaProcessor.resetAdaptiveParametersAndMemory();
        Toast.makeText(this, "Adaptive parameters and memory reset!", Toast.LENGTH_SHORT).show();
        loadCurrentAIData(); // Reload all data as it might have changed
        qnaProcessor.resetProcessorState(); // Force network rebuild on next query
    }

    // --- JSON Import Functionality ---

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        String jsonString = readTextFromUri(uri);
                        processJsonTrainingData(jsonString);
                        Toast.makeText(this, "JSON data imported successfully! (Restart app for full effect)", Toast.LENGTH_LONG).show();
                        loadCurrentAIData(); // Refresh UI after import
                        // Force network rebuild on next QnA run as knowledge base might have changed
                        qnaProcessor.resetProcessorState();
                    } catch (IOException e) {
                        Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    private void processJsonTrainingData(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);

        // --- Process AI Parameters (Optional in JSON) ---
        if (jsonObject.has("parameters")) {
            JSONObject params = jsonObject.getJSONObject("parameters");
            if (params.has("decayRate")) {
                qnaProcessor.setDecayRate(params.getDouble("decayRate"));
            }
            if (params.has("activationThreshold")) {
                qnaProcessor.setActivationThreshold(params.getDouble("activationThreshold"));
            }
        }

        // --- Process Word Lists ---
        if (jsonObject.has("wordLists")) {
            JSONObject wordLists = jsonObject.getJSONObject("wordLists");

            if (wordLists.has("stopWords")) {
                JSONArray stopWordsArray = wordLists.getJSONArray("stopWords");
                Set<String> newStopWords = new HashSet<>();
                for (int i = 0; i < stopWordsArray.length(); i++) {
                    newStopWords.add(stopWordsArray.getString(i).trim().toLowerCase());
                }
                TextUtils.getStopWords().clear();
                TextUtils.getStopWords().addAll(newStopWords);
            }
            if (wordLists.has("negationWords")) {
                JSONArray negationWordsArray = wordLists.getJSONArray("negationWords");
                Set<String> newNegationWords = new HashSet<>();
                for (int i = 0; i < negationWordsArray.length(); i++) {
                    newNegationWords.add(negationWordsArray.getString(i).trim().toLowerCase());
                }
                TextUtils.getNegationWords().clear();
                TextUtils.getNegationWords().addAll(newNegationWords);
            }
            if (wordLists.has("positiveWords")) {
                JSONArray positiveWordsArray = wordLists.getJSONArray("positiveWords");
                Set<String> newPositiveWords = new HashSet<>();
                for (int i = 0; i < positiveWordsArray.length(); i++) {
                    newPositiveWords.add(positiveWordsArray.getString(i).trim().toLowerCase());
                }
                TextUtils.getPositiveWords().clear();
                TextUtils.getPositiveWords().addAll(newPositiveWords);
            }
            if (wordLists.has("negativeWords")) {
                JSONArray negativeWordsArray = wordLists.getJSONArray("negativeWords");
                Set<String> newNegativeWords = new HashSet<>();
                for (int i = 0; i < negativeWordsArray.length(); i++) {
                    newNegativeWords.add(negativeWordsArray.getString(i).trim().toLowerCase());
                }
                TextUtils.getNegativeWords().clear();
                TextUtils.getNegativeWords().addAll(newNegativeWords);
            }
        }

        // --- Process Rules ---
        if (jsonObject.has("rules")) {
            JSONArray rulesArray = jsonObject.getJSONArray("rules");
            for (int i = 0; i < rulesArray.length(); i++) {
                JSONObject ruleObj = rulesArray.getJSONObject(i);
                JSONArray conditionsJson = ruleObj.getJSONArray("conditions");
                Set<String> conditions = new HashSet<>();
                for (int j = 0; j < conditionsJson.length(); j++) {
                    conditions.add(TextUtils.stem(conditionsJson.getString(j).trim()));
                }
                String consequence = TextUtils.stem(ruleObj.getString("consequence").trim());
                double confidence = ruleObj.getDouble("confidence");
                String description = ruleObj.optString("description", "Unnamed Rule");

                Rule rule = new Rule(conditions, consequence, confidence, description);
                qnaProcessor.addRule(rule); // Add method handles duplicates
            }
        }

        // --- Process Conceptual Relations ---
        if (jsonObject.has("conceptualRelations")) {
            JSONArray relationsArray = jsonObject.getJSONArray("conceptualRelations");
            for (int i = 0; i < relationsArray.length(); i++) {
                JSONObject relationObj = relationsArray.getJSONObject(i);
                String sourceConcept = TextUtils.stem(relationObj.getString("sourceConcept").trim());
                String targetConcept = TextUtils.stem(relationObj.getString("targetConcept").trim());
                ConceptRelation.RelationType type = ConceptRelation.RelationType.valueOf(relationObj.getString("type").toUpperCase());
                double strength = relationObj.getDouble("strength");

                ConceptRelation relation = new ConceptRelation(sourceConcept, targetConcept, type, strength);
                qnaProcessor.addConceptualRelation(relation); // Add method handles duplicates
            }
        }
    }
}
