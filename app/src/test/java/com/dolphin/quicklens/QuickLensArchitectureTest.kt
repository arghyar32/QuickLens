package com.dolphin.quicklens

import com.dolphin.quicklens.cts.CircleToSearchInvoker
import com.dolphin.quicklens.data.LensPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickLensArchitectureTest {

    @Test
    fun testPreferencesDefaultsAndBounds() {
        assertEquals(300, LensPreferencesRepository.DEFAULT_DELAY_MS)
        assertEquals(0, LensPreferencesRepository.MIN_DELAY_MS)
        assertEquals(1200, LensPreferencesRepository.MAX_DELAY_MS)
        assertEquals(50, LensPreferencesRepository.DELAY_STEP_MS)
        assertTrue(LensPreferencesRepository.DEFAULT_HAPTIC_ENABLED)
        assertTrue(LensPreferencesRepository.DEFAULT_USE_NATIVE_CTS)
        assertTrue(LensPreferencesRepository.DEFAULT_LAUNCHER_TRIGGER)
        assertTrue(LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE)
        assertTrue(LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_INDICATOR)
        assertEquals(60, LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_WIDTH_DP)
        assertEquals(60, LensPreferencesRepository.MIN_NAV_BAR_GESTURE_WIDTH_DP)
        assertEquals(240, LensPreferencesRepository.MAX_NAV_BAR_GESTURE_WIDTH_DP)
        assertEquals(0, LensPreferencesRepository.DEFAULT_NAV_BAR_GESTURE_OFFSET_X)
        assertEquals(-100, LensPreferencesRepository.MIN_NAV_BAR_GESTURE_OFFSET_X)
        assertEquals(100, LensPreferencesRepository.MAX_NAV_BAR_GESTURE_OFFSET_X)
    }

    @Test
    fun testGoogleSearchPackageNameConstant() {
        assertEquals("com.google.android.googlequicksearchbox", CircleToSearchInvoker.GOOGLE_SEARCH_PACKAGE)
    }

    @Test
    fun testNavigationModeEnumValues() {
        val modes = com.dolphin.quicklens.util.NavigationModeHelper.NavMode.values()
        assertEquals(3, modes.size)
        assertEquals(com.dolphin.quicklens.util.NavigationModeHelper.NavMode.THREE_BUTTON, modes[0])
        assertEquals(com.dolphin.quicklens.util.NavigationModeHelper.NavMode.TWO_BUTTON, modes[1])
        assertEquals(com.dolphin.quicklens.util.NavigationModeHelper.NavMode.GESTURE, modes[2])
    }
}
