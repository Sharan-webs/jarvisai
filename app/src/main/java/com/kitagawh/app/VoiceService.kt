package com.kitagawh.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * Jarvis himself: a foreground service that keeps listening for the word
 * "Jarvis". When it hears it, the overlay panel pops up ("What's up bruh!!"),
 * the next thing you say becomes a command, Kitagawh does it, speaks the
 * reply, and the panel closes itself.
 */
class VoiceService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_SHOW_PANEL = "com.kitagawh.app.SHOW_JARVIS_PANEL"
        const val ACTION_STOP = "com.kitagawh.app.STOP_JARVIS"

        @Volatile
        var running = false
            private set

        @Volatile
        var instance: VoiceService? = null
            private set
    }

    private enum class Mode { WAKE, COMMAND }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var mode = Mode.WAKE
    private var listening = false
    private var speaking = false
    private var stopped = false

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayStatus: TextView? = null
    private var overlayWave: WaveformView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        running = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)
        createNotificationChannel()
        startForegroundCompat()
        mainHandler.post { startWakeListening() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW_PANEL -> {
                // Easy Touch "Call Jarvis" lands here.
                mainHandler.post { onWakeWordHeard() }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopped = true
        running = false
        if (instance === this) instance = null
        hideOverlay()
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (e: Exception) {
            // Recognizer already gone.
        }
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    // ------------------------------------------------------------ foreground

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    "kitagawh_voice",
                    "Kitagawh voice",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun startForegroundCompat() {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, "kitagawh_voice")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Kitagawh is on")
            .setContentText("Say the word Jarvis and I will come.")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(2, notification)
        }
    }

    // ------------------------------------------------------------------- tts

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            ttsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    speaking = true
                }

                override fun onDone(utteranceId: String?) {
                    speaking = false
                    mainHandler.post {
                        if (mode == Mode.COMMAND) {
                            // Command finished and reply spoken: close and go
                            // back to quietly waiting for the wake word.
                            hideOverlay()
                            mode = Mode.WAKE
                            startWakeListening()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    speaking = false
                    mainHandler.post {
                        if (mode == Mode.COMMAND) {
                            hideOverlay()
                            mode = Mode.WAKE
                            startWakeListening()
                        }
                    }
                }
            })
        }
    }

    private fun speak(text: String) {
        overlayStatus?.text = text
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kitagawh_reply")
            // Safety net: if TTS stalls, still close the panel and resume.
            mainHandler.postDelayed({
                if (mode == Mode.COMMAND && !speaking) {
                    hideOverlay()
                    mode = Mode.WAKE
                    startWakeListening()
                }
            }, 6000)
        } else {
            mainHandler.postDelayed({
                if (mode == Mode.COMMAND) {
                    hideOverlay()
                    mode = Mode.WAKE
                    startWakeListening()
                }
            }, 2500)
        }
    }

    // -------------------------------------------------------------- listening

    private fun recognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

    private fun ensureRecognizer(): SpeechRecognizer {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(listener)
        }
        return recognizer!!
    }

    private fun startWakeListening() {
        if (stopped || speaking) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        mode = Mode.WAKE
        try {
            ensureRecognizer().startListening(recognizerIntent())
            listening = true
        } catch (e: Exception) {
            scheduleRestart()
        }
    }

    private fun startCommandListening() {
        if (stopped) return
        try {
            ensureRecognizer().startListening(recognizerIntent())
            listening = true
        } catch (e: Exception) {
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        mainHandler.postDelayed({
            if (!stopped && !speaking) {
                if (mode == Mode.COMMAND) startCommandListening() else startWakeListening()
            }
        }, 700)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            listening = false
        }

        override fun onError(error: Int) {
            listening = false
            if (stopped || speaking) return
            // The wake loop must never die: restart after any error.
            scheduleRestart()
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val texts = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?: arrayListOf()
            handleHeard(texts)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (mode != Mode.WAKE) return
            val texts = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?: arrayListOf()
            if (texts.any { it.lowercase(Locale.ROOT).contains("jarvis") }) {
                try {
                    recognizer?.stopListening()
                } catch (e: Exception) {
                    // Ignore.
                }
                onWakeWordHeard()
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleHeard(texts: ArrayList<String>) {
        if (texts.isEmpty()) {
            scheduleRestart()
            return
        }
        when (mode) {
            Mode.WAKE -> {
                val heard = texts.joinToString(" ").lowercase(Locale.ROOT)
                if (heard.contains("jarvis")) {
                    val after = heard.substringAfter("jarvis").trim()
                    onWakeWordHeard()
                    if (after.isNotEmpty()) {
                        // "Jarvis, open YouTube" in one breath.
                        mainHandler.postDelayed({ runCommand(after) }, 400)
                    }
                } else {
                    startWakeListening()
                }
            }
            Mode.COMMAND -> {
                runCommand(texts.first())
            }
        }
    }

    // ------------------------------------------------------------ wake + panel

    private fun onWakeWordHeard() {
        if (stopped) return
        mode = Mode.COMMAND
        showOverlay()
        overlayStatus?.text = "Listening..."
        mainHandler.postDelayed({ startCommandListening() }, 350)
    }

    /** Called from Easy Touch when the user taps Call Jarvis. */
    fun callJarvis() {
        mainHandler.post { onWakeWordHeard() }
    }

    private fun runCommand(command: String) {
        overlayStatus?.text = "Working on it..."
        CommandExecutor.execute(applicationContext, command) { reply ->
            mainHandler.post { speak(reply) }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(this)
        ) {
            // No overlay permission: still listen and speak, just no panel.
            return
        }
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_jarvis, null)
        overlayStatus = view.findViewById(R.id.jarvisStatus)
        overlayWave = view.findViewById(R.id.jarvisWave)
        view.findViewById<View>(R.id.jarvisClose).setOnClickListener {
            hideOverlay()
            mode = Mode.WAKE
            try {
                recognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore.
            }
            startWakeListening()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM
        params.y = 48
        try {
            windowManager?.addView(view, params)
            overlayView = view
            overlayWave?.startAnimating()
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun hideOverlay() {
        val view = overlayView ?: return
        overlayWave?.stopAnimating()
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            // Already removed.
        }
        overlayView = null
        overlayStatus = null
        overlayWave = null
    }
}
