package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Developer metadata recorded for every photo taken in SMART AUTO or AUTO mode.
 * Contains purely technical diagnostic data (exposure, scene classification, applied settings, quality scores).
 * Strictly stored locally on device; never contains PII or uploads to the cloud.
 */
data class CaptureMetadata(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
    val device: String,
    val cameraId: String,
    val captureMode: String, // "SMART AUTO" or "AUTO"
    val scene: SceneType,
    val sceneDetectionType: String, // "ML_KIT_AND_HEURISTICS" or "HEURISTIC"
    val lighting: LightingCondition,
    val brightnessLuma: Float,
    val faceCount: Int,
    val motionLevel: MotionLevel,
    val recommendationSummary: String,
    val appliedSettings: Map<String, String>,
    val processingProfile: ImageProcessingProfileType,
    val qualityScore: Int,
    val qualityBreakdown: Map<String, Int>
) {
    fun toFormattedLog(): String {
        return buildString {
            appendLine("=== CAPTURE METADATA [$formattedTime] ===")
            appendLine("Mode: $captureMode | Device: $device (Camera ID: $cameraId)")
            appendLine("Scene: ${scene.displayName} (Engine: $sceneDetectionType)")
            appendLine("Lighting: ${lighting.label} (Luma: ${String.format("%.1f", brightnessLuma)}%)")
            appendLine("Subject: $faceCount Faces Detected | Motion: ${motionLevel.label}")
            appendLine("Profile Applied: ${processingProfile.displayName}")
            appendLine("Applied Settings:")
            appliedSettings.forEach { (k, v) -> appendLine("  • $k: $v") }
            appendLine("Photo Quality Score: $qualityScore / 100")
            qualityBreakdown.forEach { (metric, score) -> appendLine("  • $metric: $score/100") }
        }
    }
}

/**
 * In-memory / cache store for developer metadata logs.
 */
object DeveloperMetadataStore {
    private val _metadataLogs = MutableStateFlow<List<CaptureMetadata>>(emptyList())
    val metadataLogs: StateFlow<List<CaptureMetadata>> = _metadataLogs.asStateFlow()

    fun record(metadata: CaptureMetadata) {
        val current = _metadataLogs.value.toMutableList()
        current.add(0, metadata) // newest first
        if (current.size > 50) {
            _metadataLogs.value = current.take(50)
        } else {
            _metadataLogs.value = current
        }
    }

    fun clear() {
        _metadataLogs.value = emptyList()
    }
}
