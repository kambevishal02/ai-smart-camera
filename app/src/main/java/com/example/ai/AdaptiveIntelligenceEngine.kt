package com.example.ai

import com.example.model.AbCaptureSession
import com.example.model.AdaptiveCameraProfile
import com.example.model.AdaptiveEvaluationResult
import com.example.model.AdaptiveLearningPolicy
import com.example.model.AdaptiveLearningRecord
import com.example.model.AdaptiveParameterBounds
import com.example.model.AdaptiveParameters
import com.example.model.CameraCapabilities
import com.example.model.CaptureIntent
import com.example.model.DetailedTechnicalMetrics
import com.example.model.FacePriorityMode
import com.example.model.HighlightProtectionLevel
import com.example.model.LearningRejectionReason
import com.example.model.LightingCondition
import com.example.model.LightingContextType
import com.example.model.LowLightStrategy
import com.example.model.MotionLevel
import com.example.model.SceneAnalysis
import com.example.model.TestSceneType
import com.example.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.abs

/**
 * Diagnostic explanation structure for "Why did SMART AUTO change?".
 */
data class AdaptiveExplanation(
    val scene: String,
    val lightingContext: String,
    val baseEv: Float,
    val adaptiveBias: Float,
    val finalEv: Float,
    val reason: String,
    val confidencePct: Int,
    val evidenceSamples: Int
)

/**
 * AdaptiveIntelligenceEngine (V0.5).
 *
 * Coordinates between the abstract Base CaptureIntent and dynamic device self-calibration.
 * Operates strictly on-device, conservatively, bounded, and reversibly.
 */
object AdaptiveIntelligenceEngine {

    private val _lastExplanation = MutableStateFlow<AdaptiveExplanation?>(null)
    val lastExplanation: StateFlow<AdaptiveExplanation?> = _lastExplanation.asStateFlow()

    /**
     * Modifies the Base CaptureIntent according to the current AdaptiveCameraProfile (Section 7).
     *
     * Flow:
     * Live Analysis -> SmartExposureEngine -> Base CaptureIntent -> Adaptive Profile Adjustment -> Final CaptureIntent
     */
    fun applyAdaptiveAdjustments(
        baseIntent: CaptureIntent,
        analysis: SceneAnalysis,
        capabilities: CameraCapabilities
    ): CaptureIntent {
        val testScene = TestSceneType.fromSceneType(
            scene = analysis.scene,
            isPerson = analysis.subject.isPersonPresent,
            isLowLight = analysis.lighting.condition == LightingCondition.DARK || analysis.lighting.condition == LightingCondition.VERY_DARK
        )
        val lightingContext = LightingContextType.fromAnalysis(analysis.lighting)

        val profile = AdaptiveProfileStore.currentProfile.value
        val effectiveParams = profile.resolveEffectiveParameters(testScene, lightingContext)

        val baseEv = baseIntent.preferredExposureCompensation
        val adaptiveEvBias = effectiveParams.exposureBias
        val evMin = capabilities.evRangeMin * capabilities.evStep
        val evMax = capabilities.evRangeMax * capabilities.evStep
        val finalEv = (baseEv + adaptiveEvBias).coerceIn(evMin, evMax)

        // Adjust highlight protection if high clipping bias exists
        val adjustedHighlight = if (effectiveParams.highlightProtectionBias > 0.35f) {
            when (baseIntent.highlightProtection) {
                HighlightProtectionLevel.NONE -> HighlightProtectionLevel.LOW
                HighlightProtectionLevel.LOW -> HighlightProtectionLevel.MEDIUM
                HighlightProtectionLevel.MEDIUM, HighlightProtectionLevel.HIGH -> HighlightProtectionLevel.HIGH
            }
        } else {
            baseIntent.highlightProtection
        }

        // Adjust shadow strategy if shadow recovery bias exists
        val adjustedShadow = if (effectiveParams.shadowRecoveryBias > 0.20f && baseIntent.shadowPriority == LowLightStrategy.BALANCED) {
            LowLightStrategy.SHADOW_RECOVERY
        } else if (effectiveParams.lowLightBias < -0.20f) {
            LowLightStrategy.LOW_NOISE
        } else {
            baseIntent.shadowPriority
        }

        // Adjust face priority if face exposure bias exists
        val adjustedFacePriority = if (effectiveParams.faceExposureBias > 0.20f && analysis.subject.isPersonPresent) {
            if (baseIntent.facePriority == FacePriorityMode.BALANCED) FacePriorityMode.PRIORITIZE_FACE else baseIntent.facePriority
        } else {
            baseIntent.facePriority
        }

        // Adjust enhancement processing parameters conservatively (Section 13)
        val baseEnhance = baseIntent.enhancementParams
        val adjustedEnhance = baseEnhance.copy(
            sharpnessStrength = (baseEnhance.sharpnessStrength + effectiveParams.sharpeningBias).coerceIn(0f, 1f),
            noiseReductionStrength = (baseEnhance.noiseReductionStrength + effectiveParams.noiseReductionBias).coerceIn(0f, 1f),
            warmTint = (baseEnhance.warmTint + effectiveParams.whiteBalanceBias * 0.25f).coerceIn(-0.5f, 0.5f),
            contrastMultiplier = (baseEnhance.contrastMultiplier + effectiveParams.processingStrengthBias * 0.20f).coerceIn(0.6f, 1.8f)
        )

        // Formulate human-readable reason for "Why did SMART AUTO change?"
        val explanationReason = if (effectiveParams.sampleCount >= AdaptiveLearningPolicy.MIN_SAMPLE_COUNT_FOR_UPDATE && abs(adaptiveEvBias) >= 0.05f) {
            val direction = if (adaptiveEvBias > 0) "excessive shadow loss/underexposure" else "highlight clipping/overexposure"
            "Previous ${effectiveParams.sampleCount} valid ${testScene.displayName} A/B tests showed $direction."
        } else if (effectiveParams.sampleCount > 0) {
            "Accumulating initial evidence (${effectiveParams.sampleCount}/${AdaptiveLearningPolicy.MIN_SAMPLE_COUNT_FOR_UPDATE} samples). Baseline tuning active."
        } else {
            "Operating at calibrated baseline. No adaptive drift applied."
        }

        val explanation = AdaptiveExplanation(
            scene = testScene.displayName,
            lightingContext = lightingContext.label,
            baseEv = baseEv,
            adaptiveBias = adaptiveEvBias,
            finalEv = finalEv,
            reason = explanationReason,
            confidencePct = (effectiveParams.confidence * 100).toInt(),
            evidenceSamples = effectiveParams.sampleCount
        )
        _lastExplanation.value = explanation

        val adaptiveAnnotation = if (effectiveParams.sampleCount >= AdaptiveLearningPolicy.MIN_SAMPLE_COUNT_FOR_UPDATE && abs(adaptiveEvBias) >= 0.03f) {
            " | Adaptive EV: ${String.format(Locale.US, "%+.2f", adaptiveEvBias)} (${effectiveParams.sampleCount} tests)"
        } else {
            ""
        }

        return baseIntent.copy(
            preferredExposureCompensation = finalEv,
            highlightProtection = adjustedHighlight,
            shadowPriority = adjustedShadow,
            facePriority = adjustedFacePriority,
            enhancementParams = adjustedEnhance,
            reasoning = baseIntent.reasoning + adaptiveAnnotation
        )
    }

    /**
     * Evaluates a completed A/B capture session and updates the Adaptive Profile if eligible (Section 2, 4, 5).
     */
    fun processAbCaptureSession(
        session: AbCaptureSession,
        sceneAnalysis: SceneAnalysis? = null
    ): AdaptiveEvaluationResult {
        val testScene = session.testScene
        val lightingContext = if (sceneAnalysis != null) {
            LightingContextType.fromAnalysis(sceneAnalysis.lighting)
        } else {
            LightingContextType.NORMAL
        }
        val lightingCond = sceneAnalysis?.lighting?.condition ?: LightingCondition.NORMAL
        val motion = sceneAnalysis?.motion ?: com.example.model.MotionAnalysis.DEFAULT
        val subject = sceneAnalysis?.subject ?: com.example.model.SubjectAnalysis.DEFAULT

        // 1. Evaluate learning eligibility
        val eligibility = LearningEligibilityEvaluator.evaluateSessionEligibility(session, sceneAnalysis)

        val autoMetrics = session.photoA_Metrics
        val smartMetrics = session.photoB_Metrics
        val technicalImprovement = TechnicalQualityEvaluator.computeImprovementDelta(autoMetrics, smartMetrics)

        val metricDeltas = mapOf(
            "scoreDelta" to (smartMetrics.totalTechnicalScore - autoMetrics.totalTechnicalScore).toFloat(),
            "exposureDelta" to (smartMetrics.exposureScore - autoMetrics.exposureScore).toFloat(),
            "sharpnessDelta" to (smartMetrics.sharpnessScore - autoMetrics.sharpnessScore).toFloat(),
            "highlightDelta" to (smartMetrics.highlightClippingPct - autoMetrics.highlightClippingPct),
            "shadowDelta" to (smartMetrics.shadowClippingPct - autoMetrics.shadowClippingPct),
            "dynamicRangeDelta" to (smartMetrics.dynamicRangeStops - autoMetrics.dynamicRangeStops)
        )

        if (!eligibility.isEligible) {
            val record = AdaptiveLearningRecord(
                deviceProfile = session.deviceName,
                scene = testScene,
                lighting = lightingCond,
                lightingContext = lightingContext,
                motion = motion.motionLevel,
                parameterName = "ALL",
                previousParameter = 0f,
                observedMetric = "Eligibility Failed",
                calculatedCorrection = 0f,
                newParameter = 0f,
                confidence = session.recommendation.confidence,
                sampleCount = 0,
                learningDecision = "REJECTED",
                rejectionReason = eligibility.rejectionReason,
                explanation = eligibility.explanation
            )
            AdaptiveProfileStore.recordLearningEvent(record)
            AppLogger.i("AdaptiveIntelligenceEngine", "A/B Session rejected from learning: ${eligibility.rejectionReason} - ${eligibility.explanation}")

            return AdaptiveEvaluationResult(
                scene = testScene,
                lighting = lightingCond,
                lightingContext = lightingContext,
                subject = subject,
                motion = motion,
                smartIntent = session.recommendation.captureIntent,
                autoMetrics = autoMetrics,
                smartMetrics = smartMetrics,
                metricDeltas = metricDeltas,
                overallTechnicalImprovement = technicalImprovement,
                confidence = session.recommendation.confidence,
                eligibleForLearning = false,
                rejectionReason = eligibility.rejectionReason,
                explanation = eligibility.explanation
            )
        }

        // 2. Learning is eligible: calculate observed parameter corrections
        val observedCorrections = calculateObservedCorrections(autoMetrics, smartMetrics, testScene)

        // 3. Update scene-specific parameters conservatively
        val currentProfile = AdaptiveProfileStore.currentProfile.value
        val existingParams = currentProfile.sceneParameters[testScene] ?: AdaptiveParameters.DEFAULT
        val newSampleCount = existingParams.sampleCount + 1
        val sampleConfidence = session.recommendation.confidence.coerceIn(0.70f, 0.95f)

        val (newExposureBias, updatedConfidence) = AdaptiveLearningPolicy.computeWeightedUpdate(
            currentBias = existingParams.exposureBias,
            currentConfidence = existingParams.confidence,
            currentSamples = newSampleCount,
            observedCorrection = observedCorrections["exposureBias"] ?: 0.0f,
            newConfidence = sampleConfidence
        )

        val (newHighlightBias, _) = AdaptiveLearningPolicy.computeWeightedUpdate(
            currentBias = existingParams.highlightProtectionBias,
            currentConfidence = existingParams.confidence,
            currentSamples = newSampleCount,
            observedCorrection = observedCorrections["highlightProtectionBias"] ?: 0.0f,
            newConfidence = sampleConfidence,
            maxChangePerUpdate = AdaptiveLearningPolicy.MAX_CHANGE_PER_UPDATE_MULTIPLIER,
            maxTotalDeviation = AdaptiveLearningPolicy.MAX_TOTAL_BIAS_DEVIATION
        )

        val (newShadowBias, _) = AdaptiveLearningPolicy.computeWeightedUpdate(
            currentBias = existingParams.shadowRecoveryBias,
            currentConfidence = existingParams.confidence,
            currentSamples = newSampleCount,
            observedCorrection = observedCorrections["shadowRecoveryBias"] ?: 0.0f,
            newConfidence = sampleConfidence,
            maxChangePerUpdate = AdaptiveLearningPolicy.MAX_CHANGE_PER_UPDATE_MULTIPLIER,
            maxTotalDeviation = AdaptiveLearningPolicy.MAX_TOTAL_BIAS_DEVIATION
        )

        val (newSharpnessBias, _) = AdaptiveLearningPolicy.computeWeightedUpdate(
            currentBias = existingParams.sharpeningBias,
            currentConfidence = existingParams.confidence,
            currentSamples = newSampleCount,
            observedCorrection = observedCorrections["sharpeningBias"] ?: 0.0f,
            newConfidence = sampleConfidence,
            maxChangePerUpdate = AdaptiveLearningPolicy.MAX_CHANGE_PER_UPDATE_MULTIPLIER,
            maxTotalDeviation = AdaptiveParameterBounds.MAX_SHARPENING_BIAS
        )

        val (newNoiseBias, _) = AdaptiveLearningPolicy.computeWeightedUpdate(
            currentBias = existingParams.noiseReductionBias,
            currentConfidence = existingParams.confidence,
            currentSamples = newSampleCount,
            observedCorrection = observedCorrections["noiseReductionBias"] ?: 0.0f,
            newConfidence = sampleConfidence,
            maxChangePerUpdate = AdaptiveLearningPolicy.MAX_CHANGE_PER_UPDATE_MULTIPLIER,
            maxTotalDeviation = AdaptiveParameterBounds.MAX_NOISE_REDUCTION_BIAS
        )

        val updatedSceneParams = existingParams.copy(
            exposureBias = newExposureBias,
            highlightProtectionBias = newHighlightBias,
            shadowRecoveryBias = newShadowBias,
            sharpeningBias = newSharpnessBias,
            noiseReductionBias = newNoiseBias,
            sampleCount = newSampleCount,
            confidence = updatedConfidence,
            lastUpdatedTimestamp = System.currentTimeMillis()
        ).clampToBounds()

        val updatedSceneMap = currentProfile.sceneParameters.toMutableMap()
        updatedSceneMap[testScene] = updatedSceneParams

        val newProfile = currentProfile.copy(
            sceneParameters = updatedSceneMap,
            lastModifiedTimestamp = System.currentTimeMillis()
        )

        val isUpdated = newSampleCount >= AdaptiveLearningPolicy.MIN_SAMPLE_COUNT_FOR_UPDATE
        if (isUpdated && !AdaptiveProfileStore.isLearningPaused.value) {
            AdaptiveProfileStore.updateProfile(newProfile)
        }

        val decision = if (isUpdated) "ACCEPTED_UPDATE" else "EVIDENCE_ACCUMULATED_NO_UPDATE"
        val record = AdaptiveLearningRecord(
            deviceProfile = session.deviceName,
            scene = testScene,
            lighting = lightingCond,
            lightingContext = lightingContext,
            motion = motion.motionLevel,
            parameterName = "exposureBias",
            previousParameter = existingParams.exposureBias,
            observedMetric = "Smart Luma=${String.format("%.1f", smartMetrics.brightnessLuma)}%, ShdClip=${String.format("%.1f", smartMetrics.shadowClippingPct)}%, HlClip=${String.format("%.1f", smartMetrics.highlightClippingPct)}%",
            calculatedCorrection = observedCorrections["exposureBias"] ?: 0.0f,
            newParameter = newExposureBias,
            confidence = updatedConfidence,
            sampleCount = newSampleCount,
            learningDecision = decision,
            rejectionReason = null,
            explanation = if (isUpdated) {
                "Applied weighted adaptive update for ${testScene.displayName} based on $newSampleCount consistent tests"
            } else {
                "Evidence accumulated ($newSampleCount/${AdaptiveLearningPolicy.MIN_SAMPLE_COUNT_FOR_UPDATE} samples). Calibration preserved."
            }
        )
        AdaptiveProfileStore.recordLearningEvent(record)

        return AdaptiveEvaluationResult(
            scene = testScene,
            lighting = lightingCond,
            lightingContext = lightingContext,
            subject = subject,
            motion = motion,
            smartIntent = session.recommendation.captureIntent,
            autoMetrics = autoMetrics,
            smartMetrics = smartMetrics,
            metricDeltas = metricDeltas,
            overallTechnicalImprovement = technicalImprovement,
            confidence = sampleConfidence,
            eligibleForLearning = true,
            suggestedCorrections = observedCorrections,
            parameterUpdated = isUpdated,
            explanation = record.explanation
        )
    }

    private fun calculateObservedCorrections(
        autoMetrics: DetailedTechnicalMetrics,
        smartMetrics: DetailedTechnicalMetrics,
        scene: TestSceneType
    ): Map<String, Float> {
        val corrections = mutableMapOf<String, Float>()

        // 1. Exposure & Shadow Correction:
        // If Smart Auto image had high shadow clipping (>10%) or low luma in sunsets/landscapes, suggest positive EV / shadow lift
        if (smartMetrics.shadowClippingPct > 12.0f && smartMetrics.brightnessLuma < 40.0f) {
            corrections["exposureBias"] = 0.12f
            corrections["shadowRecoveryBias"] = 0.15f
        } else if (smartMetrics.highlightClippingPct > 8.0f) {
            corrections["exposureBias"] = -0.15f
            corrections["highlightProtectionBias"] = 0.20f
        } else {
            corrections["exposureBias"] = 0.0f
        }

        // 2. Sharpness tuning:
        // If high sharpness (>85) introduces excess noise (>20), suggest slight reduction
        if (smartMetrics.sharpnessScore > 85 && smartMetrics.noiseEstimate > 22.0f) {
            corrections["sharpeningBias"] = -0.06f
            corrections["noiseReductionBias"] = 0.08f
        }

        return corrections
    }
}
