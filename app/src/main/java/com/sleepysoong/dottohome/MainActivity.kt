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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1A1A1A),
                    onPrimary = Color.White,
                    surface = Color(0xFFF5F5F7),
                    onSurface = Color(0xFF1A1A1A),
                    background = Color(0xFFF5F5F7),
                    onBackground = Color(0xFF1A1A1A)
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DotToHomeDashboard() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var config by remember { mutableStateOf(AppSettings.getConfig(context)) }
    var previewKey by remember { mutableIntStateOf(0) }
    var isApplying by remember { mutableStateOf(false) }

    // Date picker
    var showDatePicker by remember { mutableStateOf(false) }

    // Tab for lock/home
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = lock, 1 = home

    // Backdrop for glass effects
    val mainBackdrop = rememberLayerBackdrop()

    // D-Day calculations (start = today KST)
    val todayKST = AppSettings.getTodayKST()
    val totalDays = 100 // Always 100
    val remainingDays = WallpaperGenerator.getDaysBetween(todayKST, config.targetDate).coerceAtLeast(0)
    val elapsedDays = (totalDays - remainingDays).coerceIn(0, totalDays)
    val progressPercent = (elapsedDays.toFloat() / totalDays.toFloat() * 100f).coerceIn(0f, 100f)

    // Photo pickers for lock/home
    val lockPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    copyUriToInternalStorage(context, uri, "wallpaper_lock_bg.jpg")
                    withContext(Dispatchers.Main) {
                        config = config.copy(lockUseCustomImage = true)
                        AppSettings.saveConfig(context, config)
                        previewKey++
                        Toast.makeText(context, "잠금화면 배경 이미지가 설정되었습니다", Toast.LENGTH_SHORT).show()
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
                        config = config.copy(homeUseCustomImage = true)
                        AppSettings.saveConfig(context, config)
                        previewKey++
                        Toast.makeText(context, "홈화면 배경 이미지가 설정되었습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Light subtle background pattern
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "도트 투 홈",
                    fontSize = 26.sp,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "매일 업데이트되는 디데이 배경화면",
                    fontSize = 13.sp,
                    color = Color(0xFF999999),
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
                                Text(
                                    text = "디데이",
                                    fontSize = 12.sp,
                                    color = Color(0xFF999999),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (remainingDays == 0) "D-DAY" else "D-$remainingDays",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                            Text(
                                text = String.format("%.1f%%", progressPercent),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                        // Mini dot grid (10x10)
                        DotGrid(
                            elapsedDays = elapsedDays,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Target date setting
                GlassCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "목표 날짜",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Start date (read-only, always today)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF0F0F2))
                                    .padding(12.dp)
                            ) {
                                Text("시작일 (오늘)", fontSize = 11.sp, color = Color(0xFF999999))
                                Text(
                                    formatDateKorean(todayKST),
                                    fontSize = 14.sp,
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
                                    .clickable { showDatePicker = true }
                                    .padding(12.dp)
                            ) {
                                Text("목표일 (D-Day)", fontSize = 11.sp, color = Color(0xFF999999))
                                Text(
                                    formatDateKorean(config.targetDate),
                                    fontSize = 14.sp,
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
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
                            Text(
                                text = label,
                                fontSize = 14.sp,
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
                    elapsedDays = elapsedDays,
                    remainingDays = remainingDays,
                    progressPercent = progressPercent,
                    onDotOffsetYChange = { newY ->
                        config = if (selectedTab == 0) {
                            config.copy(lockDotOffsetY = newY)
                        } else {
                            config.copy(homeDotOffsetY = newY)
                        }
                        AppSettings.saveConfig(context, config)
                    },
                    onPickPhoto = {
                        val request = androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                        if (selectedTab == 0) lockPhotoLauncher.launch(request)
                        else homePhotoLauncher.launch(request)
                    },
                    onResetDefault = {
                        if (selectedTab == 0) {
                            config = config.copy(lockUseCustomImage = false)
                        } else {
                            config = config.copy(homeUseCustomImage = false)
                        }
                        AppSettings.saveConfig(context, config)
                        previewKey++
                        Toast.makeText(context, "기본 배경으로 초기화되었습니다", Toast.LENGTH_SHORT).show()
                    }
                )

                // Auto update card
                GlassCard(backdrop = mainBackdrop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "자동 업데이트",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                text = "매일 자정에 배경화면을 자동으로 업데이트합니다",
                                fontSize = 11.sp,
                                color = Color(0xFF999999),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = config.autoUpdate,
                            onCheckedChange = { isChecked ->
                                config = config.copy(autoUpdate = isChecked)
                                AppSettings.saveConfig(context, config)
                                if (isChecked) {
                                    WallpaperWorker.scheduleDailyUpdate(context)
                                    Toast.makeText(context, "자동 업데이트가 활성화되었습니다", Toast.LENGTH_SHORT).show()
                                } else {
                                    WallpaperWorker.cancelDailyUpdate(context)
                                    Toast.makeText(context, "자동 업데이트가 비활성화되었습니다", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF1A1A1A),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFD0D0D0)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Bottom apply button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                MonoLiquidButton(
                    text = if (isApplying) "적용 중..." else "배경화면 적용",
                    backdrop = mainBackdrop,
                    enabled = !isApplying,
                    onClick = {
                        isApplying = true
                        coroutineScope.launch(Dispatchers.IO) {
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
                                    Toast.makeText(context, "배경화면이 적용되었습니다!", Toast.LENGTH_SHORT).show()
                                    isApplying = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "배경화면 적용 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                    isApplying = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = config.targetDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        config = config.copy(targetDate = selected)
                        AppSettings.saveConfig(context, config)
                        previewKey++
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
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

    // Section label
    Text(
        text = "$screenLabel 미리보기",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF666666),
        letterSpacing = 0.5.sp,
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

        // Background layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(previewBackdrop)
        ) {
            if (useCustom) {
                val file = remember(useCustom, previewKey) { File(context.filesDir, bgFile) }
                if (file.exists()) {
                    val bitmap = remember(file, previewKey) { BitmapFactory.decodeFile(file.absolutePath) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
            }
        }

        // Frosted glass card in preview, positioned by dotOffsetY
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
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
                            Text(
                                text = "디데이 진행률",
                                fontSize = 9.sp,
                                color = Color(0xFF999999),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (remainingDays == 0) "D-DAY" else "D-$remainingDays",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                        Text(
                            text = String.format("%.1f%%", progressPercent),
                            fontSize = 14.sp,
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("도트 위치 조절", fontSize = 13.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium)
                Text("${(dotOffsetY * 100).toInt()}%", fontSize = 13.sp, color = Color(0xFF999999), fontWeight = FontWeight.Bold)
            }
            Slider(
                value = dotOffsetY,
                onValueChange = onDotOffsetYChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFF1A1A1A),
                    inactiveTrackColor = Color(0xFFE0E0E0),
                    thumbColor = Color(0xFF1A1A1A)
                )
            )
        }
    }

    // Background image controls
    GlassCard(backdrop = backdrop) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "$screenLabel 배경 이미지",
                fontSize = 14.sp,
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
                    Text("사진 선택", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onResetDefault,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Text("기본 배경", fontSize = 13.sp)
                }
            }

            Text(
                text = if (useCustom) "사용자 지정 이미지 사용 중" else "기본 흰색 배경 사용 중",
                fontSize = 11.sp,
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
    val totalDots = 100
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

// ── Monochrome Liquid Glass Button ────────────────────────────────────────────

@Composable
fun MonoLiquidButton(
    text: String,
    backdrop: Backdrop,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    blur(radius = 8f.dp.toPx())
                    lens(
                        refractionHeight = 5f.dp.toPx(),
                        refractionAmount = 10f.dp.toPx(),
                        chromaticAberration = false
                    )
                },
                layerBlock = {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                },
                highlight = {
                    Highlight(
                        style = HighlightStyle.Default(
                            color = Color.White.copy(alpha = 0.6f),
                            angle = -45f
                        ),
                        width = 1.dp,
                        blurRadius = 0.5.dp
                    )
                },
                shadow = {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.1f),
                        radius = 6.dp,
                        offset = DpOffset(0.dp, 3.dp)
                    )
                },
                onDrawSurface = {
                    drawRect(Color(0xFF1A1A1A).copy(alpha = if (enabled) 0.9f else 0.4f))
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    coroutineScope.launch {
                        pressScale.animateTo(0.95f, spring(dampingRatio = 0.35f, stiffness = 500f))
                        pressScale.animateTo(1.0f, spring(dampingRatio = 0.5f, stiffness = 300f))
                        onClick()
                    }
                }
            )
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
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
