package com.kitagawh.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/** Helpers for the simple number check used by Create Account / Sign In. */
object SimHelper {

    /** The phone number of the SIM in this phone, when Android lets us read it. */
    fun simNumber(context: Context): String? {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.line1Number?.takeIf { it.isNotBlank() }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Compare numbers by their last 10 digits so +91 style prefixes still match. */
    fun numbersMatch(a: String, b: String): Boolean {
        val da = a.filter { it.isDigit() }
        val db = b.filter { it.isDigit() }
        if (da.isEmpty() || db.isEmpty()) return false
        return da.takeLast(10) == db.takeLast(10)
    }
}
