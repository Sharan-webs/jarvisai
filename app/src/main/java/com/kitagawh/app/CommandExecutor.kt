package com.kitagawh.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns a spoken command into a real phone action and a spoken reply.
 * Android limits are handled honestly: where the system forbids a direct
 * action, Kitagawh opens the right settings screen and says so.
 */
object CommandExecutor {

    fun execute(context: Context, rawCommand: String, reply: (String) -> Unit) {
        val app = context.applicationContext
        val cmd = rawCommand.lowercase(Locale.ROOT).trim()

        when {
            cmd.startsWith("open ") -> reply(openApp(app, cmd.removePrefix("open ").trim()))

            cmd.contains("forget") -> {
                val ssid = cmd.removePrefix("forget")
                    .replace("wi-fi", "")
                    .replace("wifi", "")
                    .replace("network", "")
                    .trim()
                    .trim('"')
                openWifiSettings(app)
                reply(
                    if (ssid.isNotEmpty())
                        "Android does not let normal apps forget a network directly, so I opened Wi-Fi settings. Long-press $ssid and tap Forget."
                    else
                        "Android does not let normal apps forget a network directly, so I opened Wi-Fi settings. Long-press the network and tap Forget."
                )
            }

            cmd.contains("wi-fi") || cmd.contains("wifi") -> {
                reply(setWifi(app, turnOn = !cmd.contains("off")))
            }

            cmd.contains("torch") || cmd.contains("flashlight") -> {
                reply(setTorch(app, turnOn = !cmd.contains("off")))
            }

            cmd.startsWith("call ") -> reply(callNumber(app, cmd.removePrefix("call ").trim()))

            cmd.contains("screenshot") || cmd.contains("screen shot") -> {
                takeScreenshot(app, reply)
            }

            cmd.contains("time") -> {
                reply("It is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + ".")
            }

            cmd.contains("battery") -> reply(batteryStatus(app))

            cmd.contains("home screen") || cmd == "go home" -> {
                val ok = KitagawhAccessibilityService.instance?.goHome() ?: false
                reply(if (ok) "Going home." else "I need the accessibility permission for that. Open Permissions in the app.")
            }

            cmd == "go back" -> {
                val ok = KitagawhAccessibilityService.instance?.goBack() ?: false
                reply(if (ok) "Going back." else "I need the accessibility permission for that. Open Permissions in the app.")
            }

            cmd.contains("recent") -> {
                val ok = KitagawhAccessibilityService.instance?.goRecents() ?: false
                reply(if (ok) "Opening recent apps." else "I need the accessibility permission for that. Open Permissions in the app.")
            }

            else -> {
                val key = Prefs.groqKey(app)
                if (key.isBlank()) {
                    reply(GroqClient.NO_KEY_MESSAGE)
                } else {
                    GroqClient.chat(key, rawCommand) { reply(it) }
                }
            }
        }
    }

    // ------------------------------------------------------------------ apps

    private fun openApp(context: Context, name: String): String {
        if (name.isBlank()) return "Tell me which app to open."
        // Fast path for YouTube, the most common ask.
        if (name.contains("youtube") || name.contains("you tube")) {
            if (launchPackage(context, "com.google.android.youtube")) return "Opening YouTube."
        }
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }
        val match = apps.firstOrNull {
            it.loadLabel(pm).toString().lowercase(Locale.ROOT).contains(name)
        } ?: apps.firstOrNull {
            name.contains(it.loadLabel(pm).toString().lowercase(Locale.ROOT))
        }
        return if (match != null) {
            val label = match.loadLabel(pm).toString()
            if (launchPackage(context, match.activityInfo.packageName)) "Opening $label."
            else "I found $label but could not open it."
        } else {
            "I could not find an app called $name on this phone."
        }
    }

    private fun launchPackage(context: Context, pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------------ wifi

    private fun setWifi(context: Context, turnOn: Boolean): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                val wm = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val ok = wm.setWifiEnabled(turnOn)
                if (ok) {
                    if (turnOn) "Wi-Fi is on." else "Wi-Fi is off."
                } else {
                    "I could not change Wi-Fi directly."
                }
            } catch (e: Exception) {
                "I could not change Wi-Fi directly."
            }
        } else {
            // Android 10+ forbids apps from toggling Wi-Fi, so open the panel.
            try {
                val panel = Intent(Settings.Panel.ACTION_WIFI)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(panel)
                if (turnOn) "I opened the Wi-Fi panel. Tap the switch to turn Wi-Fi on."
                else "I opened the Wi-Fi panel. Tap the switch to turn Wi-Fi off."
            } catch (e: Exception) {
                openWifiSettings(context)
                "I opened Wi-Fi settings so you can flip the switch."
            }
        }
    }

    private fun openWifiSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_WIFI_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // Settings screen unavailable; nothing else we can do.
        }
    }

    // ----------------------------------------------------------------- torch

    fun setTorch(context: Context, turnOn: Boolean): String {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "This phone has no flashlight I can use."
            cm.setTorchMode(cameraId, turnOn)
            if (turnOn) "Torch is on." else "Torch is off."
        } catch (e: Exception) {
            "I could not control the torch. The camera may be busy."
        }
    }

    fun toggleTorch(context: Context): Boolean {
        // Used by the Easy Touch menu; tracks its own state.
        val newState = !torchState
        val result = setTorch(context, newState)
        if (!result.startsWith("I could not") && !result.startsWith("This phone")) {
            torchState = newState
        }
        return torchState
    }

    @Volatile
    private var torchState = false

    // ------------------------------------------------------------------ call

    private fun callNumber(context: Context, who: String): String {
        val number = who.filter { it.isDigit() || it == '+' }
        if (number.length < 3) {
            return "Tell me the number after the word call, like: call 98765 43210."
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val action = if (hasPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
        return try {
            context.startActivity(
                Intent(action, android.net.Uri.parse("tel:$number"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "Calling $number."
        } catch (e: Exception) {
            "I could not start the call."
        }
    }

    // ------------------------------------------------------------- screenshot

    private fun takeScreenshot(context: Context, reply: (String) -> Unit) {
        val service = KitagawhAccessibilityService.instance
        if (service == null) {
            reply("I need the accessibility permission to take a screenshot. Open Permissions in the app.")
            return
        }
        service.takeScreenshotBitmap { bitmap ->
            if (bitmap == null) {
                reply("The screenshot failed. On some phones it takes a second - try again.")
            } else {
                try {
                    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                    val file = File(dir, "kitagawh_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    }
                    reply("Screenshot saved in the Kitagawh pictures folder.")
                } catch (e: Exception) {
                    reply("I took the screenshot but could not save it.")
                }
            }
        }
    }

    // ---------------------------------------------------------------- battery

    private fun batteryStatus(context: Context): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        if (level < 0) return "I could not read the battery level."
        val pct = level * 100 / scale
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return "Battery is at $pct percent" + if (charging) " and charging." else "."
    }
}
