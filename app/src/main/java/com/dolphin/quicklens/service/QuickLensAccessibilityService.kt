package com.dolphin.quicklens.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.dolphin.quicklens.data.LensPreferencesRepository
import com.dolphin.quicklens.gesture.NavBarGestureOverlay
import com.dolphin.quicklens.intent.LensIntentDispatcher
import com.dolphin.quicklens.ui.TriggerActivity
import com.dolphin.quicklens.util.HapticFeedbackHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

class QuickLensAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var gestureOverlay: NavBarGestureOverlay? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        _isServiceActive.value = true
        setupGestureOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        gestureOverlay?.detach()
        gestureOverlay = null
        instance = null
        _isServiceActive.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun setupGestureOverlay() {
        gestureOverlay = NavBarGestureOverlay(this) {
            val triggerIntent = Intent(this, TriggerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(triggerIntent)
        }

        val repository = LensPreferencesRepository.getInstance(this)
        serviceScope.launch(Dispatchers.Main) {
            combine(
                repository.navBarGestureEnabled,
                repository.navBarGestureIndicator,
                repository.navBarGestureWidthDp,
                repository.navBarGestureOffsetX
            ) { enabled, indicator, widthDp, offsetX ->
                GestureOverlayConfig(enabled, indicator, widthDp, offsetX)
            }.collect { config ->
                if (config.enabled) {
                    gestureOverlay?.attach(config.indicator, config.widthDp, config.offsetX)
                } else {
                    gestureOverlay?.detach()
                }
            }
        }
    }

    private data class GestureOverlayConfig(
        val enabled: Boolean,
        val indicator: Boolean,
        val widthDp: Int,
        val offsetX: Int
    )

    fun captureAndLaunch(delayMs: Long, vibrate: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        serviceScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                withContext(Dispatchers.Main) {
                    takeScreenshot(
                        Display.DEFAULT_DISPLAY,
                        mainExecutor,
                        object : TakeScreenshotCallback {
                            override fun onSuccess(screenshot: ScreenshotResult) {
                                processScreenshot(screenshot, vibrate, onComplete)
                            }

                            override fun onFailure(errorCode: Int) {
                                onComplete?.invoke(false)
                            }
                        }
                    )
                }
            } else {
                onComplete?.invoke(false)
            }
        }
    }

    private fun processScreenshot(
        screenshot: ScreenshotResult,
        vibrate: Boolean,
        onComplete: ((Boolean) -> Unit)?
    ) {
        serviceScope.launch(Dispatchers.IO) {
            var softwareBitmap: Bitmap? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val hwBuffer = screenshot.hardwareBuffer
                    val colorSpace = screenshot.colorSpace
                    val hwBitmap = Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                    softwareBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                    hwBuffer.close()
                }

                if (softwareBitmap != null) {
                    if (vibrate) {
                        HapticFeedbackHelper.performClick(this@QuickLensAccessibilityService)
                    }

                    val outputFile = File(cacheDir, CAPTURE_FILE_NAME)
                    FileOutputStream(outputFile).use { out ->
                        softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }

                    withContext(Dispatchers.Main) {
                        LensIntentDispatcher.dispatchImage(this@QuickLensAccessibilityService, outputFile)
                    }
                    onComplete?.invoke(true)
                } else {
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                onComplete?.invoke(false)
            } finally {
                softwareBitmap?.recycle()
            }
        }
    }

    companion object {
        private const val CAPTURE_FILE_NAME = "quicklens_capture.png"

        private var instance: WeakReference<QuickLensAccessibilityService>? = null
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun isRunning(): Boolean = instance?.get() != null

        fun getInstance(): QuickLensAccessibilityService? = instance?.get()

        suspend fun awaitInstance(timeoutMs: Long = 600L): QuickLensAccessibilityService? {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < timeoutMs) {
                val service = instance?.get()
                if (service != null) return service
                delay(50)
            }
            return instance?.get()
        }

        fun isServiceEnabledInSystem(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            if (am != null) {
                val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                val myPackage = context.packageName
                val myService = QuickLensAccessibilityService::class.java.name
                for (info in enabledServices) {
                    val sInfo = info.resolveInfo.serviceInfo
                    if (sInfo.packageName == myPackage && sInfo.name == myService) {
                        return true
                    }
                }
            }

            return runCatching {
                val setting = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
                val expected = "${context.packageName}/${QuickLensAccessibilityService::class.java.name}"
                val expectedShort = "${context.packageName}/.service.QuickLensAccessibilityService"
                setting.contains(expected) || setting.contains(expectedShort)
            }.getOrDefault(false)
        }

        fun captureAndLaunchLens(
            delayMs: Long = 300L,
            vibrate: Boolean = true,
            onComplete: ((Boolean) -> Unit)? = null
        ): Boolean {
            val service = instance?.get() ?: return false
            service.captureAndLaunch(delayMs, vibrate, onComplete)
            return true
        }
    }
}
