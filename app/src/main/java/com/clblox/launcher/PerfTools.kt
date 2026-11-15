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

    /** 0 = cool, 1 = warm, 2 = hot, 3 = very hot, 4 = critical. -1 if unsupported. */
    fun thermalLevel(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1
        val status = context.getSystemService(PowerManager::class.java).currentThermalStatus
        return status.coerceIn(0, 4)
    }

    fun batterySaverOn(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isPowerSaveMode

    /** Current Game Mode of this app's category (Android 12+). Reading only. */
    fun gameMode(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "Needs Android 12+"
        return when (context.getSystemService(GameManager::class.java).gameMode) {
            GameManager.GAME_MODE_PERFORMANCE -> "Performance"
            GameManager.GAME_MODE_BATTERY -> "Battery"
            GameManager.GAME_MODE_STANDARD -> "Standard"
            else -> "Unknown"
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
