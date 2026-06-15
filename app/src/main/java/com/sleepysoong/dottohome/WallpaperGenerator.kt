package com.sleepysoong.dottohome

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import java.util.Calendar

object WallpaperGenerator {

    // Simple date diff helper
    fun getDaysBetween(start: Long, end: Long): Int {
        val s = Calendar.getInstance().apply { 
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val e = Calendar.getInstance().apply { 
            timeInMillis = end
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = e.timeInMillis - s.timeInMillis
        return (diff / (24L * 60 * 60 * 1000)).toInt()
    }

    fun generate(context: Context, width: Int, height: Int): Bitmap {
        val config = AppSettings.getConfig(context)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background
        var bgBitmap: Bitmap? = null
        if (config.useCustomImage) {
            try {
                val file = File(context.filesDir, "wallpaper_bg.jpg")
                if (file.exists()) {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    
                    // Calculate inSampleSize
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
            // Draw Premium Gradient Background
            val backgroundPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.parseColor("#0C0C14"), Color.parseColor("#150A26"), Color.parseColor("#061626")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            // Draw glowing orbs
            drawGlowingOrbs(canvas, width, height)
        }

        // 2. Calculate D-Day Information
        val today = System.currentTimeMillis()
        val totalDays = getDaysBetween(config.startDate, config.targetDate).coerceAtLeast(1)
        val remainingDays = getDaysBetween(today, config.targetDate).coerceAtLeast(0)
        val elapsedDays = (totalDays - remainingDays).coerceIn(0, totalDays)
        val progressPercent = (elapsedDays.toFloat() / totalDays.toFloat() * 100f).coerceIn(0f, 100f)

        // 3. Draw Liquid Glass Card
        // Center-low position to avoid lock-screen clock collisions
        val cardWidth = (width * 0.82f).toInt()
        // Determine grid size to allocate appropriate height
        val cols = when {
            totalDays <= 7 -> totalDays
            totalDays <= 31 -> 7
            totalDays <= 150 -> 10
            else -> 20
        }
        val rows = (totalDays + cols - 1) / cols
        val dotSpacing = (cardWidth * 0.035f).coerceIn(8f, 24f)
        val dotRadius = (cardWidth * 0.02f).coerceIn(5f, 16f)
        
        // Calculate Grid size
        val gridWidth = cols * (dotRadius * 2 + dotSpacing) - dotSpacing
        val gridHeight = rows * (dotRadius * 2 + dotSpacing) - dotSpacing
        
        // Card Height accommodates Header (D-Day + %), Grid, and Padding
        val cardHeaderHeight = 220f
        val cardPadding = 60f
        val cardHeight = (cardHeaderHeight + gridHeight + cardPadding * 2).toInt()

        val cardLeft = (width - cardWidth) / 2f
        val cardTop = (height - cardHeight) / 2f + (height * 0.05f) // push down slightly
        val cardRight = cardLeft + cardWidth
        val cardBottom = cardTop + cardHeight

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val cornerRadius = 60f

        // Apply Blur behind Card (Frosted Glass Effect)
        try {
            // Extract the card region from current background
            val cropRect = Rect(cardLeft.toInt().coerceAtLeast(0), cardTop.toInt().coerceAtLeast(0), cardRight.toInt().coerceAtMost(width), cardBottom.toInt().coerceAtMost(height))
            if (cropRect.width() > 0 && cropRect.height() > 0) {
                val croppedBg = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
                
                // Downscale for fast & smooth blur
                val scaleFactor = 4
                val miniWidth = (croppedBg.width / scaleFactor).coerceAtLeast(1)
                val miniHeight = (croppedBg.height / scaleFactor).coerceAtLeast(1)
                val miniBg = Bitmap.createScaledBitmap(croppedBg, miniWidth, miniHeight, true)
                
                // Blur the mini bitmap
                val blurredMini = boxBlur(miniBg, 6)
                
                // Upscale blurred back to original card size
                val blurredBg = Bitmap.createScaledBitmap(blurredMini, croppedBg.width, croppedBg.height, true)
                
                // Draw blurred background with a rounded clip path
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

        // Draw Card translucent overlays & shadows
        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            alpha = 20 // 8% alpha translucent white tint
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)

        // Draw Card border (Liquid Glass style subtle stroke)
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            shader = LinearGradient(
                cardLeft, cardTop, cardRight, cardBottom,
                intArrayOf(Color.WHITE, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // 4. Draw D-Day & Progress Text
        val ddayPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        
        val percentPaint = Paint().apply {
            isAntiAlias = true
            color = config.dotColor
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            alpha = 140
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val textY = cardTop + cardPadding + 60f
        
        // Draw Labels
        canvas.drawText("D-DAY PROGRESS", cardLeft + cardPadding, textY - 45f, labelPaint)
        
        // Draw main D-Day text
        val ddayStr = if (remainingDays == 0) "D-DAY" else "D-$remainingDays"
        canvas.drawText(ddayStr, cardLeft + cardPadding, textY + 40f, ddayPaint)

        // Draw percentage text
        val percentStr = String.format("%.1f%%", progressPercent)
        canvas.drawText(percentStr, cardRight - cardPadding, textY + 40f, percentPaint)

        // Draw divider
        val dividerPaint = Paint().apply {
            color = Color.WHITE
            alpha = 25
            strokeWidth = 2f
        }
        val dividerY = textY + 90f
        canvas.drawLine(cardLeft + cardPadding, dividerY, cardRight - cardPadding, dividerY, dividerPaint)

        // 5. Draw One Dot Grid
        val gridStartX = cardLeft + (cardWidth - gridWidth) / 2f + dotRadius
        val gridStartY = dividerY + 50f + dotRadius

        val fillPaint = Paint().apply {
            isAntiAlias = true
            color = config.dotColor
            style = Paint.Style.FILL
        }

        val emptyPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            alpha = 50
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        for (i in 0 until totalDays) {
            val rIdx = i / cols
            val cIdx = i % cols
            
            val cx = gridStartX + cIdx * (dotRadius * 2 + dotSpacing)
            val cy = gridStartY + rIdx * (dotRadius * 2 + dotSpacing)

            val isElapsed = i < elapsedDays
            val paintToUse = if (isElapsed) fillPaint else emptyPaint

            if (config.dotShape == "square") {
                val size = dotRadius * 2f
                val rect = RectF(cx - dotRadius, cy - dotRadius, cx + dotRadius, cy + dotRadius)
                canvas.drawRoundRect(rect, dotRadius * 0.4f, dotRadius * 0.4f, paintToUse)
            } else {
                canvas.drawCircle(cx, cy, dotRadius, paintToUse)
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
        if (cropped != src) {
            cropped.recycle()
        }
        return scaled
    }

    private fun drawGlowingOrbs(canvas: Canvas, width: Int, height: Int) {
        // Draw 3 static decorative gradient orbs to emulate the dynamic app background
        
        // Orb 1: Luxury Purple
        val purplePaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                width * 0.3f, height * 0.25f, width * 0.5f,
                intArrayOf(Color.parseColor("#9E3CFF"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.3f, height * 0.25f, width * 0.5f, purplePaint)

        // Orb 2: Premium Teal
        val tealPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                width * 0.75f, height * 0.6f, width * 0.45f,
                intArrayOf(Color.parseColor("#00FFCC"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.75f, height * 0.6f, width * 0.45f, tealPaint)

        // Orb 3: Radiant Peach
        val peachPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                width * 0.2f, height * 0.8f, width * 0.4f,
                intArrayOf(Color.parseColor("#FF6B4A"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.2f, height * 0.8f, width * 0.4f, peachPaint)
    }

    // A fast O(W*H) Box Blur
    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)
        val r = radius.coerceAtLeast(1)

        val temp = IntArray(w * h)
        
        // Horizontal pass
        for (y in 0 until h) {
            for (x in 0 until w) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0
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

        // Vertical pass
        for (x in 0 until w) {
            for (y in 0 until h) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0
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
