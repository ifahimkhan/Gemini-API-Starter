package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {
    private EditText promptEditText;
    private ImageButton sendButton, micButton;
    private ProgressBar progressBar;
    private RecyclerView chatRecycler;
    private MessageAdapter adapter;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    // Using callback API (no Guava/Futures needed)
    private GenerativeModel model;
    private SharedPreferences prefs;
    private AppDatabase db;
    private ActivityResultLauncher<String> micPermLauncher;
    private ActivityResultLauncher<Intent> voiceLauncher;
    private static final String PREFS = "settings";
    private static final String KEY_THEME = "themeMode"; // -1=follow, 1=night, 0=day
    private static final String KEY_LAST_PROMPT = "lastPrompt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Toolbar to show overflow menu
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // prefs & theme
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applySavedTheme();
        // DB
        db = AppDatabase.get(this);
        // views
        promptEditText = findViewById(R.id.promptEditText);
        sendButton = findViewById(R.id.sendButton);
        micButton = findViewById(R.id.micButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecycler = findViewById(R.id.chatRecycler);
        adapter = new MessageAdapter();
        chatRecycler.setAdapter(adapter);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        chatRecycler.setLayoutManager(lm);
        // preload last 30 interactions
        io.execute(() -> {
            List<Interaction> items = db.interactionDao().latest(30);
            for (int i = items.size() - 1; i >= 0; i--) {
                Interaction it = items.get(i);
                runOnUiThread(() -> {
                    adapter.submit(new Message(it.prompt, Message.Sender.USER, it.timestamp));
                    adapter.submit(new Message(it.response, Message.Sender.BOT, it.timestamp + 1));
                    scrollToBottom();
                });
            }
        });
        // restore last prompt
        String last = prefs.getString(KEY_LAST_PROMPT, "");
        if (!last.isEmpty()) {
            promptEditText.setText(last);
            promptEditText.setSelection(last.length());
        }
        // Model (no Futures)
        model = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);
        // mic permission
        micPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { if (granted) startVoiceInput(); }
        );
        // voice result
        voiceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getData() != null && res.getResultCode() == RESULT_OK) {
                        ArrayList<String> results =
                                res.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            promptEditText.setText(results.get(0));
                            promptEditText.setSelection(promptEditText.getText().length());
                        }
                    }
                });
        sendButton.setOnClickListener(v -> sendPrompt());
        micButton.setOnClickListener(v -> onMicClicked());
    }

    private void onMicClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startVoiceInput();
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt…");
        voiceLauncher.launch(intent);
    }

    private void sendPrompt() {
        String prompt = promptEditText.getText().toString().trim();
        promptEditText.setError(null);
        if (prompt.isEmpty()) {
            promptEditText.setError(getString(R.string.field_cannot_be_empty));
            adapter.submit(new Message(getString(R.string.aistring),
                    Message.Sender.BOT, System.currentTimeMillis()));
            scrollToBottom();
            return;
        }
        // remember last prompt
        prefs.edit().putString(KEY_LAST_PROMPT, prompt).apply();
        // Immediate UI feedback
        long now = System.currentTimeMillis();
        adapter.submit(new Message(prompt, Message.Sender.USER, now));
        adapter.submit(new Message("…", Message.Sender.TYPING, now + 1));
        scrollToBottom();
        disableSend(true);
        progressBar.setVisibility(VISIBLE);
        // Build content and call async API (Continuation)
        Content content = new Content.Builder().addText(prompt).build();
        model.generateContent(content, new Continuation<GenerateContentResponse>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                String replyText;
                try {
                    // Library gives the response directly here
                    GenerateContentResponse resp = (GenerateContentResponse) o;
                    replyText = (resp != null && resp.getText() != null)
                            ? resp.getText()
                            : "Sorry, I couldn’t generate a response right now.";
                } catch (Throwable t) {
                    Log.e("Gemini", "generate error", t);
                    replyText = "Network or API error. Please try again.";
                }
                final String finalReplyText = replyText;
                // UI updates on main
                runOnUiThread(() -> {
                    progressBar.setVisibility(GONE);
                    adapter.replaceTypingWith(new Message(finalReplyText, Message.Sender.BOT,
                            System.currentTimeMillis()));
                    scrollToBottom();
                    disableSend(false);
                });
                // persist in Room on background
                io.execute(() -> {
                    Interaction i = new Interaction();
                    i.timestamp = now;
                    i.prompt = prompt;
                    i.response = finalReplyText;
                    db.interactionDao().insert(i);
                });
            }
        });
    }

    private void disableSend(boolean disable) {
        sendButton.setEnabled(!disable);
        micButton.setEnabled(!disable);
    }

    private void scrollToBottom() {
        chatRecycler.post(() ->
                chatRecycler.smoothScrollToPosition(Math.max(0, adapter.getItemCount() - 1)));
    }

    private void applySavedTheme() {
        int mode = prefs.getInt(KEY_THEME, -1);
        if (mode == -1) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        else if (mode == 1) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_theme) {
            int current = prefs.getInt(KEY_THEME, -1);
            int next = (current == 1) ? 0 : (current == 0 ? -1 : 1);
            prefs.edit().putInt(KEY_THEME, next).apply();
            applySavedTheme();
            recreate();
            return true;
        } else if (item.getItemId() == R.id.action_clear_history) {
            io.execute(() -> {
                db.interactionDao().clear();
                runOnUiThread(() -> chatRecycler.setAdapter(new MessageAdapter()));
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}