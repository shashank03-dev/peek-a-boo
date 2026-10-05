package dev.shashank.peekaboo.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Which app is on screen, from the system's usage events. Needs the "Usage access" special
 * permission, which the user grants in system settings; without it everything reads as unknown.
 */
class ForegroundApp(private val context: Context) {
    private val usm = context.getSystemService(UsageStatsManager::class.java)
    private var lastQuery = 0L
    private var current: String? = null

    /** Package of the app in front, refreshed from events since the last call. */
    fun packageName(now: Long = System.currentTimeMillis()): String? {
        // Without usage access the query just comes back empty; start over once it's granted.
        if (!granted(context)) {
            lastQuery = 0L
            return null
        }
        val from = if (lastQuery == 0L) now - 6 * 60 * 60_000 else lastQuery - 2_000
        lastQuery = now
        val events = runCatching { usm.queryEvents(from, now) }.getOrNull() ?: return current
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            @Suppress("DEPRECATION")
            val resumed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                e.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (resumed) current = e.packageName
        }
        return current
    }

    companion object {
        fun granted(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java)
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

        /** Launchable apps, for the protected-apps picker. */
        fun launchable(context: Context): List<AppEntry> {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)
            }
            return list
                .map { it.activityInfo.packageName to it }
                .distinctBy { it.first }
                .filter { it.first != context.packageName }
                .map { (pkg, info) -> AppEntry(pkg, info.loadLabel(pm).toString(), info.loadIcon(pm)) }
                .sortedBy { it.label.lowercase() }
        }
    }
}

data class AppEntry(val packageName: String, val label: String, val icon: Drawable)
