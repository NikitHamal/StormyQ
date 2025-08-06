package com.stormy.ai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.SpannableString;
import android.text.Editable; // Added import for TextWatcher
import android.text.TextWatcher; // Added import for TextWatcher
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.stormy.ai.models.AnswerResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText contextEditText;
    private EditText questionEditText;
    private TextView resultTextView;
    private TextView confidenceTextView;
    private Button askButton;
    private Button trainButton;
    private Button loadDocumentButton;

    private QnAProcessor qnaProcessor;
    private TextHighlighter textHighlighter;

    private String currentFullContext = ""; // Stores the full context, whether typed or loaded
    private static final int PICK_DOCUMENT_REQUEST_CODE = 2;
    private static final int MAX_DISPLAY_CONTEXT_LENGTH = 500; // Max chars to show in contextEditText

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize views
        contextEditText = findViewById(R.id.contextEditText);
        questionEditText = findViewById(R.id.questionEditText);
        resultTextView = findViewById(R.id.resultTextView);
        confidenceTextView = findViewById(R.id.confidenceTextView);
        askButton = findViewById(R.id.askButton);
        trainButton = findViewById(R.id.trainButton);
        loadDocumentButton = findViewById(R.id.loadDocumentButton);

        // Initialize processors (get the singleton instance)
        qnaProcessor = QnAProcessor.getInstance();
        textHighlighter = new TextHighlighter();

        // Set up listeners
        askButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processQuestion();
            }
        });

        trainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TrainActivity.class);
                startActivity(intent);
            }
        });

        loadDocumentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDocumentPicker();
            }
        });

        // Ensure contextEditText is editable
        contextEditText.setFocusableInTouchMode(true);
        contextEditText.setClickable(true);

        // Add a TextWatcher to update currentFullContext when user types or pastes
        contextEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this functionality
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update currentFullContext as the user types/pastes
                currentFullContext = s.toString();
                // The QnAProcessor will handle network rebuild checks internally when findAnswer is called.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed for this functionality
            }
        });


        // Initially hide the confidence score TextView
        confidenceTextView.setVisibility(View.GONE);
        // Set initial hint for context
        contextEditText.setHint(getString(R.string.context_hint));
    }

    /**
     * Opens a system file picker to select text-based documents.
     */
    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain"); // Prefer plain text files
        String[] mimeTypes = {"text/plain", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(intent, PICK_DOCUMENT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DOCUMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        String mimeType = getContentResolver().getType(uri);
                        if (mimeType != null && mimeType.startsWith("text/plain")) {
                            currentFullContext = readTextFromUri(uri);
                            // IMPORTANT: Reset QnAProcessor state to force network rebuild for new context
                            qnaProcessor.resetProcessorState();
                            
                            // Removed "Document Loaded (Preview):" prefix
                            if (currentFullContext.length() > MAX_DISPLAY_CONTEXT_LENGTH) {
                                contextEditText.setText(currentFullContext.substring(0, MAX_DISPLAY_CONTEXT_LENGTH) + "...");
                            } else {
                                contextEditText.setText(currentFullContext);
                            }
                            contextEditText.setHint(""); // Clear hint once content is loaded
                            Toast.makeText(this, "Document loaded successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Unsupported file type. Only plain text (.txt) is fully supported without external libraries.", Toast.LENGTH_LONG).show();
                            currentFullContext = ""; // Clear context if unsupported
                            contextEditText.setText("");
                            contextEditText.setHint(getString(R.string.context_hint));
                            // Also reset processor state if context is cleared due to unsupported type
                            qnaProcessor.resetProcessorState();
                        }
                    } catch (IOException e) {
                        Toast.makeText(this, "Error reading document: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        currentFullContext = "";
                        contextEditText.setText("");
                        contextEditText.setHint(getString(R.string.context_hint));
                        // Also reset processor state if context is cleared due to error
                        qnaProcessor.resetProcessorState();
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Reads text content from a given URI.
     * @param uri The URI of the text file.
     * @return The content of the file as a String.
     * @throws IOException If an error occurs during file reading.
     */
    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // Append newline for proper text reconstruction
            }
        }
        return stringBuilder.toString();
    }


    /**
     * Processes the user's question based on the provided context.
     * Validates input, finds the answer using QnAProcessor, and displays
     * the result along with a confidence score and reasoning summary.
     */
    private void processQuestion() {
        // Ensure currentFullContext is up-to-date with whatever is in the EditText
        // The TextWatcher handles this, so currentFullContext should always reflect
        // the EditText's content.

        String context = currentFullContext.trim(); // Use the stored full context
        String question = questionEditText.getText().toString().trim();

        // Validate input
        if (context.isEmpty()) {
            resultTextView.setText(R.string.empty_context_warning);
            confidenceTextView.setVisibility(View.GONE);
            return;
        }

        if (question.isEmpty()) {
            resultTextView.setText(R.string.empty_question_warning);
            confidenceTextView.setVisibility(View.GONE);
            return;
        }

        // Process question and find answer using the QnAProcessor
        // The QnAProcessor itself checks if the context has changed before rebuilding its network.
        AnswerResult answerResult = qnaProcessor.findAnswer(context, question);

        // Display results based on whether a valid answer was found
        if (answerResult.isValid()) {
            // Get colors for highlighting from resources
            int highlightColor = ContextCompat.getColor(this, R.color.highlight_color);
            int textColor = ContextCompat.getColor(this, R.color.text_color);

            // Highlight the answer within a snippet of the context
            SpannableString highlightedSnippet = textHighlighter.highlightAnswerSnippet(
                context, // Pass the full context to the snippet highlighter
                answerResult,
                highlightColor,
                textColor
            );

            resultTextView.setText(highlightedSnippet);

            // Display the confidence score
            confidenceTextView.setText(String.format(Locale.getDefault(), "Confidence: %.2f%%", answerResult.getConfidence() * 100));
            confidenceTextView.setVisibility(View.VISIBLE);
        } else {
            // If no valid answer is found, display the "no answer" message
            resultTextView.setText(R.string.no_answer_found);
            confidenceTextView.setVisibility(View.GONE);
        }
    }
}
