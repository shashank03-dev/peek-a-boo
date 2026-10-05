package dev.shashank.peekaboo.ui

import android.Manifest
import android.annotation.SuppressLint
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

    @SuppressLint("BatteryLife")
    fun batteryIntent(ctx: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))

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
