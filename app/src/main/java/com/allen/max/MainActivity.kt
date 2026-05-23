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
    private var autoRestart = false

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
        btnLang = findViewById(R.id.btnLang)

        tts = TextToSpeech(this, this)
        val apiKey = try { assets.open("gemini_key.txt").bufferedReader().use { it.readText().trim() } } catch (e: Exception) { "" }
        commandProcessor = CommandProcessor(this, apiKey)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()

        if (!hasRequiredPermissions()) {
            requestPermissions()
        }

        btnMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                if (!isListening) {
                    autoRestart = true
                    startListening()
                } else {
                    autoRestart = false
                    stopListening()
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_CODE
                )
            }
        }

        btnLang.setOnClickListener {
            isUrduMode = !isUrduMode
            btnLang.text = if (isUrduMode) "UR" else "EN"
            tvStatus.text = if (isUrduMode) "اردو موڈ فعال" else "English mode active"
        }

        startService(Intent(this, MAXService::class.java))

        if (intent.getBooleanExtra("from_hotword", false)) {
            autoRestart = true
            Handler(Looper.getMainLooper()).postDelayed({
                if (hasRequiredPermissions()) startListening()
            }, 500)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (autoRestart) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (autoRestart) startListening()
                        }, 500)
                    }
                }
                override fun onError(utteranceId: String?) {}
            })
            speak("Hello Allen! MAX is ready. How can I help you?")
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                isListening = false
                updateUI(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    tvCommand.text = "You said: $command"
                    processCommand(command)
                } else {
                    if (autoRestart) {
                        Handler(Looper.getMainLooper()).postDelayed({
                           if (autoRestart) startListening()
                        }, 1000)
                    }
                }
            }

            override fun onError(error: Int) {
                isListening = false
                updateUI(false)
                if (autoRestart) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (autoRestart) startListening()
                    }, 1500)
                }
            }

            override fun onReadyForSpeech(params: Bundle?) { tvStatus.text = "Listening..." }
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
        if (response != "PROCESS_ASYNC") {
            tvResponse.text = "MAX: $response"
            speak(response)
        }
    }

    fun updateResponse(text: String) {
        tvResponse.text = "MAX: $text"
    }

    fun speak(text: String) {
        val isUrdu = text.any { it in '\u0600'..'\u06FF' }
        if (isUrdu) {
            tts.language = Locale("ur", "PK")
        } else {
            tts.language = Locale.US
        }
        
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MAX_UTTERANCE")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "MAX_UTTERANCE")
    }

    private fun startListening() {
        if (!hasRequiredPermissions() || isListening) return
        isListening = true
        updateUI(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            
            val language = if (isUrduMode) "ur-PK" else "en-US"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
        autoRestart = false
        updateUI(false)
    }

    private fun updateUI(listening: Boolean) {
        tvStatus.text = if (listening) "Listening..." else "Tap mic to speak"
        btnMic.isSelected = listening
        if (listening) startPulsingAnimation() else stopPulsingAnimation()
    }

    private var pulseAnimator: android.animation.ObjectAnimator? = null

    private fun startPulsingAnimation() {
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
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CAMERA
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvStatus.text = "Tap mic to speak"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoRestart = false
        tts.shutdown()
        speechRecognizer.destroy()
    }
}
