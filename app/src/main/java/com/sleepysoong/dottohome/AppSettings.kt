package com.sleepysoong.dottohome

import android.content.Context
import com.google.gson.Gson
import java.util.Calendar
import java.util.TimeZone

enum class DotShape(val label: String) {
    CIRCLE("원"),
    HEART("하트"),
    SQUARE("네모"),
    STAR("별"),
    DIAMOND("다이아몬드")
}

enum class DotColor(val label: String, val hex: String) {
    ADAPTIVE("기본 (적응형)", ""),
    BLACK("블랙", "#1A1A1A"),
    WHITE("화이트", "#FFFFFF"),
    RED("레드", "#FF5252"),
    PINK("핑크", "#FF4081"),
    PURPLE("퍼플", "#7C4DFF"),
    BLUE("블루", "#448AFF"),
    GREEN("그린", "#69F0AE"),
    YELLOW("옐로우", "#FFD740"),
    ORANGE("오렌지", "#FFAB40")
}

enum class DotGridType(val label: String) {
    GRID_10X10("10 × 10 (100개)"),
    GRID_14X7("14 × 7 (98개)"),
    GRID_20X5("20 × 5 (가로형)"),
    GRID_5X20("5 × 20 (세로형)"),
    AUTO_MATCH_DAYS("목표일에 맞춤 (자동)")
}

object GridCalculator {
    fun calculate(type: DotGridType, totalSpan: Int): Triple<Int, Int, Int> { // cols, rows, totalDots
        return when (type) {
            DotGridType.GRID_10X10 -> Triple(10, 10, 100)
            DotGridType.GRID_14X7 -> Triple(14, 7, 98)
            DotGridType.GRID_20X5 -> Triple(20, 5, 100)
            DotGridType.GRID_5X20 -> Triple(5, 20, 100)
            DotGridType.AUTO_MATCH_DAYS -> {
                val dots = totalSpan.coerceAtLeast(1)
                var cols = Math.ceil(Math.sqrt(dots.toDouble())).toInt()
                if (cols > 20) cols = 20
                if (cols < 5) cols = 5
                val rows = Math.ceil(dots.toDouble() / cols.toDouble()).toInt()
                Triple(cols, rows, dots)
            }
        }
    }
}

data class AppConfig(
    // Start date (editable, defaults to today KST)
    val startDate: Long = getTodayKST(),
    // Target D-Day date
    val targetDate: Long = getDefaultTargetDate(),

    // Lock screen settings
    val lockEnabled: Boolean = true,
    val lockUseCustomImage: Boolean = false,
    val lockDotOffsetY: Float = 0.55f, // 0..1 percentage from top

    // Home screen settings
    val homeEnabled: Boolean = true,
    val homeUseCustomImage: Boolean = false,
    val homeDotOffsetY: Float = 0.55f,

    // Custom label text for the wallpaper card
    val customLabel: String = "디데이 진행률",

    // Dot format
    val dotGridType: DotGridType = DotGridType.GRID_10X10,
    val dotShape: DotShape = DotShape.CIRCLE,
    val dotColor: DotColor = DotColor.ADAPTIVE
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
