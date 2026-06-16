package com.sleepysoong.dottohome

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
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
            val todayKST = AppConfig.getTodayKST()
            if (todayKST == AppSettings.getLastUpdateDate(applicationContext)) {
                Log.d("WallpaperWorker", "이미 오늘 배경화면이 업데이트되었습니다. 스킵합니다.")
                scheduleNextMidnightUpdate(applicationContext)
                return@withContext Result.success()
            }

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
            AppSettings.saveLastUpdateDate(applicationContext, todayKST)

            // Schedule the next update for midnight
            scheduleNextMidnightUpdate(applicationContext)

            showNotification()

            Log.d("WallpaperWorker", "배경화면 업데이트 완료!")
            Result.success()
        } catch (e: Exception) {
            Log.e("WallpaperWorker", "배경화면 업데이트 실패", e)
            Result.retry()
        }
    }

    private fun showNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "wallpaper_update_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "디데이 배경화면 알림", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "배경화면이 새로 갱신될 때 알림을 보냅니다."
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("배경화면 업데이트 완료!")
            .setContentText("오늘의 새로운 도트가 찍혔습니다. 폰 배경을 확인해보세요!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
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

            // 1. OneTimeWorkRequest for midnight
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

            // 2. Exact Alarm for midnight
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MidnightUpdateReceiver::class.java).apply {
                action = "com.sleepysoong.dottohome.ACTION_MIDNIGHT_ALARM"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pendingIntent)
                }
            } catch (e: Exception) {
                Log.e("WallpaperWorker", "알람 설정 중 오류 발생", e)
            }

            // 3. Periodic Failsafe Worker (Runs every 2 hours)
            val periodicRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(2, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "BackupPeriodicUpdate",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        fun cancelDailyUpdate(context: Context) {
            Log.d("WallpaperWorker", "업데이트 취소...")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork("BackupPeriodicUpdate")
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MidnightUpdateReceiver::class.java).apply {
                action = "com.sleepysoong.dottohome.ACTION_MIDNIGHT_ALARM"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
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
