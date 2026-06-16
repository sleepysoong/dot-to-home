package com.sleepysoong.dottohome

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class WallpaperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("WallpaperWorker", "배경화면 업데이트 시작...")

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
                if (config.homeEnabled) {
                    val homeBitmap = WallpaperGenerator.generate(applicationContext, width, height, isLockScreen = false)
                    wm.setBitmap(homeBitmap)
                }
            }

            // Record the date of the successful update
            AppSettings.saveLastUpdateDate(applicationContext, AppConfig.getTodayKST())

            // Schedule the next update for midnight
            scheduleNextMidnightUpdate(applicationContext)

            Log.d("WallpaperWorker", "배경화면 업데이트 완료!")
            Result.success()
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "배경화면 업데이트 실패", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "MidnightWallpaperUpdateWork"

        fun enqueueImmediateUpdate(context: Context) {
            Log.d("WallpaperWorker", "즉시 업데이트 큐 삽입")
            val workRequest = OneTimeWorkRequestBuilder<WallpaperWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "ImmediateWallpaperUpdate",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun scheduleNextMidnightUpdate(context: Context) {
            val kst = TimeZone.getTimeZone("Asia/Seoul")
            val now = Calendar.getInstance(kst)
            val nextMidnight = Calendar.getInstance(kst).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val delayMillis = nextMidnight.timeInMillis - now.timeInMillis

            Log.d("WallpaperWorker", "다음 자정까지 ${delayMillis / 1000}초 후 업데이트 예약...")

            val workRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelDailyUpdate(context: Context) {
            Log.d("WallpaperWorker", "업데이트 취소...")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
        
        fun scheduleDailyUpdate(context: Context) {
            val todayKST = AppConfig.getTodayKST()
            val lastUpdate = AppSettings.getLastUpdateDate(context)
            if (todayKST != lastUpdate) {
                enqueueImmediateUpdate(context)
            } else {
                scheduleNextMidnightUpdate(context)
            }
        }
    }
}
