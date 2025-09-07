package C156.Bhushan.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    private static final String PREFS_NAME = "chat_prefs";
    private static final String KEY_CHAT_HISTORY = "chat_history";

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }


    // ✅ Save chat history to SharedPreferences
    private void saveChatHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();
        for (ChatMessage msg : chatMessages) {
            if (!msg.isLoading()) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("text", msg.getMessage());
                    obj.put("isUser", msg.isUser());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(obj);
            }
        }
        editor.putString(KEY_CHAT_HISTORY, jsonArray.toString());
        editor.apply();
    }

    // ✅ Load chat history from SharedPreferences
    private void loadChatHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_CHAT_HISTORY, null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String text = obj.getString("text");
                    boolean isUser = obj.getBoolean("isUser");
                    chatMessages.add(new ChatMessage(text, isUser));
                }
                chatAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(chatMessages.size() - 1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void clearChatHistory() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("Are you sure you want to clear the entire chat history?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // ✅ Clear list
                    chatMessages.clear();
                    chatAdapter.notifyDataSetChanged();

                    // ✅ Clear SharedPreferences
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().remove(KEY_CHAT_HISTORY).apply();

                    Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }


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

        // 🎤 Speech recognition setup
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> data = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (data != null && !data.isEmpty()) {
                            promptEditText.setText(data.get(0));
                            promptEditText.setSelection(promptEditText.getText().length());
                        }
                    }
                }
        );

        ImageButton micButton = findViewById(R.id.micButton);
        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        recyclerView = findViewById(R.id.recyclerView);
        ImageButton clearChatButton = findViewById(R.id.clearChat);


        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // ✅ Load saved chat history
        loadChatHistory();

        micButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...");

            try {
                speechRecognizerLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
            }
        });

        clearChatButton.setOnClickListener(v -> clearChatHistory());

        // ✅ Create GenerativeModel
        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            // Clear input + hide keyboard
            promptEditText.setText("");
            hideKeyboard();

            // ✅ Add user message
            chatMessages.add(new ChatMessage(prompt, true));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            recyclerView.scrollToPosition(chatMessages.size() - 1);
            saveChatHistory(); // save

            // ✅ Add loading bubble
            ChatMessage loadingMsg = new ChatMessage(true);
            chatMessages.add(loadingMsg);
            int loadingPos = chatMessages.size() - 1;
            chatAdapter.notifyItemInserted(loadingPos);
            recyclerView.scrollToPosition(loadingPos);

            // ✅ Build conversation context
            StringBuilder sb = new StringBuilder();
            for (ChatMessage cm : chatMessages) {
                if (cm.isUser()) {
                    sb.append("User: ").append(cm.getMessage()).append("\n");
                } else if (!cm.isLoading()) {
                    sb.append("Assistant: ").append(cm.getMessage()).append("\n");
                }
            }
            sb.append("Assistant:");
            String combinedPrompt = sb.toString();

            // ✅ Gemini API call
            generativeModel.generateContent(combinedPrompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    runOnUiThread(() -> {
                        if (loadingPos < chatMessages.size()) {
                            chatMessages.remove(loadingPos);
                            chatAdapter.notifyItemRemoved(loadingPos);
                        }

                        try {
                            if (o instanceof GenerateContentResponse) {
                                String responseString = ((GenerateContentResponse) o).getText();
                                chatMessages.add(new ChatMessage(responseString, false));
                            } else {
                                chatMessages.add(new ChatMessage("Error: Invalid response", false));
                            }
                        } catch (Exception e) {
                            chatMessages.add(new ChatMessage("Error: " + e.getMessage(), false));
                        }

                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        recyclerView.scrollToPosition(chatMessages.size() - 1);

                        // ✅ Save history after bot reply
                        saveChatHistory();
                    });
                }
            });
        });
    }
}
