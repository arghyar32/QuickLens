package com.dolphin.quicklens.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.dolphin.quicklens.cts.CircleToSearchInvoker
import com.dolphin.quicklens.data.LensPreferencesRepository
import com.dolphin.quicklens.service.QuickLensAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TriggerActivity : Activity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableActivityTransitions()

        activityScope.launch {
            val repository = LensPreferencesRepository.getInstance(applicationContext)
            val delayMs = repository.tileTriggerDelayMs.first().toLong()
            val vibrate = repository.hapticFeedbackEnabled.first()
            val useNativeCTS = repository.useNativeCTS.first()

            if (delayMs > 0) {
                delay(delayMs)
            }

            var triggered = false

            // Strategy 1: Check if Accessibility Service is enabled in system settings
            if (QuickLensAccessibilityService.isServiceEnabledInSystem(this@TriggerActivity)) {
                // Wait briefly for service binding if app was recently cleared from RAM
                val service = QuickLensAccessibilityService.awaitInstance(timeoutMs = 600L)
                if (service != null) {
                    service.captureAndLaunch(
                        delayMs = 0L,
                        vibrate = vibrate
                    )
                    triggered = true
                }
            }

            // Strategy 2: If accessibility wasn't used/enabled, try Native CTS if requested
            if (!triggered && useNativeCTS) {
                triggered = CircleToSearchInvoker.trigger(
                    context = this@TriggerActivity,
                    entryPoint = 1,
                    vibrate = vibrate
                )
            }

            if (!triggered) {
                if (!QuickLensAccessibilityService.isServiceEnabledInSystem(this@TriggerActivity)) {
                    Toast.makeText(
                        this@TriggerActivity,
                        "Enable QuickLens in Accessibility for 1-tap capture without permissions",
                        Toast.LENGTH_LONG
                    ).show()
                    val accessibilityIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(accessibilityIntent)
                } else {
                    Toast.makeText(this@TriggerActivity, "QuickLens service is waking up, please tap again", Toast.LENGTH_SHORT).show()
                }
            }

            finishSafely()
        }
    }

    private fun disableActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun finishSafely() {
        finish()
        disableActivityTransitions()
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
