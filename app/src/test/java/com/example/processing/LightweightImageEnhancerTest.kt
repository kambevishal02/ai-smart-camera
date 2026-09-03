package com.example.processing

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import com.example.ai.ImageTechnicalMetricCalculator
import com.example.model.DetectedFace
import com.example.model.EnhancementParameters
import com.example.model.ImageProcessingProfileType
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SubjectAnalysis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LightweightImageEnhancerTest {

    private val enhancer = LightweightImageEnhancer()
    private val metricCalculator = ImageTechnicalMetricCalculator()

    @Test
    fun `test output resolution strictly matches input resolution`() = runBlocking {
        val width = 640
        val height = 480
        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val params = EnhancementParameters.defaultForProfile(ImageProcessingProfileType.PORTRAIT)
        val result = enhancer.enhanceImage(source, ImageProcessingProfileType.PORTRAIT, params)

        assertEquals("Output width must match source width", width, result.width)
        assertEquals("Output height must match source height", height, result.height)
    }

    @Test
    fun `test enhanceImageWithDetails provides complete debug info and metrics`() = runBlocking {
        val width = 200
        val height = 200
        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Fill with center subject darker than background
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x in 50..150 && y in 50..150) {
                    source.setPixel(x, y, Color.rgb(80, 70, 60)) // Darker subject
                } else {
                    source.setPixel(x, y, Color.rgb(180, 180, 180)) // Bright background
                }
            }
        }

        val sceneAnalysis = SceneAnalysis(
            scene = SceneType.PORTRAIT,
            confidence = 0.9f,
            lighting = LightingAnalysis(condition = LightingCondition.NORMAL, brightness = 50f),
            subject = SubjectAnalysis(
                isPersonPresent = true,
                numberOfFaces = 1,
                detectedFaces = listOf(
                    DetectedFace(
                        bounds = RectF(0.25f, 0.25f, 0.75f, 0.75f),
                        confidence = 0.95f
                    )
                )
            ),
            motion = MotionAnalysis(motionLevel = MotionLevel.STILL)
        )

        val params = EnhancementParameters.defaultForProfile(ImageProcessingProfileType.PORTRAIT)
        val result = enhancer.enhanceImageWithDetails(
            source = source,
            profile = ImageProcessingProfileType.PORTRAIT,
            params = params,
            sceneAnalysis = sceneAnalysis
        )

        assertNotNull(result.bitmap)
        assertNotNull(result.debugInfo)
        assertNotNull(result.metricsOriginal)
        assertNotNull(result.metricsEnhanced)

        val debug = result.debugInfo!!
        assertEquals("Final resolution string matches", "${width}x${height}", debug.finalOutputResolution)
        assertTrue("Subject luminance should increase after enhancement", debug.enhancedSubjectLuminance >= debug.originalSubjectLuminance)
        assertTrue("Subject/BG ratio after should be higher or preserved", debug.subjectBackgroundRatioAfter >= debug.subjectBackgroundRatioBefore)
        assertTrue("Detection confidence should be high", debug.detectionConfidence >= 0.8f)
    }

    @Test
    fun `test objective metrics calculation returns valid values`() = runBlocking {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, Color.rgb(120, 120, 120))
            }
        }

        val metrics = metricCalculator.calculateObjectiveMetrics(bitmap, RectF(0.2f, 0.2f, 0.8f, 0.8f))
        assertNotNull(metrics)
        assertEquals("${width}x${height}", metrics.resolution)
        assertTrue(metrics.averageLuminance in 40.0..60.0)
        assertTrue(metrics.highlightClippingPct <= 1.0)
        assertTrue(metrics.shadowClippingPct <= 1.0)
    }

    @Test
    fun `test fallback heuristic when no face is present`() = runBlocking {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val sceneAnalysis = SceneAnalysis.INITIAL.copy(
            subject = SubjectAnalysis(isPersonPresent = false, numberOfFaces = 0)
        )

        val params = EnhancementParameters.defaultForProfile(ImageProcessingProfileType.NATURAL)
        val result = enhancer.enhanceImageWithDetails(
            source = bitmap,
            profile = ImageProcessingProfileType.NATURAL,
            params = params,
            sceneAnalysis = sceneAnalysis
        )

        assertNotNull(result.debugInfo)
        assertEquals("Balanced Scene Composition", result.debugInfo?.detectionEngine)
    }
}
