package com.example.ai

import com.example.model.AbCaptureSession
import com.example.model.DetailedTechnicalMetrics
import com.example.model.LearningRejectionReason
import com.example.model.MotionLevel
import com.example.model.SceneAnalysis
import kotlin.math.abs

/**
 * Result of the learning eligibility check.
 */
data class EligibilityCheckResult(
    val isEligible: Boolean,
    val rejectionReason: LearningRejectionReason? = null,
    val explanation: String
)

/**
 * LearningEligibilityEvaluator (Section 3: DO NOT LEARN FROM BAD DATA).
 *
 * Strictly guards the self-calibration pipeline against noisy, blurry,
 * failed, or non-comparable A/B captures.
 */
object LearningEligibilityEvaluator {

    private const val MIN_TECHNICAL_SCORE_THRESHOLD = 25
    private const val MIN_SCENE_CONFIDENCE_THRESHOLD = 0.65f
    private const val MAX_ALLOWABLE_MOTION_SCORE = 0.40f
    private const val MAX_UNACCOUNTED_LUMA_SHIFT_PCT = 35.0f

    fun evaluateEligibility(
        session: AbCaptureSession,
        sceneAnalysis: SceneAnalysis? = null
    ): EligibilityCheckResult = evaluateSessionEligibility(session, sceneAnalysis)

    fun evaluateSessionEligibility(
        session: AbCaptureSession,
        sceneAnalysis: SceneAnalysis? = null
    ): EligibilityCheckResult {
        val aMetrics = session.photoA_Metrics
        val bMetrics = session.photoB_Metrics

        // 1. Metric calculation validity check
        if (!areMetricsValid(aMetrics) || !areMetricsValid(bMetrics)) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.INVALID_METRICS,
                explanation = "Computed technical metrics contained non-finite or invalid numerical values."
            )
        }

        // 2. Capture failed check
        if (session.photoA_Uri == null && session.photoA_Bitmap == null) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.CAPTURE_FAILED,
                explanation = "Standard AUTO capture frame was missing or null."
            )
        }
        if (session.photoB_Uri == null && session.photoB_Bitmap == null) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.CAPTURE_FAILED,
                explanation = "SMART AUTO capture frame was missing or null."
            )
        }

        // 3. Image quality threshold check
        if (aMetrics.totalTechnicalScore < MIN_TECHNICAL_SCORE_THRESHOLD ||
            bMetrics.totalTechnicalScore < MIN_TECHNICAL_SCORE_THRESHOLD) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.POOR_IMAGE_QUALITY,
                explanation = "Overall technical score was too low (<$MIN_TECHNICAL_SCORE_THRESHOLD) for reliable calibration."
            )
        }

        // 4. Motion stability check
        val motion = sceneAnalysis?.motion
        if (motion != null) {
            if (motion.motionLevel == MotionLevel.HIGH || motion.motionScore > MAX_ALLOWABLE_MOTION_SCORE || motion.isBlurRisk) {
                return EligibilityCheckResult(
                    isEligible = false,
                    rejectionReason = LearningRejectionReason.HIGH_MOTION_DURING_AB_CAPTURE,
                    explanation = "Camera or subject moved significantly during capture (motion score: ${String.format("%.2f", motion.motionScore)})."
                )
            }
        }

        // 5. Scene classification confidence check
        val confidence = sceneAnalysis?.confidence ?: session.recommendation.confidence
        if (confidence < MIN_SCENE_CONFIDENCE_THRESHOLD) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.LOW_SCENE_CONFIDENCE,
                explanation = "Scene detection confidence (${String.format("%.2f", confidence)}) was below $MIN_SCENE_CONFIDENCE_THRESHOLD threshold."
            )
        }

        // 6. Face detection stability check
        val hasFaceA = aMetrics.faceExposureLuma != null
        val hasFaceB = bMetrics.faceExposureLuma != null
        val isPortraitScene = session.testScene.name.contains("PORTRAIT", ignoreCase = true)
        if (isPortraitScene && (hasFaceA != hasFaceB)) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.UNSTABLE_FACE_DETECTION,
                explanation = "Face presence was inconsistent between comparative shots in portrait scene."
            )
        }

        // 7. Hardware fallback severity check
        // If hardware fallback prevented applying EV compensation completely on an exposure-critical scene
        if (session.fallbackSettings.containsKey("EV Compensation") &&
            abs(session.recommendation.exposureCompensationEv) > 0.6f) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.HARDWARE_FALLBACK_MISMATCH,
                explanation = "Device HAL failed to apply critical EV compensation, falling back to software tone curve."
            )
        }

        // 8. Comparability check: radical ambient lighting shifts
        val lumaDelta = abs(bMetrics.brightnessLuma - aMetrics.brightnessLuma)
        val appliedEv = session.recommendation.exposureCompensationEv
        // If luma changed by >35% without corresponding EV offset recommendation
        if (lumaDelta > MAX_UNACCOUNTED_LUMA_SHIFT_PCT && abs(appliedEv) < 0.3f) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.INCOMPARABLE_CAPTURES_LIGHTING_SHIFT,
                explanation = "Ambient scene lighting shifted by ${String.format("%.1f", lumaDelta)}% between comparative captures."
            )
        }

        // 9. Insufficient technical improvement check
        val improvementDelta = TechnicalQualityEvaluator.computeImprovementDelta(aMetrics, bMetrics)
        if (improvementDelta < -5.0f) {
            return EligibilityCheckResult(
                isEligible = false,
                rejectionReason = LearningRejectionReason.INSUFFICIENT_IMPROVEMENT,
                explanation = "SMART AUTO showed significant technical degradation (${String.format("%+.1f", improvementDelta)}) vs AUTO."
            )
        }

        return EligibilityCheckResult(
            isEligible = true,
            rejectionReason = null,
            explanation = "Capture meets all stability, quality, and comparability requirements for self-calibration."
        )
    }

    private fun areMetricsValid(metrics: DetailedTechnicalMetrics): Boolean {
        return !metrics.brightnessLuma.isNaN() && !metrics.brightnessLuma.isInfinite() &&
               !metrics.contrastRms.isNaN() && !metrics.contrastRms.isInfinite() &&
               !metrics.highlightClippingPct.isNaN() && !metrics.highlightClippingPct.isInfinite() &&
               !metrics.shadowClippingPct.isNaN() && !metrics.shadowClippingPct.isInfinite() &&
               !metrics.noiseEstimate.isNaN() && !metrics.noiseEstimate.isInfinite() &&
               !metrics.dynamicRangeStops.isNaN() && !metrics.dynamicRangeStops.isInfinite()
    }
}
