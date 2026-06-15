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
     * @param isLockScreen true = lock screen, false = home screen
     */
    fun generate(context: Context, width: Int, height: Int, isLockScreen: Boolean): Bitmap {
        val config = AppSettings.getConfig(context)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw background
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

        // 2. Calculate D-Day info
        val totalDays = 100 // Always 100 dots (10x10)
        val todayKST = AppConfig.getTodayKST()
        val remainingDays = getDaysBetween(todayKST, config.targetDate).coerceAtLeast(0)
        val totalSpan = getDaysBetween(config.startDate, config.targetDate).coerceAtLeast(1)
        val elapsedFromStart = getDaysBetween(config.startDate, todayKST).coerceAtLeast(0)
        // Map elapsed into 100-dot scale
        val elapsedDots = if (totalSpan >= totalDays) {
            (elapsedFromStart.toFloat() / totalSpan * totalDays).toInt().coerceIn(0, totalDays)
        } else {
            elapsedFromStart.coerceIn(0, totalDays)
        }
        val progressPercent = (elapsedDots.toFloat() / totalDays.toFloat() * 100f).coerceIn(0f, 100f)

        // 3. Dot position from config
        val dotOffsetY = if (isLockScreen) config.lockDotOffsetY else config.homeDotOffsetY

        // 4. Card dimensions
        val cardWidth = (width * 0.82f).toInt()
        val cols = 10
        val rows = 10
        val dotSpacing = (cardWidth * 0.025f).coerceIn(6f, 16f)
        val dotRadius = (cardWidth * 0.018f).coerceIn(4f, 14f)

        val gridWidth = cols * (dotRadius * 2 + dotSpacing) - dotSpacing
        val gridHeight = rows * (dotRadius * 2 + dotSpacing) - dotSpacing

        val cardHeaderHeight = 200f
        val cardPadding = 50f
        val cardHeight = (cardHeaderHeight + gridHeight + cardPadding * 2).toInt()

        val cardLeft = (width - cardWidth) / 2f
        val maxCardTop = (height - cardHeight).toFloat().coerceAtLeast(0f)
        val cardTop = (dotOffsetY * maxCardTop).coerceIn(0f, maxCardTop)
        val cardRight = cardLeft + cardWidth
        val cardBottom = cardTop + cardHeight

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val cornerRadius = 48f

        // 5. Frosted glass blur behind card
        try {
            val cropRect = Rect(
                cardLeft.toInt().coerceAtLeast(0),
                cardTop.toInt().coerceAtLeast(0),
                cardRight.toInt().coerceAtMost(width),
                cardBottom.toInt().coerceAtMost(height)
            )
            if (cropRect.width() > 0 && cropRect.height() > 0) {
                val croppedBg = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
                val scaleFactor = 4
                val miniWidth = (croppedBg.width / scaleFactor).coerceAtLeast(1)
                val miniHeight = (croppedBg.height / scaleFactor).coerceAtLeast(1)
                val miniBg = Bitmap.createScaledBitmap(croppedBg, miniWidth, miniHeight, true)
                val blurredMini = boxBlur(miniBg, 6)
                val blurredBg = Bitmap.createScaledBitmap(blurredMini, croppedBg.width, croppedBg.height, true)

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

        // 6. Card surface overlay - white glass
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

        // 7. Draw D-Day text with Pretendard font and -5% letter spacing
        val textColor = Color.parseColor("#1A1A1A")
        val subTextColor = Color.parseColor("#888888")

        // letterSpacing = -0.05 em (fontSize * -0.05)
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = subTextColor
            textSize = 30f
            typeface = getPretendardRegular(context)
            textAlign = Paint.Align.LEFT
            letterSpacing = -0.05f
        }

        val ddayPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = 72f
            typeface = getPretendardBold(context)
            textAlign = Paint.Align.LEFT
            letterSpacing = -0.05f
        }

        val percentPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = 44f
            typeface = getPretendardBold(context)
            textAlign = Paint.Align.RIGHT
            letterSpacing = -0.05f
        }

        val textY = cardTop + cardPadding + 50f
        canvas.drawText(config.customLabel, cardLeft + cardPadding, textY - 40f, labelPaint)
        val ddayStr = if (remainingDays == 0) "D-DAY" else "D-$remainingDays"
        canvas.drawText(ddayStr, cardLeft + cardPadding, textY + 35f, ddayPaint)
        val percentStr = String.format("%.1f%%", progressPercent)
        canvas.drawText(percentStr, cardRight - cardPadding, textY + 35f, percentPaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 2f
        }
        val dividerY = textY + 80f
        canvas.drawLine(cardLeft + cardPadding, dividerY, cardRight - cardPadding, dividerY, dividerPaint)

        // 8. Draw 10x10 dot grid
        val gridStartX = cardLeft + (cardWidth - gridWidth) / 2f + dotRadius
        val gridStartY = dividerY + 40f + dotRadius

        val fillPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            style = Paint.Style.FILL
        }

        val emptyPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#D0D0D0")
            style = Paint.Style.FILL
        }

        for (i in 0 until totalDays) {
            val rIdx = i / cols
            val cIdx = i % cols
            val cx = gridStartX + cIdx * (dotRadius * 2 + dotSpacing)
            val cy = gridStartY + rIdx * (dotRadius * 2 + dotSpacing)
            val isElapsed = i < elapsedDots
            canvas.drawCircle(cx, cy, dotRadius, if (isElapsed) fillPaint else emptyPaint)
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

    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)
        val r = radius.coerceAtLeast(1)
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
