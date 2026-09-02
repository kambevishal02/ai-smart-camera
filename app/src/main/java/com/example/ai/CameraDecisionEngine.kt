package com.example.ai

import com.example.model.CameraCapabilities
import com.example.model.CameraLensType
import com.example.model.CameraRecommendation
import com.example.model.EnhancementParameters
import com.example.model.FlashRecommendation
import com.example.model.FocusStrategy
import com.example.model.ImageProcessingProfileType
import com.example.model.IsoPreference
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.ShutterPreference
import com.example.model.SubjectAnalysis
import com.example.model.WhiteBalanceRecommendation
import kotlin.math.roundToInt

/**
 * Intelligent Smart Camera Decision Engine.
 *
 * Takes scene metrics, lighting conditions, subject composition, and hardware capabilities
 * to compute optimal camera control recommendations and post-capture image enhancement parameters.
 *
 * CRITICAL DIRECTIVE:
 * Strictly verifies CameraCapabilities before recommending manual controls.
 * Falls back to AUTO ISO, AUTO Shutter, and AUTO AWB whenever manual control is unsupported.
 */
class CameraDecisionEngine {

    /**
     * Evaluates full scene analysis against device camera hardware capabilities.
     */
    fun evaluate(
        analysis: SceneAnalysis,
        capabilities: CameraCapabilities
    ): CameraRecommendation {
        return evaluate(
            scene = analysis.scene,
            lighting = analysis.lighting,
            subject = analysis.subject,
            motion = analysis.motion,
            sharpness = analysis.sharpnessMetric,
            estimatedKelvin = analysis.estimatedKelvin,
            confidence = analysis.confidence,
            capabilities = capabilities
        )
    }

    /**
     * Overloaded evaluate taking modular components directly (enabling direct unit/simulation testing).
     */
    fun evaluate(
        scene: SceneType,
        lighting: LightingAnalysis,
        subject: SubjectAnalysis,
        motion: MotionAnalysis,
        sharpness: Float,
        estimatedKelvin: Int,
        confidence: Float,
        capabilities: CameraCapabilities
    ): CameraRecommendation {
        // 1. Determine Image Processing Profile based on Scene
        val profile = when (scene) {
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

        // 2. Exposure Compensation (EV) Calculation respecting hardware step and range
        var targetEvOffset = 0.0f
        when (scene) {
            SceneType.BEACH -> {
                // Protect bright sand/sea: slight positive EV offset to compensate for sensor metering underexposure
                targetEvOffset = 0.33f
            }
            SceneType.SUNSET -> {
                // Protect bright sky & preserve rich warm sunset hues: slight underexposure
                targetEvOffset = -0.50f
            }
            SceneType.PORTRAIT -> {
                // Prioritize face exposure in harsh/backlit conditions
                targetEvOffset = if (lighting.highlightClipping > 8.0f) 0.50f else 0.16f
            }
            SceneType.NIGHT -> {
                // Slight negative EV to prevent high-ISO sensor gain noise, rely on tone-curve shadow lift in post
                targetEvOffset = -0.33f
            }
            SceneType.LOW_LIGHT -> {
                targetEvOffset = 0.0f
            }
            SceneType.FOOD -> {
                // Bright, appetizing food exposure
                targetEvOffset = 0.33f
            }
            SceneType.FOREST_NATURE -> {
                // Balance sky highlights and deep foliage shadows
                targetEvOffset = if (lighting.highlightClipping > 12.0f) -0.33f else 0.0f
            }
            SceneType.ARCHITECTURE -> {
                targetEvOffset = if (lighting.highlightClipping > 15.0f) -0.33f else 0.0f
            }
            SceneType.DAYLIGHT -> {
                targetEvOffset = if (lighting.condition == LightingCondition.VERY_BRIGHT) -0.33f else 0.0f
            }
            SceneType.INDOOR -> {
                targetEvOffset = 0.0f
            }
            SceneType.UNKNOWN -> {
                targetEvOffset = 0.0f
            }
        }

        val step = if (capabilities.evStep > 0.001f) capabilities.evStep else 0.1666667f
        val calculatedEvIndex = if (capabilities.isEvCompensationSupported) {
            (targetEvOffset / step).roundToInt().coerceIn(capabilities.evRangeMin, capabilities.evRangeMax)
        } else {
            0
        }
        val clampedEv = calculatedEvIndex * step

        // 3. Focus Strategy
        val focusStrategy = when {
            subject.isPersonPresent || scene == SceneType.PORTRAIT -> FocusStrategy.FACE_PRIORITY
            scene == SceneType.FOOD -> FocusStrategy.MACRO_CLOSE_UP
            scene == SceneType.FOREST_NATURE || scene == SceneType.ARCHITECTURE -> FocusStrategy.INFINITY_LANDSCAPE
            motion.motionLevel == MotionLevel.HIGH -> FocusStrategy.CONTINUOUS_TRACKING
            else -> FocusStrategy.AUTO
        }

        // 4. White Balance Recommendation (Strictly checking hardware support)
        val wbRecommendation = if (!capabilities.isManualWhiteBalanceSupported && !capabilities.supportedAwbModes.contains("AWB_DAYLIGHT")) {
            WhiteBalanceRecommendation.AUTO
        } else {
            when (scene) {
                SceneType.SUNSET -> WhiteBalanceRecommendation.DAYLIGHT // Preserves warm sunset hues without auto-cooling
                SceneType.FOREST_NATURE -> WhiteBalanceRecommendation.CLOUDY // Richer foliage warmth
                SceneType.BEACH -> WhiteBalanceRecommendation.DAYLIGHT
                SceneType.INDOOR -> if (estimatedKelvin < 3600) WhiteBalanceRecommendation.TUNGSTEN_WARM else WhiteBalanceRecommendation.AUTO
                else -> WhiteBalanceRecommendation.AUTO
            }
        }

        // 5. Flash Recommendation
        val flashRec = when {
            !capabilities.isFlashSupported -> FlashRecommendation.OFF
            scene == SceneType.NIGHT && subject.isPersonPresent -> FlashRecommendation.FILL_TORCH
            scene == SceneType.PORTRAIT && lighting.condition == LightingCondition.DARK -> FlashRecommendation.AUTO
            lighting.condition == LightingCondition.VERY_DARK && !subject.isPersonPresent -> FlashRecommendation.OFF // Flash destroys distant night landscapes
            else -> FlashRecommendation.OFF
        }

        // 6. ISO and Shutter Recommendations (Checks hardware support, provides fallback)
        val (isoRec, shutterRec, isoPref, shutterPref) = calculateIsoAndShutter(scene, lighting, motion, capabilities)

        // 7. Recommended Lens & Zoom
        val (recommendedLensType, recommendedLensName, zoomRec) = when {
            capabilities.isFrontCamera -> Triple(CameraLensType.FRONT, "Front Selfie Camera", 1.0f)
            scene == SceneType.PORTRAIT -> Triple(CameraLensType.TELEPHOTO, "2x Telephoto / Main Lens", 2.0f.coerceAtMost(capabilities.maxZoomRatio))
            scene == SceneType.ARCHITECTURE || scene == SceneType.FOREST_NATURE -> Triple(CameraLensType.ULTRA_WIDE, "Ultra-Wide / Main Wide Lens", 1.0f)
            scene == SceneType.FOOD -> Triple(CameraLensType.MACRO, "Macro / Main Lens", 1.2f.coerceAtMost(capabilities.maxZoomRatio))
            else -> Triple(CameraLensType.MAIN_WIDE, "Main Wide Lens", 1.0f)
        }

        // 8. Human-Readable Action & Rationale Texts
        val (actionText, reasonText) = generateExplanations(scene, lighting, subject, motion)

        // 9. Fine-Tuned Enhancement Parameters according to Section 5 Profiles
        val baseEnhancement = EnhancementParameters.defaultForProfile(profile)
        val tunedEnhancement = tuneEnhancementParameters(baseEnhancement, scene, lighting, motion, sharpness)

        return CameraRecommendation(
            exposureCompensationIndex = calculatedEvIndex,
            exposureCompensationEv = clampedEv,
            focusStrategy = focusStrategy,
            flashRecommendation = flashRec,
            zoomRecommendation = zoomRec,
            whiteBalance = wbRecommendation,
            isoPreference = isoPref,
            shutterPreference = shutterPref,
            isoRecommendation = isoRec,
            shutterRecommendation = shutterRec,
            imageProcessingProfile = profile,
            enhancementParams = tunedEnhancement,
            recommendedLensType = recommendedLensType,
            recommendedLens = recommendedLensName,
            primaryActionText = actionText,
            secondaryReasonText = reasonText,
            confidence = confidence
        )
    }

    private fun calculateIsoAndShutter(
        scene: SceneType,
        lighting: LightingAnalysis,
        motion: MotionAnalysis,
        capabilities: CameraCapabilities
    ): IsoAndShutterResult {
        val isoPref = when (lighting.condition) {
            LightingCondition.VERY_BRIGHT, LightingCondition.BRIGHT -> IsoPreference.LOW_CLEAN
            LightingCondition.NORMAL -> IsoPreference.BALANCED
            LightingCondition.DARK -> IsoPreference.HIGH_SPEED
            LightingCondition.VERY_DARK -> IsoPreference.NIGHT_BOOST
        }

        val shutterPref = when {
            motion.motionLevel == MotionLevel.HIGH -> ShutterPreference.HIGH_SPEED_FREEZE
            motion.motionLevel == MotionLevel.MODERATE -> ShutterPreference.ACTION_PRIORITY
            scene == SceneType.NIGHT -> if (motion.motionLevel == MotionLevel.STILL) ShutterPreference.NIGHT_LONG else ShutterPreference.BALANCED
            scene == SceneType.BEACH || scene == SceneType.DAYLIGHT -> ShutterPreference.HIGH_SPEED_FREEZE
            else -> ShutterPreference.BALANCED
        }

        // ISO Advice String
        val isoAdvice = if (capabilities.isManualIsoSupported) {
            when (isoPref) {
                IsoPreference.LOW_CLEAN -> "ISO 50-100 (Clean Base)"
                IsoPreference.BALANCED -> "ISO 100-200"
                IsoPreference.HIGH_SPEED -> "ISO 800-1600"
                IsoPreference.NIGHT_BOOST -> "ISO 1600-3200 (Night Boost)"
                IsoPreference.AUTO -> "AUTO ISO"
            }
        } else {
            "AUTO ISO (${capabilities.isoRange})"
        }

        // Shutter Advice String
        val shutterAdvice = if (capabilities.isManualShutterSupported) {
            when (shutterPref) {
                ShutterPreference.HIGH_SPEED_FREEZE -> "1/500s - 1/1000s (Freeze Motion)"
                ShutterPreference.ACTION_PRIORITY -> "1/250s (Action Priority)"
                ShutterPreference.NIGHT_LONG -> "1/15s - 1/30s (Night Long)"
                ShutterPreference.BALANCED -> "1/60s - 1/125s (Balanced)"
                ShutterPreference.AUTO -> "AUTO Shutter"
            }
        } else {
            "AUTO Shutter (${capabilities.shutterRange})"
        }

        return IsoAndShutterResult(isoAdvice, shutterAdvice, isoPref, shutterPref)
    }

    private data class IsoAndShutterResult(
        val isoAdvice: String,
        val shutterAdvice: String,
        val isoPref: IsoPreference,
        val shutterPref: ShutterPreference
    )

    private fun tuneEnhancementParameters(
        base: EnhancementParameters,
        scene: SceneType,
        lighting: LightingAnalysis,
        motion: MotionAnalysis,
        sharpness: Float
    ): EnhancementParameters {
        var params = base

        when (scene) {
            SceneType.DAYLIGHT -> {
                // Low ISO, protect highlights, natural colour, moderate sharpening
                params = params.copy(
                    highlightCompression = (params.highlightCompression + lighting.highlightClipping / 100f).coerceIn(0.0f, 0.40f),
                    contrastMultiplier = 1.05f,
                    sharpnessStrength = 0.20f,
                    saturationMultiplier = 1.05f
                )
            }
            SceneType.LOW_LIGHT -> {
                // Avoid excessive exposure, longest practical shutter when motion is low, noise reduction, shadow recovery
                params = params.copy(
                    shadowLift = (0.30f + lighting.shadowLevel / 120f).coerceIn(0.20f, 0.55f),
                    noiseReductionStrength = 0.35f,
                    contrastMultiplier = 1.08f,
                    sharpnessStrength = 0.16f
                )
            }
            SceneType.NIGHT -> {
                // Prioritize exposure, night-friendly processing, avoid excessive sharpening, protect bright lights
                params = params.copy(
                    shadowLift = (0.40f + lighting.shadowLevel / 100f).coerceIn(0.30f, 0.65f),
                    highlightCompression = (0.35f + lighting.highlightClipping / 80f).coerceIn(0.25f, 0.60f),
                    sharpnessStrength = 0.15f, // Avoid excessive sharpening on night noise
                    noiseReductionStrength = 0.45f
                )
            }
            SceneType.PORTRAIT -> {
                // Prioritize face exposure, face-aware focus, natural skin tone, moderate background enhancement, avoid over-saturation
                params = params.copy(
                    shadowLift = 0.25f,
                    saturationMultiplier = 1.02f, // Natural, avoid over-saturation
                    warmTint = 0.10f, // Flattering natural skin tone
                    sharpnessStrength = 0.15f
                )
            }
            SceneType.FOREST_NATURE -> {
                // Protect green foliage from excessive saturation, recover shadows, preserve sky highlights, natural contrast
                params = params.copy(
                    shadowLift = (0.22f + lighting.shadowLevel / 150f).coerceIn(0.15f, 0.45f),
                    highlightCompression = (0.25f + lighting.highlightClipping / 100f).coerceIn(0.20f, 0.50f),
                    saturationMultiplier = 1.15f, // Vibrant but protected from over-saturation
                    contrastMultiplier = 1.12f
                )
            }
            SceneType.BEACH -> {
                // Protect bright highlights, reduce overexposure, natural blue/sky rendering, moderate contrast
                params = params.copy(
                    highlightCompression = (0.35f + lighting.highlightClipping / 80f).coerceIn(0.30f, 0.60f),
                    exposureOffset = -0.05f, // Reduce overexposure
                    contrastMultiplier = 1.10f,
                    saturationMultiplier = 1.20f
                )
            }
            SceneType.SUNSET -> {
                // Protect bright sky, preserve warm colours, recover foreground shadows, avoid destroying sunset colour
                params = params.copy(
                    highlightCompression = 0.35f, // Protect bright sky
                    shadowLift = 0.20f,          // Recover foreground shadows
                    warmTint = 0.35f,            // Preserve warm sunset colours
                    saturationMultiplier = 1.28f
                )
            }
            SceneType.FOOD -> {
                // Improve colour and local contrast, maintain natural food colours, moderate sharpness
                params = params.copy(
                    contrastMultiplier = 1.16f,
                    saturationMultiplier = 1.22f,
                    sharpnessStrength = 0.28f, // Moderate sharpness
                    warmTint = 0.15f
                )
            }
            SceneType.ARCHITECTURE -> {
                // Preserve straight lines, natural contrast, highlight protection, good overall sharpness
                params = params.copy(
                    highlightCompression = 0.28f,
                    contrastMultiplier = 1.15f,
                    sharpnessStrength = 0.35f, // Good overall sharpness
                    noiseReductionStrength = 0.10f
                )
            }
            SceneType.INDOOR, SceneType.UNKNOWN -> {
                params = base
            }
        }

        // If motion blur risk is high, slightly augment edge sharpness to counteract micro-blur
        if (motion.isBlurRisk) {
            params = params.copy(
                sharpnessStrength = (params.sharpnessStrength + 0.08f).coerceIn(0.0f, 0.50f)
            )
        }

        return params
    }

    private fun generateExplanations(
        scene: SceneType,
        lighting: LightingAnalysis,
        subject: SubjectAnalysis,
        motion: MotionAnalysis
    ): Pair<String, String> {
        return when (scene) {
            SceneType.DAYLIGHT -> Pair(
                "Natural Daylight Balance",
                "Clean base ISO & natural color balance"
            )
            SceneType.LOW_LIGHT -> Pair(
                "Shadow Recovery & Denoise",
                "Dim ambient lighting: adaptive tone curve & noise reduction"
            )
            SceneType.NIGHT -> Pair(
                "Night Scene Dynamic Range",
                "Lifting deep shadows & protecting bright light sources"
            )
            SceneType.PORTRAIT -> Pair(
                "Portrait & Skin Tone Optimization",
                if (subject.numberOfFaces > 1) "${subject.numberOfFaces} faces detected: group face priority focus & gentle fill"
                else "Face detected: face priority focus & soft skin tone mapping"
            )
            SceneType.FOREST_NATURE -> Pair(
                "Nature Foliage & Shadow Recovery",
                "Protecting greens from clipping & recovering forest shadows"
            )
            SceneType.BEACH -> Pair(
                "Highlight Protection & Blue Sky",
                "Bright sand & water: highlight roll-off & sky rendering"
            )
            SceneType.SUNSET -> Pair(
                "Preserving Golden Hour Warmth",
                "Sunset sky detected: protecting crimson/amber tones"
            )
            SceneType.FOOD -> Pair(
                "Food Texture & Color Enrichment",
                "Close-up food: macro focus sharpness & appetizing saturation"
            )
            SceneType.ARCHITECTURE -> Pair(
                "Geometric Line Clarity",
                "Linear perspective: clarity & edge enhancement"
            )
            SceneType.INDOOR -> Pair(
                "Indoor Color Balance",
                "Artificial lighting: warm white balance & tone smoothing"
            )
            SceneType.UNKNOWN -> Pair(
                "AI Auto Camera Optimization",
                "Balanced exposure and natural processing"
            )
        }
    }
}
