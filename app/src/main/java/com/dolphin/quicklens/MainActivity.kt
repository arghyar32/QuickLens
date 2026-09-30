package com.dolphin.quicklens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dolphin.quicklens.cts.CircleToSearchInvoker
import com.dolphin.quicklens.data.LensPreferencesRepository
import com.dolphin.quicklens.service.QuickLensAccessibilityService
import com.dolphin.quicklens.ui.SettingsScreen
import com.dolphin.quicklens.ui.SettingsViewModel
import com.dolphin.quicklens.ui.TriggerActivity
import com.dolphin.quicklens.ui.theme.QuickLensTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forceSettings = intent.getBooleanExtra(EXTRA_FORCE_SETTINGS, false)
        val repository = LensPreferencesRepository.getInstance(applicationContext)

        if (!forceSettings && repository.isLauncherDirectTriggerEnabledSync()) {
            val isAccessibilityActive = QuickLensAccessibilityService.isServiceEnabledInSystem(this)
            if (isAccessibilityActive) {
                val triggerIntent = Intent(this, TriggerActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(triggerIntent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                }
                finish()
                return
            }
        }

        enableEdgeToEdge()

        setContent {
            QuickLensTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

                SettingsScreen(
                    state = uiState,
                    onDelayChanged = viewModel::onDelayChanged,
                    onHapticsToggled = viewModel::onHapticsToggled,
                    onNativeCtsToggled = viewModel::onNativeCtsToggled,
                    onLauncherTriggerToggled = viewModel::onLauncherDirectTriggerToggled,
                    onNavBarGestureToggled = viewModel::onNavBarGestureToggled,
                    onNavBarGestureIndicatorToggled = viewModel::onNavBarGestureIndicatorToggled,
                    onNavBarGestureWidthChanged = viewModel::onNavBarGestureWidthChanged,
                    onNavBarGestureOffsetXChanged = viewModel::onNavBarGestureOffsetXChanged,
                    onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                    onOpenBatterySettings = viewModel::openBatteryOptimizationSettings,
                    onOpenAutostartSettings = viewModel::openMiuiAutostartSettings,
                    onTestTrigger = {
                        moveTaskToBack(true)
                        val triggerIntent = Intent(this, TriggerActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(triggerIntent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }

    companion object {
        const val EXTRA_FORCE_SETTINGS = "force_settings"
    }
}