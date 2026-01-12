package com.stormy.ai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.SpannableString;
import android.text.Editable; 
import android.text.TextWatcher; 
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.stormy.ai.models.AnswerResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText contextEditText;
    private EditText questionEditText;
    private TextView resultTextView;
    private TextView confidenceTextView;
    private Button askButton;
    private Button trainButton;
    private Button loadDocumentButton;
    private ProgressBar progressBar;

    private QnAProcessor qnaProcessor;
    private TextHighlighter textHighlighter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String currentFullContext = ""; 
    private static final int PICK_DOCUMENT_REQUEST_CODE = 2;
    private static final int MAX_DISPLAY_CONTEXT_LENGTH = 500; 

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
        progressBar = findViewById(R.id.progressBar);

        // Initialize processors
        qnaProcessor = QnAProcessor.getInstance();
        qnaProcessor.initialize(this);
        textHighlighter = new TextHighlighter();

        // Set up listeners
        askButton.setOnClickListener(v -> processQuestion());
        trainButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TrainActivity.class);
            startActivity(intent);
        });
        loadDocumentButton.setOnClickListener(v -> openDocumentPicker());

        contextEditText.setFocusableInTouchMode(true);
        contextEditText.setClickable(true);

        contextEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentFullContext = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        confidenceTextView.setVisibility(View.GONE);
        contextEditText.setHint(getString(R.string.context_hint));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    /**
     * Opens a system file picker to select text-based documents.
     */
    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain"); 
        String[] mimeTypes = {"text/plain", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, PICK_DOCUMENT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DOCUMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType != null && mimeType.startsWith("text/plain")) {
                        currentFullContext = readTextFromUri(uri);
                        qnaProcessor.resetProcessorState();
                        
                        if (currentFullContext.length() > MAX_DISPLAY_CONTEXT_LENGTH) {
                            contextEditText.setText(String.format("%s...", currentFullContext.substring(0, MAX_DISPLAY_CONTEXT_LENGTH)));
                        } else {
                            contextEditText.setText(currentFullContext);
                        }
                        contextEditText.setHint(""); 
                        Toast.makeText(this, "Document loaded successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Unsupported file type. Only plain text (.txt) is supported.", Toast.LENGTH_LONG).show();
                        resetContext();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Error reading document: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetContext();
                }
            }
        }
    }

    private void resetContext() {
        currentFullContext = "";
        contextEditText.setText("");
        contextEditText.setHint(getString(R.string.context_hint));
        qnaProcessor.resetProcessorState();
    }

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private void processQuestion() {
        String context = currentFullContext.trim();
        String question = questionEditText.getText().toString().trim();

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

        setLoadingState(true);

        executorService.execute(() -> {
            final AnswerResult answerResult = qnaProcessor.findAnswer(context, question);
            
            mainHandler.post(() -> {
                setLoadingState(false);
                displayResult(answerResult, context);
            });
        });
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        askButton.setEnabled(!isLoading);
        questionEditText.setEnabled(!isLoading);
        contextEditText.setEnabled(!isLoading);
        loadDocumentButton.setEnabled(!isLoading);
    }

    private void displayResult(AnswerResult answerResult, String context) {
        if (answerResult.isValid()) {
            int highlightColor = ContextCompat.getColor(this, R.color.highlight_color);
            int textColor = ContextCompat.getColor(this, R.color.text_color);

            SpannableString highlightedSnippet = textHighlighter.highlightAnswerSnippet(
                context,
                answerResult,
                highlightColor,
                textColor
            );

            resultTextView.setText(highlightedSnippet);
            confidenceTextView.setText(String.format(Locale.getDefault(), "Confidence: %.2f%%", answerResult.getConfidence() * 100));
            confidenceTextView.setVisibility(View.VISIBLE);
        } else {
            resultTextView.setText(R.string.no_answer_found);
            confidenceTextView.setVisibility(View.GONE);
        }
    }
}

}
