package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;
    private VoiceHelper voiceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Request microphone permission if not granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, 1001);
        }
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

        RecyclerView chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        GenerativeModel generativeModel = new GenerativeModel("gemini-1.5-flash", BuildConfig.API_KEY);

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);

            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            chatAdapter.addMessage(new ChatMessage(prompt, true));
            chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
            progressBar.setVisibility(VISIBLE);

            generativeModel.generateContent(prompt, new kotlin.coroutines.Continuation<GenerateContentResponse>() {
                @NonNull
                @Override
                public kotlin.coroutines.CoroutineContext getContext() {
                    return kotlin.coroutines.EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String text = response.getText();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        chatAdapter.addMessage(new ChatMessage(text, false));
                        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                    });
                }
            });
        });

        ImageButton darkModeButton = findViewById(R.id.darkModeToggleButton);
        darkModeButton.setOnClickListener(v -> ThemeToggleHelper.toggleDarkMode(this));

        voiceHelper = new VoiceHelper(this, promptEditText);
        ImageButton voiceButton = findViewById(R.id.voiceButton);
        voiceButton.setOnClickListener(v -> voiceHelper.startListening());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceHelper != null)
            voiceHelper.destroy();
    }
}
