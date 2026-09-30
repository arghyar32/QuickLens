package com.dolphin.quicklens.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dolphin.quicklens.cts.CircleToSearchInvoker
import com.dolphin.quicklens.data.LensPreferencesRepository
import com.dolphin.quicklens.service.QuickLensAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val tileTriggerDelayMs: Int = LensPreferencesRepository.DEFAULT_DELAY_MS,
    val hapticFeedbackEnabled: Boolean = LensPreferencesRepository.DEFAULT_HAPTIC_ENABLED,
    val useNativeCTS: Boolean = LensPreferencesRepository.DEFAULT_USE_NATIVE_CTS,
    val launcherDirectTrigger: Boolean = LensPreferencesRepository.DEFAULT_LAUNCHER_TRIGGER,
    val navBarGestureEnabled: Boolean = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE,
    val navBarGestureIndicator: Boolean = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_INDICATOR,
    val navBarGestureWidthDp: Int = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_WIDTH_DP,
    val navBarGestureOffsetX: Int = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_OFFSET_X,
    val isAccessibilityActive: Boolean = false,
    val isGoogleAssistantConfigured: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false
)

private data class PreferencesState(
    val delayMs: Int,
    val haptics: Boolean,
    val nativeCts: Boolean,
    val launcherTrigger: Boolean,
    val navBarGesture: Boolean,
    val navBarIndicator: Boolean,
    val navBarWidthDp: Int,
    val navBarOffsetX: Int
)

class SettingsViewModel(
    application: Application,
    private val repository: LensPreferencesRepository = LensPreferencesRepository.getInstance(application)
) : AndroidViewModel(application) {

    private val isAssistantConfiguredFlow = MutableStateFlow(
        CircleToSearchInvoker.isGoogleDefaultAssistant(application)
    )

    private val isAccessibilityEnabledFlow = MutableStateFlow(
        QuickLensAccessibilityService.isServiceEnabledInSystem(application)
    )

    private val isIgnoringBatteryOptimizationsFlow = MutableStateFlow(
        checkBatteryOptimizationIgnored(application)
    )

    private val corePrefsFlow = combine(
        repository.tileTriggerDelayMs,
        repository.hapticFeedbackEnabled,
        repository.useNativeCTS
    ) { delayMs, haptics, nativeCts ->
        Triple(delayMs, haptics, nativeCts)
    }

    private val gesturePrefsFlow = combine(
        repository.launcherDirectTrigger,
        repository.navBarGestureEnabled,
        repository.navBarGestureIndicator,
        repository.navBarGestureWidthDp,
        repository.navBarGestureOffsetX
    ) { launcherTrigger, navBarGesture, indicator, widthDp, offsetX ->
        GesturePrefs(launcherTrigger, navBarGesture, indicator, widthDp, offsetX)
    }

    private data class GesturePrefs(
        val launcherTrigger: Boolean,
        val navBarGesture: Boolean,
        val indicator: Boolean,
        val widthDp: Int,
        val offsetX: Int
    )

    private val preferencesFlow = combine(
        corePrefsFlow,
        gesturePrefsFlow
    ) { (delayMs, haptics, nativeCts), g ->
        PreferencesState(
            delayMs,
            haptics,
            nativeCts,
            g.launcherTrigger,
            g.navBarGesture,
            g.indicator,
            g.widthDp,
            g.offsetX
        )
    }

    private val systemStatusFlow = combine(
        isAccessibilityEnabledFlow,
        isAssistantConfiguredFlow,
        isIgnoringBatteryOptimizationsFlow
    ) { isAccessibility, isAssistant, isIgnoringBattery ->
        Triple(isAccessibility, isAssistant, isIgnoringBattery)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesFlow,
        systemStatusFlow
    ) { pref, (isAccessibility, isAssistant, isIgnoringBattery) ->
        SettingsUiState(
            tileTriggerDelayMs = pref.delayMs,
            hapticFeedbackEnabled = pref.haptics,
            useNativeCTS = pref.nativeCts,
            launcherDirectTrigger = pref.launcherTrigger,
            navBarGestureEnabled = pref.navBarGesture,
            navBarGestureIndicator = pref.navBarIndicator,
            navBarGestureWidthDp = pref.navBarWidthDp,
            navBarGestureOffsetX = pref.navBarOffsetX,
            isAccessibilityActive = isAccessibility,
            isGoogleAssistantConfigured = isAssistant,
            isIgnoringBatteryOptimizations = isIgnoringBattery
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            isAccessibilityActive = QuickLensAccessibilityService.isServiceEnabledInSystem(application),
            isGoogleAssistantConfigured = CircleToSearchInvoker.isGoogleDefaultAssistant(application),
            isIgnoringBatteryOptimizations = checkBatteryOptimizationIgnored(application)
        )
    )

    fun refreshStatus() {
        val app = getApplication<Application>()
        isAccessibilityEnabledFlow.value = QuickLensAccessibilityService.isServiceEnabledInSystem(app)
        isAssistantConfiguredFlow.value = CircleToSearchInvoker.isGoogleDefaultAssistant(app)
        isIgnoringBatteryOptimizationsFlow.value = checkBatteryOptimizationIgnored(app)
    }

    private fun checkBatteryOptimizationIgnored(application: Application): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = application.getSystemService(PowerManager::class.java)
            powerManager?.isIgnoringBatteryOptimizations(application.packageName) ?: false
        } else {
            true
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(intent)
    }

    fun openAssistantSettings() {
        CircleToSearchInvoker.openAssistantSettings(getApplication())
    }

    fun openBatteryOptimizationSettings() {
        val app = getApplication<Application>()
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${app.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        runCatching { app.startActivity(intent) }
    }

    fun openMiuiAutostartSettings() {
        val app = getApplication<Application>()
        val intents = listOf(
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
            }
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(app.packageManager) != null) {
                runCatching {
                    app.startActivity(intent)
                    return
                }
            }
        }
    }

    fun onDelayChanged(delayMs: Int) {
        viewModelScope.launch {
            repository.setTileTriggerDelayMs(delayMs)
        }
    }

    fun onHapticsToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun onNativeCtsToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUseNativeCTS(enabled)
        }
    }

    fun onLauncherDirectTriggerToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLauncherDirectTrigger(enabled)
        }
    }

    fun onNavBarGestureToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNavBarGestureEnabled(enabled)
        }
    }

    fun onNavBarGestureIndicatorToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNavBarGestureIndicator(enabled)
        }
    }

    fun onNavBarGestureWidthChanged(widthDp: Int) {
        viewModelScope.launch {
            repository.setNavBarGestureWidthDp(widthDp)
        }
    }

    fun onNavBarGestureOffsetXChanged(offsetX: Int) {
        viewModelScope.launch {
            repository.setNavBarGestureOffsetX(offsetX)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application) as T
        }
    }
}
