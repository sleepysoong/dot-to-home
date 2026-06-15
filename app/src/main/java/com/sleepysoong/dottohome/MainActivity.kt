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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F15)
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

    // Configuration state
    var config by remember { mutableStateOf(AppSettings.getConfig(context)) }
    var previewKey by remember { mutableIntStateOf(0) }
    var isApplying by remember { mutableStateOf(false) }

    // Date Picker States
    var showDatePicker by remember { mutableStateOf(false) }
    var pickingStartDate by remember { mutableStateOf(true) }

    // Background backdrop for main app screen
    val mainBackdrop = rememberLayerBackdrop()

    // D-Day Progress Calculations
    val today = System.currentTimeMillis()
    val totalDays = WallpaperGenerator.getDaysBetween(config.startDate, config.targetDate).coerceAtLeast(1)
    val remainingDays = WallpaperGenerator.getDaysBetween(today, config.targetDate).coerceAtLeast(0)
    val elapsedDays = (totalDays - remainingDays).coerceIn(0, totalDays)
    val progressPercent = (elapsedDays.toFloat() / totalDays.toFloat() * 100f).coerceIn(0f, 100f)

    // Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    copyUriToInternalStorage(context, uri, "wallpaper_bg.jpg")
                    withContext(Dispatchers.Main) {
                        config = config.copy(useCustomImage = true)
                        AppSettings.saveConfig(context, config)
                        previewKey++
                        Toast.makeText(context, "Custom background selected!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating organic orbs (dynamic background)
        AnimatedBackgroundOrbs(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(mainBackdrop)
        )

        // Main content column
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
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "D O T   T O   H O M E",
                    fontSize = 24.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Beautiful Daily D-Day Wallpaper Updates",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Scrollable Settings
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                
                // 1. LIVE WALLPAPER PREVIEW CARD
                Text(
                    text = "LIVE WALLPAPER PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFCC),
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                // Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(32.dp))
                ) {
                    val previewBackdrop = rememberLayerBackdrop()

                    // Background layer inside preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(previewBackdrop)
                    ) {
                        if (config.useCustomImage) {
                            val file = remember(config.useCustomImage, previewKey) { File(context.filesDir, "wallpaper_bg.jpg") }
                            if (file.exists()) {
                                val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                DefaultGradientOrbs(modifier = Modifier.fillMaxSize())
                            }
                        } else {
                            DefaultGradientOrbs(modifier = Modifier.fillMaxSize())
                        }
                    }

                    // Frosted glass card in preview
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.85f)
                            .drawBackdrop(
                                backdrop = previewBackdrop,
                                shape = { RoundedCornerShape(24.dp) },
                                effects = {
                                    vibrancy()
                                    blur(radius = 16f.dp.toPx())
                                    lens(
                                        refractionHeight = config.refractionHeight.dp.toPx(),
                                        refractionAmount = config.refractionAmount.dp.toPx(),
                                        chromaticAberration = config.chromaticAberration
                                    )
                                },
                                highlight = {
                                    Highlight(
                                        style = HighlightStyle.Default(
                                            color = Color.White.copy(alpha = 0.35f),
                                            angle = -45f
                                        ),
                                        width = 1.2.dp,
                                        blurRadius = 0.8.dp
                                    )
                                },
                                shadow = {
                                    Shadow(
                                        color = Color.Black.copy(alpha = 0.2f),
                                        radius = 12.dp,
                                        offset = DpOffset(0.dp, 6.dp)
                                    )
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "D-DAY PROGRESS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = if (remainingDays == 0) "D-DAY" else "D-$remainingDays",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = String.format("%.1f%%", progressPercent),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(config.dotColor)
                                )
                            }
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                            DotGrid(
                                totalDays = totalDays,
                                elapsedDays = elapsedDays,
                                dotColor = Color(config.dotColor),
                                dotShape = config.dotShape
                            )
                        }
                    }
                }

                // 2. CONFIGURATION SECTIONS
                Text(
                    text = "AESTHETICS & SCHEDULING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC044FF),
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                // Date Picker Settings Card
                PremiumLiquidCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "DATE CONFIGURATION",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        pickingStartDate = true
                                        showDatePicker = true
                                    }
                                    .padding(12.dp)
                            ) {
                                Text("Start Date", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                Text(formatDate(config.startDate), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        pickingStartDate = false
                                        showDatePicker = true
                                    }
                                    .padding(12.dp)
                            ) {
                                Text("Target Date (D-Day)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                Text(formatDate(config.targetDate), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }

                // Dot & Styling Card
                PremiumLiquidCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "DOT STYLE & COLORS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Color Presets
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Accent Color", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val presets = listOf(
                                    "#00FFCC", // Teal
                                    "#C044FF", // Purple
                                    "#FF7E40", // Orange
                                    "#007AFF", // Blue
                                    "#FF6B97"  // Pink
                                )
                                presets.forEach { hex ->
                                    val colorVal = android.graphics.Color.parseColor(hex)
                                    val isSelected = config.dotColor == colorVal
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorVal))
                                            .border(
                                                width = 2.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                config = config.copy(dotColor = colorVal)
                                                AppSettings.saveConfig(context, config)
                                            }
                                    )
                                }
                            }
                        }

                        // Dot Shape Segment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dot Shape", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                val shapes = listOf("circle" to "Circles", "square" to "Squares")
                                shapes.forEach { (type, label) ->
                                    val isSel = config.dotShape == type
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                config = config.copy(dotShape = type)
                                                AppSettings.saveConfig(context, config)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(label, color = if (isSel) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Background Source Card
                PremiumLiquidCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "WALLPAPER BACKGROUND",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Choose Photo", color = Color.White, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    config = config.copy(useCustomImage = false)
                                    AppSettings.saveConfig(context, config)
                                    previewKey++
                                    Toast.makeText(context, "Default premium gradient selected!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Default Gradient", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        
                        Text(
                            text = if (config.useCustomImage) "Using custom gallery image" else "Using default flowing gradient background",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                // Shaders Refraction Card (Tweak Live Shaders)
                PremiumLiquidCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "LIQUID GLASS REFRACTION SETTINGS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        // Refraction Height
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Refraction Edge Height", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Text("${config.refractionHeight.toInt()} dp", color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = config.refractionHeight,
                                onValueChange = {
                                    config = config.copy(refractionHeight = it)
                                    AppSettings.saveConfig(context, config)
                                },
                                valueRange = 2f..30f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFF00FFCC),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        // Refraction Amount
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Refraction Strength", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Text("${config.refractionAmount.toInt()} px", color = Color(0xFFC044FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = config.refractionAmount,
                                onValueChange = {
                                    config = config.copy(refractionAmount = it)
                                    AppSettings.saveConfig(context, config)
                                },
                                valueRange = 0f..60f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFFC044FF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        // Chromatic Aberration Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chromatic Dispersion (RGB Split)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Switch(
                                checked = config.chromaticAberration,
                                onCheckedChange = {
                                    config = config.copy(chromaticAberration = it)
                                    AppSettings.saveConfig(context, config)
                                }
                            )
                        }
                    }
                }

                // Daily Auto Update Worker Card
                PremiumLiquidCard(backdrop = mainBackdrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DAILY AUTO-UPDATE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Automatically updates wallpaper at midnight",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f),
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
                                        Toast.makeText(context, "Daily Auto-Updates enabled!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        WallpaperWorker.cancelDailyUpdate(context)
                                        Toast.makeText(context, "Daily Auto-Updates disabled!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Bottom Floating Actions Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                InteractiveLiquidButton(
                    text = if (isApplying) "APPLYING WALLPAPER..." else "APPLY WALLPAPER NOW",
                    backdrop = mainBackdrop,
                    tintColor = Color(0xFF00FFCC),
                    textColor = Color.Black,
                    enabled = !isApplying,
                    onClick = {
                        isApplying = true
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val wm = WallpaperManager.getInstance(context)
                                val metrics = context.resources.displayMetrics
                                val width = metrics.widthPixels
                                val height = metrics.heightPixels
                                
                                val bitmap = WallpaperGenerator.generate(context, width, height)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                                } else {
                                    wm.setBitmap(bitmap)
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Wallpaper applied successfully!", Toast.LENGTH_SHORT).show()
                                    isApplying = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to apply wallpaper: ${e.message}", Toast.LENGTH_LONG).show()
                                    isApplying = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    // Material3 DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (pickingStartDate) config.startDate else config.targetDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        config = if (pickingStartDate) {
                            config.copy(startDate = selected)
                        } else {
                            config.copy(targetDate = selected)
                        }
                        AppSettings.saveConfig(context, config)
                        previewKey++
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Helpers
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

private fun formatDate(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return String.format("%d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

@Composable
fun DotGrid(
    totalDays: Int,
    elapsedDays: Int,
    dotColor: Color,
    dotShape: String,
    modifier: Modifier = Modifier
) {
    val cols = when {
        totalDays <= 7 -> totalDays
        totalDays <= 31 -> 7
        totalDays <= 150 -> 10
        else -> 20
    }
    val rows = (totalDays + cols - 1) / cols

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (r in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (c in 0 until cols) {
                    val idx = r * cols + c
                    if (idx < totalDays) {
                        val isElapsed = idx < elapsedDays
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .then(
                                    if (isElapsed) {
                                        Modifier.background(
                                            color = dotColor,
                                            shape = if (dotShape == "square") RoundedCornerShape(3.dp) else CircleShape
                                        )
                                    } else {
                                        Modifier.background(
                                            color = Color.White.copy(alpha = 0.15f),
                                            shape = if (dotShape == "square") RoundedCornerShape(3.dp) else CircleShape
                                        )
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultGradientOrbs(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF0F0F15))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Purple
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC044FF), Color.Transparent),
                    center = Offset(width * 0.3f, height * 0.25f),
                    radius = width * 0.5f
                ),
                center = Offset(width * 0.3f, height * 0.25f),
                radius = width * 0.5f
            )

            // Orb 2: Teal
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FFCC), Color.Transparent),
                    center = Offset(width * 0.75f, height * 0.6f),
                    radius = width * 0.45f
                ),
                center = Offset(width * 0.75f, height * 0.6f),
                radius = width * 0.45f
            )

            // Orb 3: Peach
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF7E40), Color.Transparent),
                    center = Offset(width * 0.2f, height * 0.8f),
                    radius = width * 0.4f
                ),
                center = Offset(width * 0.2f, height * 0.8f),
                radius = width * 0.4f
            )
        }
    }
}

@Composable
fun AnimatedBackgroundOrbs(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")

    // Slow floating animations
    val orbX1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbX1"
    )
    val orbY1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbY1"
    )

    val orbX2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbX2"
    )
    val orbY2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbY2"
    )

    val orbX3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbX3"
    )
    val orbY3 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbY3"
    )

    Box(modifier = modifier.background(Color(0xFF0F0F15))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Purple
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC044FF), Color.Transparent),
                    center = Offset(width * orbX1, height * orbY1),
                    radius = width * 0.45f
                ),
                center = Offset(width * orbX1, height * orbY1),
                radius = width * 0.45f
            )

            // Orb 2: Teal
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FFCC), Color.Transparent),
                    center = Offset(width * orbX2, height * orbY2),
                    radius = width * 0.40f
                ),
                center = Offset(width * orbX2, height * orbY2),
                radius = width * 0.40f
            )

            // Orb 3: Peach
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF7E40), Color.Transparent),
                    center = Offset(width * orbX3, height * orbY3),
                    radius = width * 0.35f
                ),
                center = Offset(width * orbX3, height * orbY3),
                radius = width * 0.35f
            )
        }
    }
}

@Composable
fun PremiumLiquidCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(20.dp) },
                effects = {
                    blur(radius = 16f.dp.toPx())
                    lens(
                        refractionHeight = 8f.dp.toPx(),
                        refractionAmount = 12f.dp.toPx(),
                        chromaticAberration = true
                    )
                },
                highlight = {
                    Highlight(
                        style = HighlightStyle.Default(
                            color = Color.White.copy(alpha = 0.3f),
                            angle = -45f
                        ),
                        width = 1.2.dp,
                        blurRadius = 0.8.dp
                    )
                },
                shadow = {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.25f),
                        radius = 12.dp,
                        offset = DpOffset(0.dp, 6.dp)
                    )
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun InteractiveLiquidButton(
    text: String,
    backdrop: Backdrop,
    tintColor: Color,
    textColor: Color,
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
                    vibrancy()
                    blur(radius = 10f.dp.toPx())
                    lens(
                        refractionHeight = 8f.dp.toPx(),
                        refractionAmount = 14f.dp.toPx(),
                        chromaticAberration = true
                    )
                },
                layerBlock = {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
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
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = 8.dp,
                        offset = DpOffset(0.dp, 4.dp)
                    )
                },
                onDrawSurface = {
                    drawRect(tintColor.copy(alpha = if (enabled) 0.18f else 0.08f))
                    drawRect(Color.White.copy(alpha = 0.04f))
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    coroutineScope.launch {
                        pressScale.animateTo(0.94f, spring(dampingRatio = 0.35f, stiffness = 500f))
                        pressScale.animateTo(1.0f, spring(dampingRatio = 0.5f, stiffness = 300f))
                        onClick()
                    }
                }
            )
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) (if (textColor == Color.Black) tintColor else textColor) else Color.White.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
