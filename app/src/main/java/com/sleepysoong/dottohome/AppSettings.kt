package com.sleepysoong.dottohome

import android.content.Context
import com.google.gson.Gson
import java.util.Calendar
import java.util.TimeZone

data class AppConfig(
    // Start date (editable, defaults to today KST)
    val startDate: Long = getTodayKST(),
    // Target D-Day date
    val targetDate: Long = getDefaultTargetDate(),

    // Lock screen settings
    val lockUseCustomImage: Boolean = false,
    val lockDotOffsetY: Float = 0.55f, // 0..1 percentage from top

    // Home screen settings
    val homeUseCustomImage: Boolean = false,
    val homeDotOffsetY: Float = 0.55f,

    // Custom label text for the wallpaper card
    val customLabel: String = "디데이 진행률"
) {
    companion object {
        fun getTodayKST(): Long {
            val kst = TimeZone.getTimeZone("Asia/Seoul")
            val cal = Calendar.getInstance(kst)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getDefaultTargetDate(): Long {
            val kst = TimeZone.getTimeZone("Asia/Seoul")
            val cal = Calendar.getInstance(kst)
            cal.add(Calendar.DAY_OF_YEAR, 100)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}

object AppSettings {
    private const val PREFS_NAME = "dot_to_home_prefs"
    private const val KEY_CONFIG = "app_config_v3"
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
