package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.model.EnhancementParameters
import com.example.model.ImageProcessingProfileType
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * High-performance, local image enhancement pipeline.
 * Combines fast ColorMatrix grading with non-linear tone curve mapping (LUT)
 * and adaptive sharpness / denoising filters.
 */
class LightweightImageEnhancer : IImageProcessor {

    override suspend fun enhanceImage(
        source: Bitmap,
        profile: ImageProcessingProfileType,
        params: EnhancementParameters
    ): Bitmap = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. Build Tone Curve Lookup Table (LUT) for Shadow Lift, Highlight Compression, Exposure & Contrast
        val toneLut = buildToneCurveLut(params)

        // 2. Apply Tone Curve + Color Grading via Bitmap pixel processing
        val width = source.width
        val height = source.height
        val enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Use Hardware Canvas + ColorMatrix for fast Color & Saturation & Tint pass
        val colorMatrix = buildColorMatrix(params)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        val canvas = Canvas(enhanced)
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 3. Pixel-level LUT Tone curve mapping & micro-contrast pass
        val pixels = IntArray(width * height)
        enhanced.getPixels(pixels, 0, width, 0, 0, width, height)

        val needsToneMapping = params.shadowLift > 0.01f || params.highlightCompression > 0.01f ||
                params.contrastMultiplier != 1.0f || params.exposureOffset != 0.0f

        if (needsToneMapping) {
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val a = (pixel ushr 24) and 0xFF
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF

                val newR = toneLut[r]
                val newG = toneLut[g]
                val newB = toneLut[b]

                pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        // 4. Sharpness unsharp masking / edge enhancement if requested
        if (params.sharpnessStrength > 0.05f) {
            applyAdaptiveSharpening(pixels, width, height, params.sharpnessStrength)
        }

        // 5. Denoising in dark regions for night/low light if requested
        if (params.noiseReductionStrength > 0.10f) {
            applySelectiveDenoise(pixels, width, height, params.noiseReductionStrength)
        }

        enhanced.setPixels(pixels, 0, width, 0, 0, width, height)

        val duration = System.currentTimeMillis() - startTime
        AppLogger.logImageProcessingCompleted(profile, duration, width, height)

        enhanced
    }

    /**
     * Builds a 256-element non-linear tone response curve lookup table.
     */
    private fun buildToneCurveLut(params: EnhancementParameters): IntArray {
        val lut = IntArray(256)
        val shadowLift = params.shadowLift
        val highlightComp = params.highlightCompression
        val contrast = params.contrastMultiplier
        val exposure = params.exposureOffset

        for (i in 0..255) {
            var norm = i / 255.0f

            // Exposure offset
            if (exposure != 0f) {
                norm = (norm * (1.0f + exposure)).coerceIn(0f, 1f)
            }

            // Shadow lift (smooth quadratic bell curve peaking in low-mids)
            if (shadowLift > 0f) {
                val shadowFactor = (1.0f - norm).pow(1.8f)
                norm += (shadowLift * 0.45f * shadowFactor)
            }

            // Highlight compression (smooth roll-off protecting top range)
            if (highlightComp > 0f) {
                val highlightFactor = norm.pow(2.0f)
                norm -= (highlightComp * 0.35f * highlightFactor)
            }

            // S-Curve Contrast adjustment around mid-tone 0.5
            if (contrast != 1.0f) {
                norm = (0.5f + (norm - 0.5f) * contrast).coerceIn(0f, 1f)
            }

            lut[i] = (norm * 255.0f).toInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Constructs a ColorMatrix for saturation, vibrance, and warm golden/cool tinting.
     */
    private fun buildColorMatrix(params: EnhancementParameters): ColorMatrix {
        val matrix = ColorMatrix()

        // Saturation matrix
        val sat = params.saturationMultiplier
        val satMatrix = ColorMatrix().apply { setSaturation(sat) }
        matrix.postConcat(satMatrix)

        // Warm Tint Matrix (Shifts Red and Amber up, Blue slightly down for Golden Hour / Portrait / Food)
        if (params.warmTint != 0f) {
            val warm = params.warmTint
            val rScale = 1.0f + (warm * 0.25f)
            val gScale = 1.0f + (warm * 0.10f)
            val bScale = 1.0f - (warm * 0.18f)

            val tintMatrix = ColorMatrix(
                floatArrayOf(
                    rScale, 0f, 0f, 0f, 0f,
                    0f, gScale, 0f, 0f, 0f,
                    0f, 0f, bScale, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            matrix.postConcat(tintMatrix)
        }

        return matrix
    }

    /**
     * Fast 3x3 unsharp mask / edge crisping filter.
     */
    private fun applyAdaptiveSharpening(
        pixels: IntArray,
        width: Int,
        height: Int,
        strength: Float
    ) {
        val copy = pixels.clone()
        val weight = strength.coerceIn(0f, 0.6f)
        val centerMult = 1.0f + (4.0f * weight)

        // Process interior pixels in steps
        for (y in 1 until height - 1) {
            val yOffset = y * width
            val yPrev = (y - 1) * width
            val yNext = (y + 1) * width

            for (x in 1 until width - 1) {
                val center = copy[yOffset + x]
                val top = copy[yPrev + x]
                val bottom = copy[yNext + x]
                val left = copy[yOffset + x - 1]
                val right = copy[yOffset + x + 1]

                val a = (center ushr 24) and 0xFF

                val rCenter = (center ushr 16) and 0xFF
                val rNeighbors = ((top ushr 16) and 0xFF) + ((bottom ushr 16) and 0xFF) +
                        ((left ushr 16) and 0xFF) + ((right ushr 16) and 0xFF)
                val newR = (rCenter * centerMult - rNeighbors * weight).toInt().coerceIn(0, 255)

                val gCenter = (center ushr 8) and 0xFF
                val gNeighbors = ((top ushr 8) and 0xFF) + ((bottom ushr 8) and 0xFF) +
                        ((left ushr 8) and 0xFF) + ((right ushr 8) and 0xFF)
                val newG = (gCenter * centerMult - gNeighbors * weight).toInt().coerceIn(0, 255)

                val bCenter = center and 0xFF
                val bNeighbors = (top and 0xFF) + (bottom and 0xFF) +
                        (left and 0xFF) + (right and 0xFF)
                val newB = (bCenter * centerMult - bNeighbors * weight).toInt().coerceIn(0, 255)

                pixels[yOffset + x] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }
    }

    /**
     * Selective dark-region smoothing filter for low light / night noise reduction.
     */
    private fun applySelectiveDenoise(
        pixels: IntArray,
        width: Int,
        height: Int,
        strength: Float
    ) {
        val copy = pixels.clone()
        val blend = strength.coerceIn(0f, 0.5f)

        for (y in 1 until height - 1) {
            val yOffset = y * width
            val yPrev = (y - 1) * width
            val yNext = (y + 1) * width

            for (x in 1 until width - 1) {
                val center = copy[yOffset + x]
                val r = (center ushr 16) and 0xFF
                val g = (center ushr 8) and 0xFF
                val b = center and 0xFF
                val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                // Only apply noise reduction in dark shadow regions (luma < 80)
                if (luma < 80) {
                    val a = (center ushr 24) and 0xFF
                    val top = copy[yPrev + x]
                    val bottom = copy[yNext + x]
                    val left = copy[yOffset + x - 1]
                    val right = copy[yOffset + x + 1]

                    val avgR = (r + ((top ushr 16) and 0xFF) + ((bottom ushr 16) and 0xFF) +
                            ((left ushr 16) and 0xFF) + ((right ushr 16) and 0xFF)) / 5
                    val avgG = (g + ((top ushr 8) and 0xFF) + ((bottom ushr 8) and 0xFF) +
                            ((left ushr 8) and 0xFF) + ((right ushr 8) and 0xFF)) / 5
                    val avgB = (b + (top and 0xFF) + (bottom and 0xFF) +
                            (left and 0xFF) + (right and 0xFF)) / 5

                    val finalR = (r * (1f - blend) + avgR * blend).toInt().coerceIn(0, 255)
                    val finalG = (g * (1f - blend) + avgG * blend).toInt().coerceIn(0, 255)
                    val finalB = (b * (1f - blend) + avgB * blend).toInt().coerceIn(0, 255)

                    pixels[yOffset + x] = (a shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                }
            }
        }
    }
}
