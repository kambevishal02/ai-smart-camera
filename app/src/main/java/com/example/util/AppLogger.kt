package com.example.util

import android.util.Log
import com.example.model.CameraHardwareInfo
import com.example.model.CameraRecommendation
import com.example.model.ImageProcessingProfileType
import com.example.model.LightingCondition
import com.example.model.SceneType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Structured logger for developer diagnostic logs as requested:
 * - Scene detected
 * - Lighting detected
 * - Camera capabilities
 * - Recommendation selected
 * - Capture completed
 * - Image processing completed
 */
object AppLogger {
    private const val TAG = "AISmartCamera"

    data class LogEntry(
        val timestamp: String,
        val tag: String,
        val message: String,
        val level: String = "INFO"
    )

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private const val MAX_LOGS = 250

    @Synchronized
    fun addLog(category: String, message: String, level: String = "INFO") {
        val timeStr = dateFormat.format(Date())
        val entry = LogEntry(timeStr, category, message, level)
        val current = _logs.value.toMutableList()
        if (current.size >= MAX_LOGS) {
            current.removeAt(0)
        }
        current.add(entry)
        _logs.value = current

        when (level) {
            "DEBUG" -> Log.d(TAG, "[$category] $message")
            "WARN" -> Log.w(TAG, "[$category] $message")
            "ERROR" -> Log.e(TAG, "[$category] $message")
            else -> Log.i(TAG, "[$category] $message")
        }
    }

    fun i(category: String, message: String) = addLog(category, message, "INFO")
    fun d(category: String, message: String) = addLog(category, message, "DEBUG")
    fun w(category: String, message: String) = addLog(category, message, "WARN")
    fun e(category: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$message (${throwable.message})" else message
        addLog(category, fullMsg, "ERROR")
    }

    fun logSceneDetected(scene: SceneType, confidence: Float) {
        val percent = (confidence * 100).toInt()
        addLog("SCENE", "Scene detected: ${scene.displayName} (confidence: $percent%)")
    }

    fun logLightingDetected(lighting: LightingCondition, luminance: Float) {
        addLog("LIGHTING", "Lighting detected: ${lighting.label} (luma: ${String.format(Locale.US, "%.1f", luminance)})")
    }

    fun logCameraCapabilities(caps: CameraHardwareInfo) {
        addLog("HARDWARE", "Camera capabilities: ID=${caps.cameraId}, Lens=${caps.lensFacingName}, Level=${caps.hardwareLevel}, Res=${caps.sensorResolution}, EV=[${caps.evRangeMin}..${caps.evRangeMax}, step=${caps.evStep}], Flash=${caps.isFlashSupported}, ManualISO=${caps.isManualIsoSupported}")
    }

    fun logRecommendationSelected(rec: CameraRecommendation) {
        addLog("RECOMMENDATION", "Recommendation selected: Action='${rec.primaryActionText}', Profile=${rec.imageProcessingProfile.displayName}, EV=${rec.exposureCompensationIndex} (${rec.exposureCompensationEv}EV), Focus=${rec.focusStrategy.label}, Flash=${rec.flashRecommendation.label}")
    }

    fun logCaptureCompleted(uriString: String, profile: ImageProcessingProfileType) {
        addLog("CAPTURE", "Capture completed: Profile=${profile.displayName}, Saved to: $uriString")
    }

    fun logImageProcessingCompleted(profile: ImageProcessingProfileType, durationMs: Long, width: Int, height: Int) {
        addLog("PROCESSING", "Image processing completed: Profile=${profile.displayName}, Dimensions=${width}x${height}, Duration=${durationMs}ms")
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
