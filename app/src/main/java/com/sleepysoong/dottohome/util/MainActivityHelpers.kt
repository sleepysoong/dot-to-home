package com.sleepysoong.dottohome.util

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sleepysoong.dottohome.AppConfig
import com.sleepysoong.dottohome.R
import com.sleepysoong.dottohome.WallpaperGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

const val TRACKING = -0.05f

private var applyJob: Job? = null

fun applyWallpaperDebounced(
    context: Context,
    config: AppConfig,
    coroutineScope: CoroutineScope,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    applyJob?.cancel()
    applyJob = coroutineScope.launch(Dispatchers.IO) {
        delay(800)
        withContext(Dispatchers.Main) { onStart() }
        try {
            val wm = WallpaperManager.getInstance(context)
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (config.lockEnabled) {
                    val lockBitmap = WallpaperGenerator.generate(context, width, height, isLockScreen = true)
                    wm.setBitmap(lockBitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
                if (config.homeEnabled) {
                    val homeBitmap = WallpaperGenerator.generate(context, width, height, isLockScreen = false)
                    wm.setBitmap(homeBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                }
            } else {
                if (config.homeEnabled) {
                    val homeBitmap = WallpaperGenerator.generate(context, width, height, isLockScreen = false)
                    wm.setBitmap(homeBitmap)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "배경화면이 적용되었습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "배경화면 적용 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            withContext(Dispatchers.Main) { onFinish() }
        }
    }
}

fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
}

fun formatDateKorean(millis: Long): String {
    val kst = TimeZone.getTimeZone("Asia/Seoul")
    val cal = Calendar.getInstance(kst).apply { timeInMillis = millis }
    return String.format(
        "%d년 %d월 %d일",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

@Composable
fun PretendardText(
    text: String,
    fontSize: Int,
    color: Color = Color(0xFF1A1A1A),
    fontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize.sp,
        color = color,
        fontWeight = fontWeight,
        fontFamily = Pretendard,
        letterSpacing = TRACKING.sp,
        modifier = modifier
    )
}
