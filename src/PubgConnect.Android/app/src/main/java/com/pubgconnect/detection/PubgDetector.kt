package com.pubgconnect.detection

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log

object PubgDetector {

    private const val TAG = "PubgDetector"

    // Recognized PUBG Mobile Android Package Names
    val PUBG_PACKAGE_NAMES = setOf(
        "com.tencent.ig",           // PUBG Mobile (Global)
        "com.pubg.krmobile",        // PUBG Mobile (Korea / Japan)
        "com.vng.pubgmobile",       // PUBG Mobile (Vietnam)
        "com.pubg.imobile",         // Battlegrounds Mobile India (BGMI)
        "com.tencent.tmgp.pubgmhd", // PUBG Mobile (HD / Timi)
        "com.rekoo.pubgm"           // PUBG Mobile (Taiwan)
    )

    /**
     * Checks if Usage Access permission is granted by the user.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Opens the Android system Settings screen for Usage Access.
     */
    fun openUsageStatsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Queries the most recent foreground application using UsageStatsManager.
     * Guaranteed ultra-low resource usage by inspecting only recent events (last 15 seconds).
     */
    fun isPubgRunning(context: Context): Boolean {
        if (!hasUsageStatsPermission(context)) {
            return false
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return false

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 30 // Look back 30 seconds

        return try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            var lastForegroundPackage: String? = null

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPackage = event.packageName
                } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                    event.eventType == UsageEvents.Event.ACTIVITY_STOPPED
                ) {
                    if (event.packageName == lastForegroundPackage) {
                        lastForegroundPackage = null
                    }
                }
            }

            if (lastForegroundPackage != null && PUBG_PACKAGE_NAMES.contains(lastForegroundPackage)) {
                Log.d(TAG, "PUBG Mobile foreground activity detected: $lastForegroundPackage")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking UsageStats: ${e.message}", e)
            false
        }
    }

    /**
     * Checks if any PUBG Mobile package is installed on this device.
     */
    fun isPubgInstalled(context: Context): Boolean {
        val pm = context.packageManager
        for (pkg in PUBG_PACKAGE_NAMES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: Exception) { }
        }
        return false
    }
}
