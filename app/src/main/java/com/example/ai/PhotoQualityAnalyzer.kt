package com.example.ai

import com.example.model.LightingAnalysis
import com.example.model.MotionAnalysis
import com.example.model.PhotoQualityScore
import com.example.model.SceneType
import com.example.model.SubjectAnalysis
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * PhotoQualityAnalyzer computes an objective, technical image-quality score from 0-100.
 *
 * IMPORTANT NOTE ON SCOPE & CLAIMS:
 * This analyzer measures quantifiable physical attributes (sensor exposure distance from 18% gray,
 * gradient edge sharpness, motion blur stability, dynamic range clipping).
 * It is STRICTLY a technical image-quality metric and does not claim to measure artistic,
 * creative, or aesthetic quality.
 */
class PhotoQualityAnalyzer {

    fun calculate(
        lighting: LightingAnalysis,
        subject: SubjectAnalysis,
        motion: MotionAnalysis,
        sharpnessMetric: Float,
        scene: SceneType
    ): PhotoQualityScore {
        // 1. Exposure Score: optimal target is balanced midtones (~50% brightness)
        val lumaDist = abs(lighting.brightness - 50.0f)
        val exposureScore = max(10, (100.0f - (lumaDist * 1.6f)).toInt()).coerceIn(0, 100)

        // 2. Brightness Score: penalize extreme under/over exposure
        val brightnessScore = when {
            lighting.brightness < 15.0f -> max(15, (lighting.brightness * 3.0f).toInt())
            lighting.brightness > 85.0f -> max(20, (100.0f - (lighting.brightness - 85.0f) * 4.0f).toInt())
            else -> 90
        }.coerceIn(0, 100)

        // 3. Sharpness Score: based on gradient edge variance
        val sharpnessScore = min(100, max(20, (sharpnessMetric * 1.35f).toInt()))

        // 4. Stability Score: inverse of motion score
        val stabilityScore = max(10, (100.0f - (motion.motionScore * 90.0f)).toInt()).coerceIn(0, 100)

        // 5. Highlight & Shadow Clipping Penalties
        val highlightScore = max(10, (100.0f - lighting.highlightClipping * 3.0f).toInt()).coerceIn(0, 100)
        val shadowScore = max(10, (100.0f - lighting.shadowLevel * 2.5f).toInt()).coerceIn(0, 100)
        val dynamicRangeScore = ((highlightScore + shadowScore) / 2).coerceIn(0, 100)

        // Weighted aggregate total score
        val total = (
            exposureScore * 0.26f +
            sharpnessScore * 0.24f +
            stabilityScore * 0.22f +
            dynamicRangeScore * 0.18f +
            brightnessScore * 0.10f
        ).toInt().coerceIn(1, 99)

        val ratingLabel = when {
            total >= 90 -> "EXCELLENT"
            total >= 80 -> "GREAT"
            total >= 68 -> "GOOD"
            total >= 50 -> "FAIR"
            else -> "SUBOPTIMAL"
        }

        val tips = mutableListOf<String>()
        if (motion.isBlurRisk) tips.add("Hold device steady to prevent motion blur")
        if (lighting.brightness < 20.0f) tips.add("Low ambient light: keep still for exposure")
        if (lighting.highlightClipping > 10.0f) tips.add("Bright highlights detected: adjusting exposure compensation")
        if (subject.isPersonPresent) tips.add("Subject detected: face priority focus active")
        if (tips.isEmpty()) tips.add("Camera framing & exposure are optimal")

        return PhotoQualityScore(
            totalScore = total,
            exposureScore = exposureScore,
            brightnessScore = brightnessScore,
            sharpnessScore = sharpnessScore,
            stabilityScore = stabilityScore,
            highlightScore = highlightScore,
            shadowScore = shadowScore,
            dynamicRangeScore = dynamicRangeScore,
            ratingLabel = ratingLabel,
            tips = tips
        )
    }
}
