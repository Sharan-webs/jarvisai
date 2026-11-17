package com.kitagawh.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton

/**
 * Easy Touch settings: pick a feature, then tap a pentagon slot to move the
 * feature into that place. Tapping a slot that already holds a feature swaps
 * the two features, so nothing is ever lost.
 */
class EasyTouchSettingsActivity : BaseActivity() {

    private var selectedFeature: String? = null
    private lateinit var featuresContainer: LinearLayout
    private lateinit var slotsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easy_touch_settings)
        setTitle("Easy Touch Settings")

        featuresContainer = findViewById(R.id.featuresContainer)
        slotsContainer = findViewById(R.id.slotsContainer)

        findViewById<MaterialButton>(R.id.easyTouchSaveButton).setOnClickListener {
            // Changes are saved the moment they are made; this button also
            // tells a running Easy Touch to redraw itself, then goes back.
            reloadEasyTouch()
            Toast.makeText(this, "Easy Touch layout saved.", Toast.LENGTH_SHORT).show()
            goBack()
        }
        render()
    }

    private fun render() {
        featuresContainer.removeAllViews()
        slotsContainer.removeAllViews()

        for (feature in Prefs.ALL_FEATURES) {
            val selected = feature == selectedFeature
            featuresContainer.addView(rowButton(
                text = Prefs.featureLabel(feature) +
                    if (selected) "  (selected - now tap a slot)" else "",
                highlighted = selected
            ) {
                selectedFeature = feature
                render()
            })
        }

        val slots = Prefs.getSlots(this)
        for (i in slots.indices) {
            val slotNumber = i + 1
            slotsContainer.addView(rowButton(
                text = "Slot $slotNumber: ${Prefs.featureLabel(slots[i])}",
                highlighted = false
            ) {
                val feature = selectedFeature
                if (feature == null) {
                    Toast.makeText(
                        this,
                        "First tap a feature above, then tap the slot.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val current = Prefs.getSlots(this)
                    val oldIndex = current.indexOf(feature)
                    val displaced = current[i]
                    current[i] = feature
                    if (oldIndex >= 0) current[oldIndex] = displaced
                    Prefs.putSlots(this, current)
                    selectedFeature = null
                    reloadEasyTouch()
                    render()
                }
            })
        }
    }

    private fun rowButton(
        text: String,
        highlighted: Boolean,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(28, 28, 28, 28)
            setBackgroundResource(R.drawable.bg_card)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 16
            layoutParams = lp
            if (highlighted) {
                setBackgroundColor(getColor(R.color.accent))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun reloadEasyTouch() {
        if (EasyTouchService.running) {
            startService(
                Intent(this, EasyTouchService::class.java)
                    .setAction(EasyTouchService.ACTION_RELOAD)
            )
        }
    }
}
