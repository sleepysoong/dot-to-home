package com.sleepysoong.dottohome.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.sleepysoong.dottohome.AppConfig
import com.sleepysoong.dottohome.AppSettings
import com.sleepysoong.dottohome.DDayItem
import com.sleepysoong.dottohome.DotColor
import com.sleepysoong.dottohome.DotShape
import com.sleepysoong.dottohome.WallpaperGenerator
import com.sleepysoong.dottohome.ui.components.*
import com.sleepysoong.dottohome.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Date picker state: which item index + which date (start vs target)
    var showDatePicker by remember { mutableStateOf(false) }
    var pickingItemIndex by remember { mutableIntStateOf(0) }
    var pickingStartDate by remember { mutableStateOf(true) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = lock, 1 = home

    // Expanded accordion state per item
    var expandedIndex by remember { mutableIntStateOf(0) }

    val mainBackdrop = rememberLayerBackdrop()

    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val subTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF999999)
    val dividerColor = if (isDark) Color(0xFF333333) else Color(0xFFE8E8E8)
    val textFieldBg = if (isDark) Color(0xFF222224) else Color(0xFFF0F0F2)

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

                // ── D-Day items section header ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PretendardText("디데이 목록", 16, textColor, FontWeight.Bold)
                    PretendardText("${config.ddayItems.size}/5", 13, subTextColor, FontWeight.Medium)
                }

                // ── D-Day item accordion cards ─────────────────────────────────
                config.ddayItems.forEachIndexed { index, item ->
                    val isExpanded = expandedIndex == index
                    val todayKST = AppConfig.getTodayKST()
                    val totalSpan = WallpaperGenerator.getDaysBetween(item.startDate, item.targetDate).coerceAtLeast(1)
                    val remainingDays = WallpaperGenerator.getDaysBetween(todayKST, item.targetDate).coerceAtLeast(0)
                    val elapsedFromStart = WallpaperGenerator.getDaysBetween(item.startDate, todayKST).coerceAtLeast(0)
                    val cols = 10; val rows = 10; val totalDots = 100
                    val elapsedDots = if (totalSpan > 0)
                        (elapsedFromStart.toFloat() / totalSpan * 100).toInt().coerceIn(0, 100)
                    else 100
                    val progressPercent = (elapsedFromStart.toFloat() / totalSpan.toFloat() * 100f).coerceIn(0f, 100f)

                    GlassCard(backdrop = mainBackdrop, isDark = isDark) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            // ── Accordion header ────────────────────────────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        expandedIndex = if (isExpanded) -1 else index
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    PretendardText(
                                        item.label.ifBlank { "디데이 ${index + 1}" },
                                        14, textColor, FontWeight.SemiBold
                                    )
                                    PretendardText(
                                        if (remainingDays == 0) "D-DAY" else "D-$remainingDays  •  ${String.format("%.1f%%", progressPercent)}",
                                        12, subTextColor, FontWeight.Normal,
                                        Modifier.padding(top = 2.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Delete button (disabled if only 1 item)
                                    if (config.ddayItems.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val newItems = config.ddayItems.toMutableList()
                                                newItems.removeAt(index)
                                                if (expandedIndex >= newItems.size) {
                                                    expandedIndex = newItems.size - 1
                                                }
                                                updateConfig(config.copy(ddayItems = newItems))
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "삭제",
                                                tint = if (isDark) Color(0xFFFF6B6B) else Color(0xFFE53E3E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "접기" else "펼치기",
                                        tint = subTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                                Spacer(Modifier.height(12.dp))

                                // ── Dot preview ─────────────────────────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        PretendardText("디데이", 11, subTextColor, FontWeight.Medium)
                                        PretendardText(
                                            if (remainingDays == 0) "D-DAY" else "D-$remainingDays",
                                            28, textColor, FontWeight.Bold
                                        )
                                    }
                                    PretendardText(
                                        String.format("%.1f%%", progressPercent),
                                        18, textColor, FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                DotGrid(cols, rows, totalDots, elapsedDots, isDark, item.dotShape, item.dotColor, Modifier.fillMaxWidth())

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                                Spacer(Modifier.height(12.dp))

                                // ── Label text field ─────────────────────────────
                                PretendardText("라벨", 12, subTextColor, FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                var labelValue by remember(item.id) { mutableStateOf(item.label) }
                                OutlinedTextField(
                                    value = labelValue,
                                    onValueChange = { v ->
                                        labelValue = v
                                        val newItems = config.ddayItems.toMutableList()
                                        newItems[index] = item.copy(label = v)
                                        updateConfig(config.copy(ddayItems = newItems))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontFamily = Pretendard, fontSize = 14.sp, color = textColor),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDark) Color.White else Color(0xFF1A1A1A),
                                        unfocusedBorderColor = dividerColor,
                                        focusedContainerColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.5f),
                                        unfocusedContainerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.3f),
                                        cursorColor = textColor
                                    )
                                )

                                Spacer(Modifier.height(12.dp))

                                // ── Date pickers ─────────────────────────────────
                                PretendardText("날짜 설정", 12, subTextColor, FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(textFieldBg)
                                            .clickable {
                                                pickingItemIndex = index
                                                pickingStartDate = true
                                                showDatePicker = true
                                            }
                                            .padding(12.dp)
                                    ) {
                                        PretendardText("시작일", 11, subTextColor)
                                        PretendardText(
                                            formatDateKorean(item.startDate), 13, textColor,
                                            FontWeight.Bold, Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(textFieldBg)
                                            .clickable {
                                                pickingItemIndex = index
                                                pickingStartDate = false
                                                showDatePicker = true
                                            }
                                            .padding(12.dp)
                                    ) {
                                        PretendardText("목표일 (D-Day)", 11, subTextColor)
                                        PretendardText(
                                            formatDateKorean(item.targetDate), 13, textColor,
                                            FontWeight.Bold, Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // ── Shape selector ───────────────────────────────
                                PretendardText("도트 모양", 12, subTextColor, FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DotShape.values().forEach { shape ->
                                        val isSelected = item.dotShape == shape
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
                                                .clickable {
                                                    val newItems = config.ddayItems.toMutableList()
                                                    newItems[index] = item.copy(dotShape = shape)
                                                    updateConfig(config.copy(ddayItems = newItems))
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            PretendardText(
                                                shape.label, 12, chipText,
                                                if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // ── Color selector ───────────────────────────────
                                PretendardText("도트 색상", 12, subTextColor, FontWeight.Medium)
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DotColor.values().forEach { colorOpt ->
                                        val isSelected = item.dotColor == colorOpt
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
                                                .border(
                                                    if (isSelected) 2.dp else 0.dp,
                                                    if (isSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent,
                                                    CircleShape
                                                )
                                                .clickable {
                                                    val newItems = config.ddayItems.toMutableList()
                                                    newItems[index] = item.copy(dotColor = colorOpt)
                                                    updateConfig(config.copy(ddayItems = newItems))
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (colorOpt == DotColor.ADAPTIVE) {
                                                PretendardText(
                                                    "A", 14,
                                                    if (isDark) Color.Black else Color.White,
                                                    FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Add D-Day button ───────────────────────────────────────────
                if (config.ddayItems.size < 5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .drawBackdrop(
                                backdrop = mainBackdrop, shape = { RoundedCornerShape(18.dp) },
                                effects = { blur(radius = 8f.dp.toPx()); lens(refractionHeight = 3f.dp.toPx(), refractionAmount = 5f.dp.toPx(), chromaticAberration = false) },
                                highlight = { Highlight(style = HighlightStyle.Default(color = Color.White.copy(alpha = 0.15f), angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                                onDrawSurface = {
                                    drawRect(
                                        if (isDark) Color(0xFF1A1A1C).copy(alpha = 0.5f)
                                        else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            )
                            .clickable {
                                if (config.ddayItems.size < 5) {
                                    val newItems = config.ddayItems + DDayItem()
                                    expandedIndex = newItems.size - 1
                                    updateConfig(config.copy(ddayItems = newItems))
                                }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PretendardText("+ 디데이 추가", 14, subTextColor, FontWeight.SemiBold)
                    }
                }

                // ── Segmented control and Toggle ───────────────────────────────
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
                            val newConfig = if (selectedTab == 0)
                                config.copy(lockEnabled = checked)
                            else
                                config.copy(homeEnabled = checked)
                            updateConfig(newConfig)
                        },
                        backdrop = mainBackdrop,
                        isDark = isDark
                    )
                }

                // ── Wallpaper preview ─────────────────────────────────────────
                WallpaperPreviewSection(
                    isLockScreen = selectedTab == 0,
                    config = config,
                    previewKey = previewKey,
                    backdrop = mainBackdrop,
                    isDark = isDark,
                    onDotOffsetYChange = { newY ->
                        val newConfig = if (selectedTab == 0)
                            config.copy(lockDotOffsetY = newY)
                        else
                            config.copy(homeDotOffsetY = newY)
                        updateConfig(newConfig)
                    },
                    onPickPhoto = {
                        val req = androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                        if (selectedTab == 0) lockPhotoLauncher.launch(req)
                        else homePhotoLauncher.launch(req)
                    },
                    onResetDefault = {
                        val newConfig = if (selectedTab == 0)
                            config.copy(lockUseCustomImage = false)
                        else
                            config.copy(homeUseCustomImage = false)
                        updateConfig(newConfig)
                        Toast.makeText(context, "기본 배경으로 초기화되었습니다", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showDatePicker) {
        val safeIndex = pickingItemIndex.coerceIn(0, config.ddayItems.size - 1)
        val item = config.ddayItems[safeIndex]
        val initialDate = if (pickingStartDate) item.startDate else item.targetDate
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        val newItems = config.ddayItems.toMutableList()
                        newItems[safeIndex] = if (pickingStartDate)
                            item.copy(startDate = selected)
                        else
                            item.copy(targetDate = selected)
                        updateConfig(config.copy(ddayItems = newItems))
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
