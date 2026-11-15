package com.clblox.launcher

import android.app.GameManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Safe, system-level helpers only. We cannot (and do not try to) change Roblox's own
 * files or FPS cap. These only adjust things Android lets any app ask the user about.
 */
object PerfTools {

    fun thermalStatus(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Not supported on this Android version"
        val pm = context.getSystemService(PowerManager::class.java)
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "Cool"
            PowerManager.THERMAL_STATUS_LIGHT -> "Warm"
            PowerManager.THERMAL_STATUS_MODERATE -> "Hot (performance may drop)"
            PowerManager.THERMAL_STATUS_SEVERE -> "Very hot"
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "Critical - stop playing"
            else -> "Unknown"
        }
    }

    fun batterySaverOn(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isPowerSaveMode

    /** Current Game Mode of this app's category (Android 12+). Reading only. */
    fun gameMode(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "Not supported (Android 12+)"
        return when (context.getSystemService(GameManager::class.java).gameMode) {
            GameManager.GAME_MODE_PERFORMANCE -> "Performance"
            GameManager.GAME_MODE_BATTERY -> "Battery"
            GameManager.GAME_MODE_STANDARD -> "Standard"
            else -> "Unsupported / unknown"
        }
    }

    fun dndAccessGranted(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun setDnd(context: Context, enabled: Boolean): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return false
        nm.setInterruptionFilter(
            if (enabled) NotificationManager.INTERRUPTION_FILTER_NONE
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return true
    }

    fun dndPermissionIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun displaySettingsIntent(): Intent =
        Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun batterySettingsIntent(): Intent =
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
