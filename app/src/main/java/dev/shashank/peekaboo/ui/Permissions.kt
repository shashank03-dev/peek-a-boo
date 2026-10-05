package dev.shashank.peekaboo.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object Permissions {
    fun camera(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun notifications(ctx: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun overlay(ctx: Context) = Settings.canDrawOverlays(ctx)

    fun battery(ctx: Context) =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun overlayIntent(ctx: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))

    /**
     * Where the user can make the app's battery use unrestricted. Asking directly needs a
     * permission Google Play restricts, so this opens settings instead: the app's own page on
     * Android 12+ (App battery usage › Unrestricted), the optimisation list before that.
     */
    fun batteryIntent(ctx: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) appSettingsIntent(ctx)
        else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun appSettingsIntent(ctx: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
}

data class PermissionSnapshot(
    val camera: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
    val battery: Boolean,
) {
    companion object {
        fun of(ctx: Context) = PermissionSnapshot(
            Permissions.camera(ctx), Permissions.overlay(ctx), Permissions.notifications(ctx), Permissions.battery(ctx),
        )
    }
}
