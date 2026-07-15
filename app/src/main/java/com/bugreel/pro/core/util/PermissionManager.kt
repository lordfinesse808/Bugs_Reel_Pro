package com.bugreel.pro.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import javax.inject.Inject
import javax.inject.Singleton

// CLAUDE.md Anti-Pattern 3 — every runtime permission request goes through this class.
// Screen composables call the ViewModel, never a permission API directly.
@Singleton
class PermissionManager @Inject constructor() {

    // Set by the caller once the MediaProjection ActivityResult callback reports RESULT_OK.
    var mediaProjectionGranted: Boolean = false

    fun requestMediaProjection(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        val projectionManager =
            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        launcher.launch(projectionManager.createScreenCaptureIntent())
    }

    fun requestOverlayPermission(context: Context) {
        val intent = OemCompat.overlayPermissionGrantIntent(context)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun requestNotificationPermission(launcher: ActivityResultLauncher<String>) {
        // POST_NOTIFICATIONS is a runtime permission only from API 33 (Tiramisu); below that,
        // notifications are granted at install time and there is nothing to request.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun isOverlayPermissionGranted(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun isMediaProjectionPermissionGranted(): Boolean = mediaProjectionGranted
}
