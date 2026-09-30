package com.dolphin.quicklens.util

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings

object NavigationModeHelper {

    enum class NavMode {
        THREE_BUTTON,
        TWO_BUTTON,
        GESTURE
    }

    /**
     * Determines whether the device is using Gesture Navigation or 3-Button Navigation.
     * Android standard values for "navigation_mode":
     * 0 = 3-button navigation
     * 1 = 2-button navigation (Android 9 pill)
     * 2 = Full gesture navigation
     */
    fun getNavigationMode(context: Context): NavMode {
        try {
            val mode = Settings.Secure.getInt(context.contentResolver, "navigation_mode")
            return when (mode) {
                2 -> NavMode.GESTURE
                1 -> NavMode.TWO_BUTTON
                0 -> NavMode.THREE_BUTTON
                else -> NavMode.GESTURE
            }
        } catch (_: Exception) {
            // Setting might not be present or accessible on custom OEM builds
        }

        try {
            val res = context.resources
            val resId = res.getIdentifier("config_navBarInteractionMode", "integer", "android")
            if (resId > 0) {
                return when (res.getInteger(resId)) {
                    2 -> NavMode.GESTURE
                    1 -> NavMode.TWO_BUTTON
                    0 -> NavMode.THREE_BUTTON
                    else -> NavMode.GESTURE
                }
            }
        } catch (_: Exception) {
            // Resource not found
        }

        return NavMode.GESTURE
    }

    fun isGestureNavigation(context: Context): Boolean {
        return getNavigationMode(context) == NavMode.GESTURE
    }

    /**
     * Registers a ContentObserver on Settings.Secure "navigation_mode" so changes
     * in system navigation settings are detected dynamically.
     */
    fun registerObserver(context: Context, onChange: (NavMode) -> Unit): ContentObserver? {
        return try {
            val uri: Uri = Settings.Secure.getUriFor("navigation_mode") ?: return null
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    onChange(getNavigationMode(context))
                }
            }
            context.contentResolver.registerContentObserver(uri, false, observer)
            observer
        } catch (_: Exception) {
            null
        }
    }

    fun unregisterObserver(context: Context, observer: ContentObserver?) {
        if (observer == null) return
        try {
            context.contentResolver.unregisterContentObserver(observer)
        } catch (_: Exception) {
            // Ignored
        }
    }
}
