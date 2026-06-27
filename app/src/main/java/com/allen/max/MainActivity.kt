package com.allen.max

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var btnMic: ImageButton
    private lateinit var btnLang: TextView
    private var isUrduMode = false
    private var isListening = false
    private var conversationMode = false
    private var isSpeaking = false

    companion object {
        const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15: enable edge-to-edge rendering
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        // Apply insets so content isn't hidden by system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)
        tvCommand = findViewById(R.id.tvCommand)
        tvResponse = findViewById(R.id.tvResponse)
        btnMic = findViewById(R.id.btnMic)
        btnLang = findViewById(R.id.btnLang)

        tts = TextToSpeech(this, this)
        val apiKey = com.allen.max.utils.AIHelper.GEMINI_KEY
        commandProcessor = CommandProcessor(this, apiKey)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()

        if (!hasRequiredPermissions()) requestPermissions()

        btnMic.setOnClickListener {
            if (!hasRequiredPermissions()) {
                requestPermissions()
                return@setOnClickListener
            }
            if (conversationMode) {
                stopConversationMode()
            } else {
                if (!isListening) startListening()
                else stopListening()
            }
        }

        btnMic.setOnLongClickListener {
            if (!hasRequiredPermissions()) return@setOnLongClickListener true
            if (!conversationMode) startConversationMode()
            else stopConversationMode()
            true
        }

        btnLang.setOnClickListener {
            isUrduMode = !isUrduMode
            btnLang.text = if (isUrduMode) "UR" else "EN"
            tvStatus.text = if (isUrduMode) "اردو موڈ فعال" else "English mode active"
        }

        startService(Intent(this, MAXService::class.java))
    }

    private fun startConversationMode() {
        conversationMode = true
        tvStatus.text = "Conversation ON — tap to stop"
        startPulsingAnimation()
        speak("Conversation mode on. I am listening!") {
            startListening()
        }
    }

    private fun stopConversationMode() {
        conversationMode = false
        isListening = false
        speechRecognizer.stopListening()
        stopPulsingAnimation()
        tvStatus.text = "Tap mic to speak, long press for conversation"
        speak("Conversation mode off.") {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            speak("Hello Allen! MAX is ready.") {}
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    tvCommand.text = "You: $command"
                    processCommand(command)
                } else {
                    if (conversationMode) {
                        Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 500)
                    } else {
                        updateUI(false)
                    }
                }
            }

            override fun onError(error: Int) {
                isListening = false
                if (conversationMode) {
                    Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 1000)
                } else {
                    updateUI(false)
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = if (conversationMode) "Listening..." else "Listening..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { tvStatus.text = "Processing..." }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processCommand(command: String) {
        val response = commandProcessor.process(command.lowercase())
        if (response != "PROCESS_ASYNC") {
            tvResponse.text = "MAX: $response"
            speak(response) {
                if (conversationMode) {
                    Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 300)
                }
            }
        }
    }

    fun updateResponse(text: String) {
        runOnUiThread {
            tvResponse.text = "MAX: $text"
            speak(text) {
                if (conversationMode) {
                    Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 300)
                }
            }
        }
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        val isUrdu = text.any { it in '\u0600'..'\u06FF' }
        tts.language = if (isUrdu) Locale("ur", "PK") else Locale.US

        val utteranceId = "MAX_${System.currentTimeMillis()}"
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(uid: String?) { isSpeaking = true }
            override fun onDone(uid: String?) {
                isSpeaking = false
                Handler(Looper.getMainLooper()).post { onDone() }
            }
            override fun onError(uid: String?) {
                isSpeaking = false
                Handler(Looper.getMainLooper()).post { onDone() }
            }
        })

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun startListening() {
        if (!hasRequiredPermissions() || isListening || isSpeaking) return
        isListening = true
        updateUI(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val language = if (isUrduMode) "ur-PK" else "en-US"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
        updateUI(false)
    }

    private fun updateUI(listening: Boolean) {
        if (conversationMode) return
        tvStatus.text = if (listening) "Listening..." else "Tap mic to speak"
        btnMic.isSelected = listening
        if (listening) startPulsingAnimation() else stopPulsingAnimation()
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CALL_LOG
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private var pulseAnimator: android.animation.ObjectAnimator? = null

    private fun startPulsingAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            btnMic,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.2f)
        ).apply {
            duration = 800
            repeatCount = android.animation.ObjectAnimator.INFINITE
            repeatMode = android.animation.ObjectAnimator.REVERSE
            start()
        }
    }

    private fun stopPulsingAnimation() {
        pulseAnimator?.cancel()
        btnMic.scaleX = 1.0f
        btnMic.scaleY = 1.0f
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CALL_LOG
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            tvStatus.text = "Tap mic to speak"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        conversationMode = false
        tts.shutdown()
        speechRecognizer.destroy()
    }
}
