package com.kitagawh.app

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors

/**
 * Kitagawh's hands on the phone: global actions, tap-by-text and screenshots.
 *
 * Honest limit: Android gives NO app a way to force accessibility to stay on
 * or to turn itself back on - only the user can enable it in Settings.
 * So this service runs a detect-and-prompt watchdog instead: whenever the
 * service disconnects, it posts a notification asking the user to re-enable
 * it. That is the strongest behaviour Android legitimately allows.
 */
class KitagawhAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: KitagawhAccessibilityService? = null
            private set

        private const val CHANNEL_ID = "kitagawh_watchdog"
        private const val NOTIF_ID = 77
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // All good: remove any earlier "please re-enable" notification.
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIF_ID)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Kitagawh acts on commands, not on events, so there is nothing to do here.
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        promptReEnable()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    // ------------------------------------------------------------ watchdog

    private fun promptReEnable() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Kitagawh permission alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val openSettings = PendingIntent.getActivity(
            this,
            0,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Kitagawh accessibility turned off")
            .setContentText("Tap to turn it back on so Kitagawh can keep working for you.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openSettings)
            .build()
        try {
            nm.notify(NOTIF_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted; nothing more we can do.
        }
    }

    // -------------------------------------------------------- global actions

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun goRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    // ------------------------------------------------------------ tap by text

    /** Finds a visible element containing [text] and clicks it. */
    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text) ?: return false
        for (node in nodes) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) {
                    val ok = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (ok) return true
                }
                current = current.parent
            }
        }
        return false
    }

    // ------------------------------------------------------------- screenshot

    /**
     * Takes a screenshot using the accessibility API (Android 11+).
     * On older versions, or on failure, the callback receives null.
     */
    fun takeScreenshotBitmap(callback: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            callback(null)
            return
        }
        val executor = Executors.newSingleThreadExecutor()
        val mainHandler = Handler(Looper.getMainLooper())
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                executor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        val bitmap = Bitmap.wrapHardwareBuffer(
                            result.hardwareBuffer, result.colorSpace
                        )
                        val copy = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        try {
                            result.hardwareBuffer.close()
                        } catch (e: Exception) {
                            // Already closed; ignore.
                        }
                        mainHandler.post { callback(copy) }
                    }

                    override fun onFailure(errorCode: Int) {
                        mainHandler.post { callback(null) }
                    }
                }
            )
        } catch (e: Exception) {
            callback(null)
        }
    }
}
