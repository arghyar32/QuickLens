package com.dolphin.quicklens.cts

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

object CircleToSearchInvoker {

    private const val TAG = "QuickLensCTS"
    const val GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"

    fun isGoogleDefaultAssistant(context: Context): Boolean {
        return runCatching {
            val setting = Settings.Secure.getString(context.contentResolver, "assistant")
                ?: Settings.Secure.getString(context.contentResolver, "voice_interaction_service")
                ?: ""
            setting.contains(GOOGLE_SEARCH_PACKAGE)
        }.getOrDefault(false)
    }

    fun openAssistantSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                runCatching {
                    context.startActivity(intent)
                    return
                }
            }
        }
    }

    @SuppressLint("PrivateApi")
    fun trigger(context: Context, entryPoint: Int = 1, vibrate: Boolean = true): Boolean {
        val result = runCatching {
            val bundle = Bundle().apply {
                putLong("invocation_time_ms", SystemClock.elapsedRealtime())
                putInt("omni.entry_point", entryPoint)
                putBoolean("micts_trigger", true)
            }

            val iVimsClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val vis = getServiceMethod.invoke(null, "voiceinteraction")

            val stubClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
            val vims = asInterfaceMethod.invoke(null, vis)

            val invoked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HiddenApiBypass.invoke(
                    iVimsClass,
                    vims,
                    "showSessionFromSession",
                    null,
                    bundle,
                    7,
                    "hyperOS_home"
                ) as? Boolean ?: false
            } else {
                HiddenApiBypass.invoke(
                    iVimsClass,
                    vims,
                    "showSessionFromSession",
                    null,
                    bundle,
                    7
                ) as? Boolean ?: false
            }
            invoked
        }.onFailure { e ->
            Log.e(TAG, "CircleToSearch trigger error: ${e.message}", e)
        }.getOrDefault(false)

        if (result && vibrate) {
            performVibration(context)
        }
        return result
    }

    private fun performVibration(context: Context) {
        runCatching {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setFlags(128)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
                    attr
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1, 75, 76), -1, attr)
            }
        }
    }
}
