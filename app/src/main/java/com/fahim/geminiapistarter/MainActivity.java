package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.Candidate;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rv;
    private MessagesAdapter adapter;
    private TextInputEditText promptEditText;
    private ProgressBar progressBar;

    private GenerativeModelFutures geminiModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar + theme toggle
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_theme) {
                int current = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                boolean dark = (current == android.content.res.Configuration.UI_MODE_NIGHT_YES);
                AppCompatDelegate.setDefaultNightMode(
                        dark ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES
                ); // Programmatic toggle per docs [10]
                return true;
            }
            return false;
        });

        // RecyclerView setup
        rv = findViewById(R.id.messagesRecyclerView);
        adapter = new MessagesAdapter();
        rv.setAdapter(adapter);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm); // Standard list setup [3]

        promptEditText = findViewById(R.id.promptEditText);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Gemini Java futures client
        GenerativeModel ai = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        geminiModel = GenerativeModelFutures.from(ai); // Java-friendly wrapper [11]

        findViewById(R.id.sendButton).setOnClickListener(v -> onSendClicked());
        findViewById(R.id.micButton).setOnClickListener(v -> {
            // TODO: Speech-to-text integration
        });
    }

    private void onSendClicked() {
        String text = String.valueOf(promptEditText.getText()).trim();
        if (TextUtils.isEmpty(text)) return;

        // Add user message
        adapter.addMessage(new Message(System.currentTimeMillis(), text, true));
        promptEditText.setText("");
        rv.scrollToPosition(adapter.getItemCount() - 1);

        // Show progress
        progressBar.setVisibility(VISIBLE);

        // Build prompt and call Gemini
        Content prompt = new Content.Builder().addText(text).build();
        ListenableFuture<GenerateContentResponse> future = geminiModel.generateContent(prompt); // [11]

        Log.i("Gemini", "Sending prompt: " + text);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse result) {
                String reply = extractText(result);
                Log.i("Gemini", "Raw reply: " + reply);
                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);
                    String safe = (reply == null || reply.trim().isEmpty())
                            ? "No response text." : reply;
                    adapter.addMessage(new Message(System.currentTimeMillis(), safe, false));
                    rv.scrollToPosition(adapter.getItemCount() - 1);
                });
            }
            @Override public void onFailure(@NonNull Throwable t) {
                Log.e("Gemini", "API error", t);
                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);
                    adapter.addMessage(new Message(System.currentTimeMillis(),
                            "Error: " + t.getMessage(), false));
                    rv.scrollToPosition(adapter.getItemCount() - 1);
                });
            }
        }, getMainExecutor());
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null) return null;
        try {
            String t = response.getText();
            if (t != null && !t.trim().isEmpty()) return t;
            if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                Candidate c = response.getCandidates().get(0);
                if (c.getContent() != null && c.getContent().getParts() != null) {
                    for (Part p : c.getContent().getParts()) {
                        if (p instanceof TextPart) {
                            return ((TextPart) p).getText();
                        }
                    }
                }
            }
        } catch (Exception ignore) { }
        return null;
    }
}
