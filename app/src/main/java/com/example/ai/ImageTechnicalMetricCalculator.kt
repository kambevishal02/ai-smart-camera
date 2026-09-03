package com.example.ai

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.model.DetailedTechnicalMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Objective Technical Metric Calculator.
 *
 * Direct pixel-level measurement engine for comparing Standard AUTO vs SMART AUTO captures.
 * Evaluates exposure, brightness, contrast, highlight clipping, shadow clipping, sharpness,
 * noise, dynamic range, color cast, and face exposure.
 *
 * NOTE: All calculated scores represent physical signal and sensor accuracy metrics.
 * They do NOT represent artistic or creative quality.
 */
class ImageTechnicalMetricCalculator {

    suspend fun calculateMetrics(
        bitmap: Bitmap,
        faceBoundingBox: RectF? = null
    ): DetailedTechnicalMetrics = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        // Downsample to max 640x480 for fast, consistent metric analysis
        val sampleScale = max(1, max(width / 640, height / 480))
        val sampleW = width / sampleScale
        val sampleH = height / sampleScale

        val pixels = IntArray(sampleW * sampleH)
        val scaledBitmap = if (sampleScale > 1) {
            Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
        } else {
            bitmap
        }
        scaledBitmap.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        val totalPixels = pixels.size
        var sumLuma = 0.0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0

        var highlightClippedCount = 0
        var shadowClippedCount = 0

        val histogram = IntArray(256)
        val lumaGrid = FloatArray(totalPixels)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF

            sumR += r
            sumG += g
            sumB += b

            // ITU-R BT.601 standard luma formula
            val y = (0.299f * r + 0.587f * g + 0.114f * b)
            lumaGrid[i] = y
            sumLuma += y

            val yInt = y.toInt().coerceIn(0, 255)
            histogram[yInt]++

            if (y >= 248f || (r >= 250 && g >= 250 && b >= 250)) {
                highlightClippedCount++
            }
            if (y <= 12f) {
                shadowClippedCount++
            }
        }

        val meanLuma = (sumLuma / totalPixels).toFloat()
        val meanBrightnessPct = (meanLuma / 255.0f) * 100.0f
        val meanR = (sumR / totalPixels).toFloat()
        val meanG = (sumG / totalPixels).toFloat()
        val meanB = (sumB / totalPixels).toFloat()

        // 1. RMS Contrast
        var sumSquaredDiff = 0.0
        for (i in lumaGrid.indices) {
            val diff = lumaGrid[i] - meanLuma
            sumSquaredDiff += (diff * diff)
        }
        val rmsContrast = sqrt(sumSquaredDiff / totalPixels).toFloat()
        val normalizedContrast = ((rmsContrast / 128.0f) * 100.0f).coerceIn(0.0f, 100.0f)

        // 2. Clipping Percentages
        val highlightClipPct = ((highlightClippedCount.toFloat() / totalPixels) * 100.0f).coerceIn(0.0f, 100.0f)
        val shadowClipPct = ((shadowClippedCount.toFloat() / totalPixels) * 100.0f).coerceIn(0.0f, 100.0f)

        // 3. Sharpness Score (Discrete Laplacian edge variance)
        var laplacianSum = 0.0
        var laplacianSqSum = 0.0
        var edgeCount = 0

        for (y in 1 until sampleH - 1) {
            val row = y * sampleW
            val rowPrev = (y - 1) * sampleW
            val rowNext = (y + 1) * sampleW

            for (x in 1 until sampleW - 1) {
                val center = lumaGrid[row + x]
                val top = lumaGrid[rowPrev + x]
                val bottom = lumaGrid[rowNext + x]
                val left = lumaGrid[row + x - 1]
                val right = lumaGrid[row + x + 1]

                // Standard 4-neighbor Laplacian operator: L = 4*center - (top + bottom + left + right)
                val lap = abs(4f * center - (top + bottom + left + right))
                laplacianSum += lap
                laplacianSqSum += (lap * lap)
                edgeCount++
            }
        }

        val laplacianMean = laplacianSum / edgeCount
        val laplacianVar = (laplacianSqSum / edgeCount) - (laplacianMean * laplacianMean)
        // Convert variance to 0-100 sharpness score
        val sharpnessScore = ((sqrt(max(0.0, laplacianVar)) * 4.2f).toInt()).coerceIn(10, 99)

        // 4. Noise Estimation (Standard deviation of high-frequency residuals in flat regions)
        var flatPatchVarSum = 0.0
        var flatPatchCount = 0
        val patchSize = 8

        for (y in 0 until (sampleH - patchSize) step patchSize) {
            for (x in 0 until (sampleW - patchSize) step patchSize) {
                var pSum = 0.0
                var pSqSum = 0.0
                for (py in 0 until patchSize) {
                    for (px in 0 until patchSize) {
                        val v = lumaGrid[(y + py) * sampleW + (x + px)]
                        pSum += v
                        pSqSum += (v * v)
                    }
                }
                val n = (patchSize * patchSize).toDouble()
                val pMean = pSum / n
                val pVar = (pSqSum / n) - (pMean * pMean)

                // Only consider low-variance patches as flat background
                if (pVar in 0.5..45.0) {
                    flatPatchVarSum += sqrt(max(0.0, pVar))
                    flatPatchCount++
                }
            }
        }
        val noiseEstimate = if (flatPatchCount > 0) {
            ((flatPatchVarSum / flatPatchCount) * 12.0f).toFloat().coerceIn(0.0f, 100.0f)
        } else {
            15.0f
        }

        // 5. Dynamic Range Estimation (Spread from 1st percentile to 99th percentile in EV stops)
        val p1Threshold = (totalPixels * 0.01).toInt()
        val p99Threshold = (totalPixels * 0.99).toInt()

        var cum = 0
        var lumaMinVal = 1
        var lumaMaxVal = 254
        for (i in 0..255) {
            cum += histogram[i]
            if (cum >= p1Threshold && lumaMinVal == 1) {
                lumaMinVal = max(1, i)
            }
            if (cum >= p99Threshold) {
                lumaMaxVal = min(255, i)
                break
            }
        }
        val ratio = (lumaMaxVal.toDouble() / lumaMinVal.toDouble()).coerceAtLeast(1.0)
        val dynamicRangeStops = (ln(ratio) / ln(2.0)).toFloat().coerceIn(1.0f, 13.5f)

        // 6. Color Cast Offset (Deviation from neutral gray balance)
        val colorCastOffset = ((abs(meanR - meanG) + abs(meanB - meanG)) / 2.55f).coerceIn(0.0f, 100.0f)

        // 7. Face Exposure (if face bounding box provided)
        var faceExposureLuma: Float? = null
        if (faceBoundingBox != null && !faceBoundingBox.isEmpty) {
            val fxStart = (faceBoundingBox.left * sampleW).toInt().coerceIn(0, sampleW - 1)
            val fxEnd = (faceBoundingBox.right * sampleW).toInt().coerceIn(fxStart + 1, sampleW)
            val fyStart = (faceBoundingBox.top * sampleH).toInt().coerceIn(0, sampleH - 1)
            val fyEnd = (faceBoundingBox.bottom * sampleH).toInt().coerceIn(fyStart + 1, sampleH)

            var faceLumaSum = 0.0
            var facePixelCount = 0
            for (fy in fyStart until fyEnd) {
                for (fx in fxStart until fxEnd) {
                    faceLumaSum += lumaGrid[fy * sampleW + fx]
                    facePixelCount++
                }
            }
            if (facePixelCount > 0) {
                faceExposureLuma = ((faceLumaSum / facePixelCount) / 255.0f * 100.0f).toFloat()
            }
        }

        // 8. Exposure Score (Target balanced 18% gray target ~48-52% brightness)
        val lumaDist = abs(meanBrightnessPct - 50.0f)
        val exposureScore = (100.0f - (lumaDist * 1.5f) - (highlightClipPct * 2.0f) - (shadowClipPct * 1.5f))
            .toInt()
            .coerceIn(10, 99)

        // 9. Total Objective Technical Score
        val highlightPenalty = highlightClipPct * 2.5f
        val shadowPenalty = shadowClipPct * 2.0f
        val noisePenalty = noiseEstimate * 0.25f
        val drBonus = (dynamicRangeStops / 12.0f) * 20.0f

        val totalScore = (
            exposureScore * 0.30f +
            sharpnessScore * 0.30f +
            (100.0f - highlightPenalty).coerceIn(0f, 100f) * 0.15f +
            (100.0f - shadowPenalty).coerceIn(0f, 100f) * 0.15f +
            drBonus +
            (100.0f - noisePenalty).coerceIn(0f, 100f) * 0.10f
        ).toInt().coerceIn(1, 99)

        val rating = when {
            totalScore >= 88 -> "OPTIMAL"
            totalScore >= 75 -> "GOOD"
            totalScore >= 60 -> "ACCEPTABLE"
            else -> "SUBOPTIMAL"
        }

        DetailedTechnicalMetrics(
            exposureScore = exposureScore,
            brightnessLuma = meanBrightnessPct,
            contrastRms = normalizedContrast,
            highlightClippingPct = highlightClipPct,
            shadowClippingPct = shadowClipPct,
            sharpnessScore = sharpnessScore,
            noiseEstimate = noiseEstimate,
            dynamicRangeStops = dynamicRangeStops,
            colorCastOffset = colorCastOffset,
            faceExposureLuma = faceExposureLuma,
            totalTechnicalScore = totalScore,
            ratingLabel = rating
        )
    }

    /**
     * Calculates the exact Objective Photo Quality Metrics required for
     * rigorous Before vs After photo comparison and developer diagnostics.
     */
    suspend fun calculateObjectiveMetrics(
        bitmap: Bitmap,
        subjectBounds: RectF? = null
    ): com.example.model.ObjectivePhotoQualityMetrics = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        val sampleScale = max(1, max(width / 640, height / 480))
        val sampleW = width / sampleScale
        val sampleH = height / sampleScale

        val pixels = IntArray(sampleW * sampleH)
        val scaledBitmap = if (sampleScale > 1) {
            Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
        } else {
            bitmap
        }
        scaledBitmap.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        val totalPixels = pixels.size
        var sumLuma = 0.0
        var sumSat = 0.0
        var highlightClipCount = 0
        var shadowClipCount = 0

        val lumaGrid = FloatArray(totalPixels)

        // Subject region calculation
        val hasSubject = subjectBounds != null && !subjectBounds.isEmpty
        val sLeft = if (hasSubject) (subjectBounds!!.left * sampleW).toInt().coerceIn(0, sampleW - 1) else (sampleW * 0.25f).toInt()
        val sRight = if (hasSubject) (subjectBounds!!.right * sampleW).toInt().coerceIn(sLeft + 1, sampleW) else (sampleW * 0.75f).toInt()
        val sTop = if (hasSubject) (subjectBounds!!.top * sampleH).toInt().coerceIn(0, sampleH - 1) else (sampleH * 0.20f).toInt()
        val sBottom = if (hasSubject) (subjectBounds!!.bottom * sampleH).toInt().coerceIn(sTop + 1, sampleH) else (sampleH * 0.80f).toInt()

        var sumSubjLuma = 0.0
        var subjPixelCount = 0
        var sumBgLuma = 0.0
        var bgPixelCount = 0

        for (y in 0 until sampleH) {
            val rowOffset = y * sampleW
            val inSubjY = y in sTop until sBottom
            for (x in 0 until sampleW) {
                val p = pixels[rowOffset + x]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF

                val luma = (0.299f * r + 0.587f * g + 0.114f * b)
                lumaGrid[rowOffset + x] = luma
                sumLuma += luma

                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val sat = if (maxC > 0) ((maxC - minC).toFloat() / maxC) * 100f else 0f
                sumSat += sat

                if (luma >= 245f || (r >= 248 && g >= 248 && b >= 248)) {
                    highlightClipCount++
                }
                if (luma <= 15f) {
                    shadowClipCount++
                }

                if (inSubjY && x in sLeft until sRight) {
                    sumSubjLuma += luma
                    subjPixelCount++
                } else {
                    sumBgLuma += luma
                    bgPixelCount++
                }
            }
        }

        val meanLuma = (sumLuma / totalPixels).toFloat()
        val avgLumaPct = (meanLuma / 255f) * 100f
        val avgSatPct = (sumSat / totalPixels).toFloat()

        // RMS Contrast
        var sumSqDiff = 0.0
        for (i in lumaGrid.indices) {
            val d = lumaGrid[i] - meanLuma
            sumSqDiff += (d * d)
        }
        val rmsContrast = ((sqrt(sumSqDiff / totalPixels) / 128.0) * 100.0).toFloat().coerceIn(0f, 100f)

        // Clipping %
        val highlightClipPct = ((highlightClipCount.toFloat() / totalPixels) * 100f).coerceIn(0f, 100f)
        val shadowClipPct = ((shadowClipCount.toFloat() / totalPixels) * 100f).coerceIn(0f, 100f)

        // Laplacian Variance Sharpness
        var lapSum = 0.0
        var lapSqSum = 0.0
        var edgeCount = 0
        for (y in 1 until sampleH - 1) {
            val rO = y * sampleW
            val rP = (y - 1) * sampleW
            val rN = (y + 1) * sampleW
            for (x in 1 until sampleW - 1) {
                val center = lumaGrid[rO + x]
                val lap = abs(4f * center - (lumaGrid[rP + x] + lumaGrid[rN + x] + lumaGrid[rO + x - 1] + lumaGrid[rO + x + 1]))
                lapSum += lap
                lapSqSum += (lap * lap)
                edgeCount++
            }
        }
        val lapMean = if (edgeCount > 0) lapSum / edgeCount else 0.0
        val lapVar = if (edgeCount > 0) (lapSqSum / edgeCount) - (lapMean * lapMean) else 0.0
        val sharpnessLaplacianVar = max(0.0, lapVar).toFloat()

        // Noise estimate
        var patchVarSum = 0.0
        var patchCount = 0
        val pSize = 8
        for (y in 0 until (sampleH - pSize) step pSize) {
            for (x in 0 until (sampleW - pSize) step pSize) {
                var pS = 0.0
                var pSqS = 0.0
                for (py in 0 until pSize) {
                    for (px in 0 until pSize) {
                        val v = lumaGrid[(y + py) * sampleW + (x + px)]
                        pS += v
                        pSqS += (v * v)
                    }
                }
                val n = (pSize * pSize).toDouble()
                val mean = pS / n
                val variance = (pSqS / n) - (mean * mean)
                if (variance in 0.5..40.0) {
                    patchVarSum += sqrt(max(0.0, variance))
                    patchCount++
                }
            }
        }
        val noiseEstimate = if (patchCount > 0) ((patchVarSum / patchCount) * 10.0f).toFloat().coerceIn(0f, 100f) else 12.0f

        // Subject vs Background Luminance
        val subjLuma = if (subjPixelCount > 0) ((sumSubjLuma / subjPixelCount) / 255.0 * 100.0).toFloat() else avgLumaPct
        val bgLuma = if (bgPixelCount > 0) ((sumBgLuma / bgPixelCount) / 255.0 * 100.0).toFloat() else avgLumaPct
        val subjBgRatio = (subjLuma / max(1.0f, bgLuma))

        com.example.model.ObjectivePhotoQualityMetrics(
            width = width,
            height = height,
            resolution = "${width}x${height}",
            sharpnessLaplacianVar = sharpnessLaplacianVar,
            highlightClippingPct = highlightClipPct,
            shadowClippingPct = shadowClipPct,
            rmsContrast = rmsContrast,
            averageLuminance = avgLumaPct,
            subjectLuminance = subjLuma,
            backgroundLuminance = bgLuma,
            subjectBackgroundLuminanceRatio = subjBgRatio,
            saturation = avgSatPct,
            noiseEstimate = noiseEstimate
        )
    }
}
