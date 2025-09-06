package com.fahim.geminiapistarter;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.EditText;

import java.util.ArrayList;

public class VoiceHelper {

    private final Activity activity;
    private final EditText targetEditText;
    private final SpeechRecognizer speechRecognizer;

    public VoiceHelper(Activity activity, EditText editText) {
        this.activity = activity;
        this.targetEditText = editText;
        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        initListener();
    }

    private void initListener() {
        speechRecognizer.setRecognitionListener(new SimpleRecognitionListener() {
            @Override
            public void onResults(android.os.Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && matches.size() > 0) {
                    targetEditText.setText(matches.get(0));
                }
            }
        });
    }

    public void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer.startListening(intent);
    }

    public void destroy() {
        speechRecognizer.destroy();
    }
}
