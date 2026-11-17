package com.kitagawh.app

import android.content.Context
import android.content.SharedPreferences

/** All Kitagawh data lives in one SharedPreferences file. */
object Prefs {
    private const val FILE = "kitagawh_prefs"

    const val KEY_NAME = "account_name"
    const val KEY_NUMBER = "account_number"
    const val KEY_AGE = "account_age"
    const val KEY_ABOUT_ACCEPTED = "about_accepted"
    const val KEY_GROQ = "api_groq"
    const val KEY_OPENAI = "api_openai"
    const val KEY_GEMINI = "api_gemini"
    const val KEY_JARVIS_ON = "jarvis_on"
    const val KEY_EASY_TOUCH_ON = "easy_touch_on"
    const val KEY_ICON_SIZE = "et_icon_size_dp"
    const val KEY_ICON_COLOUR = "et_icon_colour"
    const val KEY_ICON_ANIM = "et_icon_anim"
    const val KEY_SLOTS = "et_slots"

    // Easy Touch feature ids
    const val FEATURE_SCREEN_SEARCH = "SCREEN_SEARCH"
    const val FEATURE_TORCH = "TORCH"
    const val FEATURE_CALL_JARVIS = "CALL_JARVIS"
    const val FEATURE_SETTINGS = "SETTINGS"
    const val FEATURE_DESIGN = "DESIGN"

    val ALL_FEATURES = listOf(
        FEATURE_SCREEN_SEARCH,
        FEATURE_TORCH,
        FEATURE_CALL_JARVIS,
        FEATURE_SETTINGS,
        FEATURE_DESIGN
    )

    val DEFAULT_SLOTS = listOf(
        FEATURE_SCREEN_SEARCH,
        FEATURE_TORCH,
        FEATURE_CALL_JARVIS,
        FEATURE_SETTINGS,
        FEATURE_DESIGN
    )

    fun featureLabel(feature: String): String = when (feature) {
        FEATURE_SCREEN_SEARCH -> "Screen Search"
        FEATURE_TORCH -> "Torch"
        FEATURE_CALL_JARVIS -> "Call Jarvis"
        FEATURE_SETTINGS -> "Settings"
        FEATURE_DESIGN -> "Design"
        else -> feature
    }

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getString(context: Context, key: String, def: String = ""): String =
        prefs(context).getString(key, def) ?: def

    fun putString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun getBool(context: Context, key: String, def: Boolean = false): Boolean =
        prefs(context).getBoolean(key, def)

    fun putBool(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    fun getInt(context: Context, key: String, def: Int): Int =
        prefs(context).getInt(key, def)

    fun putInt(context: Context, key: String, value: Int) {
        prefs(context).edit().putInt(key, value).apply()
    }

    fun hasAccount(context: Context): Boolean =
        getString(context, KEY_NUMBER).isNotBlank()

    fun groqKey(context: Context): String = getString(context, KEY_GROQ).trim()

    fun getSlots(context: Context): MutableList<String> {
        val raw = getString(context, KEY_SLOTS)
        if (raw.isBlank()) return DEFAULT_SLOTS.toMutableList()
        val parts = raw.split(",").filter { it.isNotBlank() }
        if (parts.size != 5) return DEFAULT_SLOTS.toMutableList()
        return parts.toMutableList()
    }

    fun putSlots(context: Context, slots: List<String>) {
        putString(context, KEY_SLOTS, slots.joinToString(","))
    }

    fun clearApiKeys(context: Context) {
        prefs(context).edit()
            .remove(KEY_GROQ)
            .remove(KEY_OPENAI)
            .remove(KEY_GEMINI)
            .apply()
    }
}
