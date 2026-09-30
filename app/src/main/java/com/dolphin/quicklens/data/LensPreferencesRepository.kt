package com.dolphin.quicklens.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quicklens_preferences")

class LensPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val TILE_TRIGGER_DELAY_MS = intPreferencesKey("tile_trigger_delay_ms")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val USE_NATIVE_CTS = booleanPreferencesKey("use_native_cts")
        val LAUNCHER_DIRECT_TRIGGER = booleanPreferencesKey("launcher_direct_trigger")
        val NAV_BAR_GESTURE_ENABLED = booleanPreferencesKey("nav_bar_gesture_enabled")
        val NAV_BAR_GESTURE_INDICATOR = booleanPreferencesKey("nav_bar_gesture_indicator")
        val NAV_BAR_GESTURE_WIDTH_DP = intPreferencesKey("nav_bar_gesture_width_dp")
        val NAV_BAR_GESTURE_OFFSET_X = intPreferencesKey("nav_bar_gesture_offset_x")
    }

    val tileTriggerDelayMs: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.TILE_TRIGGER_DELAY_MS] ?: DEFAULT_DELAY_MS
        }

    val hapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] ?: DEFAULT_HAPTIC_ENABLED
        }

    val useNativeCTS: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USE_NATIVE_CTS] ?: DEFAULT_USE_NATIVE_CTS
        }

    val launcherDirectTrigger: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAUNCHER_DIRECT_TRIGGER] ?: DEFAULT_LAUNCHER_TRIGGER
        }

    val navBarGestureEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NAV_BAR_GESTURE_ENABLED] ?: DEFAULT_NAV_BAR_GESTURE
        }

    val navBarGestureIndicator: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NAV_BAR_GESTURE_INDICATOR] ?: DEFAULT_NAV_BAR_GESTURE_INDICATOR
        }

    val navBarGestureWidthDp: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NAV_BAR_GESTURE_WIDTH_DP] ?: DEFAULT_NAV_BAR_GESTURE_WIDTH_DP
        }

    val navBarGestureOffsetX: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NAV_BAR_GESTURE_OFFSET_X] ?: DEFAULT_NAV_BAR_GESTURE_OFFSET_X
        }

    suspend fun setTileTriggerDelayMs(delayMs: Int) {
        val sanitizedDelay = delayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TILE_TRIGGER_DELAY_MS] = sanitizedDelay
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun setUseNativeCTS(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_NATIVE_CTS] = enabled
        }
    }

    suspend fun setLauncherDirectTrigger(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAUNCHER_DIRECT_TRIGGER] = enabled
        }
    }

    suspend fun setNavBarGestureEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAV_BAR_GESTURE_ENABLED] = enabled
        }
    }

    suspend fun setNavBarGestureIndicator(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAV_BAR_GESTURE_INDICATOR] = enabled
        }
    }

    suspend fun setNavBarGestureWidthDp(widthDp: Int) {
        val sanitized = widthDp.coerceIn(MIN_NAV_BAR_GESTURE_WIDTH_DP, MAX_NAV_BAR_GESTURE_WIDTH_DP)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAV_BAR_GESTURE_WIDTH_DP] = sanitized
        }
    }

    suspend fun setNavBarGestureOffsetX(offsetX: Int) {
        val sanitized = offsetX.coerceIn(MIN_NAV_BAR_GESTURE_OFFSET_X, MAX_NAV_BAR_GESTURE_OFFSET_X)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAV_BAR_GESTURE_OFFSET_X] = sanitized
        }
    }

    fun isLauncherDirectTriggerEnabledSync(): Boolean {
        return runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(250) {
                context.dataStore.data.first()[PreferencesKeys.LAUNCHER_DIRECT_TRIGGER]
            } ?: DEFAULT_LAUNCHER_TRIGGER
        }
    }

    companion object {
        const val DEFAULT_DELAY_MS = 300
        const val MIN_DELAY_MS = 0
        const val MAX_DELAY_MS = 1200
        const val DELAY_STEP_MS = 50
        const val DEFAULT_HAPTIC_ENABLED = true
        const val DEFAULT_USE_NATIVE_CTS = true
        const val DEFAULT_LAUNCHER_TRIGGER = true
        const val DEFAULT_NAV_BAR_GESTURE = true
        const val DEFAULT_NAV_BAR_GESTURE_INDICATOR = true
        const val DEFAULT_NAV_BAR_GESTURE_WIDTH_DP = 60
        const val MIN_NAV_BAR_GESTURE_WIDTH_DP = 60
        const val MAX_NAV_BAR_GESTURE_WIDTH_DP = 240
        const val DEFAULT_NAV_BAR_GESTURE_OFFSET_X = 0
        const val MIN_NAV_BAR_GESTURE_OFFSET_X = -100
        const val MAX_NAV_BAR_GESTURE_OFFSET_X = 100

        @Volatile
        private var instance: LensPreferencesRepository? = null

        fun getInstance(context: Context): LensPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: LensPreferencesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
