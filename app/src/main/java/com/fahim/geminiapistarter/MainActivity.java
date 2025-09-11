package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
    private ProgressBar progressBar;
    private LinearLayout messageContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        messageContainer = findViewById(R.id.messageContainer);

        // Add name and roll number at the top ONLY ONCE
        if (messageContainer.getChildCount() == 0) {
            TextView userInfoTextView = new TextView(this);
            userInfoTextView.setText("Name: Santoshi Quenim | Roll No: C065");
            userInfoTextView.setTextSize(16f);
            userInfoTextView.setPadding(8, 8, 8, 16);
            userInfoTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            userInfoTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            messageContainer.addView(userInfoTextView);
        }

        // Create GenerativeModel
        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash",
                BuildConfig.GEMINI_API_KEY);

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            // Add user message bubble
            addMessageToContainer(prompt, true);

            // Clear input
            promptEditText.setText("");

            // Show progress
            progressBar.setVisibility(VISIBLE);

            // Call AI
            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String responseString = response.getText();
                    assert responseString != null;
                    Log.d("Response", responseString);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);

                        // Add AI message bubble
                        addMessageToContainer(responseString, false);
                    });
                }
            });
        });
    }

    /**
     * Adds a message bubble to the chat container.
     *
     * @param message The text to display
     * @param isUser  True if message is from the user, false if from AI
     */
    private void addMessageToContainer(String message, boolean isUser) {
        TextView msgTextView = new TextView(this);
        msgTextView.setText(message);
        msgTextView.setTextSize(16f);
        msgTextView.setPadding(16, 12, 16, 12);
        msgTextView.setBackgroundResource(isUser ? R.drawable.user_bubble : R.drawable.ai_bubble);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        params.gravity = isUser ? Gravity.END : Gravity.START;
        msgTextView.setLayoutParams(params);

        messageContainer.addView(msgTextView);

        // Scroll to bottom
        messageContainer.post(() -> {
            ((ScrollView) messageContainer.getParent()).fullScroll(ScrollView.FOCUS_DOWN);
        });
    }
}
