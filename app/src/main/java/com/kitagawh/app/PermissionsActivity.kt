package com.kitagawh.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

/**
 * The permissions page: every row shows a LIVE status (re-checked in
 * onResume, so it updates the moment you come back from a settings screen)
 * and opens the real system screen for that permission when tapped.
 */
class PermissionsActivity : BaseActivity() {

    private data class Row(
        val title: String,
        val why: String,
        val granted: () -> Boolean,
        val action: () -> Unit
    )

    private lateinit var container: LinearLayout
    private var rows: List<Row> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        setTitle("Permissions")
        container = findViewById(R.id.permContainer)
        rows = buildRows()
        findViewById<MaterialButton>(R.id.permissionsContinueButton).setOnClickListener {
            if (Prefs.hasAccount(this)) {
                navigate(HomeActivity::class.java)
                finish()
            } else {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderRows()
    }

    private fun renderRows() {
        container.removeAllViews()
        for (row in rows) {
            container.addView(buildRowView(row))
        }
    }

    private fun buildRowView(row: Row): LinearLayout {
        val granted = row.granted()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(28, 28, 28, 28)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 24
            layoutParams = lp
            isClickable = true
            isFocusable = true
            setOnClickListener { row.action() }
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(this).apply {
            text = row.title
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        texts.addView(TextView(this).apply {
            text = row.why
            textSize = 13f
            setTextColor(getColor(R.color.text_secondary))
        })
        texts.addView(TextView(this).apply {
            text = if (granted) "Status: ON" else "Status: OFF - tap to open settings"
            textSize = 13f
            setTextColor(
                getColor(if (granted) R.color.success_green else R.color.error_red)
            )
        })
        layout.addView(texts)
        layout.addView(TextView(this).apply {
            text = if (granted) "ON" else "OPEN"
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        return layout
    }

    // ------------------------------------------------------------ row checks

    private fun runtimeGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun accessibilityEnabled(): Boolean {
        val expected = "$packageName/${KitagawhAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) } ||
            KitagawhAccessibilityService.instance != null
    }

    private fun buildRows(): List<Row> = listOf(
        Row(
            "Accessibility",
            "Kitagawh's hands: taps, screenshots and screen control for your commands.",
            { accessibilityEnabled() }
        ) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        Row(
            "Display over other apps (Overlay)",
            "Shows the floating Easy Touch icon and the Jarvis panel on any screen.",
            { Settings.canDrawOverlays(this) }
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        },
        Row(
            "Microphone",
            "So Kitagawh can hear the word Jarvis and your commands.",
            { runtimeGranted(Manifest.permission.RECORD_AUDIO) }
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), 101
            )
        },
        Row(
            "Camera and Torch",
            "Powers the flashlight commands.",
            { runtimeGranted(Manifest.permission.CAMERA) }
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 102
            )
        },
        Row(
            "Phone calls",
            "Lets Kitagawh place the calls you ask for.",
            { runtimeGranted(Manifest.permission.CALL_PHONE) }
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
                ),
                103
            )
        },
        Row(
            "Notifications",
            "Keeps the listening and Easy Touch status visible, and alerts you if accessibility turns off.",
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runtimeGranted(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    true
                }
            }
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 104
                )
            } else {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
            }
        },
        Row(
            "Ignore battery optimisation",
            "Stops Android from killing Kitagawh in the background, so Easy Touch and voice keep running after the app is closed.",
            {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(packageName)
            }
        ) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        },
        Row(
            "Wi-Fi control",
            "Used by Wi-Fi on and off commands. On Android 10 and above, Android itself only allows this from the system panel, which Kitagawh opens for you.",
            { runtimeGranted(Manifest.permission.CHANGE_WIFI_STATE) }
        ) {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    )
}
