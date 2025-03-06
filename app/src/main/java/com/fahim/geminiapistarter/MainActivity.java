package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.util.Pair;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.Chat;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_VOICE_INPUT = 1;
    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView messages;

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
        ImageButton voiceButton=findViewById(R.id.micButton);
        progressBar = findViewById(R.id.progressBar);
        messages = findViewById(R.id.messages);



        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);
        Chat chat =generativeModel.startChat(List.of());
        List <Pair<String,String>> messageHistory=new ArrayList<>();
        MessageAdapter messageAdapter = new MessageAdapter(messageHistory);
        messages.setAdapter(messageAdapter);
        messages.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }
            chat.sendMessage(prompt, new Continuation<GenerateContentResponse>() {
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
                        messageHistory.add(new Pair<>(responseString,"model"));
                        messageAdapter.notifyItemInserted(messageHistory.size() - 1);
                        progressBar.setVisibility(GONE);
                    });
                }
            });

            messageHistory.add(new Pair<>(prompt,"user"));
            messageAdapter.notifyItemInserted(messageHistory.size() - 1);
            promptEditText.setText("");
            progressBar.setVisibility(VISIBLE);
        });
        voiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt...");
            startActivityForResult(intent, REQUEST_VOICE_INPUT);
        });
        String[]audioPermission={Manifest.permission.RECORD_AUDIO};
        requestPermissions(audioPermission,2);


        ToggleButton themToggle=findViewById(R.id.themToggle);
        themToggle.setOnCheckedChangeListener(((buttonView,isChecked)->
        {
            findViewById(R.id.Container).setBackgroundColor(isChecked?Color.BLACK: Color.WHITE);
        }));

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VOICE_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                promptEditText.setText(results.get(0));
            }
        }
    }
}