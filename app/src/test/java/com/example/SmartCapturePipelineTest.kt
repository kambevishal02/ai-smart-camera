package com.example

import com.example.ai.CameraDecisionEngine
import com.example.model.CameraCapabilities
import com.example.model.CaptureMetadata
import com.example.model.DeveloperMetadataStore
import com.example.model.DeviceCapabilityLevel
import com.example.model.EnhancementParameters
import com.example.model.ImageProcessingProfileType
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import com.example.model.PhotoQualityScore
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SmartCaptureStatus
import com.example.model.SubjectAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmartCapturePipelineTest {

    private val engine = CameraDecisionEngine()
    private val genericHardware = CameraCapabilities(
        deviceName = "Android Generic Reference",
        activeCameraId = "0",
        lensFacingName = "Rear Camera",
        isFrontCamera = false,
        hardwareLevel = DeviceCapabilityLevel.LIMITED,
        evRangeMin = -24,
        evRangeMax = 24,
        evStep = 0.166667f,
        isEvCompensationSupported = true,
        isFlashSupported = true
    )

    @Test
    fun `test all 10 scene types map to valid processing profiles and recommendations`() {
        val testScenes = listOf(
            SceneType.DAYLIGHT to ImageProcessingProfileType.NATURAL,
            SceneType.PORTRAIT to ImageProcessingProfileType.PORTRAIT,
            SceneType.LOW_LIGHT to ImageProcessingProfileType.LOW_LIGHT,
            SceneType.NIGHT to ImageProcessingProfileType.NIGHT,
            SceneType.BEACH to ImageProcessingProfileType.BEACH,
            SceneType.SUNSET to ImageProcessingProfileType.SUNSET,
            SceneType.FOREST_NATURE to ImageProcessingProfileType.LANDSCAPE,
            SceneType.FOOD to ImageProcessingProfileType.FOOD,
            SceneType.ARCHITECTURE to ImageProcessingProfileType.ARCHITECTURE,
            SceneType.INDOOR to ImageProcessingProfileType.NATURAL
        )

        for ((scene, expectedProfile) in testScenes) {
            val analysis = SceneAnalysis.INITIAL.copy(
                scene = scene,
                confidence = 0.88f
            )
            val recommendation = engine.evaluate(analysis, genericHardware)
            assertNotNull(recommendation)
            assertEquals("Profile for scene $scene mismatch", expectedProfile, recommendation.imageProcessingProfile)
            assertTrue("Primary action text should not be blank", recommendation.primaryActionText.isNotBlank())
        }
    }

    @Test
    fun `test metadata recording in DeveloperMetadataStore`() {
        DeveloperMetadataStore.clear()
        val metadata = CaptureMetadata(
            device = "Pixel 8 Pro",
            cameraId = "0",
            captureMode = "SMART AUTO",
            scene = SceneType.FOREST_NATURE,
            sceneDetectionType = "HEURISTIC",
            lighting = LightingCondition.NORMAL,
            brightnessLuma = 52.0f,
            faceCount = 0,
            motionLevel = MotionLevel.STILL,
            recommendationSummary = "Lush Nature & Greenery Optimization",
            appliedSettings = mapOf("EV Compensation" to "+0.0 EV"),
            processingProfile = ImageProcessingProfileType.LANDSCAPE,
            qualityScore = 92,
            qualityBreakdown = mapOf("Exposure" to 90, "Sharpness" to 94)
        )

        DeveloperMetadataStore.record(metadata)
        val logs = DeveloperMetadataStore.metadataLogs.value
        assertEquals(1, logs.size)
        assertEquals("SMART AUTO", logs[0].captureMode)
        assertEquals(SceneType.FOREST_NATURE, logs[0].scene)
        assertEquals(92, logs[0].qualityScore)
    }

    @Test
    fun `test enhancement parameters defaults for conservative enhancement`() {
        val portraitParams = EnhancementParameters.defaultForProfile(ImageProcessingProfileType.PORTRAIT)
        assertTrue(portraitParams.skinTonePreservation > 0.5f)
        assertTrue(portraitParams.saturationBoost in 1.0f..1.1f) // never over-saturated

        val nightParams = EnhancementParameters.defaultForProfile(ImageProcessingProfileType.NIGHT)
        assertTrue(nightParams.shadowLift > 0.15f)
        assertTrue(nightParams.noiseReductionStrength > 0.15f)
    }

    @Test
    fun `test smart capture status labels`() {
        assertEquals("ANALYZING SCENE...", SmartCaptureStatus.ANALYZING.label)
        assertEquals("AI OPTIMIZED • READY", SmartCaptureStatus.READY.label)
        assertEquals("CAPTURING...", SmartCaptureStatus.CAPTURING.label)
        assertEquals("ENHANCING PHOTO...", SmartCaptureStatus.PROCESSING.label)
        assertEquals("PHOTO SAVED", SmartCaptureStatus.SAVED.label)
    }
}
