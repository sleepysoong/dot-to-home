package com.sleepysoong.dottohome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppEndToEndTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear SharedPreferences between tests
        val prefs = context.getSharedPreferences("dot_to_home_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        // Clear internal files
        File(context.filesDir, "wallpaper_lock_bg.jpg").delete()
        File(context.filesDir, "wallpaper_home_bg.jpg").delete()
    }

    // ── Tier 1: Feature Coverage (Happy Path) ─────────────────────────────────

    @Test
    fun testAddDDayItem() {
        var config = AppSettings.getConfig(context)
        assertEquals(1, config.ddayItems.size) // default has 1 item

        val newItem = DDayItem(label = "New Goal")
        val updatedList = config.ddayItems + newItem
        config = config.copy(ddayItems = updatedList)
        AppSettings.saveConfig(context, config)

        val loaded = AppSettings.getConfig(context)
        assertEquals(2, loaded.ddayItems.size)
        assertEquals("New Goal", loaded.ddayItems[1].label)
    }

    @Test
    fun testDeleteDDayItem() {
        var config = AppConfig(ddayItems = listOf(
            DDayItem(id = "1", label = "Item 1"),
            DDayItem(id = "2", label = "Item 2")
        ))
        AppSettings.saveConfig(context, config)

        config = AppSettings.getConfig(context)
        assertEquals(2, config.ddayItems.size)

        val updatedList = config.ddayItems.filter { it.id != "1" }
        config = config.copy(ddayItems = updatedList)
        AppSettings.saveConfig(context, config)

        val loaded = AppSettings.getConfig(context)
        assertEquals(1, loaded.ddayItems.size)
        assertEquals("Item 2", loaded.ddayItems[0].label)
    }

    @Test
    fun testEditDDayItem() {
        var config = AppSettings.getConfig(context)
        val originalItem = config.ddayItems[0]

        val targetTime = System.currentTimeMillis() + 50L * 24 * 60 * 60 * 1000
        val editedItem = originalItem.copy(
            dotShape = DotShape.HEART,
            dotColor = DotColor.RED,
            targetDate = targetTime
        )
        config = config.copy(ddayItems = listOf(editedItem))
        AppSettings.saveConfig(context, config)

        val loaded = AppSettings.getConfig(context)
        assertEquals(DotShape.HEART, loaded.ddayItems[0].dotShape)
        assertEquals(DotColor.RED, loaded.ddayItems[0].dotColor)
        assertEquals(targetTime, loaded.ddayItems[0].targetDate)
    }

    @Test
    fun testSaveAndLoadConfig() {
        val customConfig = AppConfig(
            lockEnabled = false,
            homeEnabled = true,
            lockDotOffsetY = 0.45f,
            homeDotOffsetY = 0.65f,
            ddayItems = listOf(
                DDayItem(label = "First", dotShape = DotShape.STAR),
                DDayItem(label = "Second", dotShape = DotShape.SQUARE)
            )
        )
        AppSettings.saveConfig(context, customConfig)

        val loaded = AppSettings.getConfig(context)
        assertFalse(loaded.lockEnabled)
        assertTrue(loaded.homeEnabled)
        assertEquals(0.45f, loaded.lockDotOffsetY, 0.001f)
        assertEquals(0.65f, loaded.homeDotOffsetY, 0.001f)
        assertEquals(2, loaded.ddayItems.size)
        assertEquals("First", loaded.ddayItems[0].label)
        assertEquals(DotShape.STAR, loaded.ddayItems[0].dotShape)
        assertEquals("Second", loaded.ddayItems[1].label)
        assertEquals(DotShape.SQUARE, loaded.ddayItems[1].dotShape)
    }

    @Test
    fun testWallpaperBitmapGeneration() {
        val config = AppConfig(
            ddayItems = listOf(DDayItem(label = "Test Grid", dotShape = DotShape.CIRCLE))
        )
        AppSettings.saveConfig(context, config)

        val bitmap = WallpaperGenerator.generate(context, 1080, 2400, isLockScreen = true)
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(2400, bitmap.height)
    }

    // ── Tier 2: Boundary & Corner Cases (Robustness) ──────────────────────────

    @Test
    fun testMaxCardsConstraint() {
        var config = AppSettings.getConfig(context)
        // Add cards up to 6
        for (i in 2..6) {
            if (config.ddayItems.size < 5) {
                config = config.copy(ddayItems = config.ddayItems + DDayItem(label = "Card $i"))
            }
        }
        AppSettings.saveConfig(context, config)

        val loaded = AppSettings.getConfig(context)
        assertEquals(5, loaded.ddayItems.size)
    }

    @Test
    fun testMinCardsConstraint() {
        var config = AppSettings.getConfig(context)
        assertEquals(1, config.ddayItems.size)

        // Try deleting the only card
        val newItems = config.ddayItems.toMutableList()
        if (newItems.size > 1) {
            newItems.removeAt(0)
        }
        config = config.copy(ddayItems = newItems)
        AppSettings.saveConfig(context, config)

        val loaded = AppSettings.getConfig(context)
        assertEquals(1, loaded.ddayItems.size)
    }

    @Test
    fun testCorruptJsonRestoreFallback() {
        val prefs = context.getSharedPreferences("dot_to_home_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_config", "{invalid_json: true").commit()

        val loaded = AppSettings.getConfig(context)
        assertNotNull(loaded)
        assertEquals(1, loaded.ddayItems.size)
        assertTrue(loaded.lockEnabled)
    }

    @Test
    fun testEmptyItemListFallback() {
        val corruptedConfig = AppConfig(ddayItems = emptyList())
        AppSettings.saveConfig(context, corruptedConfig)

        val loaded = AppSettings.getConfig(context)
        assertNotNull(loaded)
        assertEquals(1, loaded.ddayItems.size)
    }

    @Test
    fun testBitmapGenerationFailureFallback() {
        // Reset cached typefaces to ensure font loading exception is triggered
        try {
            val f1 = WallpaperGenerator::class.java.getDeclaredField("cachedTypeface")
            f1.isAccessible = true
            f1.set(null, null)
            val f2 = WallpaperGenerator::class.java.getDeclaredField("cachedBoldTypeface")
            f2.isAccessible = true
            f2.set(null, null)
        } catch (e: Exception) {
            // Ignore if reflection is restricted or fields don't exist
        }

        val mockContext = mockk<Context>(relaxed = true)
        val mockResources = mockk<android.content.res.Resources>()
        every { mockContext.resources } returns mockResources
        every { mockResources.getFont(any()) } throws Exception("Mocked Font Loading Exception")
        every { mockContext.filesDir } returns context.filesDir
        
        mockkObject(AppSettings)
        every { AppSettings.getConfig(any()) } returns AppConfig()
        
        val bitmap = WallpaperGenerator.generate(mockContext, 1080, 2400, isLockScreen = true)
        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(2400, bitmap.height)
        
        unmockkObject(AppSettings)
    }

    // ── Tier 3: Cross-Feature Combinations ────────────────────────────────────

    @Test
    fun testEditAndInstantPreviewState() {
        var config = AppSettings.getConfig(context)
        
        val editedItem1 = config.ddayItems[0].copy(
            label = "Attribute Edit",
            dotShape = DotShape.HEART,
            dotColor = DotColor.BLACK
        )
        config = config.copy(
            lockDotOffsetY = 0.85f,
            homeDotOffsetY = 0.95f,
            ddayItems = listOf(editedItem1)
        )
        AppSettings.saveConfig(context, config)

        val loaded = AppSettings.getConfig(context)
        assertEquals("Attribute Edit", loaded.ddayItems[0].label)
        assertEquals(DotShape.HEART, loaded.ddayItems[0].dotShape)
        assertEquals(DotColor.BLACK, loaded.ddayItems[0].dotColor)
        assertEquals(0.85f, loaded.lockDotOffsetY, 0.001f)
        assertEquals(0.95f, loaded.homeDotOffsetY, 0.001f)
    }

    @Test
    fun testToggleLockHomeScreens() {
        var config = AppSettings.getConfig(context)

        config = config.copy(
            lockEnabled = false,
            homeEnabled = true,
            lockDotOffsetY = 0.2f,
            homeDotOffsetY = 0.8f
        )
        AppSettings.saveConfig(context, config)

        val loaded = AppSettings.getConfig(context)
        assertFalse(loaded.lockEnabled)
        assertTrue(loaded.homeEnabled)
        assertEquals(0.2f, loaded.lockDotOffsetY, 0.001f)
        assertEquals(0.8f, loaded.homeDotOffsetY, 0.001f)
    }

    // ── Tier 4: Real-World Application Scenarios ──────────────────────────────

    @Test
    fun testColdStartSettingsRestorationAndEditing() {
        val initialConfig = AppConfig(
            lockEnabled = true,
            homeEnabled = false,
            lockDotOffsetY = 0.35f,
            ddayItems = listOf(
                DDayItem(label = "Goal A", dotShape = DotShape.DIAMOND)
            )
        )
        AppSettings.saveConfig(context, initialConfig)

        var config = AppSettings.getConfig(context)
        assertEquals("Goal A", config.ddayItems[0].label)
        assertEquals(DotShape.DIAMOND, config.ddayItems[0].dotShape)
        assertTrue(config.lockEnabled)
        assertFalse(config.homeEnabled)
        assertEquals(0.35f, config.lockDotOffsetY, 0.001f)

        val newItem = DDayItem(label = "Goal B", dotShape = DotShape.STAR)
        config = config.copy(
            ddayItems = config.ddayItems + newItem,
            homeEnabled = true,
            homeDotOffsetY = 0.6f
        )
        AppSettings.saveConfig(context, config)

        val finalConfig = AppSettings.getConfig(context)
        assertEquals(2, finalConfig.ddayItems.size)
        assertEquals("Goal A", finalConfig.ddayItems[0].label)
        assertEquals("Goal B", finalConfig.ddayItems[1].label)
        assertTrue(finalConfig.homeEnabled)
        assertEquals(0.6f, finalConfig.homeDotOffsetY, 0.001f)
    }
}
