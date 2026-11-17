package com.kitagawh.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

/**
 * The homepage: the always-rotating sphere with its aura, a Start AI path to
 * the power screen, and the Easy Touch on/off switch. Supports
 * picture-in-picture so Kitagawh can shrink into a small floating window.
 */
class HomeActivity : BaseActivity() {

    private lateinit var statusText: TextView
    private lateinit var easyTouchButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<android.view.View>(R.id.backArrow).setOnClickListener { goBack() }
        findViewById<MaterialButton>(R.id.homeSettingsButton).setOnClickListener {
            navigate(SettingsActivity::class.java)
        }

        val name = Prefs.getString(this, Prefs.KEY_NAME)
        findViewById<TextView>(R.id.homeGreeting).text =
            if (name.isNotBlank()) "Kitagawh is ready, $name." else "Kitagawh is ready."

        statusText = findViewById(R.id.homeStatus)
        easyTouchButton = findViewById(R.id.easyTouchToggleButton)

        findViewById<MaterialButton>(R.id.startAiButton).setOnClickListener {
            navigate(PowerActivity::class.java)
        }
        findViewById<MaterialButton>(R.id.permissionsButton).setOnClickListener {
            navigate(PermissionsActivity::class.java)
        }
        easyTouchButton.setOnClickListener { toggleEasyTouch() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val jarvisOn = Prefs.getBool(this, Prefs.KEY_JARVIS_ON) && VoiceService.running
        val etOn = Prefs.getBool(this, Prefs.KEY_EASY_TOUCH_ON) && EasyTouchService.running
        statusText.text =
            "Jarvis is ${if (jarvisOn) "ON" else "off"}. " +
                "Easy Touch is ${if (etOn) "ON" else "off"}."
        easyTouchButton.text = if (etOn) "Stop Easy Touch" else "Start Easy Touch"
    }

    private fun toggleEasyTouch() {
        val etOn = Prefs.getBool(this, Prefs.KEY_EASY_TOUCH_ON) && EasyTouchService.running
        if (etOn) {
            startService(
                Intent(this, EasyTouchService::class.java)
                    .setAction(EasyTouchService.ACTION_STOP)
            )
            Prefs.putBool(this, Prefs.KEY_EASY_TOUCH_ON, false)
            Toast.makeText(this, "Easy Touch off.", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this)
            ) {
                Toast.makeText(
                    this,
                    "Easy Touch needs the overlay permission first. Opening Permissions.",
                    Toast.LENGTH_LONG
                ).show()
                navigate(PermissionsActivity::class.java)
                return
            }
            ContextCompat.startForegroundService(
                this, Intent(this, EasyTouchService::class.java)
            )
            Prefs.putBool(this, Prefs.KEY_EASY_TOUCH_ON, true)
            Toast.makeText(
                this,
                "Easy Touch on. The floating face stays even after the app closes.",
                Toast.LENGTH_LONG
            ).show()
        }
        refreshStatusDelayed()
    }

    private fun refreshStatusDelayed() {
        window.decorView.postDelayed({ refreshStatus() }, 600)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If the user presses Home while Jarvis is running, shrink into PiP
        // instead of vanishing, where the device supports it.
        if (Prefs.getBool(this, Prefs.KEY_JARVIS_ON) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            try {
                enterPictureInPictureMode(
                    android.app.PictureInPictureParams.Builder().build()
                )
            } catch (e: Exception) {
                // PiP not available on this device; ignore.
            }
        }
    }
}
