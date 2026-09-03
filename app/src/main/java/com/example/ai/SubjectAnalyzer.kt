package com.example.ai

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.example.model.DetectedFace
import com.example.model.SubjectAnalysis
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * SubjectAnalyzer integrates real offline Google ML Kit Face Detection for accurate face
 * and portrait composition tracking, complemented by skin-tone cluster heuristics as fallback.
 *
 * Runs 100% locally on device with zero cloud or network dependencies.
 */
class SubjectAnalyzer {

    // Fast mode for real-time camera viewfinder analysis (<15ms per frame)
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.12f)
        .build()

    private val faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)

    /**
     * Primary analysis method taking the live ImageProxy frame and executing real ML Kit Face Detection.
     * Computes face brightness, relative exposure, highlight clipping, and shadow levels.
     */
    @OptIn(ExperimentalGetImage::class)
    fun analyzeWithFrame(
        imageProxy: ImageProxy,
        skinPixelCount: Int,
        totalSampleCount: Int,
        minSkinX: Float,
        maxSkinX: Float,
        minSkinY: Float,
        maxSkinY: Float,
        sceneLumaArray: Array<IntArray>? = null,
        sceneMeanLuma: Float = 128f
    ): SubjectAnalysis {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val task = faceDetector.process(inputImage)
                // Synchronous await with short timeout to prevent blocking frame pipeline
                val mlKitFaces = Tasks.await(task, 250, TimeUnit.MILLISECONDS)

                if (mlKitFaces.isNotEmpty()) {
                    val frameWidth = imageProxy.width.toFloat()
                    val frameHeight = imageProxy.height.toFloat()

                    val detectedFaces = mlKitFaces.map { face ->
                        val box = face.boundingBox
                        val normBox = RectF(
                            (box.left / frameWidth).coerceIn(0f, 1f),
                            (box.top / frameHeight).coerceIn(0f, 1f),
                            (box.right / frameWidth).coerceIn(0f, 1f),
                            (box.bottom / frameHeight).coerceIn(0f, 1f)
                        )

                        // Calculate face exposure metrics from sampled luma grid
                        var faceBrightness = 50f
                        var faceClipping = 0f
                        var faceShadow = 0f
                        var faceExpRel = 0f

                        if (sceneLumaArray != null && sceneLumaArray.isNotEmpty()) {
                            val gridH = sceneLumaArray.size
                            val gridW = sceneLumaArray[0].size
                            val minGX = (normBox.left * gridW).toInt().coerceIn(0, gridW - 1)
                            val maxGX = (normBox.right * gridW).toInt().coerceIn(0, gridW - 1)
                            val minGY = (normBox.top * gridH).toInt().coerceIn(0, gridH - 1)
                            val maxGY = (normBox.bottom * gridH).toInt().coerceIn(0, gridH - 1)

                            var faceLumaSum = 0L
                            var faceSamples = 0
                            var faceHighClips = 0
                            var faceShadowClips = 0

                            for (gy in minGY..maxGY) {
                                for (gx in minGX..maxGX) {
                                    val l = sceneLumaArray[gy][gx]
                                    faceLumaSum += l
                                    faceSamples++
                                    if (l > 240) faceHighClips++
                                    if (l < 30) faceShadowClips++
                                }
                            }

                            if (faceSamples > 0) {
                                val meanFaceLuma = faceLumaSum.toFloat() / faceSamples
                                faceBrightness = (meanFaceLuma / 2.55f).coerceIn(0f, 100f)
                                faceClipping = (faceHighClips.toFloat() / faceSamples) * 100f
                                faceShadow = (faceShadowClips.toFloat() / faceSamples) * 100f
                                val sceneBrightness = (sceneMeanLuma / 2.55f).coerceIn(0f, 100f)
                                faceExpRel = faceBrightness - sceneBrightness
                            }
                        }

                        DetectedFace(
                            bounds = normBox,
                            confidence = 0.95f,
                            faceBrightness = faceBrightness,
                            faceExposureRelativeToScene = faceExpRel,
                            faceClipping = faceClipping,
                            faceShadowLevel = faceShadow
                        )
                    }

                    val maxFaceArea = detectedFaces.maxOfOrNull {
                        it.bounds.width() * it.bounds.height()
                    } ?: 0f

                    val subjectSize = when {
                        maxFaceArea > 0.15f -> "Large"
                        maxFaceArea > 0.05f -> "Medium"
                        else -> "Small"
                    }

                    val isPortrait = maxFaceArea > 0.04f || detectedFaces.size in 1..4

                    val skinRatio = if (totalSampleCount > 0) skinPixelCount.toFloat() / totalSampleCount else 0.05f

                    val primaryFace = detectedFaces.maxByOrNull { it.bounds.width() * it.bounds.height() }
                        ?: detectedFaces.first()

                    return SubjectAnalysis(
                        numberOfFaces = detectedFaces.size,
                        isPersonPresent = true,
                        approximateSubjectSize = subjectSize,
                        isLikelyPortrait = isPortrait,
                        detectedFaces = detectedFaces,
                        skinRatio = skinRatio,
                        primaryFaceBrightness = primaryFace.faceBrightness,
                        primaryFaceExposureRelativeToScene = primaryFace.faceExposureRelativeToScene,
                        primaryFaceClipping = primaryFace.faceClipping,
                        primaryFaceShadowLevel = primaryFace.faceShadowLevel
                    )
                }
            } catch (e: Exception) {
                // Graceful fallback to skin-cluster heuristic
            }
        }

        // Heuristic fallback if ML Kit returns no faces or frame has no mediaImage
        return analyze(
            skinPixelCount = skinPixelCount,
            totalSampleCount = totalSampleCount,
            minSkinX = minSkinX,
            maxSkinX = maxSkinX,
            minSkinY = minSkinY,
            maxSkinY = maxSkinY
        )
    }

    /**
     * Fallback heuristic method based on chromatic skin-tone pixel clustering.
     */
    fun analyze(
        skinPixelCount: Int,
        totalSampleCount: Int,
        minSkinX: Float,
        maxSkinX: Float,
        minSkinY: Float,
        maxSkinY: Float
    ): SubjectAnalysis {
        if (totalSampleCount <= 0) {
            return SubjectAnalysis.DEFAULT
        }

        val skinRatio = skinPixelCount.toFloat() / totalSampleCount
        val detectedFaces = mutableListOf<DetectedFace>()

        var isPersonPresent = false
        var isLikelyPortrait = false
        var approximateSubjectSize = "None"

        if (skinRatio > 0.035f && maxSkinX > minSkinX && maxSkinY > minSkinY) {
            val faceWidth = maxSkinX - minSkinX
            val faceHeight = maxSkinY - minSkinY
            val boundingAreaRatio = faceWidth * faceHeight

            if (faceWidth in 0.07f..0.85f && faceHeight in 0.07f..0.85f) {
                isPersonPresent = true

                val faceBox = RectF(
                    max(0.0f, minSkinX - 0.04f),
                    max(0.0f, minSkinY - 0.04f),
                    min(1.0f, maxSkinX + 0.04f),
                    min(1.0f, maxSkinY + 0.04f)
                )

                val confidence = min(0.96f, 0.60f + skinRatio * 2.5f)
                detectedFaces.add(DetectedFace(bounds = faceBox, confidence = confidence))

                approximateSubjectSize = when {
                    boundingAreaRatio > 0.20f -> "Large"
                    boundingAreaRatio > 0.06f -> "Medium"
                    else -> "Small"
                }

                isLikelyPortrait = skinRatio > 0.08f || boundingAreaRatio > 0.08f
            }
        }

        return SubjectAnalysis(
            numberOfFaces = detectedFaces.size,
            isPersonPresent = isPersonPresent,
            approximateSubjectSize = approximateSubjectSize,
            isLikelyPortrait = isLikelyPortrait,
            detectedFaces = detectedFaces,
            skinRatio = skinRatio
        )
    }
}

