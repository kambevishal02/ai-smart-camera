package com.example.model

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 11 Standard Test Scenes required for V0.3 Smart Camera Calibration and A/B Testing.
 */
enum class TestSceneType(
    val displayName: String,
    val description: String,
    val correspondingScene: SceneType
) {
    DAYLIGHT("Daylight", "Bright outdoor natural lighting, high dynamic range", SceneType.DAYLIGHT),
    INDOOR("Indoor", "Mixed artificial ambient lighting, moderate dynamic range", SceneType.INDOOR),
    LOW_LIGHT("Low Light", "Subdued ambient lighting, shadow preservation required", SceneType.LOW_LIGHT),
    NIGHT("Night", "Dark nocturnal environment with point light sources", SceneType.NIGHT),
    PORTRAIT_DAYLIGHT("Portrait Daylight", "Subject portrait in daylight, skin tone priority", SceneType.PORTRAIT),
    PORTRAIT_LOW_LIGHT("Portrait Low Light", "Subject portrait in dim light, face exposure & noise control", SceneType.PORTRAIT),
    FOREST("Forest", "Lush greenery, dense canopy with sky highlights", SceneType.FOREST_NATURE),
    BEACH("Beach", "High-reflectivity sand and water, bright sunny conditions", SceneType.BEACH),
    SUNSET("Sunset", "Golden hour sunset with intense horizon warmth", SceneType.SUNSET),
    FOOD("Food", "Close-up culinary subject with micro-textures & warmth", SceneType.FOOD),
    ARCHITECTURE("Architecture", "Geometric structures, straight lines & structural contrast", SceneType.ARCHITECTURE);

    companion object {
        fun fromSceneType(scene: SceneType, isPerson: Boolean, isLowLight: Boolean): TestSceneType {
            return when (scene) {
                SceneType.DAYLIGHT -> TestSceneType.DAYLIGHT
                SceneType.INDOOR -> if (isPerson) TestSceneType.PORTRAIT_DAYLIGHT else TestSceneType.INDOOR
                SceneType.LOW_LIGHT -> if (isPerson) TestSceneType.PORTRAIT_LOW_LIGHT else TestSceneType.LOW_LIGHT
                SceneType.NIGHT -> if (isPerson) TestSceneType.PORTRAIT_LOW_LIGHT else TestSceneType.NIGHT
                SceneType.PORTRAIT -> if (isLowLight) TestSceneType.PORTRAIT_LOW_LIGHT else TestSceneType.PORTRAIT_DAYLIGHT
                SceneType.FOREST_NATURE -> TestSceneType.FOREST
                SceneType.BEACH -> TestSceneType.BEACH
                SceneType.SUNSET -> TestSceneType.SUNSET
                SceneType.FOOD -> TestSceneType.FOOD
                SceneType.ARCHITECTURE -> TestSceneType.ARCHITECTURE
                SceneType.UNKNOWN -> TestSceneType.DAYLIGHT
            }
        }
    }
}

/**
 * Detailed technical metrics calculated objectively from captured frame pixels.
 *
 * NOTE: These metrics quantify physical sensor and signal attributes.
 * They do NOT represent artistic or creative quality.
 */
data class DetailedTechnicalMetrics(
    val exposureScore: Int,               // 0-100: proximity to balanced 18% gray target
    val brightnessLuma: Float,            // 0-100%: mean luminance (Y)
    val contrastRms: Float,               // 0-100: RMS standard deviation of luminance
    val highlightClippingPct: Float,      // 0-100%: % of pixels with Y >= 248
    val shadowClippingPct: Float,         // 0-100%: % of pixels with Y <= 12
    val sharpnessScore: Int,              // 0-100: Laplacian variance / high-frequency gradient
    val noiseEstimate: Float,             // 0-100: estimated high-frequency residual in uniform patches
    val dynamicRangeStops: Float,         // 0-14 EV: estimated usable dynamic range
    val colorCastOffset: Float,           // 0-100: chromaticity distance from gray-world balance
    val faceExposureLuma: Float?,         // 0-100%: mean luminance in face bounding box (if detected)
    val totalTechnicalScore: Int,         // 0-100: overall technical quality index
    val ratingLabel: String               // e.g. "OPTIMAL", "GOOD", "ACCEPTABLE", "SUBOPTIMAL"
)

/**
 * Encapsulates a complete A/B Test capture session.
 * Compares PHOTO A (Standard AUTO) and PHOTO B (SMART AUTO) under identical framing.
 */
data class AbCaptureSession(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
    val testScene: TestSceneType,
    val deviceName: String,
    val cameraId: String,
    val hardwareLevel: String,
    // Photo A (Standard AUTO)
    val photoA_Uri: Uri? = null,
    val photoA_Bitmap: Bitmap? = null,
    val photoA_Metrics: DetailedTechnicalMetrics,
    val photoA_AppliedSettings: Map<String, String>,
    // Photo B (SMART AUTO)
    val photoB_Uri: Uri? = null,
    val photoB_Bitmap: Bitmap? = null,
    val photoB_Metrics: DetailedTechnicalMetrics,
    val photoB_AppliedSettings: Map<String, String>,
    // Engine Decisions
    val recommendation: CameraRecommendation,
    val fallbackSettings: Map<String, String>,
    val processingProfile: ImageProcessingProfileType,
    val appliedCalibrationSummary: String = "Generic Baseline"
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("sessionId", id)
        json.put("timestamp", timestamp)
        json.put("formattedTime", formattedTime)
        json.put("testScene", testScene.name)
        json.put("testSceneDisplayName", testScene.displayName)
        json.put("deviceName", deviceName)
        json.put("cameraId", cameraId)
        json.put("hardwareLevel", hardwareLevel)
        json.put("processingProfile", processingProfile.name)
        json.put("calibrationSummary", appliedCalibrationSummary)

        // Photo A Metrics
        val autoMetricsJson = JSONObject().apply {
            put("exposureScore", photoA_Metrics.exposureScore)
            put("brightnessLuma", photoA_Metrics.brightnessLuma)
            put("contrastRms", photoA_Metrics.contrastRms)
            put("highlightClippingPct", photoA_Metrics.highlightClippingPct)
            put("shadowClippingPct", photoA_Metrics.shadowClippingPct)
            put("sharpnessScore", photoA_Metrics.sharpnessScore)
            put("noiseEstimate", photoA_Metrics.noiseEstimate)
            put("dynamicRangeStops", photoA_Metrics.dynamicRangeStops)
            put("colorCastOffset", photoA_Metrics.colorCastOffset)
            photoA_Metrics.faceExposureLuma?.let { put("faceExposureLuma", it) }
            put("totalTechnicalScore", photoA_Metrics.totalTechnicalScore)
        }
        json.put("photoA_StandardAuto_Metrics", autoMetricsJson)

        // Photo B Metrics
        val smartMetricsJson = JSONObject().apply {
            put("exposureScore", photoB_Metrics.exposureScore)
            put("brightnessLuma", photoB_Metrics.brightnessLuma)
            put("contrastRms", photoB_Metrics.contrastRms)
            put("highlightClippingPct", photoB_Metrics.highlightClippingPct)
            put("shadowClippingPct", photoB_Metrics.shadowClippingPct)
            put("sharpnessScore", photoB_Metrics.sharpnessScore)
            put("noiseEstimate", photoB_Metrics.noiseEstimate)
            put("dynamicRangeStops", photoB_Metrics.dynamicRangeStops)
            put("colorCastOffset", photoB_Metrics.colorCastOffset)
            photoB_Metrics.faceExposureLuma?.let { put("faceExposureLuma", it) }
            put("totalTechnicalScore", photoB_Metrics.totalTechnicalScore)
        }
        json.put("photoB_SmartAuto_Metrics", smartMetricsJson)

        // Delta
        val deltaJson = JSONObject().apply {
            put("scoreDelta", photoB_Metrics.totalTechnicalScore - photoA_Metrics.totalTechnicalScore)
            put("exposureDelta", photoB_Metrics.exposureScore - photoA_Metrics.exposureScore)
            put("sharpnessDelta", photoB_Metrics.sharpnessScore - photoA_Metrics.sharpnessScore)
            put("highlightClippingDelta", photoB_Metrics.highlightClippingPct - photoA_Metrics.highlightClippingPct)
            put("shadowClippingDelta", photoB_Metrics.shadowClippingPct - photoA_Metrics.shadowClippingPct)
            put("dynamicRangeDelta", photoB_Metrics.dynamicRangeStops - photoA_Metrics.dynamicRangeStops)
        }
        json.put("metricDeltas", deltaJson)

        // Settings
        val appliedSettingsJson = JSONObject()
        photoB_AppliedSettings.forEach { (k, v) -> appliedSettingsJson.put(k, v) }
        json.put("appliedSettingsSmart", appliedSettingsJson)

        val fallbackJson = JSONObject()
        fallbackSettings.forEach { (k, v) -> fallbackJson.put(k, v) }
        json.put("fallbackSettings", fallbackJson)

        return json
    }
}

/**
 * Local in-memory and exportable store for A/B testing sessions and checklist tracking.
 */
object AbTestStore {
    private val _sessions = MutableStateFlow<List<AbCaptureSession>>(emptyList())
    val sessions: StateFlow<List<AbCaptureSession>> = _sessions.asStateFlow()

    private val _testedScenes = MutableStateFlow<Set<TestSceneType>>(emptySet())
    val testedScenes: StateFlow<Set<TestSceneType>> = _testedScenes.asStateFlow()

    fun recordSession(session: AbCaptureSession) {
        val current = _sessions.value.toMutableList()
        current.add(0, session)
        if (current.size > 100) {
            _sessions.value = current.take(100)
        } else {
            _sessions.value = current
        }

        val tested = _testedScenes.value.toMutableSet()
        tested.add(session.testScene)
        _testedScenes.value = tested
    }

    fun clear() {
        _sessions.value = emptyList()
        _testedScenes.value = emptySet()
    }

    fun isSceneTested(scene: TestSceneType): Boolean {
        return _testedScenes.value.contains(scene)
    }

    fun getTestedCount(): Int = _testedScenes.value.size

    /**
     * Exports all A/B test sessions as a structured JSON report.
     */
    fun exportToJsonReport(): String {
        val root = JSONObject()
        root.put("reportType", "SMART_CAMERA_AB_TEST_REPORT")
        root.put("version", "0.3.0")
        root.put("generatedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("totalSessionsRecorded", _sessions.value.size)
        root.put("testedScenesCount", _testedScenes.value.size)
        root.put("totalScenesChecklist", TestSceneType.values().size)

        val sessionsArray = JSONArray()
        _sessions.value.forEach { session ->
            sessionsArray.put(session.toJsonObject())
        }
        root.put("sessions", sessionsArray)

        return root.toString(2)
    }

    /**
     * Exports all A/B test sessions as a CSV report for spreadsheet analysis.
     */
    fun exportToCsvReport(): String {
        val sb = StringBuilder()
        // CSV Header
        sb.appendLine("SessionID,Timestamp,Scene,Device,CameraID,HardwareLevel,Profile,AutoScore,SmartScore,ScoreDelta,AutoExposure,SmartExposure,AutoSharpness,SmartSharpness,AutoHighlightClipPct,SmartHighlightClipPct,AutoShadowClipPct,SmartShadowClipPct,AutoDynamicRangeStops,SmartDynamicRangeStops,SmartEV,Calibration")

        _sessions.value.forEach { s ->
            val delta = s.photoB_Metrics.totalTechnicalScore - s.photoA_Metrics.totalTechnicalScore
            val ev = s.photoB_AppliedSettings["EV Index"] ?: "0"
            sb.appendLine(
                "\"${s.id}\",\"${s.formattedTime}\",\"${s.testScene.displayName}\",\"${s.deviceName}\",\"${s.cameraId}\",\"${s.hardwareLevel}\",\"${s.processingProfile.displayName}\",${s.photoA_Metrics.totalTechnicalScore},${s.photoB_Metrics.totalTechnicalScore},$delta,${s.photoA_Metrics.exposureScore},${s.photoB_Metrics.exposureScore},${s.photoA_Metrics.sharpnessScore},${s.photoB_Metrics.sharpnessScore},${String.format(Locale.US, "%.1f", s.photoA_Metrics.highlightClippingPct)},${String.format(Locale.US, "%.1f", s.photoB_Metrics.highlightClippingPct)},${String.format(Locale.US, "%.1f", s.photoA_Metrics.shadowClippingPct)},${String.format(Locale.US, "%.1f", s.photoB_Metrics.shadowClippingPct)},${String.format(Locale.US, "%.1f", s.photoA_Metrics.dynamicRangeStops)},${String.format(Locale.US, "%.1f", s.photoB_Metrics.dynamicRangeStops)},\"$ev\",\"${s.appliedCalibrationSummary}\""
            )
        }
        return sb.toString()
    }
}
