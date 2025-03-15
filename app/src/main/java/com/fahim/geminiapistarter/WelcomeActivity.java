package com.fahim.geminiapistarter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Find the button and set click listener
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            // Navigate to MainActivity using Intent
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Optional: Closes WelcomeActivity
        });
    }
}
