package com.example.model

/**
 * Adaptive Learning Policy.
 * Defines mathematical limits, confidence thresholds, and damping factors
 * to guarantee that self-calibration is conservative, stable, and reversible.
 */
object AdaptiveLearningPolicy {

    /**
     * Maximum parameter shift allowed in a single learning update.
     * Prevents any sudden jumps or single-session calibration shocks.
     */
    const val MAX_CHANGE_PER_UPDATE_EV = 0.08f
    const val MAX_CHANGE_PER_UPDATE_MULTIPLIER = 0.05f

    /**
     * Maximum cumulative deviation allowed from baseline calibration.
     */
    const val MAX_TOTAL_EXPOSURE_DEVIATION_EV = 0.60f
    const val MAX_TOTAL_BIAS_DEVIATION = 0.40f

    /**
     * Minimum number of consistent, learning-eligible A/B test samples
     * before any parameter adjustment is applied.
     * 1 result: NO CHANGE
     * < 5 results: ACCUMULATE ONLY
     * 5-9 results: SMALL UPDATE
     * >= 10 results: STRONGER UPDATE
     */
    const val MIN_SAMPLE_COUNT_FOR_UPDATE = 5
    const val STRONG_SAMPLE_COUNT_THRESHOLD = 10

    /**
     * Minimum confidence required in scene classification and metric stability.
     */
    const val MIN_CONFIDENCE_FOR_UPDATE = 0.70f

    /**
     * Minimum technical score improvement (Photo B vs Photo A) required
     * to validate that SMART AUTO was performing effectively.
     */
    const val MIN_TECHNICAL_SCORE_IMPROVEMENT = 2

    /**
     * Cooldown in milliseconds between successive adaptive profile updates.
     */
    const val UPDATE_COOLDOWN_MS = 500L

    /**
     * Mathematical confidence-weighted update function:
     * newBias = (oldBias * oldConfidence + observedCorrection * newConfidence) / (oldConfidence + newConfidence)
     * Clamped strictly to MAX_CHANGE_PER_UPDATE.
     */
    fun computeWeightedUpdate(
        currentBias: Float,
        currentConfidence: Float,
        currentSamples: Int,
        observedCorrection: Float,
        newConfidence: Float,
        maxChangePerUpdate: Float = MAX_CHANGE_PER_UPDATE_EV,
        maxTotalDeviation: Float = MAX_TOTAL_EXPOSURE_DEVIATION_EV
    ): Pair<Float, Float> {
        // Enforce safe sample count: if fewer than MIN_SAMPLE_COUNT_FOR_UPDATE, no bias change
        if (currentSamples < MIN_SAMPLE_COUNT_FOR_UPDATE) {
            val updatedConf = ((currentConfidence * currentSamples) + newConfidence) / (currentSamples + 1).toFloat()
            return Pair(currentBias, updatedConf.coerceIn(0.0f, 1.0f))
        }

        // Apply progressive damping factor based on sample count
        val sampleDamping = when {
            currentSamples >= STRONG_SAMPLE_COUNT_THRESHOLD -> 1.0f
            currentSamples >= 5 -> 0.70f
            else -> 0.45f
        }

        val effectiveConfidence = newConfidence * sampleDamping
        val totalWeight = currentConfidence + effectiveConfidence
        if (totalWeight <= 0.0001f) {
            return Pair(currentBias, newConfidence)
        }

        val targetBias = (currentBias * currentConfidence + observedCorrection * effectiveConfidence) / totalWeight
        val desiredDelta = targetBias - currentBias
        val clampedDelta = desiredDelta.coerceIn(-maxChangePerUpdate, maxChangePerUpdate)
        val newBias = (currentBias + clampedDelta).coerceIn(-maxTotalDeviation, maxTotalDeviation)
        val updatedConfidence = (totalWeight / 2.0f).coerceIn(0.1f, 1.0f)

        return Pair(newBias, updatedConfidence)
    }
}
