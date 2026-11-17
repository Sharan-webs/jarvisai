package com.kitagawh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

/**
 * The power slide: one big button. Power On runs a powering-on animation,
 * starts the voice service, and the button flips to Power Off.
 */
class PowerActivity : BaseActivity() {

    private lateinit var powerButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var powering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_power)
        setTitle("Power")

        powerButton = findViewById(R.id.powerButton)
        statusText = findViewById(R.id.powerStatusText)
        progress = findViewById(R.id.powerProgress)

        powerButton.setOnClickListener {
            if (powering) return@setOnClickListener
            val on = Prefs.getBool(this, Prefs.KEY_JARVIS_ON) && VoiceService.running
            if (on) powerOff() else powerOn()
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (!powering) refresh()
    }

    private fun refresh() {
        val on = Prefs.getBool(this, Prefs.KEY_JARVIS_ON) && VoiceService.running
        powerButton.text = if (on) "Power Off Jarvis" else "Power On Jarvis"
        statusText.text = if (on) {
            "Jarvis is RUNNING. Say the word Jarvis anywhere."
        } else {
            "Jarvis is off."
        }
    }

    private fun powerOn() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), 201
            )
            Toast.makeText(
                this,
                "Jarvis needs the microphone permission. Allow it, then press Power On again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        powering = true
        powerButton.isEnabled = false
        statusText.text = "Powering on..."
        progress.visibility = View.VISIBLE
        progress.progress = 0

        val steps = listOf(
            "Waking the brain...",
            "Opening the ears...",
            "Charging the voice...",
            "Jarvis is RUNNING. Say the word Jarvis anywhere."
        )
        for (i in steps.indices) {
            handler.postDelayed({
                progress.progress = (i + 1) * 100 / steps.size
                statusText.text = steps[i]
                if (i == steps.lastIndex) {
                    ContextCompat.startForegroundService(
                        this, Intent(this, VoiceService::class.java)
                    )
                    Prefs.putBool(this, Prefs.KEY_JARVIS_ON, true)
                    powering = false
                    powerButton.isEnabled = true
                    progress.visibility = View.GONE
                    powerButton.text = "Power Off Jarvis"
                    // Gentle fade back to the calm running state.
                    statusText.alpha = 0f
                    statusText.animate().alpha(1f).setDuration(500).start()
                }
            }, 650L * (i + 1))
        }
    }

    private fun powerOff() {
        startService(
            Intent(this, VoiceService::class.java).setAction(VoiceService.ACTION_STOP)
        )
        Prefs.putBool(this, Prefs.KEY_JARVIS_ON, false)
        statusText.text = "Jarvis is off."
        powerButton.text = "Power On Jarvis"
        Toast.makeText(this, "Jarvis powered off.", Toast.LENGTH_SHORT).show()
    }
}
