package com.example.ai

import com.example.model.DetailedTechnicalMetrics
import kotlin.math.roundToInt

/**
 * Technical Quality Evaluator (V0.5).
 * Computes an objective, multi-metric Technical Quality Score based on physical signal attributes.
 *
 * NOTE: This explicitly measures signal fidelity and exposure bounds.
 * It does NOT claim to measure or represent human artistic preference.
 */
data class TechnicalQualityWeights(
    val exposureWeight: Float = 0.25f,
    val highlightProtectionWeight: Float = 0.20f,
    val shadowPreservationWeight: Float = 0.15f,
    val sharpnessWeight: Float = 0.15f,
    val dynamicRangeWeight: Float = 0.15f,
    val colorAccuracyWeight: Float = 0.10f,
    val noisePenaltyWeight: Float = 0.10f
) {
    companion object {
        val DEFAULT = TechnicalQualityWeights()
    }
}

object TechnicalQualityEvaluator {

    var activeWeights: TechnicalQualityWeights = TechnicalQualityWeights.DEFAULT

    /**
     * Calculates the composite Technical Quality Score (0-100) from physical metrics.
     */
    fun evaluateTechnicalQualityScore(
        metrics: DetailedTechnicalMetrics,
        weights: TechnicalQualityWeights = activeWeights
    ): Float {
        // 1. Exposure component (0-100)
        val expScore = metrics.exposureScore.toFloat().coerceIn(0f, 100f)

        // 2. Highlight protection component: 100 - (highlightClipping * 5)
        val hlScore = (100f - (metrics.highlightClippingPct * 5.0f)).coerceIn(0f, 100f)

        // 3. Shadow preservation component: 100 - (shadowClipping * 4)
        val shdScore = (100f - (metrics.shadowClippingPct * 4.0f)).coerceIn(0f, 100f)

        // 4. Sharpness component (0-100)
        val sharpScore = metrics.sharpnessScore.toFloat().coerceIn(0f, 100f)

        // 5. Dynamic range component: normalized from 0-14 EV stops to 0-100
        val drScore = ((metrics.dynamicRangeStops / 12.0f) * 100f).coerceIn(0f, 100f)

        // 6. Color accuracy component: 100 - colorCastOffset
        val colorScore = (100f - (metrics.colorCastOffset * 2.0f)).coerceIn(0f, 100f)

        // 7. Noise penalty component: noiseEstimate (0-100)
        val noisePenalty = metrics.noiseEstimate.coerceIn(0f, 100f)

        val totalWeight = weights.exposureWeight +
                weights.highlightProtectionWeight +
                weights.shadowPreservationWeight +
                weights.sharpnessWeight +
                weights.dynamicRangeWeight +
                weights.colorAccuracyWeight

        val rawScore = (
                expScore * weights.exposureWeight +
                hlScore * weights.highlightProtectionWeight +
                shdScore * weights.shadowPreservationWeight +
                sharpScore * weights.sharpnessWeight +
                drScore * weights.dynamicRangeWeight +
                colorScore * weights.colorAccuracyWeight
                ) / totalWeight

        val penalizedScore = rawScore - (noisePenalty * weights.noisePenaltyWeight)
        return penalizedScore.coerceIn(0f, 100f)
    }

    fun computeImprovementDelta(
        autoMetrics: DetailedTechnicalMetrics,
        smartMetrics: DetailedTechnicalMetrics,
        weights: TechnicalQualityWeights = activeWeights
    ): Float {
        val scoreA = evaluateTechnicalQualityScore(autoMetrics, weights)
        val scoreB = evaluateTechnicalQualityScore(smartMetrics, weights)
        return scoreB - scoreA
    }
}
