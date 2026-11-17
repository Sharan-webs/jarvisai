package com.kitagawh.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Base activity: wires the top-left back arrow (present in every screen's
 * layout) so it simply calls finish() - the previous screen is still alive
 * underneath, so nothing reloads.
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        findViewById<View>(R.id.backArrow)?.setOnClickListener { goBack() }
    }

    protected fun setTitle(text: String) {
        findViewById<TextView>(R.id.topTitle)?.text = text
    }

    protected fun goBack() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    protected fun navigate(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    protected fun navigate(cls: Class<*>) {
        navigate(Intent(this, cls))
    }
}
