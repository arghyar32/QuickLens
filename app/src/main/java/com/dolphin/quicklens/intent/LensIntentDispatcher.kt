package com.dolphin.quicklens.intent

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dolphin.quicklens.R
import java.io.File

object LensIntentDispatcher {

    private const val GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"
    private const val LENS_SHARE_ACTIVITY = "com.google.android.apps.search.lens.LensShareActivity"
    private const val STANDALONE_LENS_PACKAGE = "com.google.ar.lens"

    fun dispatchImage(context: Context, imageFile: File): Boolean {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            return false
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = runCatching {
            FileProvider.getUriForFile(context, authority, imageFile)
        }.getOrElse {
            return false
        }

        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }

        val packageManager = context.packageManager

        // Priority 1: Direct Component (LensShareActivity)
        val directComponent = ComponentName(GOOGLE_SEARCH_PACKAGE, LENS_SHARE_ACTIVITY)
        val directIntent = Intent(baseIntent).apply {
            component = directComponent
        }

        if (isComponentAvailable(packageManager, directIntent)) {
            grantUriPermissions(context, contentUri, GOOGLE_SEARCH_PACKAGE)
            context.startActivity(directIntent)
            return true
        }

        // Priority 2: Explicit package targeting Google Search App
        val googleSearchIntent = Intent(baseIntent).apply {
            setPackage(GOOGLE_SEARCH_PACKAGE)
        }
        if (isIntentResolvable(packageManager, googleSearchIntent)) {
            grantUriPermissions(context, contentUri, GOOGLE_SEARCH_PACKAGE)
            context.startActivity(googleSearchIntent)
            return true
        }

        // Priority 3: Standalone Google Lens App
        val standaloneLensIntent = Intent(baseIntent).apply {
            setPackage(STANDALONE_LENS_PACKAGE)
        }
        if (isIntentResolvable(packageManager, standaloneLensIntent)) {
            grantUriPermissions(context, contentUri, STANDALONE_LENS_PACKAGE)
            context.startActivity(standaloneLensIntent)
            return true
        }

        // Fallback
        Toast.makeText(context, context.getString(R.string.toast_lens_not_found), Toast.LENGTH_SHORT).show()
        val chooserIntent = Intent.createChooser(baseIntent, "Visual Search").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
        return false
    }

    private fun grantUriPermissions(context: Context, uri: Uri, targetPackage: String) {
        runCatching {
            context.grantUriPermission(
                targetPackage,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun isComponentAvailable(packageManager: PackageManager, intent: Intent): Boolean {
        return runCatching {
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo != null
        }.getOrDefault(false)
    }

    private fun isIntentResolvable(packageManager: PackageManager, intent: Intent): Boolean {
        return runCatching {
            val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolved.isNotEmpty()
        }.getOrDefault(false)
    }
}
