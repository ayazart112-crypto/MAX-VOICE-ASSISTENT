package com.allen.max

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.allen.max.commands.CommandProcessor
import com.allen.max.services.MAXService
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var tvStatus: TextView
    private lateinit var tvCommand: TextView
    private lateinit var tvResponse: TextView
    private lateinit var btnMic: View
    private var isListening = false

    companion object {
        const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvCommand = findViewById(R.id.tvCommand)
        tvResponse = findViewById(R.id.tvResponse)
        btnMic = findViewById(R.id.btnMic)

        tts = TextToSpeech(this, this)
        commandProcessor = CommandProcessor(this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()
        requestPermissions()

        btnMic.setOnClickListener {
            if (!isListening) startListening() else stopListening()
        }

        // Start background service
        startService(Intent(this, MAXService::class.java))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            speak("Hello Allen! MAX is ready. How can I help you?")
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    tvCommand.text = "You said: $command"
                    processCommand(command)
                }
                isListening = false
                updateUI(false)
            }

            override fun onError(error: Int) {
                isListening = false
                updateUI(false)
                speak("Sorry, I didn't catch that. Please try again.")
            }

            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "Listening..."
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processCommand(command: String) {
        val response = commandProcessor.process(command.lowercase())
        tvResponse.text = "MAX: $response"
        speak(response)
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            isListening = true
            updateUI(true)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer.startListening(intent)
        }
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
        updateUI(false)
    }

    private fun updateUI(listening: Boolean) {
        tvStatus.text = if (listening) "Listening..." else "Tap mic to speak"
        btnMic.isSelected = listening
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.FLASHLIGHT,
            Manifest.permission.SET_ALARM,
            Manifest.permission.CHANGE_AUDIO_SETTINGS,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}
