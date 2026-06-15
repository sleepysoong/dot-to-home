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
            Log.d("WallpaperWorker", "배경화면 자동 업데이트 시작...")

            val metrics = applicationContext.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            val wm = WallpaperManager.getInstance(applicationContext)

            val config = AppSettings.getConfig(applicationContext)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (config.lockEnabled) {
                    val lockBitmap = WallpaperGenerator.generate(applicationContext, width, height, isLockScreen = true)
                    wm.setBitmap(lockBitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
                if (config.homeEnabled) {
                    val homeBitmap = WallpaperGenerator.generate(applicationContext, width, height, isLockScreen = false)
                    wm.setBitmap(homeBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                }
            } else {
                // Pre-N, can only set one wallpaper
                if (config.homeEnabled) {
                    val homeBitmap = WallpaperGenerator.generate(applicationContext, width, height, isLockScreen = false)
                    wm.setBitmap(homeBitmap)
                }
            }

            Log.d("WallpaperWorker", "배경화면 업데이트 완료!")
            Result.success()
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "배경화면 업데이트 실패", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyWallpaperUpdateWork"

        fun scheduleDailyUpdate(context: Context) {
            Log.d("WallpaperWorker", "일일 배경화면 업데이트 예약...")

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
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancelDailyUpdate(context: Context) {
            Log.d("WallpaperWorker", "일일 배경화면 업데이트 취소...")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
