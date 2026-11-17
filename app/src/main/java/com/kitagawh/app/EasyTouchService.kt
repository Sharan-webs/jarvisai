package com.kitagawh.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.cos
import kotlin.math.sin

/**
 * Easy Touch: the floating "-_-" icon that runs even when the app is closed.
 * Tapping it (and only tapping it) opens the pentagon menu with five
 * assignable feature slots. Dragging moves the icon around the screen.
 */
class EasyTouchService : Service() {

    companion object {
        const val ACTION_RELOAD = "com.kitagawh.app.EASY_TOUCH_RELOAD"
        const val ACTION_STOP = "com.kitagawh.app.EASY_TOUCH_STOP"

        @Volatile
        var running = false
            private set
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var iconView: TextView? = null
    private var iconParams: WindowManager.LayoutParams? = null
    private var iconAnimator: AnimatorSet? = null

    private var menuView: FrameLayout? = null
    private var resultView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForegroundCompat()
        showIcon()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RELOAD -> {
                rebuildIcon()
                closeMenu()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        closeMenu()
        closeResult()
        iconAnimator?.cancel()
        iconView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Already removed.
            }
        }
        iconView = null
        super.onDestroy()
    }

    // ------------------------------------------------------------ foreground

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    "kitagawh_easy_touch",
                    "Kitagawh Easy Touch",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun startForegroundCompat() {
        val notification: Notification = NotificationCompat.Builder(this, "kitagawh_easy_touch")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Kitagawh Easy Touch is on")
            .setContentText("Tap the floating face to open the menu.")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                3, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(3, notification)
        }
    }

    // ------------------------------------------------------------------ icon

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rebuildIcon() {
        iconView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Already removed.
            }
        }
        iconView = null
        showIcon()
    }

    private fun showIcon() {
        if (iconView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            Toast.makeText(
                this,
                "Easy Touch needs the overlay permission. Open Permissions in the app.",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
            return
        }
        val sizeDp = Prefs.getInt(this, Prefs.KEY_ICON_SIZE, 64)
        val colourName = Prefs.getString(this, Prefs.KEY_ICON_COLOUR, "Black")
        val bgColour = colourValue(colourName)

        val icon = TextView(this).apply {
            text = "-_-"
            textSize = (sizeDp * 0.34f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(bgColour)
                setStroke(dp(2), Color.WHITE)
            }
            elevation = dp(6).toFloat()
        }
        val params = WindowManager.LayoutParams(
            dp(sizeDp), dp(sizeDp),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = dp(16)
        params.y = dp(180)

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var downTime = 0L
        icon.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downX).toInt()
                    params.y = startY + (event.rawY - downY).toInt()
                    try {
                        windowManager?.updateViewLayout(v, params)
                    } catch (e: Exception) {
                        // View already gone.
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = kotlin.math.abs(event.rawX - downX) +
                        kotlin.math.abs(event.rawY - downY)
                    val quick = System.currentTimeMillis() - downTime < 350
                    if (moved < dp(10) && quick) {
                        // A tap on the floating icon - and only a tap - opens
                        // the pentagon menu, like a mod menu.
                        if (menuView == null) openMenu() else closeMenu()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(icon, params)
            iconView = icon
            iconParams = params
            applyIconAnimation(icon)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun colourValue(name: String): Int = when (name) {
        "Purple" -> Color.parseColor("#4A148C")
        "Blue" -> Color.parseColor("#0D47A1")
        "Green" -> Color.parseColor("#1B5E20")
        "Red" -> Color.parseColor("#B71C1C")
        "White" -> Color.parseColor("#424242")
        else -> Color.BLACK
    }

    private fun applyIconAnimation(icon: View) {
        iconAnimator?.cancel()
        val anim = Prefs.getString(this, Prefs.KEY_ICON_ANIM, "Pulse")
        val set = AnimatorSet()
        when (anim) {
            "Rotate" -> {
                val a = ObjectAnimator.ofFloat(icon, "rotation", 0f, 360f)
                a.duration = 3200
                a.repeatCount = ValueAnimator.INFINITE
                a.interpolator = LinearInterpolator()
                set.play(a)
            }
            "Pulse" -> {
                val sx = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.14f)
                val sy = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.14f)
                for (a in listOf(sx, sy)) {
                    a.duration = 800
                    a.repeatCount = ValueAnimator.INFINITE
                    a.repeatMode = ValueAnimator.REVERSE
                }
                set.playTogether(sx, sy)
            }
            "Bounce" -> {
                val a = ObjectAnimator.ofFloat(icon, "translationY", 0f, -16f)
                a.duration = 650
                a.repeatCount = ValueAnimator.INFINITE
                a.repeatMode = ValueAnimator.REVERSE
                set.play(a)
            }
            "Fade" -> {
                val a = ObjectAnimator.ofFloat(icon, "alpha", 1f, 0.35f)
                a.duration = 900
                a.repeatCount = ValueAnimator.INFINITE
                a.repeatMode = ValueAnimator.REVERSE
                set.play(a)
            }
            "Shake" -> {
                val a = ObjectAnimator.ofFloat(
                    icon, "translationX", 0f, -9f, 9f, -9f, 9f, 0f
                )
                a.duration = 700
                a.repeatCount = ValueAnimator.INFINITE
                set.play(a)
            }
            else -> return
        }
        iconAnimator = set
        set.start()
    }

    // ------------------------------------------------------------------ menu

    private fun openMenu() {
        if (menuView != null) return
        val size = dp(300)
        val container = FrameLayout(this)
        val pentagon = PentagonView(this)
        container.addView(
            pentagon,
            FrameLayout.LayoutParams(size, size)
        )

        val slots = Prefs.getSlots(this)
        val center = size / 2f
        val ring = size * 0.335f
        val buttonW = dp(92)
        val buttonH = dp(52)
        for (i in 0 until 5) {
            val angle = Math.toRadians((-90 + i * 72).toDouble())
            val x = center + (cos(angle) * ring).toFloat()
            val y = center + (sin(angle) * ring).toFloat()
            val feature = slots.getOrElse(i) { Prefs.DEFAULT_SLOTS[i] }
            val button = Button(this).apply {
                text = Prefs.featureLabel(feature)
                textSize = 12f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(14).toFloat()
                    setColor(Color.parseColor("#3A3A4D"))
                    setStroke(dp(1), Color.parseColor("#7C4DFF"))
                }
                setOnClickListener { runFeature(feature) }
            }
            val lp = FrameLayout.LayoutParams(buttonW, buttonH)
            lp.leftMargin = (x - buttonW / 2).toInt()
            lp.topMargin = (y - buttonH / 2).toInt()
            container.addView(button, lp)
        }

        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        try {
            windowManager?.addView(container, params)
            menuView = container
        } catch (e: Exception) {
            menuView = null
        }
    }

    private fun closeMenu() {
        menuView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Already removed.
            }
        }
        menuView = null
    }

    // -------------------------------------------------------------- features

    private fun runFeature(feature: String) {
        when (feature) {
            Prefs.FEATURE_TORCH -> {
                val on = CommandExecutor.toggleTorch(applicationContext)
                Toast.makeText(
                    this,
                    if (on) "Torch is on." else "Torch is off.",
                    Toast.LENGTH_SHORT
                ).show()
                closeMenu()
            }
            Prefs.FEATURE_CALL_JARVIS -> {
                closeMenu()
                val voice = VoiceService.instance
                if (voice != null) {
                    voice.callJarvis()
                } else {
                    val intent = Intent(this, VoiceService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    Prefs.putBool(this, Prefs.KEY_JARVIS_ON, true)
                    mainHandler.postDelayed({
                        VoiceService.instance?.callJarvis()
                    }, 900)
                }
            }
            Prefs.FEATURE_SCREEN_SEARCH -> {
                closeMenu()
                // Hide the floating icon so it is not inside the screenshot.
                iconView?.visibility = View.GONE
                mainHandler.postDelayed({
                    ScreenSearchHelper.run(applicationContext) { explanation ->
                        mainHandler.post {
                            iconView?.visibility = View.VISIBLE
                            showResult(explanation)
                        }
                    }
                }, 400)
            }
            Prefs.FEATURE_SETTINGS -> {
                closeMenu()
                startActivity(
                    Intent(this, EasyTouchSettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            Prefs.FEATURE_DESIGN -> {
                closeMenu()
                startActivity(
                    Intent(this, EasyTouchDesignActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    private fun showResult(text: String) {
        closeResult()
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_result, null)
        view.findViewById<TextView>(R.id.resultText).text = text
        view.findViewById<View>(R.id.resultClose).setOnClickListener { closeResult() }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM
        params.y = 40
        try {
            windowManager?.addView(view, params)
            resultView = view
        } catch (e: Exception) {
            resultView = null
        }
    }

    private fun closeResult() {
        resultView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Already removed.
            }
        }
        resultView = null
    }
}
