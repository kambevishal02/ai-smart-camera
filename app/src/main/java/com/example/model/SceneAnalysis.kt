package com.example.model

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri

/**
 * Output of the LightingAnalyzer.
 * Estimates environmental luminance, shadow levels, contrast, and highlight clipping.
 */
data class LightingAnalysis(
    val brightness: Float = 50.0f,          // Numeric brightness value from 0.0 to 100.0
    val darkness: Float = 50.0f,            // Darkness estimate from 0.0 to 100.0
    val contrast: Float = 50.0f,            // Contrast ratio / dynamic spread 0.0 to 100.0
    val highlightClipping: Float = 0.0f,    // % of pixels clipping high (> 240)
    val shadowLevel: Float = 0.0f,          // % of pixels clipping low (< 20)
    val condition: LightingCondition = LightingCondition.NORMAL
) {
    companion object {
        val DEFAULT = LightingAnalysis(
            brightness = 50.0f,
            darkness = 50.0f,
            contrast = 50.0f,
            highlightClipping = 1.0f,
            shadowLevel = 2.0f,
            condition = LightingCondition.NORMAL
        )
    }
}

/**
 * Detected human face details with localized exposure and dynamic range analysis.
 */
data class DetectedFace(
    val bounds: RectF, // Normalized 0.0 to 1.0 coordinates relative to frame
    val confidence: Float = 0.9f,
    val faceBrightness: Float = 50.0f,            // 0.0 to 100.0 luminance inside face bounding box
    val faceExposureRelativeToScene: Float = 0.0f, // positive: brighter than scene, negative: darker than scene
    val faceClipping: Float = 0.0f,                // % of face pixels clipping high (> 240)
    val faceShadowLevel: Float = 0.0f              // % of face pixels clipping low (< 20)
)

/**
 * Output of the SubjectAnalyzer.
 * Local/offline detection of faces, person presence, and subject framing.
 */
data class SubjectAnalysis(
    val numberOfFaces: Int = 0,
    val isPersonPresent: Boolean = false,
    val approximateSubjectSize: String = "None", // "None", "Small", "Medium", "Large"
    val isLikelyPortrait: Boolean = false,
    val detectedFaces: List<DetectedFace> = emptyList(),
    val skinRatio: Float = 0.0f,
    val primaryFaceBrightness: Float = 50.0f,
    val primaryFaceExposureRelativeToScene: Float = 0.0f,
    val primaryFaceClipping: Float = 0.0f,
    val primaryFaceShadowLevel: Float = 0.0f
) {
    companion object {
        val DEFAULT = SubjectAnalysis()
    }
}

/**
 * Output of the MotionAnalyzer.
 * Frame-to-frame temporal movement and blur risk estimation.
 */
data class MotionAnalysis(
    val motionScore: Float = 0.05f, // 0.0 (still) to 1.0 (heavy motion)
    val motionLevel: MotionLevel = MotionLevel.STILL,
    val isBlurRisk: Boolean = false
) {
    companion object {
        val DEFAULT = MotionAnalysis()
    }
}

/**
 * Output of PhotoQualityAnalyzer.
 * Objective, technical image-quality score from 0-100 based on exposure, brightness,
 * clipping, sharpness, and motion blur.
 *
 * NOTE: This is strictly a technical quality metric, not an artistic judgment.
 */
data class PhotoQualityScore(
    val totalScore: Int = 85,          // 0 to 100
    val exposureScore: Int = 85,       // 0 to 100
    val brightnessScore: Int = 85,     // 0 to 100
    val sharpnessScore: Int = 80,      // 0 to 100
    val stabilityScore: Int = 90,      // 0 to 100
    val highlightScore: Int = 85,      // 0 to 100 (penalty for blown highlights)
    val shadowScore: Int = 85,         // 0 to 100 (penalty for crushed shadows)
    val dynamicRangeScore: Int = 85,   // 0 to 100
    val ratingLabel: String = "GOOD",  // "EXCELLENT", "GREAT", "GOOD", "FAIR", "SUBOPTIMAL"
    val tips: List<String> = listOf("AI camera optimization active")
) {
    companion object {
        val DEFAULT = PhotoQualityScore()
    }
}

/**
 * Full aggregated scene analysis combining all modular analyzers.
 */
data class SceneAnalysis(
    val scene: SceneType = SceneType.DAYLIGHT,
    val confidence: Float = 0.85f,
    val lighting: LightingAnalysis = LightingAnalysis.DEFAULT,
    val subject: SubjectAnalysis = SubjectAnalysis.DEFAULT,
    val motion: MotionAnalysis = MotionAnalysis.DEFAULT,
    val photoQuality: PhotoQualityScore = PhotoQualityScore.DEFAULT,
    val sharpnessMetric: Float = 45.0f,
    val estimatedKelvin: Int = 5400,
    val greenVegetationRatio: Float = 0.1f,
    val warmColorRatio: Float = 0.2f,
    val coolBlueRatio: Float = 0.2f,
    val edgeDensity: Float = 0.25f,
    val skyDetected: Boolean = false,
    val dominantHue: Float = 120.0f,
    val timestampMs: Long = System.currentTimeMillis()
) {
    // Convenient getters for backward compatibility with legacy consumers
    val brightnessLuma: Float get() = lighting.brightness * 2.55f
    val lightingLevel: LightingCondition get() = lighting.condition
    val faceCount: Int get() = subject.numberOfFaces
    val faces: List<DetectedFace> get() = subject.detectedFaces
    val motionScore: Float get() = motion.motionScore
    val motionLevel: MotionLevel get() = motion.motionLevel
    val highlightClippingPercent: Float get() = lighting.highlightClipping
    val shadowClippingPercent: Float get() = lighting.shadowLevel
    val photoScore: PhotoQualityScore get() = photoQuality

    companion object {
        val INITIAL = SceneAnalysis()
    }
}

/**
 * Alias to support existing component imports seamlessly.
 */
typealias FrameAnalysisResult = SceneAnalysis
typealias AiPhotoScore = PhotoQualityScore

/**
 * Model representing a captured and processed photo.
 */
data class CapturedPhoto(
    val id: String = System.currentTimeMillis().toString(),
    val uri: Uri? = null,
    val originalBitmap: Bitmap? = null,
    val enhancedBitmap: Bitmap? = null,
    val profileApplied: ImageProcessingProfileType = ImageProcessingProfileType.NATURAL,
    val sceneAtCapture: SceneType = SceneType.DAYLIGHT,
    val photoScoreAtCapture: Int = 85,
    val timestamp: Long = System.currentTimeMillis(),
    val sceneAnalysis: SceneAnalysis? = null,
    val recommendation: CameraRecommendation? = null,
    val savedUri: Uri? = uri,
    val rawBitmap: Bitmap? = originalBitmap,
    val qualityScore: PhotoQualityScore? = null,
    val debugInfo: SubjectEnhancementDebugInfo? = null,
    val metricsOriginal: ObjectivePhotoQualityMetrics? = null,
    val metricsEnhanced: ObjectivePhotoQualityMetrics? = null
) {
    val displayBitmap: Bitmap? get() = enhancedBitmap ?: originalBitmap ?: rawBitmap
}
