package com.kitagawh.app

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Screen Search: screenshot the current screen through the accessibility
 * service, send it to the Groq vision model, and hand back a plain-language
 * explanation of what is on screen. Without a Groq key (or without the
 * accessibility permission) it returns a clear fallback message instead.
 */
object ScreenSearchHelper {

    const val NO_ACCESSIBILITY_MESSAGE =
        "Screen Search needs the Kitagawh accessibility permission. Open the app, go to Permissions, and turn on Kitagawh Accessibility."

    const val NO_KEY_MESSAGE =
        "I can see your screen, but I need a brain to explain it. Add your free Groq API key in Settings, in the API section, then try Screen Search again."

    fun run(context: Context, callback: (String) -> Unit) {
        val service = KitagawhAccessibilityService.instance
        if (service == null) {
            callback(NO_ACCESSIBILITY_MESSAGE)
            return
        }
        service.takeScreenshotBitmap { bitmap ->
            if (bitmap == null) {
                callback("I could not capture the screen. Screen capture needs Android 11 or above - try again in a moment.")
                return@takeScreenshotBitmap
            }
            val key = Prefs.groqKey(context)
            if (key.isBlank()) {
                callback(NO_KEY_MESSAGE)
                return@takeScreenshotBitmap
            }
            val base64 = try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                callback("I captured the screen but could not prepare the image.")
                return@takeScreenshotBitmap
            }
            GroqClient.describeScreen(key, base64) { description ->
                callback(description)
            }
        }
    }
}
