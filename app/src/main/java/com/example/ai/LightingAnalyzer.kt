package com.example.ai

import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * LightingAnalyzer analyzes luminance histograms, shadow/highlight clipping, and dynamic contrast.
 * Computes a numeric brightness value (0-100) and categorizes the environmental lighting.
 */
class LightingAnalyzer {

    /**
     * Analyzes subsampled luminance samples (0..255).
     */
    fun analyze(
        lumaSamples: IntArray,
        sampleCount: Int
    ): LightingAnalysis {
        if (sampleCount <= 0 || lumaSamples.isEmpty()) {
            return LightingAnalysis.DEFAULT
        }

        var totalLuma = 0L
        var highlightCount = 0
        var shadowCount = 0
        val count = min(sampleCount, lumaSamples.size)

        for (i in 0 until count) {
            val luma = lumaSamples[i]
            totalLuma += luma
            if (luma > 240) highlightCount++
            if (luma < 20) shadowCount++
        }

        val meanLuma = totalLuma.toFloat() / count
        // Map 0..255 mean luma to 0..100 numeric brightness
        val numericBrightness = (meanLuma / 2.55f).coerceIn(0.0f, 100.0f)
        val numericDarkness = (100.0f - numericBrightness).coerceIn(0.0f, 100.0f)

        val highlightPercent = (highlightCount.toFloat() / count) * 100.0f
        val shadowPercent = (shadowCount.toFloat() / count) * 100.0f

        // Calculate contrast standard deviation
        var varianceSum = 0.0
        for (i in 0 until count) {
            val diff = lumaSamples[i] - meanLuma
            varianceSum += diff * diff
        }
        val stdDev = sqrt(varianceSum / count)
        val contrastScore = (stdDev / 1.28f).toFloat().coerceIn(0.0f, 100.0f)

        // Classify approximate lighting condition
        val condition = when {
            numericBrightness < 15.0f -> LightingCondition.VERY_DARK
            numericBrightness < 35.0f -> LightingCondition.DARK
            numericBrightness < 70.0f -> LightingCondition.NORMAL
            numericBrightness < 88.0f -> LightingCondition.BRIGHT
            else -> LightingCondition.VERY_BRIGHT
        }

        return LightingAnalysis(
            brightness = numericBrightness,
            darkness = numericDarkness,
            contrast = contrastScore,
            highlightClipping = highlightPercent,
            shadowLevel = shadowPercent,
            condition = condition
        )
    }
}
