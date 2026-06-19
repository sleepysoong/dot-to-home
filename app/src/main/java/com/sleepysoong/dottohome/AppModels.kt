package com.sleepysoong.dottohome

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

// ── Enums ──────────────────────────────────────────────────────────────────────

enum class DotShape(val label: String) {
    CIRCLE("원"),
    SQUARE("네모"),
    HEART("하트"),
    STAR("별"),
    DIAMOND("다이아")
}

enum class DotColor(val label: String, val hex: String) {
    ADAPTIVE("어댑티브", "#000000"),
    WHITE("흰색", "#FFFFFF"),
    BLACK("검정", "#1A1A1A"),
    RED("빨강", "#FF4757"),
    ORANGE("주황", "#FF6B35"),
    YELLOW("노랑", "#FFD700"),
    GREEN("초록", "#2ED573"),
    BLUE("파랑", "#1E90FF"),
    PURPLE("보라", "#A855F7"),
    PINK("핑크", "#FF69B4")
}

// ── DDay Item (one card on the wallpaper) ────────────────────────────────────

data class DDayItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "디데이 진행률",
    val startDate: Long = System.currentTimeMillis(),
    val targetDate: Long = System.currentTimeMillis() + 100L * 24 * 60 * 60 * 1000,
    val dotShape: DotShape = DotShape.CIRCLE,
    val dotColor: DotColor = DotColor.ADAPTIVE
)

// ── AppConfig ─────────────────────────────────────────────────────────────────

data class AppConfig(
    // Global wallpaper settings
    val lockEnabled: Boolean = true,
    val homeEnabled: Boolean = true,
    val lockUseCustomImage: Boolean = false,
    val homeUseCustomImage: Boolean = false,
    val lockDotOffsetY: Float = 0.15f,
    val homeDotOffsetY: Float = 0.15f,
    // Multiple D-Day items
    val ddayItems: List<DDayItem> = listOf(DDayItem())
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
    }
}

// ── AppSettings ───────────────────────────────────────────────────────────────

object AppSettings {
    private const val PREFS_NAME = "dot_to_home_prefs"
    private const val KEY_CONFIG = "app_config"
    private const val KEY_LAST_UPDATE = "last_update_date"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfig(context: Context): AppConfig {
        val json = prefs(context).getString(KEY_CONFIG, null) ?: return AppConfig()
        return try {
            val raw = Gson().fromJson(json, RawAppConfig::class.java)
            migrateFromRaw(raw)
        } catch (e: Exception) {
            AppConfig()
        }
    }

    fun saveConfig(context: Context, config: AppConfig) {
        val json = Gson().toJson(config)
        prefs(context).edit().putString(KEY_CONFIG, json).apply()
    }

    fun getLastUpdateDate(context: Context): Long =
        prefs(context).getLong(KEY_LAST_UPDATE, 0L)

    fun saveLastUpdateDate(context: Context, date: Long) {
        prefs(context).edit().putLong(KEY_LAST_UPDATE, date).apply()
    }

    // Migration: if the saved JSON has old fields (startDate, targetDate, etc.),
    // convert to the new ddayItems format
    private fun migrateFromRaw(raw: RawAppConfig): AppConfig {
        val items = if (raw.ddayItems != null && raw.ddayItems.isNotEmpty()) {
            raw.ddayItems
        } else {
            // Legacy: single item from top-level fields
            listOf(
                DDayItem(
                    label = raw.customLabel ?: "디데이 진행률",
                    startDate = raw.startDate ?: System.currentTimeMillis(),
                    targetDate = raw.targetDate
                        ?: (System.currentTimeMillis() + 100L * 24 * 60 * 60 * 1000),
                    dotShape = raw.dotShape ?: DotShape.CIRCLE,
                    dotColor = raw.dotColor ?: DotColor.ADAPTIVE
                )
            )
        }
        return AppConfig(
            lockEnabled = raw.lockEnabled ?: true,
            homeEnabled = raw.homeEnabled ?: true,
            lockUseCustomImage = raw.lockUseCustomImage ?: false,
            homeUseCustomImage = raw.homeUseCustomImage ?: false,
            lockDotOffsetY = raw.lockDotOffsetY ?: 0.15f,
            homeDotOffsetY = raw.homeDotOffsetY ?: 0.15f,
            ddayItems = items
        )
    }

    // Internal class for flexible deserialization (handles old AND new format)
    private data class RawAppConfig(
        val lockEnabled: Boolean? = null,
        val homeEnabled: Boolean? = null,
        val lockUseCustomImage: Boolean? = null,
        val homeUseCustomImage: Boolean? = null,
        val lockDotOffsetY: Float? = null,
        val homeDotOffsetY: Float? = null,
        // New format
        val ddayItems: List<DDayItem>? = null,
        // Legacy fields
        val customLabel: String? = null,
        val startDate: Long? = null,
        val targetDate: Long? = null,
        val dotShape: DotShape? = null,
        val dotColor: DotColor? = null
    )
}

// ── GridCalculator (kept for compatibility) ────────────────────────────────────

enum class DotGridType(val label: String) {
    TEN_TEN("10x10")
}

object GridCalculator {
    fun calculate(
        type: DotGridType = DotGridType.TEN_TEN,
        totalDays: Int = 100
    ): Triple<Int, Int, Int> {
        return Triple(10, 10, 100)
    }
}
