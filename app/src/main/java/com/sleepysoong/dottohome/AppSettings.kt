package com.sleepysoong.dottohome

import android.content.Context
import android.graphics.Color
import com.google.gson.Gson
import java.util.Calendar

data class AppConfig(
    val startDate: Long = System.currentTimeMillis(),
    val targetDate: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000, // Default 30 days
    val useCustomImage: Boolean = false,
    val dotColor: Int = Color.parseColor("#00FFCC"), // Default Neon Teal
    val dotShape: String = "circle", // "circle" or "square"
    val autoUpdate: Boolean = false,
    val refractionHeight: Float = 10f,
    val refractionAmount: Float = 20f,
    val chromaticAberration: Boolean = true
)

object AppSettings {
    private const val PREFS_NAME = "dot_to_home_prefs"
    private const val KEY_CONFIG = "app_config"
    private val gson = Gson()

    fun getConfig(context: Context): AppConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CONFIG, null) ?: return AppConfig()
        return try {
            gson.fromJson(json, AppConfig::class.java) ?: AppConfig()
        } catch (e: Exception) {
            AppConfig()
        }
    }

    fun saveConfig(context: Context, config: AppConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(config)
        prefs.edit().putString(KEY_CONFIG, json).apply()
    }
}
