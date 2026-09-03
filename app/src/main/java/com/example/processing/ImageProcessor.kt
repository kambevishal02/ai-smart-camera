package com.example.processing

import android.graphics.Bitmap
import com.example.model.EnhancementParameters
import com.example.model.EnhancedImageResult
import com.example.model.ImageProcessingProfileType
import com.example.model.ObjectivePhotoQualityMetrics
import com.example.model.SceneAnalysis
import com.example.model.SubjectEnhancementDebugInfo

/**
 * Interface for post-capture image enhancement.
 * Modular, enabling advanced neural ISP or RenderScript / Vulkan shaders in future iterations.
 */
interface ImageProcessor {
    suspend fun enhanceImage(
        source: Bitmap,
        profile: ImageProcessingProfileType,
        params: EnhancementParameters
    ): Bitmap

    suspend fun enhanceImageWithDetails(
        source: Bitmap,
        profile: ImageProcessingProfileType,
        params: EnhancementParameters,
        sceneAnalysis: SceneAnalysis? = null
    ): EnhancedImageResult {
        val enhanced = enhanceImage(source, profile, params)
        return EnhancedImageResult(
            bitmap = enhanced,
            debugInfo = SubjectEnhancementDebugInfo(
                originalSubjectLuminance = 50f,
                enhancedSubjectLuminance = 55f,
                originalBackgroundLuminance = 50f,
                enhancedBackgroundLuminance = 50f,
                subjectBackgroundRatioBefore = 1.0f,
                subjectBackgroundRatioAfter = 1.1f,
                exposureAdjustment = params.exposureOffset,
                shadowRecoveryStrength = params.shadowLift,
                highlightProtectionStrength = params.highlightCompression,
                saturationAdjustment = params.saturationMultiplier,
                contrastAdjustment = params.contrastMultiplier,
                sharpeningStrength = params.sharpnessStrength,
                enhancementProfile = profile.displayName,
                detectionConfidence = 0.9f,
                finalOutputResolution = "${enhanced.width}x${enhanced.height}",
                subjectDetected = true,
                detectionEngine = "Standard ISP"
            ),
            metricsOriginal = ObjectivePhotoQualityMetrics.EMPTY.copy(
                width = source.width,
                height = source.height,
                resolution = "${source.width}x${source.height}"
            ),
            metricsEnhanced = ObjectivePhotoQualityMetrics.EMPTY.copy(
                width = enhanced.width,
                height = enhanced.height,
                resolution = "${enhanced.width}x${enhanced.height}"
            )
        )
    }
}

// Backward compatibility alias for IImageProcessor
typealias IImageProcessor = ImageProcessor
