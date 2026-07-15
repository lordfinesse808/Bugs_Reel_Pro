package com.bugreel.pro.feature.capture.service

import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// CLAUDE.md Section 8 — screen recording via MediaProjectionManager. Handles the system
// consent dialog outcome (the [mediaProjection] handed in already represents a granted
// consent) and runs teardown work on [scope], which is tied to CaptureService's lifecycle,
// so nothing here blocks the caller's thread longer than the synchronous Media APIs require.
class MediaProjectionHandler(
    private val mediaProjection: MediaProjection,
    private val scope: CoroutineScope
) {

    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null

    @Volatile
    private var recording = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            // The system (or user, via the system capture-in-progress UI) revoked projection
            // out from under us. Tear down on the service scope rather than whatever thread
            // this callback lands on.
            scope.launch { stopRecording() }
        }
    }

    fun startRecording(outputFile: File): Boolean {
        if (recording) return false
        return try {
            val metrics = Resources.getSystem().displayMetrics
            val recorder = buildMediaRecorder(metrics, outputFile)
            recorder.prepare()

            mediaProjection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            virtualDisplay = mediaProjection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                null
            )

            recorder.start()
            mediaRecorder = recorder
            this.outputFile = outputFile
            recording = true
            true
        } catch (e: Exception) {
            releaseInternal()
            false
        }
    }

    fun stopRecording(): File? {
        if (!recording) return null
        val finishedFile = outputFile
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: RuntimeException) {
            // MediaRecorder.stop() throws if called within ~1s of start() with no frames
            // written yet; the recording is torn down regardless, so this is non-fatal here.
        }
        releaseInternal()
        return finishedFile
    }

    fun isRecording(): Boolean = recording

    private fun buildMediaRecorder(metrics: DisplayMetrics, outputFile: File): MediaRecorder {
        // TODO: OemCompat.mediaProjectionReprompts() — on Samsung/MIUI, if the :capture
        // process restarts mid-recording the system re-prompts the capture consent dialog.
        // CaptureService must detect a restart and surface a fresh consent request instead
        // of silently failing to record.
        @Suppress("DEPRECATION")
        val recorder = MediaRecorder()
        return recorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOutputFile(outputFile.absolutePath)
            setVideoSize(metrics.widthPixels, metrics.heightPixels)
            setVideoFrameRate(VIDEO_FRAME_RATE)
            setVideoEncodingBitRate(VIDEO_BIT_RATE)
        }
    }

    private fun releaseInternal() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaRecorder = null
        outputFile = null
        recording = false
        try {
            mediaProjection.unregisterCallback(projectionCallback)
        } catch (e: IllegalArgumentException) {
            // Already unregistered if onStop() triggered this teardown path.
        }
    }

    private companion object {
        const val VIRTUAL_DISPLAY_NAME = "BugReelCapture"
        const val VIDEO_FRAME_RATE = 30
        const val VIDEO_BIT_RATE = 8 * 1024 * 1024
    }
}
