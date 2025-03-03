package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;
    private ArrayList<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private ChatDatabase chatDatabase;
    private ImageButton micButton;
    private static final String KEY_PROMPT_TEXT = "prompt_text";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_APP_IN_BACKGROUND = "appInBackground";
    private static final int REQ_CODE_SPEECH_INPUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        chatDatabase = ChatDatabase.getInstance(this);
        if (savedInstanceState == null) {
            boolean wasInBackground = prefs.getBoolean(KEY_APP_IN_BACKGROUND, false);
            if (wasInBackground) {
                chatDatabase.chatMessageDao().deleteAll();
            }
            prefs.edit().putBoolean(KEY_APP_IN_BACKGROUND, false).apply();
        }
        promptEditText = findViewById(R.id.promptEditText);
        ImageButton sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        micButton = (ImageButton) findViewById(R.id.ttsButton);
        if (micButton == null) {
            micButton = (ImageButton) findViewById(R.id.ttsButton_bottom);
        }
        Switch modeToggle = (Switch) findViewById(R.id.modeToggle);
        modeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
        chatMessages = new ArrayList<>();
        List<ChatMessageEntity> savedEntities = chatDatabase.chatMessageDao().getAllMessages();
        for (ChatMessageEntity entity : savedEntities) {
            chatMessages.add(new ChatMessage(entity.message, entity.isUser));
        }
        if (savedInstanceState != null) {
            String savedPrompt = savedInstanceState.getString(KEY_PROMPT_TEXT, "");
            promptEditText.setText(savedPrompt);
        }
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        micButton.setOnClickListener(v -> startSpeechRecognition());
        sendButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }
            processPrompt(prompt);
        });
    }

    private void processPrompt(String prompt) {
        ChatMessage userMsg = new ChatMessage(prompt, true);
        chatMessages.add(userMsg);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        chatDatabase.chatMessageDao().insert(new ChatMessageEntity(prompt, true));
        promptEditText.setText("");
        progressBar.setVisibility(VISIBLE);
        final GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);
        generativeModel.generateContent(prompt, new Continuation<GenerateContentResponse>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }
            @Override
            public void resumeWith(@NonNull Object result) {
                final GenerateContentResponse response = (GenerateContentResponse) result;
                final String responseString = response.getText();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(GONE);
                        ChatMessage geminiMsg = new ChatMessage(responseString, false);
                        chatMessages.add(geminiMsg);
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                        chatDatabase.chatMessageDao().insert(new ChatMessageEntity(responseString, false));
                    }
                });
            }
        });
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                processPrompt(spokenText);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_APP_IN_BACKGROUND, false).apply();
        int textColor = getThemeColor(android.R.attr.textColorPrimary);
        int hintColor = getThemeColor(android.R.attr.textColorHint);
        promptEditText.setTextColor(textColor);
        promptEditText.setHintTextColor(hintColor);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_APP_IN_BACKGROUND, true).apply();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PROMPT_TEXT, promptEditText.getText().toString());
    }

    public int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static class ChatMessage implements Parcelable {
        private String message;
        private boolean isUser;
        public ChatMessage(String message, boolean isUser) {
            this.message = message;
            this.isUser = isUser;
        }
        protected ChatMessage(Parcel in) {
            message = in.readString();
            isUser = in.readByte() != 0;
        }
        public static final Creator<ChatMessage> CREATOR = new Creator<ChatMessage>() {
            @Override
            public ChatMessage createFromParcel(Parcel in) {
                return new ChatMessage(in);
            }
            @Override
            public ChatMessage[] newArray(int size) {
                return new ChatMessage[size];
            }
        };
        public String getMessage() {
            return message;
        }
        public boolean isUser() {
            return isUser;
        }
        @Override
        public int describeContents() {
            return 0;
        }
        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(message);
            parcel.writeByte((byte) (isUser ? 1 : 0));
        }
    }

    public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private ArrayList<ChatMessage> messages;
        public ChatAdapter(ArrayList<ChatMessage> messages) {
            this.messages = messages;
        }
        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
            return new ChatViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage chatMessage = messages.get(position);
            holder.bind(chatMessage);
        }
        @Override
        public int getItemCount() {
            return messages.size();
        }
        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageTextView;
            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                messageTextView = itemView.findViewById(R.id.messageTextView);
            }
            public void bind(ChatMessage chatMessage) {
                messageTextView.setText(chatMessage.getMessage());
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) messageTextView.getLayoutParams();
                if (chatMessage.isUser()) {
                    messageTextView.setBackgroundResource(R.drawable.user_message_bg);
                    params.gravity = Gravity.END;
                } else {
                    messageTextView.setBackgroundResource(R.drawable.gemini_message_bg);
                    params.gravity = Gravity.START;
                }
                messageTextView.setLayoutParams(params);
            }
        }
    }
}
