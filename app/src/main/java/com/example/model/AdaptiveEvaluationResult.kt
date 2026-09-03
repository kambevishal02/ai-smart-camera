package com.example.model

import org.json.JSONObject

/**
 * Rejection reasons enforcing the "Do NOT learn from bad data" mandate.
 */
enum class LearningRejectionReason(val label: String, val explanation: String) {
    POOR_IMAGE_QUALITY(
        "Poor Image Quality",
        "Image quality was below technical baseline threshold (insufficient detail or sensor blackout)."
    ),
    HIGH_MOTION_DURING_AB_CAPTURE(
        "High Motion During A/B Capture",
        "Significant camera or subject motion occurred between Photo A and Photo B."
    ),
    LOW_SCENE_CONFIDENCE(
        "Low Scene Confidence",
        "Scene classification confidence was below 0.65 threshold."
    ),
    UNSTABLE_FACE_DETECTION(
        "Unstable Face Detection",
        "Face presence or bounding boxes were inconsistent between comparative captures."
    ),
    CAPTURE_FAILED(
        "Capture Failed",
        "Hardware capture pipeline returned incomplete image data or failed frame acquisition."
    ),
    HARDWARE_FALLBACK_MISMATCH(
        "Hardware Fallback Mismatch",
        "Hardware capability limitations forced substantial fallback from the requested intent."
    ),
    INCOMPARABLE_CAPTURES_LIGHTING_SHIFT(
        "Incomparable Captures (Lighting Shift)",
        "Ambient lighting shifted significantly between A and B captures beyond camera control."
    ),
    INVALID_METRICS(
        "Invalid Metrics",
        "Computed technical metrics contained invalid or non-finite values."
    ),
    INSUFFICIENT_IMPROVEMENT(
        "Insufficient Improvement",
        "Technical score improvement did not meet minimum threshold for positive reinforcement."
    )
}

/**
 * Encapsulates the evaluation of an A/B test capture session for self-calibration.
 */
data class AdaptiveEvaluationResult(
    val scene: TestSceneType,
    val lighting: LightingCondition,
    val lightingContext: LightingContextType,
    val subject: SubjectAnalysis,
    val motion: MotionAnalysis,
    val smartIntent: CaptureIntent,
    val autoMetrics: DetailedTechnicalMetrics,
    val smartMetrics: DetailedTechnicalMetrics,
    val metricDeltas: Map<String, Float>,
    val overallTechnicalImprovement: Float,
    val confidence: Float,
    val eligibleForLearning: Boolean,
    val rejectionReason: LearningRejectionReason? = null,
    val suggestedCorrections: Map<String, Float> = emptyMap(),
    val parameterUpdated: Boolean = false,
    val explanation: String = ""
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("scene", scene.name)
            put("lighting", lighting.name)
            put("lightingContext", lightingContext.name)
            put("overallTechnicalImprovement", overallTechnicalImprovement)
            put("confidence", confidence)
            put("eligibleForLearning", eligibleForLearning)
            rejectionReason?.let { put("rejectionReason", it.name) }
            put("explanation", explanation)

            val deltasJson = JSONObject()
            metricDeltas.forEach { (k, v) -> deltasJson.put(k, v) }
            put("metricDeltas", deltasJson)

            val correctionsJson = JSONObject()
            suggestedCorrections.forEach { (k, v) -> correctionsJson.put(k, v) }
            put("suggestedCorrections", correctionsJson)
        }
    }
}
