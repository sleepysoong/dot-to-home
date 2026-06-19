package com.sleepysoong.dottohome

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sleepysoong.dottohome.ui.screens.DotToHomeDashboard
import com.sleepysoong.dottohome.util.Pretendard
import com.sleepysoong.dottohome.util.TRACKING

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        WallpaperWorker.scheduleDailyUpdate(this)

        setContent {
            val isDark = isSystemInDarkTheme()
            val defaultTypography = Typography(
                displayLarge = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                displayMedium = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                displaySmall = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                headlineLarge = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                headlineMedium = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                headlineSmall = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                titleLarge = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                titleMedium = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                titleSmall = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                bodyLarge = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                bodyMedium = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                bodySmall = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                labelLarge = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                labelMedium = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp),
                labelSmall = TextStyle(fontFamily = Pretendard, letterSpacing = TRACKING.sp)
            )

            MaterialTheme(
                colorScheme = if (isDark) darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    surface = Color(0xFF141416),
                    onSurface = Color.White,
                    background = Color(0xFF141416),
                    onBackground = Color.White
                ) else lightColorScheme(
                    primary = Color(0xFF1A1A1A),
                    onPrimary = Color.White,
                    surface = Color(0xFFF5F5F7),
                    onSurface = Color(0xFF1A1A1A),
                    background = Color(0xFFF5F5F7),
                    onBackground = Color(0xFF1A1A1A)
                ),
                typography = defaultTypography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DotToHomeDashboard(isDark = isDark)
                }
            }
        }
    }
}
