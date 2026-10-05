package dev.shashank.peekaboo.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class Sensitivity(val label: String, val dwellMs: Long, val minFaceFraction: Float, val maxYawDeg: Float) {
    Low("Relaxed", 1500, 0.14f, 25f),
    Balanced("Balanced", 800, 0.10f, 32f),
    High("Paranoid", 350, 0.07f, 40f),
}

/** When the Privacy Shield covers the screen. */
enum class ShieldMode(val label: String) {
    Off("Off"),
    OnPeek("On peek"),
    Always("Always"),
}

/** What the shield looks like. All styles stay see-through enough to keep apps usable. */
enum class ShieldStyle(val label: String) {
    Louver("Louver"),
    Dim("Dim"),
    Grain("Grain"),
}

data class GuardSettings(
    val guardEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
    val showNotch: Boolean = true,
    val haptics: Boolean = true,
    val snapshots: Boolean = true,
    val strictMode: Boolean = false,
    val startOnBoot: Boolean = true,
    val sensitivity: Sensitivity = Sensitivity.Balanced,
    val notchOffsetDp: Int = 0,
    // Pro
    val shieldMode: ShieldMode = ShieldMode.OnPeek,
    val shieldStyle: ShieldStyle = ShieldStyle.Louver,
    /** Shield opacity in percent; capped so touches still reach the app underneath. */
    val shieldStrength: Int = 70,
    val blackoutOnPeek: Boolean = false,
    /** Shield and blackout only kick in while one of [protectedApps] is open. */
    val protectedOnly: Boolean = false,
    val protectedApps: Set<String> = emptySet(),
    val strangerAlert: Boolean = true,
)

class SettingsRepository(private val context: Context) {
    private object K {
        val guard = booleanPreferencesKey("guard_enabled")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val notch = booleanPreferencesKey("show_notch")
        val haptics = booleanPreferencesKey("haptics")
        val snapshots = booleanPreferencesKey("snapshots")
        val strict = booleanPreferencesKey("strict_mode")
        val boot = booleanPreferencesKey("start_on_boot")
        val sensitivity = intPreferencesKey("sensitivity")
        val notchOffset = intPreferencesKey("notch_offset")
        val shieldMode = intPreferencesKey("shield_mode")
        val shieldStyle = intPreferencesKey("shield_style")
        val shieldStrength = intPreferencesKey("shield_strength")
        val blackout = booleanPreferencesKey("blackout_on_peek")
        val protectedOnly = booleanPreferencesKey("protected_only")
        val protectedApps = stringSetPreferencesKey("protected_apps")
        val stranger = booleanPreferencesKey("stranger_alert")
        val proCached = booleanPreferencesKey("pro_cached")
    }

    val settings: Flow<GuardSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): GuardSettings = settings.first()

    private fun Preferences.toSettings() = GuardSettings(
        guardEnabled = this[K.guard] ?: false,
        onboardingDone = this[K.onboarding] ?: false,
        showNotch = this[K.notch] ?: true,
        haptics = this[K.haptics] ?: true,
        snapshots = this[K.snapshots] ?: true,
        strictMode = this[K.strict] ?: false,
        startOnBoot = this[K.boot] ?: true,
        sensitivity = Sensitivity.entries.getOrElse(this[K.sensitivity] ?: 1) { Sensitivity.Balanced },
        notchOffsetDp = this[K.notchOffset] ?: 0,
        shieldMode = ShieldMode.entries.getOrElse(this[K.shieldMode] ?: 1) { ShieldMode.OnPeek },
        shieldStyle = ShieldStyle.entries.getOrElse(this[K.shieldStyle] ?: 0) { ShieldStyle.Louver },
        shieldStrength = this[K.shieldStrength] ?: 70,
        blackoutOnPeek = this[K.blackout] ?: false,
        protectedOnly = this[K.protectedOnly] ?: false,
        protectedApps = this[K.protectedApps] ?: emptySet(),
        strangerAlert = this[K.stranger] ?: true,
    )

    /** Last known Pro entitlement, so the guard knows it before Play billing has answered. */
    val proCached: Flow<Boolean> = context.dataStore.data.map { it[K.proCached] ?: false }

    suspend fun setGuardEnabled(v: Boolean) = context.dataStore.edit { it[K.guard] = v }
    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[K.onboarding] = v }
    suspend fun setShowNotch(v: Boolean) = context.dataStore.edit { it[K.notch] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[K.haptics] = v }
    suspend fun setSnapshots(v: Boolean) = context.dataStore.edit { it[K.snapshots] = v }
    suspend fun setStrictMode(v: Boolean) = context.dataStore.edit { it[K.strict] = v }
    suspend fun setStartOnBoot(v: Boolean) = context.dataStore.edit { it[K.boot] = v }
    suspend fun setSensitivity(v: Sensitivity) = context.dataStore.edit { it[K.sensitivity] = v.ordinal }
    suspend fun setNotchOffset(v: Int) = context.dataStore.edit { it[K.notchOffset] = v }
    suspend fun setShieldMode(v: ShieldMode) = context.dataStore.edit { it[K.shieldMode] = v.ordinal }
    suspend fun setShieldStyle(v: ShieldStyle) = context.dataStore.edit { it[K.shieldStyle] = v.ordinal }
    suspend fun setShieldStrength(v: Int) = context.dataStore.edit { it[K.shieldStrength] = v }
    suspend fun setBlackoutOnPeek(v: Boolean) = context.dataStore.edit { it[K.blackout] = v }
    suspend fun setProtectedOnly(v: Boolean) = context.dataStore.edit { it[K.protectedOnly] = v }
    suspend fun setProtectedApp(pkg: String, on: Boolean) = context.dataStore.edit {
        val cur = it[K.protectedApps] ?: emptySet()
        it[K.protectedApps] = if (on) cur + pkg else cur - pkg
    }
    suspend fun setStrangerAlert(v: Boolean) = context.dataStore.edit { it[K.stranger] = v }
    suspend fun setProCached(v: Boolean) = context.dataStore.edit { it[K.proCached] = v }
}
