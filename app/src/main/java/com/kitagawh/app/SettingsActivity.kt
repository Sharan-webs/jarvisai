package com.kitagawh.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import java.io.File

/**
 * Kitagawh settings: restart, clear cache, clear API keys, the API section
 * (named boxes for Groq / OpenAI / Gemini, each with an info guide, Save and
 * Edit), and Contact Admin.
 */
class SettingsActivity : BaseActivity() {

    private data class Provider(
        val name: String,
        val prefKey: String,
        val guide: String
    )

    private val providers = listOf(
        Provider(
            "Groq", Prefs.KEY_GROQ,
            "How to get a free Groq API key:\n\n" +
                "1. Open console.groq.com in your browser.\n" +
                "2. Sign up with your email or Google account.\n" +
                "3. Go to API Keys in the left menu.\n" +
                "4. Tap Create API Key and copy it.\n" +
                "5. Paste it here and tap Save.\n\n" +
                "Groq is free and powers Kitagawh's brain and Screen Search in this version."
        ),
        Provider(
            "OpenAI", Prefs.KEY_OPENAI,
            "How to get an OpenAI API key:\n\n" +
                "1. Open platform.openai.com and sign in.\n" +
                "2. Go to API Keys.\n" +
                "3. Create a new secret key and copy it.\n" +
                "4. Paste it here and tap Save.\n\n" +
                "Note: OpenAI is a paid service. The key is stored for future use; this version talks to Groq."
        ),
        Provider(
            "Gemini", Prefs.KEY_GEMINI,
            "How to get a free Gemini API key:\n\n" +
                "1. Open aistudio.google.com and sign in.\n" +
                "2. Tap Get API Key, then Create API Key.\n" +
                "3. Copy the key.\n" +
                "4. Paste it here and tap Save.\n\n" +
                "Note: The key is stored for future use; this version talks to Groq."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setTitle("Settings")

        findViewById<MaterialButton>(R.id.restartButton).setOnClickListener {
            restartApp()
        }
        findViewById<MaterialButton>(R.id.clearCacheButton).setOnClickListener {
            clearCache()
        }
        findViewById<MaterialButton>(R.id.clearApiButton).setOnClickListener {
            Prefs.clearApiKeys(this)
            Toast.makeText(this, "All API keys cleared.", Toast.LENGTH_SHORT).show()
            rebuildApiBoxes()
        }
        findViewById<MaterialButton>(R.id.contactAdminButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:csharan2122011@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Kitagawh support")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "No email app found. Write to csharan2122011@gmail.com",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        rebuildApiBoxes()
    }

    // ------------------------------------------------------------- api boxes

    private fun rebuildApiBoxes() {
        val container = findViewById<LinearLayout>(R.id.apiContainer)
        container.removeAllViews()
        for (provider in providers) {
            container.addView(buildProviderBox(provider))
        }
    }

    private fun buildProviderBox(provider: Provider): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(28, 28, 28, 28)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 24
            layoutParams = lp
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val infoButton = TextView(this).apply {
            text = "i"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_floating_icon)
            layoutParams = LinearLayout.LayoutParams(64, 64)
            setOnClickListener {
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("How to get a ${provider.name} API key")
                    .setMessage(provider.guide)
                    .setPositiveButton("Got it", null)
                    .show()
            }
        }
        header.addView(infoButton)
        header.addView(TextView(this).apply {
            text = provider.name
            textSize = 17f
            setTextColor(getColor(R.color.text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginStart = 20
            layoutParams = lp
        })
        box.addView(header)

        val saved = Prefs.getString(this, provider.prefKey)
        val input = EditText(this).apply {
            hint = if (saved.isBlank()) "Paste ${provider.name} API key" else "Key saved - tap Edit to change"
            setText(if (saved.isBlank()) "" else saved)
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
            isEnabled = saved.isBlank()
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 16
            layoutParams = lp
        }
        box.addView(input)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 12
            layoutParams = lp
        }
        val saveButton = MaterialButton(this).apply {
            text = "Save"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val value = input.text?.toString()?.trim().orEmpty()
                if (value.isEmpty()) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Paste a key first.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Prefs.putString(this@SettingsActivity, provider.prefKey, value)
                    input.isEnabled = false
                    Toast.makeText(
                        this@SettingsActivity,
                        "${provider.name} key saved.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        val editButton = MaterialButton(this).apply {
            text = "Edit"
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = 16
            layoutParams = lp
            setOnClickListener {
                input.isEnabled = true
                input.requestFocus()
                input.setSelection(input.text?.length ?: 0)
            }
        }
        buttons.addView(saveButton)
        buttons.addView(editButton)
        box.addView(buttons)
        return box
    }

    // ---------------------------------------------------------------- actions

    private fun clearCache() {
        var freed = 0L
        fun deleteRecursively(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { deleteRecursively(it) }
            }
            val size = if (file.isFile) file.length() else 0L
            if (file.delete()) freed += size
        }
        try {
            deleteRecursively(cacheDir)
            externalCacheDir?.let { deleteRecursively(it) }
        } catch (e: Exception) {
            // Cache partially cleared; fine.
        }
        val kb = freed / 1024
        Toast.makeText(
            this,
            if (kb > 0) "Cache cleared. Freed about $kb KB." else "Cache is already clean.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun restartApp() {
        Toast.makeText(this, "Restarting Kitagawh...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SplashActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(intent)
        finishAffinity()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
