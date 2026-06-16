package com.sleepysoong.dottohome

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.TimeZone

// ── Pretendard Font Family ────────────────────────────────────────────────────

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

private const val TRACKING = -0.05f

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DotToHomeDashboard(isDark: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var config by remember { mutableStateOf(AppSettings.getConfig(context)) }
    var previewKey by remember { mutableIntStateOf(0) }
    var isApplying by remember { mutableStateOf(false) }

    val updateConfig: (AppConfig) -> Unit = { newConfig ->
        config = newConfig
        AppSettings.saveConfig(context, newConfig)
        previewKey++
        
        applyWallpaperDebounced(
            context = context,
            config = newConfig,
            coroutineScope = coroutineScope,
            onStart = { isApplying = true },
            onFinish = { isApplying = false }
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var pickingStartDate by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = lock, 1 = home

    val mainBackdrop = rememberLayerBackdrop()

    val todayKST = AppConfig.getTodayKST()
    val totalSpan = WallpaperGenerator.getDaysBetween(config.startDate, config.targetDate).coerceAtLeast(1)
    val remainingDays = WallpaperGenerator.getDaysBetween(todayKST, config.targetDate).coerceAtLeast(0)
    val elapsedFromStart = WallpaperGenerator.getDaysBetween(config.startDate, todayKST).coerceAtLeast(0)

    val (cols, rows, totalDots) = Triple(10, 10, 100)

    val elapsedDots = if (totalSpan > 0) {
        (elapsedFromStart.toFloat() / totalSpan * 100).toInt().coerceIn(0, 100)
    } else {
        100
    }
    val progressPercent = (elapsedFromStart.toFloat() / totalSpan.toFloat() * 100f).coerceIn(0f, 100f)

    val lockPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                copyUriToInternalStorage(context, uri, "wallpaper_lock_bg.jpg")
                withContext(Dispatchers.Main) {
                    updateConfig(config.copy(lockUseCustomImage = true))
                    Toast.makeText(context, "잠금화면 배경이 설정되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val homePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                copyUriToInternalStorage(context, uri, "wallpaper_home_bg.jpg")
                withContext(Dispatchers.Main) {
                    updateConfig(config.copy(homeUseCustomImage = true))
                    Toast.makeText(context, "홈화면 배경이 설정되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val subTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF999999)
    val dividerColor = if (isDark) Color(0xFF333333) else Color(0xFFE8E8E8)
    val textFieldBg = if (isDark) Color(0xFF222224) else Color(0xFFF0F0F2)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(mainBackdrop)
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) listOf(Color(0xFF101012), Color(0xFF0A0A0C), Color(0xFF101012))
                        else listOf(Color(0xFFF5F5F7), Color(0xFFEEEEF0), Color(0xFFF5F5F7))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 12.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "도트 투 홈",
                        fontSize = 26.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Pretendard,
                        letterSpacing = TRACKING.sp
                    )
                    Text(
                        text = "매일 업데이트되는 디데이 배경화면",
                        fontSize = 13.sp,
                        color = subTextColor,
                        fontWeight = FontWeight.Normal,
                        fontFamily = Pretendard,
                        letterSpacing = TRACKING.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = textColor,
                        strokeWidth = 2.dp
                    )
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard(backdrop = mainBackdrop, isDark = isDark) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                PretendardText("디데이", 12, subTextColor, FontWeight.Medium)
                                PretendardText(if (remainingDays == 0) "D-DAY" else "D-$remainingDays", 32, textColor, FontWeight.Bold)
                            }
                            PretendardText(String.format("%.1f%%", progressPercent), 20, textColor, FontWeight.Bold)
                        }
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                        DotGrid(cols, rows, totalDots, elapsedDots, isDark, config.dotShape, config.dotColor, Modifier.fillMaxWidth())
                    }
                }

                GlassCard(backdrop = mainBackdrop, isDark = isDark) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PretendardText("날짜 설정", 14, textColor, FontWeight.Bold)
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(textFieldBg)
                                    .clickable { pickingStartDate = true; showDatePicker = true }
                                    .padding(12.dp)
                            ) {
                                PretendardText("시작일", 11, subTextColor)
                                PretendardText(formatDateKorean(config.startDate), 14, textColor, FontWeight.Bold, Modifier.padding(top = 4.dp))
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(textFieldBg)
                                    .clickable { pickingStartDate = false; showDatePicker = true }
                                    .padding(12.dp)
                            ) {
                                PretendardText("목표일 (D-Day)", 11, subTextColor)
                                PretendardText(formatDateKorean(config.targetDate), 14, textColor, FontWeight.Bold, Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }

                GlassCard(backdrop = mainBackdrop, isDark = isDark) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PretendardText("도트 모양 설정", 14, textColor, FontWeight.Bold)
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DotShape.values().forEach { shape ->
                                val isSelected = config.dotShape == shape
                                val chipBg = if (isSelected) {
                                    if (isDark) Color.White else Color(0xFF1A1A1A)
                                } else {
                                    if (isDark) Color(0xFF222222) else Color(0xFFE8E8E8)
                                }
                                val chipText = if (isSelected) {
                                    if (isDark) Color.Black else Color.White
                                } else subTextColor

                                Box(
                                    modifier = Modifier
                                        .clip(Capsule())
                                        .background(chipBg)
                                        .clickable { updateConfig(config.copy(dotShape = shape)) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    PretendardText(shape.label, 12, chipText, if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                GlassCard(backdrop = mainBackdrop, isDark = isDark) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PretendardText("도트 색상 설정", 14, textColor, FontWeight.Bold)
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DotColor.values().forEach { colorOpt ->
                                val isSelected = config.dotColor == colorOpt
                                val colorVal = if (colorOpt == DotColor.ADAPTIVE) {
                                    if (isDark) Color.White else Color(0xFF1A1A1A)
                                } else {
                                    Color(android.graphics.Color.parseColor(colorOpt.hex))
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .drawBackdrop(
                                            backdrop = mainBackdrop, shape = { CircleShape },
                                            effects = { blur(radius = 4f.dp.toPx()); lens(refractionHeight = 1f.dp.toPx(), refractionAmount = 2f.dp.toPx(), chromaticAberration = false) },
                                            highlight = { Highlight(style = HighlightStyle.Default(color = Color.White.copy(alpha = 0.3f), angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                                            shadow = { Shadow(color = Color.Black.copy(alpha = 0.3f), radius = 4.dp, offset = DpOffset(0.dp, 2.dp)) },
                                            onDrawSurface = { drawRect(colorVal) }
                                        )
                                        .border(if (isSelected) 2.dp else 0.dp, if (isSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent, CircleShape)
                                        .clickable { updateConfig(config.copy(dotColor = colorOpt)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (colorOpt == DotColor.ADAPTIVE) {
                                        PretendardText("A", 14, if (isDark) Color.Black else Color.White, FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                GlassCard(backdrop = mainBackdrop, isDark = isDark) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PretendardText("배경화면 텍스트 설정", 14, textColor, FontWeight.Bold)
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                        var textValue by remember(config.customLabel) { mutableStateOf(config.customLabel) }
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { 
                                textValue = it
                                updateConfig(config.copy(customLabel = it))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontFamily = Pretendard, fontSize = 14.sp, color = textColor),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) Color.White else Color(0xFF1A1A1A),
                                unfocusedBorderColor = dividerColor,
                                focusedContainerColor = if (isDark) Color.White.copy(alpha=0.1f) else Color.White.copy(alpha=0.5f),
                                unfocusedContainerColor = if (isDark) Color.White.copy(alpha=0.05f) else Color.White.copy(alpha=0.3f),
                                cursorColor = textColor
                            )
                        )
                    }
                }

                // Segmented control and Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSegmentedControl(
                        options = listOf("잠금화면", "홈화면"),
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it },
                        backdrop = mainBackdrop,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )

                    val isCurrentEnabled = if (selectedTab == 0) config.lockEnabled else config.homeEnabled
                    GlassToggle(
                        checked = isCurrentEnabled,
                        onCheckedChange = { checked ->
                            val newConfig = if (selectedTab == 0) config.copy(lockEnabled = checked) else config.copy(homeEnabled = checked)
                            updateConfig(newConfig)
                        },
                        backdrop = mainBackdrop,
                        isDark = isDark
                    )
                }

                WallpaperPreviewSection(
                    isLockScreen = selectedTab == 0,
                    config = config,
                    previewKey = previewKey,
                    backdrop = mainBackdrop,
                    isDark = isDark,
                    cols = cols,
                    rows = rows,
                    totalDots = totalDots,
                    elapsedDays = elapsedDots,
                    remainingDays = remainingDays,
                    progressPercent = progressPercent,
                    onDotOffsetYChange = { newY ->
                        val newConfig = if (selectedTab == 0) config.copy(lockDotOffsetY = newY) else config.copy(homeDotOffsetY = newY)
                        updateConfig(newConfig)
                    },
                    onPickPhoto = {
                        val req = androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        if (selectedTab == 0) lockPhotoLauncher.launch(req) else homePhotoLauncher.launch(req)
                    },
                    onResetDefault = {
                        val newConfig = if (selectedTab == 0) config.copy(lockUseCustomImage = false) else config.copy(homeUseCustomImage = false)
                        updateConfig(newConfig)
                        Toast.makeText(context, "기본 배경으로 초기화되었습니다", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showDatePicker) {
        val initialDate = if (pickingStartDate) config.startDate else config.targetDate
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        val newConfig = if (pickingStartDate) config.copy(startDate = selected) else config.copy(targetDate = selected)
                        updateConfig(newConfig)
                    }
                    showDatePicker = false
                }) {
                    PretendardText("확인", 14, MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    PretendardText("취소", 14, MaterialTheme.colorScheme.primary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ── Glass UI Components ───────────────────────────────────────────────────────

@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val trackColor = if (isDark) Color(0xFF2A2A2C).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.4f)
    val selectionColor = if (isDark) Color(0xFF444444).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val unselectedTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF888888)

    Box(
        modifier = modifier
            .height(48.dp)
            .drawBackdrop(
                backdrop = backdrop, shape = { Capsule() },
                effects = { blur(radius = 12f.dp.toPx()); lens(refractionHeight = 3f.dp.toPx(), refractionAmount = 5f.dp.toPx(), chromaticAberration = false) },
                highlight = { Highlight(style = HighlightStyle.Default(color = Color.White.copy(alpha = 0.2f), angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                onDrawSurface = { drawRect(trackColor) }
            )
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { idx, label ->
                val isSelected = selectedIndex == idx
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .padding(4.dp)
                        .clip(Capsule())
                        .background(if (isSelected) selectionColor else Color.Transparent)
                        .clickable { onSelect(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    PretendardText(
                        text = label,
                        fontSize = 14,
                        color = if (isSelected) textColor else unselectedTextColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val trackColor = if (checked) {
        if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1A1A1A).copy(alpha = 0.8f)
    } else {
        if (isDark) Color(0xFF2A2A2C).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
    }
    val thumbColor = if (checked) {
        if (isDark) Color(0xFF1A1A1A) else Color.White
    } else {
        if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF888888).copy(alpha = 0.8f)
    }

    val animatedOffset by animateFloatAsState(targetValue = if (checked) 1f else 0f)

    BoxWithConstraints(
        modifier = modifier
            .width(64.dp)
            .height(48.dp)
            .clip(Capsule())
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) }
            .drawBackdrop(
                backdrop = backdrop, shape = { Capsule() },
                effects = { blur(radius = 8f.dp.toPx()); lens(refractionHeight = 2f.dp.toPx(), refractionAmount = 4f.dp.toPx(), chromaticAberration = false) },
                highlight = { Highlight(style = HighlightStyle.Default(color = Color.White.copy(alpha = 0.3f), angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                onDrawSurface = { drawRect(trackColor) }
            )
            .padding(6.dp)
    ) {
        val thumbRadius = 18.dp
        val maxOffset = maxWidth - (thumbRadius * 2)
        val offset = maxOffset * animatedOffset

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = offset)
                .size(thumbRadius * 2)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    backdrop: Backdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val trackSurface = if (isDark) Color(0xFF222222).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.5f)
    val fillSurface = if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFF1A1A1A).copy(alpha = 0.2f)
    val thumbSurface = if (isDark) Color(0xFFDDDDDD) else Color(0xFF1A1A1A).copy(alpha = 0.9f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newVal = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(newVal)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val newVal = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(newVal)
                }
            }
    ) {
        val width = maxWidth
        val thumbRadius = 16.dp
        val availableWidth = width - thumbRadius * 2
        val thumbOffset = availableWidth * value

        // Track
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(14.dp)
                .drawBackdrop(
                    backdrop = backdrop, shape = { Capsule() },
                    effects = { blur(radius = 6f.dp.toPx()); lens(refractionHeight = 2f.dp.toPx(), refractionAmount = 4f.dp.toPx(), chromaticAberration = false) },
                    highlight = { Highlight(style = HighlightStyle.Default(color = Color.White.copy(alpha = 0.3f), angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                    shadow = { Shadow(color = Color.Black.copy(alpha = 0.1f), radius = 3.dp, offset = DpOffset(0.dp, 2.dp)) },
                    onDrawSurface = { drawRect(trackSurface) }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(thumbOffset + thumbRadius)
                    .clip(Capsule())
                    .background(fillSurface)
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(thumbRadius * 2)
                .drawBackdrop(
                    backdrop = backdrop, shape = { CircleShape },
                    effects = { blur(radius = 8f.dp.toPx()); lens(refractionHeight = 4f.dp.toPx(), refractionAmount = 8f.dp.toPx(), chromaticAberration = false) },
                    highlight = { Highlight(style = HighlightStyle.Default(color = Color.White.copy(alpha = 0.5f), angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                    shadow = { Shadow(color = Color.Black.copy(alpha = 0.2f), radius = 5.dp, offset = DpOffset(0.dp, 3.dp)) },
                    onDrawSurface = { drawRect(thumbSurface) }
                )
        )
    }
}

@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = if (isDark) Color(0xFF2A2A2C).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.85f)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.06f)
    val highlightColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop, shape = { RoundedCornerShape(18.dp) },
                effects = { blur(radius = 12f.dp.toPx()); lens(refractionHeight = 5f.dp.toPx(), refractionAmount = 8f.dp.toPx(), chromaticAberration = false) },
                highlight = { Highlight(style = HighlightStyle.Default(color = highlightColor, angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                shadow = { Shadow(color = shadowColor, radius = 8.dp, offset = DpOffset(0.dp, 3.dp)) },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .padding(16.dp),
        content = content
    )
}

// ── Other Components ──────────────────────────────────────────────────────────

@Composable
fun DotGrid(
    cols: Int,
    rows: Int,
    totalDots: Int,
    elapsedDays: Int,
    isDark: Boolean,
    shape: DotShape,
    color: DotColor,
    modifier: Modifier = Modifier
) {
    val filledColor = if (color == DotColor.ADAPTIVE) {
        if (isDark) Color.White else Color(0xFF1A1A1A)
    } else {
        Color(android.graphics.Color.parseColor(color.hex))
    }
    val emptyColor = if (isDark) Color(0xFF444444) else Color(0xFFD8D8D8)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth.value
        val spacingRatio = 0.8f
        val dotDiameter = availableWidth / (cols + (cols - 1) * spacingRatio)
        val spacing = (dotDiameter * spacingRatio).dp
        val dotSize = dotDiameter.dp
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (r in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (c in 0 until cols) {
                        val idx = r * cols + c
                        if (idx < totalDots) {
                            val isElapsed = idx < elapsedDays
                            DotShapeView(
                                size = dotSize,
                                color = if (isElapsed) filledColor else emptyColor,
                                shape = shape
                            )
                        } else {
                            Box(modifier = Modifier.size(dotSize)) // placeholder
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DotShapeView(size: androidx.compose.ui.unit.Dp, color: Color, shape: DotShape) {
    Canvas(modifier = Modifier.size(size)) {
        val cx = size.toPx() / 2f
        val cy = size.toPx() / 2f
        val radius = size.toPx() / 2f
        
        when (shape) {
            DotShape.CIRCLE -> {
                drawCircle(color = color, radius = radius, center = Offset(cx, cy))
            }
            DotShape.SQUARE -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                    cornerRadius = CornerRadius(radius * 0.3f, radius * 0.3f)
                )
            }
            DotShape.DIAMOND -> {
                val path = Path().apply {
                    moveTo(cx, cy - radius)
                    lineTo(cx + radius, cy)
                    lineTo(cx, cy + radius)
                    lineTo(cx - radius, cy)
                    close()
                }
                drawPath(path, color = color)
            }
            DotShape.STAR -> {
                val path = Path().apply {
                    val innerRadius = radius * 0.4f
                    val angles = 5
                    val startAngle = -Math.PI / 2
                    for (i in 0 until angles * 2) {
                        val r = if (i % 2 == 0) radius else innerRadius
                        val angle = startAngle + i * Math.PI / angles
                        val px = cx + (r * Math.cos(angle)).toFloat()
                        val py = cy + (r * Math.sin(angle)).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(path, color = color)
            }
            DotShape.HEART -> {
                val path = Path().apply {
                    val topY = cy - radius * 0.5f
                    val bottomY = cy + radius
                    moveTo(cx, topY + radius * 0.3f)
                    cubicTo(cx - radius * 1.2f, cy - radius * 1.2f, cx - radius * 1.2f, cy + radius * 0.2f, cx, bottomY)
                    cubicTo(cx + radius * 1.2f, cy + radius * 0.2f, cx + radius * 1.2f, cy - radius * 1.2f, cx, topY + radius * 0.3f)
                    close()
                }
                drawPath(path, color = color)
            }
        }
    }
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

@Composable
fun WallpaperPreviewSection(
    isLockScreen: Boolean,
    config: AppConfig,
    previewKey: Int,
    backdrop: Backdrop,
    isDark: Boolean,
    cols: Int,
    rows: Int,
    totalDots: Int,
    elapsedDays: Int,
    remainingDays: Int,
    progressPercent: Float,
    onDotOffsetYChange: (Float) -> Unit,
    onPickPhoto: () -> Unit,
    onResetDefault: () -> Unit
) {
    val context = LocalContext.current
    val screenLabel = if (isLockScreen) "잠금화면" else "홈화면"
    val isEnabled = if (isLockScreen) config.lockEnabled else config.homeEnabled
    val useCustom = if (isLockScreen) config.lockUseCustomImage else config.homeUseCustomImage
    val bgFile = if (isLockScreen) "wallpaper_lock_bg.jpg" else "wallpaper_home_bg.jpg"
    val dotOffsetY = if (isLockScreen) config.lockDotOffsetY else config.homeDotOffsetY

    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val subTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF999999)
    val dividerColor = if (isDark) Color(0xFF333333) else Color(0xFFE8E8E8)

    val previewAlpha = if (isEnabled) 1f else 0.4f

    PretendardText(
        text = "$screenLabel 미리보기" + if (!isEnabled) " (비활성화됨)" else "",
        fontSize = 13,
        fontWeight = FontWeight.Bold,
        color = subTextColor,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp, max = 600.dp)
            .aspectRatio(9f / 16f, matchHeightConstraintsFirst = false)
            .clip(RoundedCornerShape(24.dp))
    ) {
        val previewBackdrop = rememberLayerBackdrop()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(previewBackdrop)
        ) {
            if (useCustom) {
                val filePath = remember(bgFile) { File(context.filesDir, bgFile).absolutePath }
                val bitmap = remember(filePath, previewKey) {
                    try { BitmapFactory.decodeFile(filePath) } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().alpha(previewAlpha),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(if (isDark) Color.Black else Color.White).alpha(previewAlpha))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(if (isDark) Color.Black else Color.White).alpha(previewAlpha))
            }
        }

        if (isEnabled) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxOffset = maxHeight * 0.5f 
                val actualOffset = maxOffset * dotOffsetY

                val cardSurface = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)
                val cardHighlight = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f)

                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .widthIn(min = maxWidth * 0.6f, max = maxWidth * 0.88f)
                        .align(Alignment.TopCenter)
                        .offset(y = actualOffset)
                        .drawBackdrop(
                            backdrop = previewBackdrop, shape = { RoundedCornerShape(20.dp) },
                            effects = { blur(radius = 14f.dp.toPx()); lens(refractionHeight = 6f.dp.toPx(), refractionAmount = 10f.dp.toPx(), chromaticAberration = false) },
                            highlight = { Highlight(style = HighlightStyle.Default(color = cardHighlight, angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                            shadow = { Shadow(color = Color.Black.copy(alpha = 0.08f), radius = 8.dp, offset = DpOffset(0.dp, 4.dp)) },
                            onDrawSurface = { drawRect(cardSurface) }
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                PretendardText(config.customLabel, 9, subTextColor, FontWeight.Medium)
                                PretendardText(if (remainingDays == 0) "D-DAY" else "D-$remainingDays", 20, textColor, FontWeight.Bold)
                            }
                            PretendardText(String.format("%.1f%%", progressPercent), 14, textColor, FontWeight.Bold)
                        }

                        HorizontalDivider(color = dividerColor, thickness = 1.dp)

                        DotGrid(cols, rows, totalDots, elapsedDays, isDark, config.dotShape, config.dotColor, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    GlassCard(backdrop = backdrop, isDark = isDark) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PretendardText("도트 위치 조절", 13, textColor, FontWeight.Medium)
                PretendardText("${(dotOffsetY * 100).toInt()}%", 13, subTextColor, FontWeight.Bold)
            }
            GlassSlider(value = dotOffsetY, onValueChange = onDotOffsetYChange, backdrop = backdrop, isDark = isDark)
        }
    }

    GlassCard(backdrop = backdrop, isDark = isDark) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PretendardText("$screenLabel 배경 이미지", 14, textColor, FontWeight.Bold)
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPickPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if (isDark) Color.Black else Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    PretendardText("사진 선택", 13, if (isDark) Color.Black else Color.White)
                }

                OutlinedButton(
                    onClick = onResetDefault,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                ) {
                    PretendardText("기본 배경", 13, textColor)
                }
            }
            PretendardText(if (useCustom) "사용자 지정 이미지 사용 중" else "기본 배경 사용 중", 11, subTextColor)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
}

private fun formatDateKorean(millis: Long): String {
    val kst = TimeZone.getTimeZone("Asia/Seoul")
    val cal = Calendar.getInstance(kst).apply { timeInMillis = millis }
    return String.format("%d년 %d월 %d일", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}
