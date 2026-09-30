package com.dolphin.quicklens.gesture

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import com.dolphin.quicklens.data.LensPreferencesRepository
import com.dolphin.quicklens.service.QuickLensAccessibilityService
import com.dolphin.quicklens.util.HapticFeedbackHelper
import com.dolphin.quicklens.util.NavigationModeHelper
import kotlin.math.abs

class NavBarGestureOverlay(
    private val service: QuickLensAccessibilityService,
    private val onTrigger: () -> Unit
) {

    private val windowManager: WindowManager? =
        service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var overlayView: FrameLayout? = null
    private var pillIndicatorView: View? = null
    private var isAttached = false
    private var currentShowIndicator: Boolean = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_INDICATOR
    private var currentWidthDp: Int = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_WIDTH_DP
    private var currentOffsetXPercent: Int = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_OFFSET_X
    private var currentNavMode = NavigationModeHelper.NavMode.GESTURE
    private var navModeObserver: ContentObserver? = null

    private val handler = Handler(Looper.getMainLooper())
    private var holdRunnable: Runnable? = null
    private var isHoldTriggered = false

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop.toFloat()

    @SuppressLint("ClickableViewAccessibility")
    fun attach(
        showIndicator: Boolean = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_INDICATOR,
        widthDp: Int = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_WIDTH_DP,
        offsetXPercent: Int = LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_OFFSET_X
    ) {
        if (isAttached) {
            updateConfig(showIndicator, widthDp, offsetXPercent)
            return
        }

        if (windowManager == null) return

        currentShowIndicator = showIndicator
        currentWidthDp = widthDp
        currentOffsetXPercent = offsetXPercent
        currentNavMode = NavigationModeHelper.getNavigationMode(service)

        // Observe system navigation mode changes (e.g. user toggles Gestures <-> 3-Button in settings)
        navModeObserver = NavigationModeHelper.registerObserver(service) { newMode ->
            currentNavMode = newMode
            updateConfig(currentShowIndicator, currentWidthDp, currentOffsetXPercent)
        }

        try {
            val density = service.resources.displayMetrics.density
            val screenWidthPx = service.resources.displayMetrics.widthPixels
            val widthPx = (currentWidthDp * density).toInt()
            val heightPx = (NAV_BAR_HEIGHT_DP * density).toInt()

            val maxOffsetPx = (screenWidthPx - widthPx).coerceAtLeast(0) / 2
            val xPx = ((currentOffsetXPercent / 100f) * maxOffsetPx).toInt()

            val params = WindowManager.LayoutParams(
                widthPx,
                heightPx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                x = xPx
                y = 0
            }

            val pillWidthPx = ((currentWidthDp * 0.55f).coerceIn(32f, 120f) * density).toInt()
            val pillView = View(service).apply {
                // In 3-Button mode, system Home icon is already present; only show pill in Gesture mode
                if (showIndicator && currentNavMode == NavigationModeHelper.NavMode.GESTURE) {
                    background = createIndicatorDrawable(density, false)
                } else {
                    setBackgroundColor(0x00000000)
                }
            }

            val pillParams = FrameLayout.LayoutParams(
                pillWidthPx,
                (PILL_HEIGHT_DP * density).toInt()
            ).apply {
                gravity = Gravity.CENTER
            }

            val rootLayout = FrameLayout(service).apply {
                setBackgroundColor(0x00000000)
                addView(pillView, pillParams)
                setOnTouchListener { _, event -> handleTouch(event, pillView, density) }
            }

            windowManager.addView(rootLayout, params)
            overlayView = rootLayout
            pillIndicatorView = pillView
            isAttached = true
        } catch (_: Exception) {
            isAttached = false
        }
    }

    fun updateConfig(
        showIndicator: Boolean,
        widthDp: Int = currentWidthDp,
        offsetXPercent: Int = currentOffsetXPercent
    ) {
        currentShowIndicator = showIndicator
        currentWidthDp = widthDp
        currentOffsetXPercent = offsetXPercent

        val density = service.resources.displayMetrics.density
        val screenWidthPx = service.resources.displayMetrics.widthPixels
        val widthPx = (currentWidthDp * density).toInt()
        val maxOffsetPx = (screenWidthPx - widthPx).coerceAtLeast(0) / 2
        val xPx = ((currentOffsetXPercent / 100f) * maxOffsetPx).toInt()

        overlayView?.let { root ->
            val params = root.layoutParams as? WindowManager.LayoutParams
            if (params != null) {
                params.width = widthPx
                params.x = xPx
                try {
                    windowManager?.updateViewLayout(root, params)
                } catch (_: Exception) {}
            }
        }

        pillIndicatorView?.let { pill ->
            val pillWidthPx = ((currentWidthDp * 0.55f).coerceIn(32f, 120f) * density).toInt()
            val pillParams = pill.layoutParams as? FrameLayout.LayoutParams
            if (pillParams != null) {
                pillParams.width = pillWidthPx
                pill.layoutParams = pillParams
            }

            if (showIndicator && currentNavMode == NavigationModeHelper.NavMode.GESTURE) {
                pill.background = createIndicatorDrawable(density, false)
            } else {
                pill.setBackgroundColor(0x00000000)
            }
        }
    }

    fun updateIndicator(showIndicator: Boolean) {
        updateConfig(showIndicator, currentWidthDp, currentOffsetXPercent)
    }

    fun detach() {
        cancelHold()
        NavigationModeHelper.unregisterObserver(service, navModeObserver)
        navModeObserver = null

        if (!isAttached || overlayView == null || windowManager == null) return

        try {
            windowManager.removeView(overlayView)
        } catch (_: Exception) {
            // View may already be removed
        } finally {
            overlayView = null
            pillIndicatorView = null
            isAttached = false
        }
    }

    private fun handleTouch(event: MotionEvent, pillView: View, density: Float): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                downTime = System.currentTimeMillis()
                isHoldTriggered = false
                currentNavMode = NavigationModeHelper.getNavigationMode(service)

                if (currentShowIndicator && currentNavMode == NavigationModeHelper.NavMode.GESTURE) {
                    pillView.background = createIndicatorDrawable(density, true)
                }
                scheduleHold()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = event.rawY - downY
                val dx = event.rawX - downX
                if (dy < -touchSlop || abs(dx) > touchSlop * 1.5f) {
                    // Finger moved significantly - cancel search hold timer
                    cancelHold()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dy = event.rawY - downY
                val dx = event.rawX - downX
                val elapsed = System.currentTimeMillis() - downTime
                cancelHold()

                if (currentShowIndicator && currentNavMode == NavigationModeHelper.NavMode.GESTURE) {
                    pillView.background = createIndicatorDrawable(density, false)
                }

                if (isHoldTriggered) {
                    return true
                }

                // 1. Swipe up gesture (Gesture navigation Home / Recents proxy)
                if (dy < -touchSlop) {
                    if (elapsed > RECENTS_HOLD_THRESHOLD_MS && abs(dy) > touchSlop * 2) {
                        // Dragged up and held -> Open Recent Apps
                        HapticFeedbackHelper.performClick(service)
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                    } else {
                        // Quick swipe up -> Go Home (no haptic vibration as requested)
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    }
                    return true
                }

                // 2. Quick Tap:
                // If using 3-Button (or 2-Button) navigation, tapping the center Home button MUST execute Home!
                if (abs(dy) <= touchSlop && abs(dx) <= touchSlop && elapsed < HOLD_DURATION_MS) {
                    if (currentNavMode != NavigationModeHelper.NavMode.GESTURE) {
                        HapticFeedbackHelper.performClick(service)
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    }
                    return true
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelHold()
                if (currentShowIndicator && currentNavMode == NavigationModeHelper.NavMode.GESTURE) {
                    pillView.background = createIndicatorDrawable(density, false)
                }
                return isHoldTriggered
            }

            else -> return false
        }
    }

    private fun scheduleHold() {
        cancelHold()
        val runnable = Runnable {
            isHoldTriggered = true
            HapticFeedbackHelper.performClick(service)
            onTrigger()
        }
        holdRunnable = runnable
        handler.postDelayed(runnable, HOLD_DURATION_MS)
    }

    private fun cancelHold() {
        holdRunnable?.let { handler.removeCallbacks(it) }
        holdRunnable = null
    }

    private fun createIndicatorDrawable(density: Float, isPressed: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            if (isPressed) {
                setColor(0x994285F4.toInt()) // Active touch accent
                setStroke((1.5f * density).toInt(), 0xCCFFFFFF.toInt())
            } else {
                setColor(0x55555555.toInt()) // Subtle translucent pill
                setStroke((1 * density).toInt(), 0x33FFFFFF.toInt())
            }
        }
    }

    companion object {
        private const val HOLD_DURATION_MS = 350L
        private const val RECENTS_HOLD_THRESHOLD_MS = 280L
        private const val NAV_BAR_HEIGHT_DP = 42
        private const val PILL_HEIGHT_DP = 5
    }
}
