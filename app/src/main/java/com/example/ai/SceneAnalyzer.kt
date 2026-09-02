package com.example.ai

import androidx.camera.core.ImageProxy
import com.example.model.DetectedFace
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.MotionAnalysis
import com.example.model.PhotoQualityScore
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SubjectAnalysis
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pluggable interface for scene analysis.
 * Enables zero-dependency local analysis initially, with modular replacement
 * by an on-device Machine Learning model (e.g. TensorFlow Lite / MediaPipe / ML Kit) in the future.
 */
interface ISceneAnalyzer {
    fun analyzeFrame(imageProxy: ImageProxy): SceneAnalysis
    fun resetHistory()
}

/**
 * Modular SceneAnalyzer implementation.
 *
 * NOTE ON ML vs HEURISTICS:
 * This initial implementation extracts physical frame characteristics (chrominance distributions,
 * luminance histograms, directional edge gradients, skin clusters, and temporal diff grids)
 * using deterministic heuristics. It does NOT pretend that these heuristics are ML/AI.
 * It is structured with clean analyzer components (LightingAnalyzer, SubjectAnalyzer, MotionAnalyzer,
 * PhotoQualityAnalyzer) so that a local TFLite or MediaPipe vision model can replace the heuristic
 * classification stage without breaking the rest of the application.
 */
class SceneAnalyzer(
    private val lightingAnalyzer: LightingAnalyzer = LightingAnalyzer(),
    private val subjectAnalyzer: SubjectAnalyzer = SubjectAnalyzer(),
    private val motionAnalyzer: MotionAnalyzer = MotionAnalyzer(),
    private val photoQualityAnalyzer: PhotoQualityAnalyzer = PhotoQualityAnalyzer()
) : ISceneAnalyzer {

    // Grid dimension for temporal motion comparison
    private val motionGridW = 32
    private val motionGridH = 24

    override fun resetHistory() {
        motionAnalyzer.reset()
    }

    override fun analyzeFrame(imageProxy: ImageProxy): SceneAnalysis {
        val width = imageProxy.width
        val height = imageProxy.height
        val planes = imageProxy.planes

        if (planes.isEmpty() || width <= 0 || height <= 0) {
            return SceneAnalysis.INITIAL
        }

        // Subsampling step to keep analysis fast and lightweight on mid-range phones (< 8ms execution)
        val step = max(4, width / 80)
        val sampledW = width / step
        val sampledH = height / step
        val maxSamples = sampledW * sampledH
        val lumaSamples = IntArray(maxSamples)

        var sampleCount = 0
        var greenCount = 0
        var warmCount = 0
        var coolBlueCount = 0
        var skinCount = 0
        var skyPixelCount = 0

        // Edge gradient accumulators for sharpness & architecture
        var edgeSum = 0.0
        var edgeSquareSum = 0.0
        var verticalEdgeCount = 0
        var horizontalEdgeCount = 0

        // Face / Skin candidate bounding box tracking
        var minSkinX = Float.MAX_VALUE
        var maxSkinX = Float.MIN_VALUE
        var minSkinY = Float.MAX_VALUE
        var maxSkinY = Float.MIN_VALUE

        val yPlane = planes[0]
        val yBuffer = yPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val hasUv = planes.size >= 3
        val uBuffer = if (hasUv) planes[1].buffer else null
        val vBuffer = if (hasUv) planes[2].buffer else null
        val uvRowStride = if (hasUv) planes[1].rowStride else 0
        val uvPixelStride = if (hasUv) planes[1].pixelStride else 1

        val currentLumaArray = Array(sampledH) { IntArray(sampledW) }

        for (sy in 0 until sampledH) {
            val y = sy * step
            for (sx in 0 until sampledW) {
                val x = sx * step

                val yIndex = y * yRowStride + x * yPixelStride
                if (yIndex >= yBuffer.capacity()) continue

                val luma = yBuffer.get(yIndex).toInt() and 0xFF
                currentLumaArray[sy][sx] = luma
                lumaSamples[sampleCount] = luma
                sampleCount++

                // Extract approximate U and V chrominance if available
                var u = 128
                var v = 128
                if (hasUv && uBuffer != null && vBuffer != null) {
                    val uvX = x / 2
                    val uvY = y / 2
                    val uIndex = uvY * uvRowStride + uvX * uvPixelStride
                    val vIndex = uvY * planes[2].rowStride + uvX * planes[2].pixelStride
                    if (uIndex < uBuffer.capacity() && vIndex < vBuffer.capacity()) {
                        u = uBuffer.get(uIndex).toInt() and 0xFF
                        v = vBuffer.get(vIndex).toInt() and 0xFF
                    }
                }

                // YUV to approximate RGB for color ratios
                val r = min(255, max(0, (luma + 1.402 * (v - 128)).toInt()))
                val g = min(255, max(0, (luma - 0.344136 * (u - 128) - 0.714136 * (v - 128)).toInt()))
                val b = min(255, max(0, (luma + 1.772 * (u - 128)).toInt()))

                // Color metrics
                // 1. Green foliage detection (Green notably higher than Red and Blue)
                if (g > 70 && g > r * 1.18 && g > b * 1.15) {
                    greenCount++
                }

                // 2. Cool Blue / Cyan (Sky, ocean)
                if (b > 90 && b > r * 1.15 && (b > g * 0.95)) {
                    coolBlueCount++
                    // Check if in upper 35% of frame (Sky)
                    if (sy < sampledH * 0.35 && luma > 100) {
                        skyPixelCount++
                    }
                }

                // 3. Warm sunset/amber (High Red, moderate Green, low Blue)
                if (r > 110 && r > b * 1.35 && g > b * 1.05) {
                    warmCount++
                }

                // 4. Skin tone detection in YCbCr / RGB space
                if (u in 75..130 && v in 130..175 && r > g && g > b && luma in 50..225) {
                    skinCount++
                    val normX = x.toFloat() / width
                    val normY = y.toFloat() / height
                    minSkinX = min(minSkinX, normX)
                    maxSkinX = max(maxSkinX, normX)
                    minSkinY = min(minSkinY, normY)
                    maxSkinY = max(maxSkinY, normY)
                }
            }
        }

        val validSamples = max(1, sampleCount)

        // 1. Run LightingAnalyzer
        val lightingAnalysis = lightingAnalyzer.analyze(lumaSamples, validSamples)

        // 2. Run SubjectAnalyzer (Real ML Kit Face Detection + skin cluster fallback)
        val subjectAnalysis = subjectAnalyzer.analyzeWithFrame(
            imageProxy = imageProxy,
            skinPixelCount = skinCount,
            totalSampleCount = validSamples,
            minSkinX = minSkinX,
            maxSkinX = maxSkinX,
            minSkinY = minSkinY,
            maxSkinY = maxSkinY
        )

        // 3. Edge gradient variance for Sharpness and Architecture
        for (sy in 1 until sampledH - 1) {
            for (sx in 1 until sampledW - 1) {
                val dx = abs(currentLumaArray[sy][sx + 1] - currentLumaArray[sy][sx - 1])
                val dy = abs(currentLumaArray[sy + 1][sx] - currentLumaArray[sy - 1][sx])
                val grad = (dx + dy).toDouble()

                edgeSum += grad
                edgeSquareSum += grad * grad

                if (dx > 30 && dy < 15) verticalEdgeCount++
                if (dy > 30 && dx < 15) horizontalEdgeCount++
            }
        }

        val interiorPixels = max(1, (sampledH - 2) * (sampledW - 2))
        val meanGrad = edgeSum / interiorPixels
        val gradVariance = (edgeSquareSum / interiorPixels) - (meanGrad * meanGrad)
        val sharpnessMetric = min(100.0f, max(0.0f, sqrt(max(0.0, gradVariance)).toFloat() * 1.5f))

        val geometricEdges = verticalEdgeCount + horizontalEdgeCount
        val edgeDensity = geometricEdges.toFloat() / interiorPixels

        // 4. Run MotionAnalyzer
        val currentMotionGrid = ByteArray(motionGridW * motionGridH)
        val motionBlockW = width / motionGridW
        val motionBlockH = height / motionGridH

        for (gy in 0 until motionGridH) {
            for (gx in 0 until motionGridW) {
                val px = min(width - 1, gx * motionBlockW + motionBlockW / 2)
                val py = min(height - 1, gy * motionBlockH + motionBlockH / 2)
                val yIndex = py * yRowStride + px * yPixelStride
                val lumaVal = if (yIndex < yBuffer.capacity()) yBuffer.get(yIndex).toInt() and 0xFF else 128
                val gridIdx = gy * motionGridW + gx
                currentMotionGrid[gridIdx] = lumaVal.toByte()
            }
        }
        val motionAnalysis = motionAnalyzer.analyze(currentMotionGrid)

        // 5. Environmental & Color Ratios
        val greenRatio = greenCount.toFloat() / validSamples
        val warmRatio = warmCount.toFloat() / validSamples
        val coolBlueRatio = coolBlueCount.toFloat() / validSamples
        val skyRatio = skyPixelCount.toFloat() / max(1, (sampledW * (sampledH * 0.35)).toInt())
        val skyDetected = skyRatio > 0.25f

        // Estimated Kelvin temperature
        val estimatedKelvin = when {
            warmRatio > 0.45f -> 3200
            warmRatio > 0.25f -> 4500
            coolBlueRatio > 0.40f -> 7500
            else -> 5500
        }

        // 6. Classify Scene Category
        val (classifiedScene, confidence) = classifyScene(
            lighting = lightingAnalysis,
            subject = subjectAnalysis,
            greenRatio = greenRatio,
            warmRatio = warmRatio,
            coolBlueRatio = coolBlueRatio,
            skyRatio = skyRatio,
            edgeDensity = edgeDensity,
            sharpness = sharpnessMetric
        )

        // 7. Run PhotoQualityAnalyzer
        val photoQuality = photoQualityAnalyzer.calculate(
            lighting = lightingAnalysis,
            subject = subjectAnalysis,
            motion = motionAnalysis,
            sharpnessMetric = sharpnessMetric,
            scene = classifiedScene
        )

        return SceneAnalysis(
            scene = classifiedScene,
            confidence = confidence,
            lighting = lightingAnalysis,
            subject = subjectAnalysis,
            motion = motionAnalysis,
            photoQuality = photoQuality,
            sharpnessMetric = sharpnessMetric,
            estimatedKelvin = estimatedKelvin,
            greenVegetationRatio = greenRatio,
            warmColorRatio = warmRatio,
            coolBlueRatio = coolBlueRatio,
            edgeDensity = edgeDensity,
            skyDetected = skyDetected,
            dominantHue = if (warmRatio > 0.3f) 35f else if (greenRatio > 0.3f) 120f else 210f,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun classifyScene(
        lighting: LightingAnalysis,
        subject: SubjectAnalysis,
        greenRatio: Float,
        warmRatio: Float,
        coolBlueRatio: Float,
        skyRatio: Float,
        edgeDensity: Float,
        sharpness: Float
    ): Pair<SceneType, Float> {
        // 1. Extreme dark / Night condition takes precedence
        if (lighting.condition == LightingCondition.VERY_DARK) {
            return Pair(SceneType.NIGHT, 0.94f)
        }

        // 2. Human subject / Portrait
        if (subject.isLikelyPortrait || subject.numberOfFaces > 0) {
            return Pair(SceneType.PORTRAIT, 0.92f)
        }

        // 3. Sunset / Golden hour (Rich warm tones, moderate to low ambient)
        if (warmRatio > 0.30f && (lighting.brightness in 18.0f..65.0f)) {
            return Pair(SceneType.SUNSET, 0.89f)
        }

        // 4. Forest / Nature (High green ratio)
        if (greenRatio > 0.28f) {
            return Pair(SceneType.FOREST_NATURE, 0.91f)
        }

        // 5. Beach (Bright sky + turquoise/water tones + bright luminance)
        if (skyRatio > 0.28f && coolBlueRatio > 0.25f && lighting.brightness > 55.0f) {
            return Pair(SceneType.BEACH, 0.88f)
        }

        // 6. Food (Warm close-up tones, high center texture/sharpness, moderate warm ratio)
        if (warmRatio > 0.22f && sharpness > 35.0f && lighting.brightness in 30.0f..75.0f && edgeDensity < 0.15f) {
            return Pair(SceneType.FOOD, 0.84f)
        }

        // 7. Architecture (High geometric/straight line edge density)
        if (edgeDensity > 0.14f && sharpness > 40.0f) {
            return Pair(SceneType.ARCHITECTURE, 0.87f)
        }

        // 8. Low Light / Indoor / Daylight ambient fallbacks
        if (lighting.condition == LightingCondition.DARK) {
            return Pair(SceneType.LOW_LIGHT, 0.88f)
        }

        if (warmRatio > 0.20f && lighting.brightness < 55.0f) {
            return Pair(SceneType.INDOOR, 0.83f)
        }

        if (lighting.condition == LightingCondition.NORMAL || lighting.condition == LightingCondition.BRIGHT || lighting.condition == LightingCondition.VERY_BRIGHT) {
            return Pair(SceneType.DAYLIGHT, 0.86f)
        }

        return Pair(SceneType.UNKNOWN, 0.50f)
    }
}
