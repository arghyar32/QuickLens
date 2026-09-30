package com.dolphin.quicklens.service

import android.content.Intent
import android.media.projection.MediaProjection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

object CaptureTokenHolder {

    data class ProjectionConsent(
        val resultCode: Int,
        val data: Intent
    )

    private val consentRef = AtomicReference<ProjectionConsent?>(null)
    private val mediaProjectionRef = AtomicReference<MediaProjection?>(null)

    private val _isCaptureReady = MutableStateFlow(false)
    val isCaptureReady: StateFlow<Boolean> = _isCaptureReady.asStateFlow()

    fun setConsent(resultCode: Int, data: Intent) {
        consentRef.set(ProjectionConsent(resultCode, data))
        _isCaptureReady.value = true
    }

    fun getConsent(): ProjectionConsent? = consentRef.get()

    fun setMediaProjection(projection: MediaProjection?) {
        val previous = mediaProjectionRef.getAndSet(projection)
        if (previous != null && previous != projection) {
            runCatching { previous.stop() }
        }
        _isCaptureReady.value = projection != null || consentRef.get() != null
    }

    fun getMediaProjection(): MediaProjection? = mediaProjectionRef.get()

    fun isReady(): Boolean = mediaProjectionRef.get() != null || consentRef.get() != null

    fun clear() {
        val projection = mediaProjectionRef.getAndSet(null)
        runCatching { projection?.stop() }
        consentRef.set(null)
        _isCaptureReady.value = false
    }
}
