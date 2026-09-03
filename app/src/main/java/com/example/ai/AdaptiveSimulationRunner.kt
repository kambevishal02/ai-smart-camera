package com.example.ai

import com.example.model.AbCaptureSession
import com.example.model.AdaptiveEvaluationResult
import com.example.model.CameraCapabilities
import com.example.model.CameraRecommendation
import com.example.model.CaptureIntent
import com.example.model.DetailedTechnicalMetrics
import com.example.model.ImageProcessingProfileType
import com.example.model.LearningRejectionReason
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SubjectAnalysis
import com.example.model.TestSceneType

/**
 * Deterministic Simulation Engine for V0.5 Adaptive Camera Intelligence (Section 16).
 *
 * Provides reproducible, offline test suites to prove:
 * 1. Insufficient samples (<3) yield NO parameter change.
 * 2. Consistent samples (>=5) yield conservative, bounded positive bias for target scene.
 * 3. Scene isolation: Sunset learning does not drift Daylight or Portrait profiles.
 * 4. Bad data rejection: Noisy data, high motion, and low confidence are strictly rejected.
 */
object AdaptiveSimulationRunner {

    data class SimulationOutcome(
        val scenarioName: String,
        val samplesRun: Int,
        val initialExposureBias: Float,
        val finalExposureBias: Float,
        val finalShadowBias: Float,
        val rejectionCount: Int,
        val eligibleCount: Int,
        val lastRejectionReason: LearningRejectionReason?,
        val successVerification: String
    )

    /**
     * Creates a synthetic A/B session tailored to test specific edge conditions.
     */
    fun createSyntheticSession(
        scene: TestSceneType,
        smartLuma: Float = 32.0f,
        smartShadowClip: Float = 16.0f,
        smartHighlightClip: Float = 2.0f,
        motionScore: Float = 0.05f,
        confidence: Float = 0.85f,
        technicalScoreA: Int = 60,
        technicalScoreB: Int = 74,
        includeFallbackMismatch: Boolean = false
    ): Pair<AbCaptureSession, SceneAnalysis> {
        val autoMetrics = DetailedTechnicalMetrics(
            exposureScore = 55,
            brightnessLuma = 42.0f,
            contrastRms = 22.0f,
            highlightClippingPct = 4.0f,
            shadowClippingPct = 14.0f,
            sharpnessScore = 60,
            noiseEstimate = 12.0f,
            dynamicRangeStops = 8.5f,
            colorCastOffset = 5.0f,
            faceExposureLuma = null,
            totalTechnicalScore = technicalScoreA,
            ratingLabel = "ACCEPTABLE"
        )

        val smartMetrics = DetailedTechnicalMetrics(
            exposureScore = 72,
            brightnessLuma = smartLuma,
            contrastRms = 28.0f,
            highlightClippingPct = smartHighlightClip,
            shadowClippingPct = smartShadowClip,
            sharpnessScore = 76,
            noiseEstimate = 11.0f,
            dynamicRangeStops = 10.2f,
            colorCastOffset = 4.0f,
            faceExposureLuma = null,
            totalTechnicalScore = technicalScoreB,
            ratingLabel = "GOOD"
        )

        val fallbackMap = if (includeFallbackMismatch) {
            mapOf("EV Compensation" to "Fallback to Software Tone Curve")
        } else {
            emptyMap()
        }

        val rec = CameraRecommendation(
            exposureCompensationIndex = -2,
            exposureCompensationEv = -0.33f,
            focusStrategy = com.example.model.FocusStrategy.AUTO,
            flashRecommendation = com.example.model.FlashRecommendation.OFF,
            zoomRecommendation = 1.0f,
            whiteBalance = com.example.model.WhiteBalanceRecommendation.AUTO,
            isoPreference = com.example.model.IsoPreference.BALANCED,
            shutterPreference = com.example.model.ShutterPreference.BALANCED,
            isoRecommendation = "ISO 100",
            shutterRecommendation = "1/120s",
            imageProcessingProfile = ImageProcessingProfileType.SUNSET,
            enhancementParams = com.example.model.EnhancementParameters.defaultForProfile(ImageProcessingProfileType.SUNSET),
            recommendedLensType = com.example.model.CameraLensType.MAIN_WIDE,
            recommendedLens = "Main Wide Lens",
            primaryActionText = "Smart Sunset Calibration",
            secondaryReasonText = "Dynamic Range Preservation",
            confidence = confidence,
            captureIntent = CaptureIntent.DEFAULT
        )

        val session = AbCaptureSession(
            testScene = scene,
            deviceName = "Simulated Android Device",
            cameraId = "0",
            hardwareLevel = "LEVEL_3",
            photoA_Metrics = autoMetrics,
            photoA_AppliedSettings = mapOf("Mode" to "Auto"),
            photoB_Metrics = smartMetrics,
            photoB_AppliedSettings = mapOf("EV Index" to "-2 (-0.33 EV)"),
            recommendation = rec,
            fallbackSettings = fallbackMap,
            processingProfile = ImageProcessingProfileType.SUNSET
        )

        val motionLevel = if (motionScore > 0.40f) MotionLevel.HIGH else MotionLevel.STILL
        val sceneAnalysis = SceneAnalysis(
            scene = scene.correspondingScene,
            lighting = LightingAnalysis(
                condition = LightingCondition.NORMAL,
                brightness = smartLuma,
                contrast = 60f,
                highlightClipping = smartHighlightClip,
                darkness = 20f,
                shadowLevel = smartShadowClip
            ),
            subject = SubjectAnalysis.DEFAULT,
            motion = MotionAnalysis(
                motionScore = motionScore,
                motionLevel = motionLevel,
                isBlurRisk = motionScore > 0.40f
            ),
            sharpnessMetric = 75f,
            estimatedKelvin = 4800,
            confidence = confidence
        )

        return Pair(session, sceneAnalysis)
    }

    /**
     * Runs deterministic Sunset shadow loss learning test.
     */
    fun runSunsetShadowLossSimulation(): Pair<SimulationOutcome, SimulationOutcome> {
        AdaptiveProfileStore.resetToBaseline()
        val initialProfile = AdaptiveProfileStore.currentProfile.value
        val initialSunsetBias = initialProfile.sceneParameters[TestSceneType.SUNSET]?.exposureBias ?: 0.0f

        // Phase 1: 2 sessions (< 3 threshold) -> Must yield NO CHANGE
        repeat(2) {
            val (session, analysis) = createSyntheticSession(TestSceneType.SUNSET, smartLuma = 30f, smartShadowClip = 18f)
            AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        }

        val intermediateProfile = AdaptiveProfileStore.currentProfile.value
        val intermediateSunsetBias = intermediateProfile.sceneParameters[TestSceneType.SUNSET]?.exposureBias ?: 0.0f
        val phase1Outcome = SimulationOutcome(
            scenarioName = "Sunset Shadow Loss (Phase 1: 2 samples)",
            samplesRun = 2,
            initialExposureBias = initialSunsetBias,
            finalExposureBias = intermediateSunsetBias,
            finalShadowBias = intermediateProfile.sceneParameters[TestSceneType.SUNSET]?.shadowRecoveryBias ?: 0.0f,
            rejectionCount = 0,
            eligibleCount = 2,
            lastRejectionReason = null,
            successVerification = if (intermediateSunsetBias == 0.0f) "PASSED: Preserved 0.0 bias (insufficient evidence)" else "FAILED"
        )

        // Phase 2: 3 more sessions (total 5 samples) -> Must yield conservative positive update
        repeat(3) {
            val (session, analysis) = createSyntheticSession(TestSceneType.SUNSET, smartLuma = 30f, smartShadowClip = 18f)
            AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        }

        val finalProfile = AdaptiveProfileStore.currentProfile.value
        val finalSunsetBias = finalProfile.sceneParameters[TestSceneType.SUNSET]?.exposureBias ?: 0.0f
        val finalShadowBias = finalProfile.sceneParameters[TestSceneType.SUNSET]?.shadowRecoveryBias ?: 0.0f

        val phase2Outcome = SimulationOutcome(
            scenarioName = "Sunset Shadow Loss (Phase 2: 5 samples)",
            samplesRun = 5,
            initialExposureBias = initialSunsetBias,
            finalExposureBias = finalSunsetBias,
            finalShadowBias = finalShadowBias,
            rejectionCount = 0,
            eligibleCount = 5,
            lastRejectionReason = null,
            successVerification = if (finalSunsetBias > 0.0f && finalShadowBias > 0.0f) "PASSED: Conservative positive update applied (+${String.format("%.2f", finalSunsetBias)} EV, +${String.format("%.2f", finalShadowBias)} shadow)" else "FAILED"
        )

        return Pair(phase1Outcome, phase2Outcome)
    }

    /**
     * Verifies Daylight scene isolation when Sunset has learned.
     */
    fun verifySceneIsolation(): SimulationOutcome {
        val currentProfile = AdaptiveProfileStore.currentProfile.value
        val daylightBias = currentProfile.sceneParameters[TestSceneType.DAYLIGHT]?.exposureBias ?: 0.0f
        return SimulationOutcome(
            scenarioName = "Daylight Scene Isolation",
            samplesRun = 0,
            initialExposureBias = 0.0f,
            finalExposureBias = daylightBias,
            finalShadowBias = currentProfile.sceneParameters[TestSceneType.DAYLIGHT]?.shadowRecoveryBias ?: 0.0f,
            rejectionCount = 0,
            eligibleCount = 0,
            lastRejectionReason = null,
            successVerification = if (daylightBias == 0.0f) "PASSED: Daylight profile completely isolated from Sunset learning" else "FAILED: Cross-scene leakage"
        )
    }

    /**
     * Tests rejection of bad/noisy data.
     */
    fun testNoisyDataRejection(): SimulationOutcome {
        val (session, analysis) = createSyntheticSession(
            scene = TestSceneType.INDOOR,
            technicalScoreA = 15,
            technicalScoreB = 20 // Below 25 threshold
        )
        val result = AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        return SimulationOutcome(
            scenarioName = "Noisy/Sub-par Image Quality",
            samplesRun = 1,
            initialExposureBias = 0.0f,
            finalExposureBias = 0.0f,
            finalShadowBias = 0.0f,
            rejectionCount = 1,
            eligibleCount = 0,
            lastRejectionReason = result.rejectionReason,
            successVerification = if (!result.eligibleForLearning && result.rejectionReason == LearningRejectionReason.POOR_IMAGE_QUALITY) {
                "PASSED: Correctly rejected (POOR_IMAGE_QUALITY)"
            } else {
                "FAILED"
            }
        )
    }

    /**
     * Tests rejection when camera motion is high.
     */
    fun testHighMotionRejection(): SimulationOutcome {
        val (session, analysis) = createSyntheticSession(
            scene = TestSceneType.DAYLIGHT,
            motionScore = 0.75f // High motion
        )
        val result = AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        return SimulationOutcome(
            scenarioName = "High Motion During Capture",
            samplesRun = 1,
            initialExposureBias = 0.0f,
            finalExposureBias = 0.0f,
            finalShadowBias = 0.0f,
            rejectionCount = 1,
            eligibleCount = 0,
            lastRejectionReason = result.rejectionReason,
            successVerification = if (!result.eligibleForLearning && result.rejectionReason == LearningRejectionReason.HIGH_MOTION_DURING_AB_CAPTURE) {
                "PASSED: Correctly rejected (HIGH_MOTION_DURING_AB_CAPTURE)"
            } else {
                "FAILED"
            }
        )
    }

    /**
     * Tests rejection when scene classification confidence is low.
     */
    fun testLowConfidenceRejection(): SimulationOutcome {
        val (session, analysis) = createSyntheticSession(
            scene = TestSceneType.FOREST,
            confidence = 0.45f // Below 0.65 threshold
        )
        val result = AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        return SimulationOutcome(
            scenarioName = "Low Scene Classification Confidence",
            samplesRun = 1,
            initialExposureBias = 0.0f,
            finalExposureBias = 0.0f,
            finalShadowBias = 0.0f,
            rejectionCount = 1,
            eligibleCount = 0,
            lastRejectionReason = result.rejectionReason,
            successVerification = if (!result.eligibleForLearning && result.rejectionReason == LearningRejectionReason.LOW_SCENE_CONFIDENCE) {
                "PASSED: Correctly rejected (LOW_SCENE_CONFIDENCE)"
            } else {
                "FAILED"
            }
        )
    }
}
