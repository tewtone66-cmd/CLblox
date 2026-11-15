package com.clblox.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Opens the OFFICIAL Roblox app through its public deep link.
 * Nothing in the Roblox app is modified.
 */
object RobloxLauncher {
    const val ROBLOX_PACKAGE = "com.roblox.client"

    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(ROBLOX_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    /** Accepts a plain place id or a roblox.com/games/<id>/... link. Returns null if invalid. */
    fun parsePlaceId(input: String): Long? {
        val text = input.trim()
        text.toLongOrNull()?.let { return it }
        return Regex("/games/(\\d+)").find(text)?.groupValues?.get(1)?.toLongOrNull()
    }

    /** @return true if an app handled the intent. */
    fun launchGame(context: Context, placeId: Long): Boolean {
        val uri = Uri.parse("https://www.roblox.com/games/$placeId")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(ROBLOX_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun openStorePage(context: Context) {
        val uri = Uri.parse("market://details?id=$ROBLOX_PACKAGE")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$ROBLOX_PACKAGE")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
