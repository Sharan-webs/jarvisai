package com.kitagawh.app

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton

/**
 * Easy Touch design: icon size (slider), icon colour, and one of five icon
 * animations - Rotate, Pulse, Bounce, Fade, Shake.
 */
class EasyTouchDesignActivity : BaseActivity() {

    private val colours = listOf("Black", "Purple", "Blue", "Green", "Red", "White")
    private val animations = listOf("Rotate", "Pulse", "Bounce", "Fade", "Shake")

    private lateinit var colourGroup: RadioGroup
    private lateinit var animationGroup: RadioGroup
    private lateinit var sizeLabel: TextView
    private var sizeDp: Int = 64

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easy_touch_design)
        setTitle("Easy Touch Design")

        colourGroup = findViewById(R.id.colourGroup)
        animationGroup = findViewById(R.id.animationGroup)
        sizeLabel = findViewById(R.id.iconSizeLabel)

        sizeDp = Prefs.getInt(this, Prefs.KEY_ICON_SIZE, 64)
        val seek = findViewById<SeekBar>(R.id.iconSizeSeek)
        seek.progress = sizeDp - 40
        updateSizeLabel()
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                sizeDp = progress + 40
                updateSizeLabel()
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        val savedColour = Prefs.getString(this, Prefs.KEY_ICON_COLOUR, "Black")
        for ((index, colour) in colours.withIndex()) {
            colourGroup.addView(RadioButton(this).apply {
                text = colour
                id = 1000 + index
                isChecked = colour == savedColour
                setTextColor(getColor(R.color.text_primary))
                textSize = 15f
            })
        }

        val savedAnim = Prefs.getString(this, Prefs.KEY_ICON_ANIM, "Pulse")
        for ((index, anim) in animations.withIndex()) {
            animationGroup.addView(RadioButton(this).apply {
                text = anim
                id = 2000 + index
                isChecked = anim == savedAnim
                setTextColor(getColor(R.color.text_primary))
                textSize = 15f
            })
        }

        findViewById<MaterialButton>(R.id.designSaveButton).setOnClickListener {
            Prefs.putInt(this, Prefs.KEY_ICON_SIZE, sizeDp)
            Prefs.putString(
                this, Prefs.KEY_ICON_COLOUR,
                selectedText(colourGroup, colours, "Black")
            )
            Prefs.putString(
                this, Prefs.KEY_ICON_ANIM,
                selectedText(animationGroup, animations, "Pulse")
            )
            if (EasyTouchService.running) {
                startService(
                    Intent(this, EasyTouchService::class.java)
                        .setAction(EasyTouchService.ACTION_RELOAD)
                )
            }
            Toast.makeText(this, "Design saved.", Toast.LENGTH_SHORT).show()
            goBack()
        }
    }

    private fun updateSizeLabel() {
        sizeLabel.text = "Icon size: $sizeDp dp"
    }

    private fun selectedText(group: RadioGroup, options: List<String>, fallback: String): String {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i) as? RadioButton ?: continue
            if (child.isChecked) return child.text?.toString() ?: fallback
        }
        return fallback
    }
}
