package com.jarvis.assistant.data

import android.content.Context
import android.content.SharedPreferences

class Prefs private constructor(private val sp: SharedPreferences) {
    var baseUrl: String
        get() = sp.getString(KEY_BASE, DEFAULT_BASE) ?: DEFAULT_BASE
        set(v) = sp.edit().putString(KEY_BASE, v.trim()).apply()
    var apiKey: String
        get() = sp.getString(KEY_KEY, "") ?: ""
        set(v) = sp.edit().putString(KEY_KEY, v.trim()).apply()
    var model: String
        get() = sp.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(v) = sp.edit().putString(KEY_MODEL, v.trim()).apply()
    var wakeWordEnabled: Boolean
        get() = sp.getBoolean(KEY_WAKE, false)
        set(v) = sp.edit().putBoolean(KEY_WAKE, v).apply()
    var wakePhrase: String
        get() = sp.getString(KEY_PHRASE, "jarvis") ?: "jarvis"
        set(v) = sp.edit().putString(KEY_PHRASE, v.trim().lowercase()).apply()
    var ttsRate: Float
        get() = sp.getFloat(KEY_RATE, 1.0f)
        set(v) = sp.edit().putFloat(KEY_RATE, v).apply()
    var announceNotifications: Boolean
        get() = sp.getBoolean(KEY_ANNOUNCE, false)
        set(v) = sp.edit().putBoolean(KEY_ANNOUNCE, v).apply()
    var quietStart: Int
        get() = sp.getInt(KEY_QS, 23)
        set(v) = sp.edit().putInt(KEY_QS, v).apply()
    var quietEnd: Int
        get() = sp.getInt(KEY_QE, 7)
        set(v) = sp.edit().putInt(KEY_QE, v).apply()

    fun inQuietHours(hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): Boolean {
        val s = quietStart; val e = quietEnd
        return if (s == e) false else if (s < e) hour in s until e else hour >= s || hour < e
    }

    companion object {
        const val DEFAULT_BASE = "https://openrouter.ai/api/v1"
        const val DEFAULT_MODEL = "google/gemini-2.5-flash"
        private const val KEY_BASE = "baseUrl", KEY_KEY = "apiKey", KEY_MODEL = "model"
        private const val KEY_WAKE = "wakeWordEnabled", KEY_PHRASE = "wakePhrase"
        private const val KEY_RATE = "ttsRate", KEY_ANNOUNCE = "announceNotifications"
        private const val KEY_QS = "quietStart", KEY_QE = "quietEnd"
        @Volatile private var inst: Prefs? = null
        fun init(ctx: Context) {
            if (inst == null) synchronized(Prefs::class.java) {
                if (inst == null) inst = Prefs(ctx.applicationContext.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE))
            }
        }
        fun get(ctx: Context): Prefs { init(ctx); return inst!! }
    }
}
