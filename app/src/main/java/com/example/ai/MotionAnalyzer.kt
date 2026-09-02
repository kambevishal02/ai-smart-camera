package com.example.ai

import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import kotlin.math.abs
import kotlin.math.min

/**
 * MotionAnalyzer estimates frame-to-frame temporal difference on a downsampled grid
 * to gauge camera shake and subject motion.
 */
class MotionAnalyzer {

    private val gridW = 32
    private val gridH = 24
    private var previousGrid: ByteArray? = null

    fun reset() {
        previousGrid = null
    }

    /**
     * Analyzes temporal difference between current and previous frame luminance grids.
     */
    fun analyze(currentLumaGrid: ByteArray): MotionAnalysis {
        val prev = previousGrid
        if (prev == null || prev.size != currentLumaGrid.size) {
            previousGrid = currentLumaGrid.copyOf()
            return MotionAnalysis(motionScore = 0.05f, motionLevel = MotionLevel.STILL, isBlurRisk = false)
        }

        var totalDiff = 0
        val size = min(prev.size, currentLumaGrid.size)
        for (i in 0 until size) {
            val cur = currentLumaGrid[i].toInt() and 0xFF
            val old = prev[i].toInt() and 0xFF
            totalDiff += abs(cur - old)
        }

        previousGrid = currentLumaGrid.copyOf()

        val avgDiff = totalDiff.toFloat() / size
        val normalizedMotion = min(1.0f, avgDiff / 40.0f)

        val motionLevel = when {
            normalizedMotion < 0.08f -> MotionLevel.STILL
            normalizedMotion < 0.25f -> MotionLevel.LOW
            normalizedMotion < 0.55f -> MotionLevel.MODERATE
            else -> MotionLevel.HIGH
        }

        val isBlurRisk = normalizedMotion > 0.35f

        return MotionAnalysis(
            motionScore = normalizedMotion,
            motionLevel = motionLevel,
            isBlurRisk = isBlurRisk
        )
    }
}
