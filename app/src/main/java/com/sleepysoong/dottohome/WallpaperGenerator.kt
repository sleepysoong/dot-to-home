package com.sleepysoong.dottohome

import android.content.Context
import android.graphics.*
import java.io.File
import java.util.Calendar
import java.util.TimeZone

object WallpaperGenerator {

    private var cachedTypeface: Typeface? = null
    private var cachedBoldTypeface: Typeface? = null

    private fun getPretendardRegular(context: Context): Typeface {
        if (cachedTypeface == null) {
            cachedTypeface = try {
                context.resources.getFont(R.font.pretendard_regular)
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
        return cachedTypeface!!
    }

    private fun getPretendardBold(context: Context): Typeface {
        if (cachedBoldTypeface == null) {
            cachedBoldTypeface = try {
                context.resources.getFont(R.font.pretendard_bold)
            } catch (e: Exception) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        return cachedBoldTypeface!!
    }

    // Always uses KST timezone for day calculations
    fun getDaysBetween(start: Long, end: Long): Int {
        val kst = TimeZone.getTimeZone("Asia/Seoul")
        val s = Calendar.getInstance(kst).apply {
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val e = Calendar.getInstance(kst).apply {
            timeInMillis = end
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = e.timeInMillis - s.timeInMillis
        return (diff / (24L * 60 * 60 * 1000)).toInt()
    }

    /**
     * Generates wallpaper bitmap for either lock or home screen.
     * Supports multiple D-Day items stacked vertically.
     * @param isLockScreen true = lock screen, false = home screen
     */
    fun generate(context: Context, width: Int, height: Int, isLockScreen: Boolean): Bitmap {
        val config = AppSettings.getConfig(context)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val todayKST = AppConfig.getTodayKST()

        // ── 1. Draw background ──────────────────────────────────────────────────
        val useCustom = if (isLockScreen) config.lockUseCustomImage else config.homeUseCustomImage
        val bgFile = if (isLockScreen) "wallpaper_lock_bg.jpg" else "wallpaper_home_bg.jpg"
        var bgBitmap: Bitmap? = null

        if (useCustom) {
            try {
                val file = File(context.filesDir, bgFile)
                if (file.exists()) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    val scaleX = options.outWidth / width
                    val scaleY = options.outHeight / height
                    val sample = Math.max(1, Math.min(scaleX, scaleY))
                    options.inJustDecodeBounds = false
                    options.inSampleSize = sample
                    val temp = BitmapFactory.decodeFile(file.absolutePath, options)
                    if (temp != null) {
                        bgBitmap = cropAndScale(temp, width, height)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, 0f, 0f, null)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        // ── 2. Layout calculation ───────────────────────────────────────────────
        val dotOffsetY = if (isLockScreen) config.lockDotOffsetY else config.homeDotOffsetY
        val items = config.ddayItems.ifEmpty { listOf(DDayItem()) }
        val itemCount = items.size

        // Card width fraction based on item count
        val cardWidthFraction = when {
            itemCount >= 3 -> 0.82f
            itemCount == 2 -> 0.85f
            else -> 0.88f
        }
        val cardWidth = (width * cardWidthFraction).toInt()
        val cardLeft = (width - cardWidth) / 2f
        val cardRight = cardLeft + cardWidth

        // Dot grid sizing
        val cols = 10
        val rows = 10
        val dotRadius = when {
            itemCount >= 3 -> 7f
            itemCount == 2 -> 9f
            else -> 12f
        }
        val dotSpacing = dotRadius * 0.8f
        val gridWidth = cols * (dotRadius * 2 + dotSpacing) - dotSpacing
        val gridHeight = rows * (dotRadius * 2 + dotSpacing) - dotSpacing

        val cardPadding = 40f
        val cardHeaderHeight = when {
            itemCount >= 3 -> 140f
            itemCount == 2 -> 160f
            else -> 180f
        }
        val cardHeight = (cardHeaderHeight + gridHeight + cardPadding * 2).toInt()
        val cardGap = 20f

        // Total height of all cards + gaps
        val totalCardsHeight = itemCount * cardHeight + (itemCount - 1) * cardGap

        // Offset group vertically
        val maxGroupTop = (height - totalCardsHeight).toFloat().coerceAtLeast(0f)
        val groupTop = (dotOffsetY * maxGroupTop).coerceIn(0f, maxGroupTop)

        val cornerRadius = 40f
        val textColor = Color.parseColor("#1A1A1A")
        val subTextColor = Color.parseColor("#888888")

        // ── 3. Draw each card ───────────────────────────────────────────────────
        items.forEachIndexed { idx, item ->
            val cardTop = groupTop + idx * (cardHeight + cardGap)
            val cardBottom = cardTop + cardHeight
            val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

            // 3a. Frosted glass blur behind card
            try {
                val cropRect = Rect(
                    cardLeft.toInt().coerceAtLeast(0),
                    cardTop.toInt().coerceAtLeast(0),
                    cardRight.toInt().coerceAtMost(width),
                    cardBottom.toInt().coerceAtMost(height)
                )
                if (cropRect.width() > 0 && cropRect.height() > 0) {
                    val croppedBg = Bitmap.createBitmap(
                        bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height()
                    )
                    val scaleFactor = 4
                    val miniWidth = (croppedBg.width / scaleFactor).coerceAtLeast(1)
                    val miniHeight = (croppedBg.height / scaleFactor).coerceAtLeast(1)
                    val miniBg = Bitmap.createScaledBitmap(croppedBg, miniWidth, miniHeight, true)
                    val blurredMini = boxBlur(miniBg, 6)
                    val blurredBg = Bitmap.createScaledBitmap(
                        blurredMini, croppedBg.width, croppedBg.height, true
                    )

                    canvas.save()
                    val clipPath = Path().apply {
                        addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
                    }
                    canvas.clipPath(clipPath)
                    canvas.drawBitmap(blurredBg, cardLeft, cardTop, null)
                    canvas.restore()

                    croppedBg.recycle()
                    miniBg.recycle()
                    blurredMini.recycle()
                    blurredBg.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3b. Card surface overlay – white glass
            val cardPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                alpha = 180
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)

            val borderPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.parseColor("#E0E0E0")
            }
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

            // 3c. D-Day info for this item
            val totalSpan = getDaysBetween(item.startDate, item.targetDate).coerceAtLeast(1)
            val remainingDays = getDaysBetween(todayKST, item.targetDate).coerceAtLeast(0)
            val elapsedFromStart = getDaysBetween(item.startDate, todayKST).coerceAtLeast(0)
            val elapsedDots = if (totalSpan > 0)
                (elapsedFromStart.toFloat() / totalSpan * 100).toInt().coerceIn(0, 100)
            else 100
            val progressPercent = (elapsedFromStart.toFloat() / totalSpan.toFloat() * 100f)
                .coerceIn(0f, 100f)

            // 3d. Text sizes scale with item count
            val labelSize = when {
                itemCount >= 3 -> 24f
                itemCount == 2 -> 27f
                else -> 30f
            }
            val ddaySize = when {
                itemCount >= 3 -> 52f
                itemCount == 2 -> 60f
                else -> 72f
            }
            val percentSize = when {
                itemCount >= 3 -> 34f
                itemCount == 2 -> 38f
                else -> 44f
            }

            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = subTextColor
                textSize = labelSize
                typeface = getPretendardRegular(context)
                textAlign = Paint.Align.LEFT
                letterSpacing = -0.05f
            }
            val ddayPaint = Paint().apply {
                isAntiAlias = true
                color = textColor
                textSize = ddaySize
                typeface = getPretendardBold(context)
                textAlign = Paint.Align.LEFT
                letterSpacing = -0.05f
            }
            val percentPaint = Paint().apply {
                isAntiAlias = true
                color = textColor
                textSize = percentSize
                typeface = getPretendardBold(context)
                textAlign = Paint.Align.RIGHT
                letterSpacing = -0.05f
            }

            val textY = cardTop + cardPadding + labelSize + 10f
            val labelX = cardLeft + cardPadding
            val valueY = textY + ddaySize * 0.85f

            canvas.drawText(item.label, labelX, textY, labelPaint)
            val ddayStr = if (remainingDays == 0) "D-DAY" else "D-$remainingDays"
            canvas.drawText(ddayStr, labelX, valueY, ddayPaint)
            val percentStr = String.format("%.1f%%", progressPercent)
            canvas.drawText(percentStr, cardRight - cardPadding, valueY, percentPaint)

            // Divider
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                strokeWidth = 2f
            }
            val dividerY = valueY + 28f
            canvas.drawLine(labelX, dividerY, cardRight - cardPadding, dividerY, dividerPaint)

            // 3e. Dot grid
            val dotBaseColor = if (item.dotColor == DotColor.ADAPTIVE)
                textColor
            else
                Color.parseColor(item.dotColor.hex)

            val fillPaint = Paint().apply {
                isAntiAlias = true
                color = dotBaseColor
                style = Paint.Style.FILL
            }
            val emptyPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#D0D0D0")
                style = Paint.Style.FILL
            }

            val gridStartX = cardLeft + (cardWidth - gridWidth) / 2f + dotRadius
            val gridStartY = dividerY + 30f + dotRadius

            for (i in 0 until 100) {
                val rIdx = i / cols
                val cIdx = i % cols
                val cx = gridStartX + cIdx * (dotRadius * 2 + dotSpacing)
                val cy = gridStartY + rIdx * (dotRadius * 2 + dotSpacing)
                val isElapsed = i < elapsedDots
                drawShape(canvas, cx, cy, dotRadius, item.dotShape, if (isElapsed) fillPaint else emptyPaint)
            }
        }

        return bitmap
    }

    private fun cropAndScale(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = src.width
        val srcHeight = src.height
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
        var cropWidth = srcWidth
        var cropHeight = srcHeight
        var xOffset = 0
        var yOffset = 0

        if (srcAspect > targetAspect) {
            cropWidth = (srcHeight * targetAspect).toInt()
            xOffset = (srcWidth - cropWidth) / 2
        } else {
            cropHeight = (srcWidth / targetAspect).toInt()
            yOffset = (srcHeight - cropHeight) / 2
        }

        val cropped = Bitmap.createBitmap(src, xOffset, yOffset, cropWidth, cropHeight)
        val scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
        if (cropped != src) cropped.recycle()
        return scaled
    }

    private fun drawShape(canvas: Canvas, cx: Float, cy: Float, radius: Float, shape: DotShape, paint: Paint) {
        when (shape) {
            DotShape.CIRCLE -> {
                canvas.drawCircle(cx, cy, radius, paint)
            }
            DotShape.SQUARE -> {
                canvas.drawRoundRect(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    radius * 0.3f, radius * 0.3f, paint
                )
            }
            DotShape.DIAMOND -> {
                val path = android.graphics.Path()
                path.moveTo(cx, cy - radius)
                path.lineTo(cx + radius, cy)
                path.lineTo(cx, cy + radius)
                path.lineTo(cx - radius, cy)
                path.close()
                canvas.drawPath(path, paint)
            }
            DotShape.STAR -> {
                val path = android.graphics.Path()
                val innerRadius = radius * 0.4f
                val angles = 5
                val startAngle = -Math.PI / 2
                for (i in 0 until angles * 2) {
                    val r = if (i % 2 == 0) radius else innerRadius
                    val angle = startAngle + i * Math.PI / angles
                    val px = cx + (r * Math.cos(angle)).toFloat()
                    val py = cy + (r * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            DotShape.HEART -> {
                val path = android.graphics.Path()
                val topY = cy - radius * 0.5f
                val bottomY = cy + radius
                path.moveTo(cx, topY + radius * 0.3f)
                path.cubicTo(cx - radius * 1.2f, cy - radius * 1.2f, cx - radius * 1.2f, cy + radius * 0.2f, cx, bottomY)
                path.cubicTo(cx + radius * 1.2f, cy + radius * 0.2f, cx + radius * 1.2f, cy - radius * 1.2f, cx, topY + radius * 0.3f)
                path.close()
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun boxBlur(src: Bitmap, blurRadius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)
        val r = blurRadius.coerceAtLeast(1)
        val temp = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (dx in -r..r) {
                    val nx = x + dx
                    if (nx in 0 until w) {
                        val color = pix[y * w + nx]
                        rSum += (color shr 16) and 0xFF
                        gSum += (color shr 8) and 0xFF
                        bSum += color and 0xFF
                        count++
                    }
                }
                temp[y * w + x] = (0xFF shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
            }
        }

        for (x in 0 until w) {
            for (y in 0 until h) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (dy in -r..r) {
                    val ny = y + dy
                    if (ny in 0 until h) {
                        val color = temp[ny * w + x]
                        rSum += (color shr 16) and 0xFF
                        gSum += (color shr 8) and 0xFF
                        bSum += color and 0xFF
                        count++
                    }
                }
                pix[y * w + x] = (0xFF shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
            }
        }

        val dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        dest.setPixels(pix, 0, w, 0, 0, w, h)
        return dest
    }
}
