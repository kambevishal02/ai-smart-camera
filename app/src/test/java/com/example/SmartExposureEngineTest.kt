package com.example

import com.example.ai.CameraDecisionEngine
import com.example.ai.SmartExposureEngine
import com.example.camera.CameraHardwareAdapter
import com.example.model.CameraCapabilities
import com.example.model.DeviceCapabilityLevel
import com.example.model.FacePriorityMode
import com.example.model.HighlightProtectionLevel
import com.example.model.MotionStrategy
import com.example.model.SimulationScenario
import com.example.model.SimulationScenariosProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmartExposureEngineTest {

    private val engine = CameraDecisionEngine()
    private val smartExposure = SmartExposureEngine()

    private val mockHardwareWithEv = CameraCapabilities(
        activeCameraId = "0",
        deviceName = "Generic Test Device",
        hardwareLevel = DeviceCapabilityLevel.LIMITED,
        isEvCompensationSupported = true,
        evStep = 0.166667f,
        evRangeMin = -12,
        evRangeMax = 12
    )

    private val mockHardwareLegacyNoEv = CameraCapabilities(
        activeCameraId = "1",
        deviceName = "Legacy Low-End Device",
        hardwareLevel = DeviceCapabilityLevel.MINIMAL,
        isEvCompensationSupported = false,
        evStep = 0.0f,
        evRangeMin = 0,
        evRangeMax = 0
    )

    @Test
    fun `test all 15 simulation scenarios evaluate successfully`() {
        for (scenario in SimulationScenario.values()) {
            val analysis = SimulationScenariosProvider.getAnalysisForScenario(scenario)
            val recommendation = engine.evaluate(analysis, mockHardwareWithEv)

            assertNotNull("Recommendation should not be null for $scenario", recommendation)
            assertNotNull("CaptureIntent should not be null for $scenario", recommendation.captureIntent)
            assertTrue("Reasoning should be non-empty for $scenario", recommendation.captureIntent.reasoning.isNotEmpty())

            val resolved = CameraHardwareAdapter.resolveIntent(recommendation.captureIntent, mockHardwareWithEv)
            assertNotNull("Resolved settings should not be null for $scenario", resolved)
        }
    }

    @Test
    fun `test harsh backlight triggers highlight protection and face compensation`() {
        val analysis = SimulationScenariosProvider.getAnalysisForScenario(SimulationScenario.HARSH_BACKLIGHT)
        val recommendation = engine.evaluate(analysis, mockHardwareWithEv)
        val intent = recommendation.captureIntent

        assertTrue(
            "Highlight protection should be at least MEDIUM",
            intent.highlightProtection == HighlightProtectionLevel.MEDIUM || intent.highlightProtection == HighlightProtectionLevel.HIGH
        )
        assertTrue(
            "Face priority should be active",
            intent.facePriority == FacePriorityMode.PRIORITIZE_FACE || intent.facePriority == FacePriorityMode.EXTREME_BACKLIGHT
        )
    }

    @Test
    fun `test extreme night preserves dark floor to prevent noise amplification`() {
        val analysis = SimulationScenariosProvider.getAnalysisForScenario(SimulationScenario.EXTREME_NIGHT_STATIC)
        val recommendation = engine.evaluate(analysis, mockHardwareWithEv)
        val intent = recommendation.captureIntent

        assertEquals(MotionStrategy.STATIC, intent.motionPriority)
        assertEquals(com.example.model.LowLightStrategy.LOW_NOISE, intent.shadowPriority)
        assertTrue(
            "Reasoning should document low-noise shadow floor or night strategy",
            intent.reasoning.contains("shadow floor", ignoreCase = true) || intent.reasoning.contains("night", ignoreCase = true)
        )
    }

    @Test
    fun `test running child high motion triggers high motion shutter strategy`() {
        val analysis = SimulationScenariosProvider.getAnalysisForScenario(SimulationScenario.RUNNING_CHILD_HIGH_MOTION)
        val recommendation = engine.evaluate(analysis, mockHardwareWithEv)
        val intent = recommendation.captureIntent

        assertEquals(MotionStrategy.HIGH_MOTION, intent.motionPriority)
    }

    @Test
    fun `test hardware adapter gracefully falls back on devices without EV compensation`() {
        val analysis = SimulationScenariosProvider.getAnalysisForScenario(SimulationScenario.BLOWN_SKY_LANDSCAPE)
        val recommendation = engine.evaluate(analysis, mockHardwareLegacyNoEv)
        val intent = recommendation.captureIntent

        val resolved = CameraHardwareAdapter.resolveIntent(intent, mockHardwareLegacyNoEv)
        assertEquals("Applied EV index should fall back to 0 on legacy devices", 0, resolved.appliedEvIndex)
        assertEquals("Applied EV offset should fall back to 0.0f", 0.0f, resolved.appliedEvOffset, 0.001f)
        assertTrue("Fallback settings should document EV compensation limitation", resolved.toFallbackMap().containsKey("EV Compensation"))
    }
}
