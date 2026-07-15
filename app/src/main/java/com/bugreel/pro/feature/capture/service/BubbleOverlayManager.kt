package com.bugreel.pro.feature.capture.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageButton
import com.bugreel.pro.BugReelApplication
import com.bugreel.pro.core.util.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

enum class BubbleMode { PRIMARY, FALLBACK }

// CLAUDE.md Section 8 / ADR-005 — BOTH the floating SYSTEM_ALERT_WINDOW bubble (PRIMARY) and
// the persistent-notification trigger (FALLBACK) exist from Sprint 1 Day 1. [mode] is resolved
// once at app start from OemCompat.needsPiPFallback() + the Sprint 0 Track 2 Play Policy
// outcome, and injected here — it never switches at runtime.
@Singleton
class BubbleOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("bubbleMode") private val mode: BubbleMode,
    private val permissionManager: PermissionManager
) {

    interface ActionListener {
        fun onCaptureTapped()
        fun onStopRequested()
    }

    var listener: ActionListener? = null

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var bubbleView: ImageButton? = null

    fun show(): Boolean = when (mode) {
        BubbleMode.PRIMARY -> showPrimaryBubble()
        BubbleMode.FALLBACK -> {
            postNotification(isCapturing = false)
            true
        }
    }

    fun hide() {
        when (mode) {
            BubbleMode.PRIMARY -> {
                bubbleView?.let { windowManager.removeView(it) }
                bubbleView = null
            }
            BubbleMode.FALLBACK -> notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    fun updateCaptureState(isCapturing: Boolean) {
        when (mode) {
            BubbleMode.PRIMARY -> bubbleView?.setImageDrawable(bubbleDrawable(isCapturing))
            BubbleMode.FALLBACK -> postNotification(isCapturing)
        }
    }

    // Used by CaptureService for the mandatory startForeground() call — the FALLBACK
    // notification IS the bubble surface, so notification-building stays owned here.
    fun buildForegroundNotification(): Notification = notification(isCapturing = false)

    private fun showPrimaryBubble(): Boolean {
        if (!permissionManager.isOverlayPermissionGranted(context)) return false
        if (bubbleView != null) return true

        val sizePx = dpToPx(BUBBLE_SIZE_DP)
        val button = ImageButton(context).apply {
            background = null
            setImageDrawable(bubbleDrawable(isCapturing = false))
            setOnClickListener { listener?.onCaptureTapped() }
            setOnLongClickListener {
                listener?.onStopRequested()
                true
            }
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = BUBBLE_MARGIN_PX
            y = BUBBLE_MARGIN_PX
        }

        windowManager.addView(button, params)
        bubbleView = button
        return true
    }

    private fun postNotification(isCapturing: Boolean) {
        notificationManager.notify(NOTIFICATION_ID, notification(isCapturing))
    }

    private fun notification(isCapturing: Boolean): Notification {
        val actionIntent = Intent(context, CaptureService::class.java).apply {
            action = if (isCapturing) ACTION_STOP_CAPTURE else ACTION_START_CAPTURE
        }
        val actionPendingIntent = PendingIntent.getService(
            context,
            NOTIFICATION_ID,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val actionLabel = if (isCapturing) "Stop Capture" else "Start Capture"
        val actionIconRes = if (isCapturing) {
            android.R.drawable.ic_menu_close_clear_cancel
        } else {
            android.R.drawable.ic_media_play
        }

        return Notification.Builder(context, BugReelApplication.CAPTURE_CHANNEL_ID)
            .setContentTitle("BugReel Pro")
            .setContentText(if (isCapturing) "Recording in progress" else "Ready to capture")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, actionIconRes),
                    actionLabel,
                    actionPendingIntent
                ).build()
            )
            .build()
    }

    private fun bubbleDrawable(isCapturing: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (isCapturing) Color.RED else Color.parseColor("#3B82F6"))
        }

    private fun dpToPx(dp: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    ).toInt()

    companion object {
        const val NOTIFICATION_ID = 4201
        const val ACTION_START_CAPTURE = "com.bugreel.pro.action.START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.bugreel.pro.action.STOP_CAPTURE"
        private const val BUBBLE_SIZE_DP = 72f
        private const val BUBBLE_MARGIN_PX = 24
    }
}
