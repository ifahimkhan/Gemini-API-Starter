package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private TextView responseTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Handle system window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        responseTextView = findViewById(R.id.displayTextView);
        progressBar = findViewById(R.id.progressBar);

        // Create GenerativeModel with API key
        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash",
                BuildConfig.GEMINI_API_KEY);

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);

            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                String string = getString(R.string.aistring);
                responseTextView.setText(TextFormatter.getBoldSpannableText(string));
                return;
            }

            progressBar.setVisibility(VISIBLE);

            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    runOnUiThread(() -> progressBar.setVisibility(GONE));

                    if (o instanceof Throwable) {
                        Throwable error = (Throwable) o;
                        Log.e("Error", "API request failed", error);
                        runOnUiThread(() -> {
                            responseTextView.setText("Request failed: " + error.getMessage());
                        });
                    } else {
                        try {
                            GenerateContentResponse response = (GenerateContentResponse) o;
                            String responseString = response.getText();
                            assert responseString != null;
                            Log.d("Response", responseString);
                            runOnUiThread(() -> {
                                responseTextView.setText(TextFormatter.getBoldSpannableText(responseString));
                            });
                        } catch (Exception e) {
                            Log.e("Error", "Exception occurred", e);
                            runOnUiThread(() -> {
                                responseTextView.setText("Error processing response: " + e.getMessage());
                            });
                        }
                    }
                }
            });
        });
    }
}