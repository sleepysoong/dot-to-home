package com.sleepysoong.dottohome.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import com.sleepysoong.dottohome.AppConfig
import com.sleepysoong.dottohome.DDayItem
import com.sleepysoong.dottohome.WallpaperGenerator
import com.sleepysoong.dottohome.util.PretendardText
import java.io.File

@Composable
fun WallpaperPreviewSection(
    isLockScreen: Boolean,
    config: AppConfig,
    previewKey: Int,
    backdrop: Backdrop,
    isDark: Boolean,
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
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(if (isDark) Color.Black else Color.White))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(if (isDark) Color.Black else Color.White))
            }
        }

        if (isEnabled) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val items = config.ddayItems.ifEmpty { listOf(DDayItem()) }
                val itemCount = items.size

                // Scale card heights in preview
                val cardWidthFraction = when {
                    itemCount >= 3 -> 0.82f
                    itemCount == 2 -> 0.85f
                    else -> 0.88f
                }

                val cardSurface = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)
                val cardHighlight = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f)

                val maxOffset = maxHeight * 0.5f
                val actualOffset = maxOffset * dotOffsetY

                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth(cardWidthFraction)
                        .align(Alignment.TopCenter)
                        .offset(y = actualOffset),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { item ->
                        val todayKST = AppConfig.getTodayKST()
                        val totalSpan = WallpaperGenerator.getDaysBetween(item.startDate, item.targetDate).coerceAtLeast(1)
                        val remainingDays = WallpaperGenerator.getDaysBetween(todayKST, item.targetDate).coerceAtLeast(0)
                        val elapsedFromStart = WallpaperGenerator.getDaysBetween(item.startDate, todayKST).coerceAtLeast(0)
                        val elapsedDots = if (totalSpan > 0)
                            (elapsedFromStart.toFloat() / totalSpan * 100).toInt().coerceIn(0, 100)
                        else 100
                        val progressPercent = (elapsedFromStart.toFloat() / totalSpan.toFloat() * 100f).coerceIn(0f, 100f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBackdrop(
                                    backdrop = previewBackdrop, shape = { RoundedCornerShape(14.dp) },
                                    effects = { blur(radius = 14f.dp.toPx()); lens(refractionHeight = 6f.dp.toPx(), refractionAmount = 10f.dp.toPx(), chromaticAberration = false) },
                                    highlight = { Highlight(style = HighlightStyle.Default(color = cardHighlight, angle = -45f), width = 1.dp, blurRadius = 0.5.dp) },
                                    shadow = { Shadow(color = Color.Black.copy(alpha = 0.08f), radius = 8.dp, offset = DpOffset(0.dp, 4.dp)) },
                                    onDrawSurface = { drawRect(cardSurface) }
                                )
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        PretendardText(item.label, 7, subTextColor, FontWeight.Medium)
                                        PretendardText(
                                            if (remainingDays == 0) "D-DAY" else "D-$remainingDays",
                                            when { itemCount >= 3 -> 14; itemCount == 2 -> 16; else -> 18 },
                                            textColor, FontWeight.Bold
                                        )
                                    }
                                    PretendardText(
                                        String.format("%.1f%%", progressPercent),
                                        when { itemCount >= 3 -> 10; itemCount == 2 -> 11; else -> 13 },
                                        textColor, FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                                DotGrid(10, 10, 100, elapsedDots, isDark, item.dotShape, item.dotColor, Modifier.fillMaxWidth())
                            }
                        }
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textColor,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
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
            PretendardText(
                if (useCustom) "사용자 지정 이미지 사용 중" else "기본 배경 사용 중",
                11, subTextColor
            )
        }
    }
}
