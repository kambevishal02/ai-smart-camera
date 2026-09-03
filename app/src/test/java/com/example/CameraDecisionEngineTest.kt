package com.example

import com.example.ai.CameraDecisionEngine
import com.example.model.CameraCapabilities
import com.example.model.DeviceCapabilityLevel
import com.example.model.FocusStrategy
import com.example.model.ImageProcessingProfileType
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SubjectAnalysis
import com.example.model.WhiteBalanceRecommendation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CameraDecisionEngineTest {

    private val engine = CameraDecisionEngine()
    private val genericHardware = CameraCapabilities(
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
    fun `sunset scene selects sunset profile and daylight white balance`() {
        val analysis = SceneAnalysis.INITIAL.copy(
            scene = SceneType.SUNSET,
            lighting = LightingAnalysis(condition = LightingCondition.NORMAL, brightness = 45f),
            warmColorRatio = 0.45f
        )

        val rec = engine.evaluate(analysis, genericHardware)
        assertEquals(ImageProcessingProfileType.SUNSET, rec.imageProcessingProfile)
        assertEquals(WhiteBalanceRecommendation.DAYLIGHT, rec.whiteBalance)
        assertTrue(rec.exposureCompensationEv <= 0f) // underexpose slightly to preserve sunset colors
    }

    @Test
    fun `forest nature scene selects forest profile and cloudy white balance`() {
        val analysis = SceneAnalysis.INITIAL.copy(
            scene = SceneType.FOREST_NATURE,
            greenVegetationRatio = 0.50f
        )

        val rec = engine.evaluate(analysis, genericHardware)
        assertEquals(ImageProcessingProfileType.FOREST, rec.imageProcessingProfile)
        assertEquals(WhiteBalanceRecommendation.CLOUDY, rec.whiteBalance)
    }

    @Test
    fun `night scene selects night profile and shadow lift`() {
        val analysis = SceneAnalysis.INITIAL.copy(
            scene = SceneType.NIGHT,
            lighting = LightingAnalysis(condition = LightingCondition.VERY_DARK, brightness = 15f, shadowLevel = 30f)
        )

        val rec = engine.evaluate(analysis, genericHardware)
        assertEquals(ImageProcessingProfileType.NIGHT, rec.imageProcessingProfile)
        assertTrue(rec.enhancementParams.shadowLift > 0.15f)
    }

    @Test
    fun `high motion triggers continuous tracking focus`() {
        val analysis = SceneAnalysis.INITIAL.copy(
            scene = SceneType.DAYLIGHT,
            motion = MotionAnalysis(motionScore = 0.6f, motionLevel = MotionLevel.HIGH, isBlurRisk = true)
        )

        val rec = engine.evaluate(analysis, genericHardware)
        assertEquals(FocusStrategy.CONTINUOUS_TRACKING, rec.focusStrategy)
    }
}
