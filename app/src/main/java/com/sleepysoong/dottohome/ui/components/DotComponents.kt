package com.sleepysoong.dottohome.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.sleepysoong.dottohome.DotColor
import com.sleepysoong.dottohome.DotShape

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
