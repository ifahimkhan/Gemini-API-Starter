package com.techsetu.geminichat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.ImageButton
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.techsetu.geminichat.data.Message
import com.techsetu.geminichat.ui.ChatAdapter
import com.techsetu.geminichat.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var input: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton

    private val REQ_SPEECH = 200
    private val REQ_AUDIO_PERMISSION = 300

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        input = findViewById(R.id.input)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)

        val adapter = ChatAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = adapter

        viewModel.messages.observe(this) {
            adapter.submitList(it)
            recyclerView.scrollToPosition(it.size - 1)
        }

        btnSend.setOnClickListener {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                viewModel.sendText(text)
                input.setText("")
            }
        }

        btnMic.setOnClickListener {
            checkAudioPermissionAndStart()
        }
    }

    private fun checkAudioPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO_PERMISSION)
                return
            }
        }
        startSpeechToText()
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your query")
        }
        startActivityForResult(intent, REQ_SPEECH)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_AUDIO_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechToText()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SPEECH && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = results?.firstOrNull()
            if (!text.isNullOrBlank()) {
                viewModel.sendText(text)
            }
        }
    }
}
