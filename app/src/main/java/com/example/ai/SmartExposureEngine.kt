package com.example.ai

import com.example.model.CalibrationParameters
import com.example.model.CameraCapabilities
import com.example.model.CameraLensType
import com.example.model.CaptureIntent
import com.example.model.DeviceCalibrationProfile
import com.example.model.EnhancementParameters
import com.example.model.FacePriorityMode
import com.example.model.FlashRecommendation
import com.example.model.HighlightProtectionLevel
import com.example.model.ImageProcessingProfileType
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.LowLightStrategy
import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import com.example.model.MotionStrategy
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SubjectAnalysis
import com.example.model.WhiteBalanceRecommendation
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * SmartExposureEngine (V0.4): Intelligent Camera Control & Exposure Strategy System.
 *
 * Evaluates live scene, environmental lighting, highlight/shadow distributions, face exposure,
 * motion, physical camera lens characteristics, and previous calibration profiles to formulate
 * a high-level, platform-independent CaptureIntent.
 *
 * Core Directives:
 * 1. Correctly expose the important subject while protecting highlights and preserving useful shadows.
 * 2. Never blindly maximize brightness.
 * 3. Never directly manipulate Camera2 objects; produce CaptureIntent for CameraHardwareAdapter.
 * 4. Temporal stability with rolling history and hysteresis to avoid exposure oscillations.
 * 5. Device-independent calibration integration via data/configuration models.
 */
class SmartExposureEngine {

    // Rolling history buffers for temporal stability (keeping 5 recent evaluations)
    private val recentBrightness = ArrayDeque<Float>(HISTORY_CAPACITY)
    private val recentEvOffsets = ArrayDeque<Float>(HISTORY_CAPACITY)
    private val recentHighlightClips = ArrayDeque<Float>(HISTORY_CAPACITY)
    private val recentMotionScores = ArrayDeque<Float>(HISTORY_CAPACITY)
    private val recentScenes = ArrayDeque<SceneType>(HISTORY_CAPACITY)

    // Last committed target EV to enforce hysteresis
    private var lastCommittedEvOffset = 0.0f
    private var lastCommittedZoom = 1.0f

    /**
     * Evaluates live metrics and produces a comprehensive CaptureIntent.
     */
    fun evaluateCaptureIntent(
        analysis: SceneAnalysis,
        capabilities: CameraCapabilities,
        calibrationProfile: DeviceCalibrationProfile? = null,
        activeCalibrationParams: CalibrationParameters? = null
    ): CaptureIntent {
        val lighting = analysis.lighting
        val subject = analysis.subject
        val motion = analysis.motion
        val scene = analysis.scene

        // 1. Update temporal smoothing buffers
        recordMetrics(lighting.brightness, lighting.highlightClipping, motion.motionScore, scene)

        val smoothedBrightness = recentBrightness.average().toFloat()
        val smoothedHighlightClipping = recentHighlightClips.average().toFloat()
        val smoothedMotionScore = recentMotionScores.average().toFloat()

        // 2. Classify Highlight Protection Level
        val highlightProtection = determineHighlightProtection(
            scene = scene,
            highlightClipping = smoothedHighlightClipping,
            skyDetected = analysis.skyDetected,
            brightness = smoothedBrightness
        )

        // 3. Classify Low-Light / Shadow Strategy
        val shadowStrategy = determineLowLightStrategy(
            scene = scene,
            lighting = lighting,
            motion = motion,
            brightness = smoothedBrightness
        )

        // 4. Classify Motion Strategy
        val motionStrategy = determineMotionStrategy(
            motion = motion,
            smoothedMotionScore = smoothedMotionScore
        )

        // 5. Classify Face Priority Requirement
        val facePriority = determineFacePriority(
            subject = subject,
            sceneBrightness = smoothedBrightness,
            highlightProtection = highlightProtection
        )

        // 6. Compute Raw Target Exposure Compensation (EV)
        val rawEvTarget = computeTargetEv(
            scene = scene,
            lighting = lighting,
            subject = subject,
            highlightProtection = highlightProtection,
            shadowStrategy = shadowStrategy,
            facePriority = facePriority,
            calibrationParams = activeCalibrationParams
        )

        // 7. Apply Hysteresis & Temporal Smoothing to EV
        val finalEvTarget = applyEvHysteresis(rawEvTarget, capabilities)

        // 8. Determine White Balance Recommendation based on Lighting & Spectral Estimations
        val whiteBalance = determineWhiteBalance(
            scene = scene,
            estimatedKelvin = analysis.estimatedKelvin,
            warmColorRatio = analysis.warmColorRatio,
            coolBlueRatio = analysis.coolBlueRatio
        )

        // 9. Intelligent Lens Selection dynamically matching scene & hardware capabilities
        val preferredLens = selectPreferredLens(
            scene = scene,
            subject = subject,
            capabilities = capabilities
        )

        // 10. Smart Zoom Recommendation with Hysteresis
        val preferredZoom = determineSmartZoom(
            scene = scene,
            subject = subject,
            capabilities = capabilities
        )

        // 11. Flash Recommendation
        val flashRec = determineFlashPreference(
            scene = scene,
            lighting = lighting,
            subject = subject,
            capabilities = capabilities
        )

        // 12. Processing Profile & Base Enhancement Parameters
        val profile = selectProcessingProfile(scene, subject)
        var baseParams = EnhancementParameters.defaultForProfile(profile)
        if (activeCalibrationParams != null) {
            baseParams = baseParams.copy(
                exposureOffset = baseParams.exposureOffset + activeCalibrationParams.exposureBias,
                highlightCompression = (baseParams.highlightCompression * activeCalibrationParams.highlightRecovery).coerceIn(0f, 1f),
                shadowLift = (baseParams.shadowLift * activeCalibrationParams.shadowRecovery).coerceIn(0f, 1f),
                contrastMultiplier = (baseParams.contrastMultiplier * activeCalibrationParams.contrast).coerceIn(0.5f, 2f),
                saturationMultiplier = (baseParams.saturationMultiplier * activeCalibrationParams.saturation).coerceIn(0.5f, 2f),
                sharpnessStrength = (baseParams.sharpnessStrength * activeCalibrationParams.sharpness).coerceIn(0f, 1f),
                noiseReductionStrength = (baseParams.noiseReductionStrength * activeCalibrationParams.noiseReduction).coerceIn(0f, 1f)
            )
        }

        // 13. Formulate Reasoning Summary
        val reasoning = buildReasoning(
            scene = scene,
            highlightProtection = highlightProtection,
            facePriority = facePriority,
            shadowStrategy = shadowStrategy,
            motionStrategy = motionStrategy,
            finalEv = finalEvTarget
        )

        val baseIntent = CaptureIntent(
            preferredExposureCompensation = finalEvTarget,
            exposurePriority = when (facePriority) {
                FacePriorityMode.PRIORITIZE_FACE, FacePriorityMode.EXTREME_BACKLIGHT -> "Subject Face Priority"
                else -> if (highlightProtection == HighlightProtectionLevel.HIGH) "Highlight Protection Priority" else "Balanced Matrix Metering"
            },
            highlightProtection = highlightProtection,
            shadowPriority = shadowStrategy,
            facePriority = facePriority,
            motionPriority = motionStrategy,
            preferredWhiteBalance = whiteBalance,
            preferredLens = preferredLens,
            preferredZoom = preferredZoom,
            flashPreference = flashRec,
            stabilizationPreference = if (motionStrategy == MotionStrategy.HIGH_MOTION) "Fast Shutter Priority" else "Standard OIS/EIS",
            processingProfile = profile,
            enhancementParams = baseParams,
            confidence = analysis.confidence,
            reasoning = reasoning
        )

        // Section 7: Adaptive Profile Adjustment converts Base CaptureIntent to Final CaptureIntent
        return AdaptiveIntelligenceEngine.applyAdaptiveAdjustments(
            baseIntent = baseIntent,
            analysis = analysis,
            capabilities = capabilities
        )
    }

    /**
     * Determines Highlight Protection level based on scene, clipping distribution, and sky presence.
     */
    fun determineHighlightProtection(
        scene: SceneType,
        highlightClipping: Float,
        skyDetected: Boolean,
        brightness: Float
    ): HighlightProtectionLevel {
        return when {
            scene == SceneType.SUNSET || (skyDetected && highlightClipping > 7.0f) || highlightClipping > 14.0f -> {
                HighlightProtectionLevel.HIGH
            }
            scene == SceneType.BEACH || (skyDetected && highlightClipping > 3.0f) || highlightClipping in 5.0f..14.0f -> {
                HighlightProtectionLevel.MEDIUM
            }
            brightness > 75.0f || highlightClipping in 1.5f..5.0f -> {
                HighlightProtectionLevel.LOW
            }
            else -> HighlightProtectionLevel.NONE
        }
    }

    /**
     * Determines low-light / shadow strategy.
     * Prevents excessive sensor noise amplification in dark scenes.
     */
    fun determineLowLightStrategy(
        scene: SceneType,
        lighting: LightingAnalysis,
        motion: MotionAnalysis,
        brightness: Float
    ): LowLightStrategy {
        val isExtremelyDark = lighting.condition == LightingCondition.VERY_DARK || brightness < 15.0f
        val isDark = lighting.condition == LightingCondition.DARK || brightness < 35.0f
        val isStaticScene = motion.motionLevel == MotionLevel.STILL || motion.motionScore < 0.10f

        return when {
            scene == SceneType.NIGHT && isStaticScene -> LowLightStrategy.LOW_NOISE
            isExtremelyDark && !isStaticScene -> LowLightStrategy.LOW_NOISE // avoid long exposure noise smear
            scene == SceneType.LOW_LIGHT || (isDark && lighting.shadowLevel > 15.0f) -> LowLightStrategy.SHADOW_RECOVERY
            else -> LowLightStrategy.BALANCED
        }
    }

    /**
     * Classifies motion strategy based on frame-to-frame movement metrics.
     */
    fun determineMotionStrategy(
        motion: MotionAnalysis,
        smoothedMotionScore: Float
    ): MotionStrategy {
        return when {
            smoothedMotionScore > 0.40f || motion.motionLevel == MotionLevel.HIGH -> MotionStrategy.HIGH_MOTION
            smoothedMotionScore > 0.20f || motion.motionLevel == MotionLevel.MODERATE -> MotionStrategy.MEDIUM_MOTION
            smoothedMotionScore > 0.08f || motion.motionLevel == MotionLevel.LOW -> MotionStrategy.LOW_MOTION
            else -> MotionStrategy.STATIC
        }
    }

    /**
     * Determines face-priority exposure requirement.
     */
    fun determineFacePriority(
        subject: SubjectAnalysis,
        sceneBrightness: Float,
        highlightProtection: HighlightProtectionLevel
    ): FacePriorityMode {
        if (!subject.isPersonPresent || subject.numberOfFaces == 0) {
            return FacePriorityMode.NONE
        }

        val faceRel = subject.primaryFaceExposureRelativeToScene
        val faceBri = subject.primaryFaceBrightness

        return when {
            faceRel < -20.0f && (highlightProtection == HighlightProtectionLevel.HIGH || highlightProtection == HighlightProtectionLevel.MEDIUM) -> {
                FacePriorityMode.EXTREME_BACKLIGHT
            }
            faceRel < -10.0f || faceBri < 35.0f -> {
                FacePriorityMode.PRIORITIZE_FACE
            }
            else -> {
                FacePriorityMode.BALANCED
            }
        }
    }

    /**
     * Computes raw target exposure compensation (EV) before hysteresis.
     */
    fun computeTargetEv(
        scene: SceneType,
        lighting: LightingAnalysis,
        subject: SubjectAnalysis,
        highlightProtection: HighlightProtectionLevel,
        shadowStrategy: LowLightStrategy,
        facePriority: FacePriorityMode,
        calibrationParams: CalibrationParameters?
    ): Float {
        var ev = 0.0f

        // 1. Base EV from Scene Classification
        when (scene) {
            SceneType.BEACH -> ev += 0.33f
            SceneType.SUNSET -> ev -= 0.50f
            SceneType.NIGHT -> ev -= 0.33f
            SceneType.LOW_LIGHT -> ev += 0.16f
            SceneType.FOREST_NATURE -> ev += 0.0f
            SceneType.ARCHITECTURE -> ev += 0.0f
            SceneType.FOOD -> ev += 0.16f
            SceneType.DAYLIGHT -> ev += 0.0f
            SceneType.INDOOR -> ev += 0.0f
            SceneType.PORTRAIT -> ev += 0.16f
            SceneType.UNKNOWN -> ev += 0.0f
        }

        // 2. Highlight Protection Bias (preventing blown highlights)
        when (highlightProtection) {
            HighlightProtectionLevel.HIGH -> ev -= 0.50f
            HighlightProtectionLevel.MEDIUM -> ev -= 0.25f
            HighlightProtectionLevel.LOW -> ev -= 0.10f
            HighlightProtectionLevel.NONE -> {}
        }

        // 3. Face Priority Bias (compensating underexposed subject faces)
        when (facePriority) {
            FacePriorityMode.EXTREME_BACKLIGHT -> ev += 0.67f
            FacePriorityMode.PRIORITIZE_FACE -> ev += 0.40f
            FacePriorityMode.BALANCED -> ev += 0.10f
            FacePriorityMode.NONE -> {}
        }

        // 4. Low Light Strategy Bias (preventing high ISO sensor gain blowout in dark scenes)
        if (shadowStrategy == LowLightStrategy.LOW_NOISE && ev > 0.0f) {
            ev = 0.0f // cap at 0 to avoid noisy sensor gain in low-noise night modes
        }

        // 5. Calibrated Exposure Bias from session or device profile
        if (calibrationParams != null) {
            ev += calibrationParams.exposureBias
        }

        // Clamp to conservative safe bounds [-1.5f .. +1.5f]
        return ev.coerceIn(-1.50f, 1.50f)
    }

    /**
     * Applies temporal hysteresis so exposure compensation only steps when the difference is meaningful (> 0.25 EV).
     */
    private fun applyEvHysteresis(targetEv: Float, capabilities: CameraCapabilities): Float {
        val step = if (capabilities.evStep > 0.001f) capabilities.evStep else 0.33f
        val delta = abs(targetEv - lastCommittedEvOffset)

        // Hysteresis threshold: requires change greater than 0.25 EV to alter committed state
        return if (delta >= 0.25f || lastCommittedEvOffset == 0.0f) {
            val steppedEv = (targetEv / step).roundToInt() * step
            lastCommittedEvOffset = steppedEv
            steppedEv
        } else {
            lastCommittedEvOffset
        }
    }

    /**
     * Determines White Balance preset based on color temperature and scene lighting.
     */
    fun determineWhiteBalance(
        scene: SceneType,
        estimatedKelvin: Int,
        warmColorRatio: Float,
        coolBlueRatio: Float
    ): WhiteBalanceRecommendation {
        return when {
            scene == SceneType.SUNSET -> WhiteBalanceRecommendation.DAYLIGHT // Preserve warm sunset spectrum
            scene == SceneType.FOREST_NATURE -> WhiteBalanceRecommendation.CLOUDY // Warm up cool green shadows
            warmColorRatio > 0.38f || estimatedKelvin < 3300 -> WhiteBalanceRecommendation.TUNGSTEN_WARM
            coolBlueRatio > 0.38f || estimatedKelvin > 7000 -> WhiteBalanceRecommendation.SHADE
            estimatedKelvin in 3800..4600 -> WhiteBalanceRecommendation.FLUORESCENT
            scene == SceneType.DAYLIGHT || scene == SceneType.BEACH -> WhiteBalanceRecommendation.DAYLIGHT
            else -> WhiteBalanceRecommendation.AUTO
        }
    }

    /**
     * Dynamically chooses the most appropriate camera lens discovered on the device.
     */
    fun selectPreferredLens(
        scene: SceneType,
        subject: SubjectAnalysis,
        capabilities: CameraCapabilities
    ): CameraLensType {
        val lenses = capabilities.physicalLenses
        if (lenses.isEmpty()) {
            return capabilities.activeLensType
        }

        val hasUltraWide = lenses.any { it.lensType == CameraLensType.ULTRA_WIDE }
        val hasTelephoto = lenses.any { it.lensType == CameraLensType.TELEPHOTO }

        return when {
            // Architecture & Landscapes: Prefer Ultra-Wide when available
            (scene == SceneType.ARCHITECTURE || (scene == SceneType.DAYLIGHT && !subject.isPersonPresent)) && hasUltraWide -> {
                CameraLensType.ULTRA_WIDE
            }
            // Portrait with isolated subject: Prefer Telephoto when available for flattering perspective
            (scene == SceneType.PORTRAIT || subject.isLikelyPortrait) && hasTelephoto -> {
                CameraLensType.TELEPHOTO
            }
            else -> CameraLensType.MAIN_WIDE
        }
    }

    /**
     * Determines smart zoom recommendation with high-confidence hysteresis.
     */
    fun determineSmartZoom(
        scene: SceneType,
        subject: SubjectAnalysis,
        capabilities: CameraCapabilities
    ): Float {
        val maxZoom = capabilities.maxZoomRatio
        if (maxZoom <= 1.05f) return 1.0f

        var desiredZoom = 1.0f

        // If portrait detected with small face, gently recommend 1.3x - 1.5x framing
        if (subject.isLikelyPortrait && subject.approximateSubjectSize == "Small") {
            desiredZoom = 1.4f.coerceAtMost(maxZoom)
        } else if (scene == SceneType.FOOD) {
            desiredZoom = 1.2f.coerceAtMost(maxZoom)
        }

        // Hysteresis: only update committed zoom if delta exceeds 0.2x
        return if (abs(desiredZoom - lastCommittedZoom) >= 0.20f) {
            lastCommittedZoom = desiredZoom
            desiredZoom
        } else {
            lastCommittedZoom
        }
    }

    /**
     * Determines flash preference.
     */
    fun determineFlashPreference(
        scene: SceneType,
        lighting: LightingAnalysis,
        subject: SubjectAnalysis,
        capabilities: CameraCapabilities
    ): FlashRecommendation {
        if (!capabilities.isFlashSupported) return FlashRecommendation.OFF

        return when {
            // Fill torch for harsh backlit portraits
            subject.isPersonPresent && subject.primaryFaceExposureRelativeToScene < -25.0f && lighting.highlightClipping > 10.0f -> {
                FlashRecommendation.FILL_TORCH
            }
            // Dark portrait or low light subject
            subject.isPersonPresent && lighting.condition == LightingCondition.VERY_DARK -> {
                FlashRecommendation.ON
            }
            else -> FlashRecommendation.OFF
        }
    }

    /**
     * Maps scene and subject to image processing profile.
     */
    fun selectProcessingProfile(
        scene: SceneType,
        subject: SubjectAnalysis
    ): ImageProcessingProfileType {
        return when (scene) {
            SceneType.DAYLIGHT -> ImageProcessingProfileType.NATURAL
            SceneType.LOW_LIGHT -> ImageProcessingProfileType.LOW_LIGHT
            SceneType.NIGHT -> ImageProcessingProfileType.NIGHT
            SceneType.PORTRAIT -> ImageProcessingProfileType.PORTRAIT
            SceneType.FOREST_NATURE -> ImageProcessingProfileType.FOREST
            SceneType.BEACH -> ImageProcessingProfileType.BEACH
            SceneType.SUNSET -> ImageProcessingProfileType.SUNSET
            SceneType.FOOD -> ImageProcessingProfileType.FOOD
            SceneType.ARCHITECTURE -> ImageProcessingProfileType.ARCHITECTURE
            SceneType.INDOOR -> if (subject.isPersonPresent) ImageProcessingProfileType.PORTRAIT else ImageProcessingProfileType.NATURAL
            SceneType.UNKNOWN -> ImageProcessingProfileType.NATURAL
        }
    }

    private fun buildReasoning(
        scene: SceneType,
        highlightProtection: HighlightProtectionLevel,
        facePriority: FacePriorityMode,
        shadowStrategy: LowLightStrategy,
        motionStrategy: MotionStrategy,
        finalEv: Float
    ): String {
        val parts = mutableListOf<String>()
        if (facePriority == FacePriorityMode.PRIORITIZE_FACE || facePriority == FacePriorityMode.EXTREME_BACKLIGHT) {
            parts.add("Face priority exposure")
        }
        if (highlightProtection != HighlightProtectionLevel.NONE) {
            parts.add("Highlight protection (${highlightProtection.label})")
        }
        if (shadowStrategy == LowLightStrategy.LOW_NOISE) {
            parts.add("Low-noise shadow floor")
        }
        if (motionStrategy == MotionStrategy.HIGH_MOTION) {
            parts.add("Motion freeze priority")
        }
        if (parts.isEmpty()) {
            parts.add("Balanced ${scene.displayName} exposure")
        }
        return parts.joinToString(" • ") + " [EV ${String.format("%+.2f", finalEv)}]"
    }

    private fun recordMetrics(brightness: Float, highlightClipping: Float, motionScore: Float, scene: SceneType) {
        if (recentBrightness.size >= HISTORY_CAPACITY) recentBrightness.removeFirst()
        if (recentHighlightClips.size >= HISTORY_CAPACITY) recentHighlightClips.removeFirst()
        if (recentMotionScores.size >= HISTORY_CAPACITY) recentMotionScores.removeFirst()
        if (recentScenes.size >= HISTORY_CAPACITY) recentScenes.removeFirst()

        recentBrightness.addLast(brightness)
        recentHighlightClips.addLast(highlightClipping)
        recentMotionScores.addLast(motionScore)
        recentScenes.addLast(scene)
    }

    fun resetHistory() {
        recentBrightness.clear()
        recentEvOffsets.clear()
        recentHighlightClips.clear()
        recentMotionScores.clear()
        recentScenes.clear()
        lastCommittedEvOffset = 0.0f
        lastCommittedZoom = 1.0f
    }

    companion object {
        private const val HISTORY_CAPACITY = 5
    }
}
