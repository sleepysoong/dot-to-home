package com.sleepysoong.dottohome.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.sleepysoong.dottohome.util.PretendardText

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
