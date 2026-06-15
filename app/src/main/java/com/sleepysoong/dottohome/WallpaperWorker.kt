package com.sleepysoong.dottohome

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("WallpaperWorker", "Starting wallpaper background task...")
            
            val metrics = applicationContext.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            
            // Render the wallpaper bitmap
            val bitmap = WallpaperGenerator.generate(applicationContext, width, height)
            
            // Set system and lock screen wallpapers
            val wm = WallpaperManager.getInstance(applicationContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } else {
                wm.setBitmap(bitmap)
            }
            
            Log.d("WallpaperWorker", "Wallpaper updated successfully!")
            Result.success()
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "Error updating wallpaper in background", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyWallpaperUpdateWork"

        fun scheduleDailyUpdate(context: Context) {
            Log.d("WallpaperWorker", "Scheduling unique daily wallpaper worker...")
            
            // Run daily (once every 24 hours)
            val workRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
                24, TimeUnit.HOURS
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // Update config if changed
                workRequest
            )
        }

        fun cancelDailyUpdate(context: Context) {
            Log.d("WallpaperWorker", "Cancelling background wallpaper worker...")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
