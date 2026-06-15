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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// -5% letter spacing = -0.05 em
private const val TRACKING = -0.05f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 자동 업데이트 강제 스케줄링
        WallpaperWorker.scheduleDailyUpdate(this)

        setContent {
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
                colorScheme = lightColorScheme(
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
                    color = Color(0xFFF5F5F7)
                ) {
                    DotToHomeDashboard()
                }
            }
        }
    }
}

// Global debouncer for wallpaper applying
private var applyJob: Job? = null

fun applyWallpaperDebounced(
    context: Context,
    coroutineScope: CoroutineScope,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    applyJob?.cancel()
    applyJob = coroutineScope.launch(Dispatchers.IO) {
        delay(800) // 800ms debounce
        withContext(Dispatchers.Main) { onStart() }
        try {
            val wm = WallpaperManager.getInstance(context)
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            val lockBitmap = WallpaperGenerator.generate(context, width, height, isLockScreen = true)
            val homeBitmap = WallpaperGenerator.generate(context, width, height, isLockScreen = false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(lockBitmap, null, true, WallpaperManager.FLAG_LOCK)
                wm.setBitmap(homeBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            } else {
                wm.setBitmap(homeBitmap)
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
fun DotToHomeDashboard() {
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
            coroutineScope = coroutineScope,
            onStart = { isApplying = true },
            onFinish = { isApplying = false }
        )
    }

    // Date picker states
    var showDatePicker by remember { mutableStateOf(false) }
    var pickingStartDate by remember { mutableStateOf(false) }

    // Tab for lock/home
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = lock, 1 = home

    val mainBackdrop = rememberLayerBackdrop()

    // D-Day calculations
    val todayKST = AppConfig.getTodayKST()
    val totalDays = 100
    val totalSpan = WallpaperGenerator.getDaysBetween(config.startDate, config.targetDate).coerceAtLeast(1)
    val remainingDays = WallpaperGenerator.getDaysBetween(todayKST, config.targetDate).coerceAtLeast(0)
    val elapsedFromStart = WallpaperGenerator.getDaysBetween(config.startDate, todayKST).coerceAtLeast(0)
    val elapsedDots = if (totalSpan >= totalDays) {
        (elapsedFromStart.toFloat() / totalSpan * totalDays).toInt().coerceIn(0, totalDays)
    } else {
        elapsedFromStart.coerceIn(0, totalDays)
    }
    val progressPercent = (elapsedDots.toFloat() / totalDays.toFloat() * 100f).coerceIn(0f, 100f)

    // Photo pickers
    val lockPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
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
    )

    val homePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
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
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Light subtle background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(mainBackdrop)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F5F7),
                            Color(0xFFEEEEF0),
                            Color(0xFFF5F5F7)
                        )
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
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.Bold,
                        fontFamily = Pretendard,
                        letterSpacing = TRACKING.sp
                    )
                    Text(
                        text = "매일 업데이트되는 디데이 배경화면",
                        fontSize = 13.sp,
                        color = Color(0xFF999999),
                        fontWeight = FontWeight.Normal,
                        fontFamily = Pretendard,
                        letterSpacing = TRACKING.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1A1A1A),
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
                // D-Day summary card
                GlassCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                PretendardText(
                                    text = "디데이",
                                    fontSize = 12,
                                    color = Color(0xFF999999),
                                    fontWeight = FontWeight.Medium
                                )
                                PretendardText(
                                    text = if (remainingDays == 0) "D-DAY" else "D-$remainingDays",
                                    fontSize = 32,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                            PretendardText(
                                text = String.format("%.1f%%", progressPercent),
                                fontSize = 20,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                        DotGrid(
                            elapsedDays = elapsedDots,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Date settings
                GlassCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PretendardText(
                            text = "날짜 설정",
                            fontSize = 14,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start date (editable)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF0F0F2))
                                    .clickable {
                                        pickingStartDate = true
                                        showDatePicker = true
                                    }
                                    .padding(12.dp)
                            ) {
                                PretendardText("시작일", fontSize = 11, color = Color(0xFF999999))
                                PretendardText(
                                    formatDateKorean(config.startDate),
                                    fontSize = 14,
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Target date (editable)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF0F0F2))
                                    .clickable {
                                        pickingStartDate = false
                                        showDatePicker = true
                                    }
                                    .padding(12.dp)
                            ) {
                                PretendardText("목표일 (D-Day)", fontSize = 11, color = Color(0xFF999999))
                                PretendardText(
                                    formatDateKorean(config.targetDate),
                                    fontSize = 14,
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Label setting
                GlassCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PretendardText(
                            text = "배경화면 텍스트 설정",
                            fontSize = 14,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                        var textValue by remember(config.customLabel) { mutableStateOf(config.customLabel) }

                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { 
                                textValue = it
                                updateConfig(config.copy(customLabel = it))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontFamily = Pretendard, fontSize = 14.sp, color = Color(0xFF1A1A1A)),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1A1A1A),
                                unfocusedBorderColor = Color(0xFFD0D0D0),
                                focusedContainerColor = Color.White.copy(alpha = 0.5f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = Color(0xFF1A1A1A)
                            )
                        )
                    }
                }

                // Tab: Lock / Home
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8E8EA)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("잠금화면" to 0, "홈화면" to 1).forEach { (label, idx) ->
                        val isSelected = selectedTab == idx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { selectedTab = idx }
                                .padding(vertical = 10.dp)
                        ) {
                            PretendardText(
                                text = label,
                                fontSize = 14,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF999999)
                            )
                        }
                    }
                }

                // Preview for selected screen
                WallpaperPreviewSection(
                    isLockScreen = selectedTab == 0,
                    config = config,
                    previewKey = previewKey,
                    backdrop = mainBackdrop,
                    elapsedDays = elapsedDots,
                    remainingDays = remainingDays,
                    progressPercent = progressPercent,
                    onDotOffsetYChange = { newY ->
                        val newConfig = if (selectedTab == 0) {
                            config.copy(lockDotOffsetY = newY)
                        } else {
                            config.copy(homeDotOffsetY = newY)
                        }
                        updateConfig(newConfig)
                    },
                    onPickPhoto = {
                        val request = androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                        if (selectedTab == 0) lockPhotoLauncher.launch(request)
                        else homePhotoLauncher.launch(request)
                    },
                    onResetDefault = {
                        val newConfig = if (selectedTab == 0) {
                            config.copy(lockUseCustomImage = false)
                        } else {
                            config.copy(homeUseCustomImage = false)
                        }
                        updateConfig(newConfig)
                        Toast.makeText(context, "기본 배경으로 초기화되었습니다", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val initialDate = if (pickingStartDate) config.startDate else config.targetDate
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        val newConfig = if (pickingStartDate) {
                            config.copy(startDate = selected)
                        } else {
                            config.copy(targetDate = selected)
                        }
                        updateConfig(newConfig)
                    }
                    showDatePicker = false
                }) {
                    PretendardText("확인", fontSize = 14, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    PretendardText("취소", fontSize = 14, color = MaterialTheme.colorScheme.primary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ── Glass Slider Component ────────────────────────────────────────────────────

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
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
                .height(10.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        blur(radius = 6f.dp.toPx())
                        lens(
                            refractionHeight = 2f.dp.toPx(),
                            refractionAmount = 4f.dp.toPx(),
                            chromaticAberration = false
                        )
                    },
                    highlight = {
                        Highlight(
                            style = HighlightStyle.Default(
                                color = Color.White.copy(alpha = 0.4f),
                                angle = -45f
                            ),
                            width = 1.dp,
                            blurRadius = 0.5.dp
                        )
                    },
                    shadow = {
                        Shadow(
                            color = Color.Black.copy(alpha = 0.05f),
                            radius = 2.dp,
                            offset = DpOffset(0.dp, 1.dp)
                        )
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.5f))
                    }
                )
        ) {
            // Fill progress
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(thumbOffset + thumbRadius)
                    .clip(Capsule())
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.15f))
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(thumbRadius * 2)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        blur(radius = 8f.dp.toPx())
                        lens(
                            refractionHeight = 4f.dp.toPx(),
                            refractionAmount = 8f.dp.toPx(),
                            chromaticAberration = false
                        )
                    },
                    highlight = {
                        Highlight(
                            style = HighlightStyle.Default(
                                color = Color.White.copy(alpha = 0.8f),
                                angle = -45f
                            ),
                            width = 1.dp,
                            blurRadius = 0.5.dp
                        )
                    },
                    shadow = {
                        Shadow(
                            color = Color.Black.copy(alpha = 0.15f),
                            radius = 4.dp,
                            offset = DpOffset(0.dp, 2.dp)
                        )
                    },
                    onDrawSurface = {
                        drawRect(Color(0xFF1A1A1A).copy(alpha = 0.9f))
                    }
                )
        )
    }
}

// ── Reusable Pretendard Text ──────────────────────────────────────────────────

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

// ── Preview section for each screen type ──────────────────────────────────────

@Composable
fun WallpaperPreviewSection(
    isLockScreen: Boolean,
    config: AppConfig,
    previewKey: Int,
    backdrop: Backdrop,
    elapsedDays: Int,
    remainingDays: Int,
    progressPercent: Float,
    onDotOffsetYChange: (Float) -> Unit,
    onPickPhoto: () -> Unit,
    onResetDefault: () -> Unit
) {
    val context = LocalContext.current
    val screenLabel = if (isLockScreen) "잠금화면" else "홈화면"
    val useCustom = if (isLockScreen) config.lockUseCustomImage else config.homeUseCustomImage
    val bgFile = if (isLockScreen) "wallpaper_lock_bg.jpg" else "wallpaper_home_bg.jpg"
    val dotOffsetY = if (isLockScreen) config.lockDotOffsetY else config.homeDotOffsetY

    PretendardText(
        text = "$screenLabel 미리보기",
        fontSize = 13,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF666666),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )

    // Preview box
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
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
                    try {
                        BitmapFactory.decodeFile(filePath)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cardHeight = 280.dp
            val maxOffset = maxHeight - cardHeight
            val actualOffset = maxOffset * dotOffsetY

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.TopCenter)
                    .offset(y = actualOffset)
                    .drawBackdrop(
                        backdrop = previewBackdrop,
                        shape = { RoundedCornerShape(20.dp) },
                        effects = {
                            blur(radius = 14f.dp.toPx())
                            lens(
                                refractionHeight = 6f.dp.toPx(),
                                refractionAmount = 10f.dp.toPx(),
                                chromaticAberration = false
                            )
                        },
                        highlight = {
                            Highlight(
                                style = HighlightStyle.Default(
                                    color = Color.White.copy(alpha = 0.4f),
                                    angle = -45f
                                ),
                                width = 1.dp,
                                blurRadius = 0.5.dp
                            )
                        },
                        shadow = {
                            Shadow(
                                color = Color.Black.copy(alpha = 0.08f),
                                radius = 8.dp,
                                offset = DpOffset(0.dp, 4.dp)
                            )
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.7f))
                        }
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
                            PretendardText(
                                text = config.customLabel,
                                fontSize = 9,
                                color = Color(0xFF999999),
                                fontWeight = FontWeight.Medium
                            )
                            PretendardText(
                                text = if (remainingDays == 0) "D-DAY" else "D-$remainingDays",
                                fontSize = 20,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                        PretendardText(
                            text = String.format("%.1f%%", progressPercent),
                            fontSize = 14,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                    DotGrid(
                        elapsedDays = elapsedDays,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Position slider
    GlassCard(backdrop = backdrop) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PretendardText("도트 위치 조절", fontSize = 13, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium)
                PretendardText("${(dotOffsetY * 100).toInt()}%", fontSize = 13, color = Color(0xFF999999), fontWeight = FontWeight.Bold)
            }
            GlassSlider(
                value = dotOffsetY,
                onValueChange = onDotOffsetYChange,
                backdrop = backdrop
            )
        }
    }

    // Background image controls
    GlassCard(backdrop = backdrop) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PretendardText(
                text = "$screenLabel 배경 이미지",
                fontSize = 14,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickPhoto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A1A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    PretendardText("사진 선택", fontSize = 13, color = Color.White)
                }

                OutlinedButton(
                    onClick = onResetDefault,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1A1A1A)
                    )
                ) {
                    PretendardText("기본 배경", fontSize = 13, color = Color(0xFF1A1A1A))
                }
            }

            PretendardText(
                text = if (useCustom) "사용자 지정 이미지 사용 중" else "기본 흰색 배경 사용 중",
                fontSize = 11,
                color = Color(0xFF999999)
            )
        }
    }
}

// ── Dot Grid (always 10×10 = 100) ─────────────────────────────────────────────

@Composable
fun DotGrid(
    elapsedDays: Int,
    modifier: Modifier = Modifier
) {
    val cols = 10

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (r in 0 until 10) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (c in 0 until cols) {
                    val idx = r * cols + c
                    val isElapsed = idx < elapsedDays
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                color = if (isElapsed) Color(0xFF1A1A1A) else Color(0xFFD8D8D8),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

// ── Glass Card Component ──────────────────────────────────────────────────────

@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(18.dp) },
                effects = {
                    blur(radius = 12f.dp.toPx())
                    lens(
                        refractionHeight = 5f.dp.toPx(),
                        refractionAmount = 8f.dp.toPx(),
                        chromaticAberration = false
                    )
                },
                highlight = {
                    Highlight(
                        style = HighlightStyle.Default(
                            color = Color.White.copy(alpha = 0.5f),
                            angle = -45f
                        ),
                        width = 1.dp,
                        blurRadius = 0.5.dp
                    )
                },
                shadow = {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.06f),
                        radius = 8.dp,
                        offset = DpOffset(0.dp, 3.dp)
                    )
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.85f))
                }
            )
            .padding(16.dp),
        content = content
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatDateKorean(millis: Long): String {
    val kst = TimeZone.getTimeZone("Asia/Seoul")
    val cal = Calendar.getInstance(kst).apply { timeInMillis = millis }
    return String.format(
        "%d년 %d월 %d일",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}
