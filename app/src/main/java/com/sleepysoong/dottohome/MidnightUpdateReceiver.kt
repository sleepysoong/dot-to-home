package com.sleepysoong.dottohome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MidnightUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("MidnightUpdateReceiver", "Received action: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_USER_PRESENT || 
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == "com.sleepysoong.dottohome.ACTION_MIDNIGHT_ALARM") {
            
            val todayKST = AppConfig.getTodayKST()
            val lastUpdate = AppSettings.getLastUpdateDate(context)
            
            if (todayKST != lastUpdate) {
                Log.d("MidnightUpdateReceiver", "Date changed. Enqueuing immediate wallpaper update.")
                WallpaperWorker.enqueueImmediateUpdate(context)
            }
        }
    }
}
