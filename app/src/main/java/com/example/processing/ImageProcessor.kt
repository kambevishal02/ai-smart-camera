package com.example.processing

import android.graphics.Bitmap
import com.example.model.EnhancementParameters
import com.example.model.ImageProcessingProfileType

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
}

// Backward compatibility alias for IImageProcessor
typealias IImageProcessor = ImageProcessor
