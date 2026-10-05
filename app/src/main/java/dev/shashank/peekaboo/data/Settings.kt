package dev.shashank.peekaboo.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    )

    suspend fun setGuardEnabled(v: Boolean) = context.dataStore.edit { it[K.guard] = v }
    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[K.onboarding] = v }
    suspend fun setShowNotch(v: Boolean) = context.dataStore.edit { it[K.notch] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[K.haptics] = v }
    suspend fun setSnapshots(v: Boolean) = context.dataStore.edit { it[K.snapshots] = v }
    suspend fun setStrictMode(v: Boolean) = context.dataStore.edit { it[K.strict] = v }
    suspend fun setStartOnBoot(v: Boolean) = context.dataStore.edit { it[K.boot] = v }
    suspend fun setSensitivity(v: Sensitivity) = context.dataStore.edit { it[K.sensitivity] = v.ordinal }
    suspend fun setNotchOffset(v: Int) = context.dataStore.edit { it[K.notchOffset] = v }
}
