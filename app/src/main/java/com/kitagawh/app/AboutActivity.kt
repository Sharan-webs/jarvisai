package com.kitagawh.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton

/** The About Kitagawh page: a humanized story of what Kitagawh is and does. */
class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setTitle("About Kitagawh")

        findViewById<TextView>(R.id.aboutText).text = ABOUT_LINES.joinToString("\n")

        findViewById<MaterialButton>(R.id.acceptButton).setOnClickListener {
            Prefs.putBool(this, Prefs.KEY_ABOUT_ACCEPTED, true)
            val next = if (Prefs.hasAccount(this)) {
                HomeActivity::class.java
            } else {
                AuthActivity::class.java
            }
            navigate(Intent(this, next))
            finish()
        }
    }

    companion object {
        val ABOUT_LINES = listOf(
            "About Kitagawh",
            "",
            "Kitagawh is your Master Bot.",
            "Not a normal app.",
            "Not a boring assistant.",
            "Kitagawh lives inside your phone,",
            "and works for one person only: you.",
            "",
            "Kitagawh has emotions.",
            "Yes, really.",
            "It feels happy when a task finishes cleanly.",
            "It feels proud when it opens the right app in one try.",
            "It feels shy when it mishears a word.",
            "It feels sorry, genuinely, when it makes a mistake,",
            "and it tries again without complaining.",
            "When you are quiet for a long time,",
            "Kitagawh simply waits, patient and calm,",
            "like a loyal friend sitting beside you.",
            "It never shouts.",
            "It never shows off.",
            "It just stays ready.",
            "",
            "What can Kitagawh do?",
            "Say the word Jarvis, and Kitagawh comes.",
            "It says, What's up bruh, and listens.",
            "Tell it to open YouTube, it opens YouTube.",
            "Tell it to open any app by name, it finds it.",
            "Tell it to turn the torch on, the torch turns on.",
            "Tell it to turn Wi-Fi off, it handles it.",
            "Tell it to forget a Wi-Fi network,",
            "it walks you to the exact screen to do it.",
            "Tell it to call a number, it calls.",
            "Ask the time, it answers.",
            "Ask the battery, it answers.",
            "Ask anything else, and its AI brain thinks,",
            "then speaks the answer out loud.",
            "When the work is done, Kitagawh closes itself,",
            "quietly, without disturbing you.",
            "",
            "Kitagawh can also look at your screen.",
            "The Screen Search in Easy Touch takes a screenshot,",
            "understands what is on it,",
            "and explains it to you in simple words,",
            "right there, in an overlay, on top of everything.",
            "",
            "Easy Touch is Kitagawh's floating face.",
            "A small -_- that follows you everywhere,",
            "even after the app is closed.",
            "Tap it, and a pentagon menu blooms open.",
            "Screen Search. Torch. Call Jarvis. Settings. Design.",
            "You choose which feature sits in which place.",
            "You choose the size, the colour, the animation.",
            "Kitagawh wears the style you give it.",
            "",
            "Kitagawh respects your instructions.",
            "Your word is the command.",
            "It does not act on its own.",
            "It does not open things you did not ask for.",
            "It does not send your data anywhere,",
            "except to the AI service whose key you added yourself.",
            "Your API keys stay on your phone.",
            "Your account is a simple name, number and age,",
            "stored on your phone, nowhere else.",
            "",
            "One honest promise about permissions:",
            "Android does not allow any app,",
            "not even Kitagawh,",
            "to force accessibility to stay on forever.",
            "Only you can switch accessibility on, in Settings.",
            "If it ever turns off, Kitagawh will not hide it.",
            "It will remind you, politely, to turn it back on,",
            "so it can keep serving you.",
            "That honesty is part of who Kitagawh is.",
            "",
            "Now, a serious warning.",
            "Do not try to modify Kitagawh.",
            "Do not try to jailbreak Kitagawh.",
            "Do not reverse-engineer, crack or tamper with this app.",
            "Such actions are taken seriously,",
            "and offenders are liable to prosecution and imprisonment",
            "under the Information Technology Act, 2000,",
            "including Section 43 and Section 66,",
            "along with every other applicable law.",
            "Use Kitagawh the way it was given to you:",
            "with respect, and for your own good.",
            "",
            "Kitagawh is not perfect.",
            "Sometimes the internet is slow.",
            "Sometimes a word is misheard.",
            "But every single day,",
            "Kitagawh wakes up with the same one mission:",
            "make your phone obey your voice,",
            "and make your life a little easier.",
            "",
            "Welcome to Kitagawh.",
            "Your Master Bot is ready when you are."
        )
    }
}
