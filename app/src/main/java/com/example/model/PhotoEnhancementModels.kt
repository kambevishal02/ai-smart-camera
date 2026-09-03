package com.example.model

import android.graphics.Bitmap

/**
 * Objective Photo Quality Metrics calculated directly from pixel analysis
 * for comparing BEFORE (Original Raw) vs AFTER (AI Enhanced).
 */
data class ObjectivePhotoQualityMetrics(
    val width: Int,
    val height: Int,
    val resolution: String = "${width}x${height}",
    val sharpnessLaplacianVar: Float,       // Sharpness / Laplacian variance
    val highlightClippingPct: Float,        // Highlight clipping % (Y >= 245)
    val shadowClippingPct: Float,           // Shadow clipping % (Y <= 15)
    val rmsContrast: Float,                 // RMS contrast (0-100)
    val averageLuminance: Float,            // Average luminance (0-100%)
    val subjectLuminance: Float,            // Subject luminance (0-100%)
    val backgroundLuminance: Float,         // Background luminance (0-100%)
    val subjectBackgroundLuminanceRatio: Float, // Subject/background luminance ratio
    val saturation: Float,                  // Saturation (0-100%)
    val noiseEstimate: Float                // Noise estimate (0-100)
) {
    companion object {
        val EMPTY = ObjectivePhotoQualityMetrics(
            width = 0,
            height = 0,
            sharpnessLaplacianVar = 0f,
            highlightClippingPct = 0f,
            shadowClippingPct = 0f,
            rmsContrast = 0f,
            averageLuminance = 0f,
            subjectLuminance = 0f,
            backgroundLuminance = 0f,
            subjectBackgroundLuminanceRatio = 1f,
            saturation = 0f,
            noiseEstimate = 0f
        )
    }
}

/**
 * Developer Debug Overlay metadata showing all subject-aware parameters,
 * luminance comparisons, and engine diagnostics.
 */
data class SubjectEnhancementDebugInfo(
    val originalSubjectLuminance: Float,
    val enhancedSubjectLuminance: Float,
    val originalBackgroundLuminance: Float,
    val enhancedBackgroundLuminance: Float,
    val subjectBackgroundRatioBefore: Float,
    val subjectBackgroundRatioAfter: Float,
    val exposureAdjustment: Float,
    val shadowRecoveryStrength: Float,
    val highlightProtectionStrength: Float,
    val saturationAdjustment: Float,
    val contrastAdjustment: Float,
    val sharpeningStrength: Float,
    val enhancementProfile: String,
    val detectionConfidence: Float,
    val finalOutputResolution: String,
    val subjectDetected: Boolean = true,
    val detectionEngine: String = "ML Kit Face Detection"
)

/**
 * Full result of subject-aware enhancement pipeline.
 */
data class EnhancedImageResult(
    val bitmap: Bitmap,
    val debugInfo: SubjectEnhancementDebugInfo,
    val metricsOriginal: ObjectivePhotoQualityMetrics,
    val metricsEnhanced: ObjectivePhotoQualityMetrics
)
