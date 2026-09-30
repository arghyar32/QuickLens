package com.dolphin.quicklens.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.dolphin.quicklens.MainActivity
import com.dolphin.quicklens.R
import com.dolphin.quicklens.intent.LensIntentDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val isCapturing = AtomicBoolean(false)

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseCapturePipeline()
            CaptureTokenHolder.clear()
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startServiceInForeground()

        val thread = HandlerThread("QuickLensCaptureThread", Process.THREAD_PRIORITY_DISPLAY).apply {
            start()
        }
        handlerThread = thread
        backgroundHandler = Handler(thread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_CAPTURE -> handleCapture()
            ACTION_STOP -> {
                releaseCapturePipeline()
                CaptureTokenHolder.clear()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != 0 && resultData != null) {
            CaptureTokenHolder.setConsent(resultCode, resultData)
            initializeMediaProjection(resultCode, resultData)
        }
    }

    private fun initializeMediaProjection(resultCode: Int, resultData: Intent) {
        if (mediaProjection != null) return

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            ?: return

        runCatching {
            val projection = projectionManager.getMediaProjection(resultCode, resultData)
            if (projection != null) {
                val handler = backgroundHandler ?: Handler(mainLooper)
                projection.registerCallback(projectionCallback, handler)
                mediaProjection = projection
                CaptureTokenHolder.setMediaProjection(projection)
            }
        }
    }

    private fun handleCapture() {
        if (!isCapturing.compareAndSet(false, true)) {
            return
        }

        serviceScope.launch {
            try {
                if (mediaProjection == null) {
                    val consent = CaptureTokenHolder.getConsent()
                    if (consent != null) {
                        initializeMediaProjection(consent.resultCode, consent.data)
                    }
                }

                val projection = mediaProjection
                if (projection == null) {
                    isCapturing.set(false)
                    return@launch
                }

                snapFrame(projection)
            } catch (e: Exception) {
                isCapturing.set(false)
            }
        }
    }

    private fun snapFrame(projection: MediaProjection) {
        val (width, height, densityDpi) = getDisplayDimensions()
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        val handler = backgroundHandler ?: Handler(mainLooper)

        reader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener {
            private val handled = AtomicBoolean(false)

            override fun onImageAvailable(r: ImageReader) {
                if (!handled.compareAndSet(false, true)) return

                val image = runCatching { r.acquireLatestImage() }.getOrNull()
                if (image != null) {
                    processImage(image, width, height)
                } else {
                    isCapturing.set(false)
                }
            }
        }, handler)

        virtualDisplay = projection.createVirtualDisplay(
            "QuickLensVirtualDisplay",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
    }

    private fun processImage(image: Image, targetWidth: Int, targetHeight: Int) {
        serviceScope.launch(Dispatchers.IO) {
            var rawBitmap: Bitmap? = null
            var finalBitmap: Bitmap? = null
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * targetWidth

                val bitmapWidth = targetWidth + rowPadding / pixelStride
                rawBitmap = Bitmap.createBitmap(bitmapWidth, targetHeight, Bitmap.Config.ARGB_8888)
                rawBitmap.copyPixelsFromBuffer(buffer)

                finalBitmap = if (rowPadding == 0) {
                    rawBitmap
                } else {
                    Bitmap.createBitmap(rawBitmap, 0, 0, targetWidth, targetHeight)
                }

                val outputFile = File(cacheDir, CAPTURE_FILE_NAME)
                FileOutputStream(outputFile).use { outStream ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                    outStream.flush()
                }

                LensIntentDispatcher.dispatchImage(this@ScreenCaptureService, outputFile)
            } finally {
                image.close()
                if (finalBitmap != null && finalBitmap != rawBitmap) {
                    finalBitmap.recycle()
                }
                rawBitmap?.recycle()

                virtualDisplay?.release()
                virtualDisplay = null
                imageReader?.close()
                imageReader = null

                isCapturing.set(false)
            }
        }
    }

    private fun getDisplayDimensions(): Triple<Int, Int, Int> {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            val densityDpi = resources.configuration.densityDpi
            Triple(bounds.width(), bounds.height(), densityDpi)
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            Triple(displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi)
        }
    }

    private fun startServiceInForeground() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_lens)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun releaseCapturePipeline() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        }
        mediaProjection = null
    }

    override fun onDestroy() {
        releaseCapturePipeline()
        handlerThread?.quitSafely()
        handlerThread = null
        backgroundHandler = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.dolphin.quicklens.action.START"
        const val ACTION_CAPTURE = "com.dolphin.quicklens.action.CAPTURE"
        const val ACTION_STOP = "com.dolphin.quicklens.action.STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val CHANNEL_ID = "quicklens_capture_channel"
        private const val NOTIFICATION_ID = 2048
        private const val CAPTURE_FILE_NAME = "quicklens_capture.png"
    }
}
