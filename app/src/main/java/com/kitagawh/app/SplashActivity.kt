package com.kitagawh.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Black splash with the pulsing white "-_-" face, then routes the user. */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val face = findViewById<TextView>(R.id.splashFace)
        val pulseX = ObjectAnimator.ofFloat(face, "scaleX", 1f, 1.12f, 1f)
        val pulseY = ObjectAnimator.ofFloat(face, "scaleY", 1f, 1.12f, 1f)
        for (anim in listOf(pulseX, pulseY)) {
            anim.duration = 900
            anim.repeatCount = ObjectAnimator.INFINITE
            anim.start()
        }
        face.alpha = 0f
        face.animate().alpha(1f).setDuration(600).start()

        Handler(Looper.getMainLooper()).postDelayed({
            val next = when {
                !Prefs.getBool(this, Prefs.KEY_ABOUT_ACCEPTED) -> AboutActivity::class.java
                !Prefs.hasAccount(this) -> AuthActivity::class.java
                else -> HomeActivity::class.java
            }
            startActivity(Intent(this, next))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }, 1700)
    }
}
