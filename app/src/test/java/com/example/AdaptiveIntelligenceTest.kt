package com.example

import com.example.ai.AdaptiveIntelligenceEngine
import com.example.ai.AdaptiveProfileStore
import com.example.ai.AdaptiveSimulationRunner
import com.example.ai.LearningEligibilityEvaluator
import com.example.ai.TechnicalQualityEvaluator
import com.example.model.AdaptiveCameraProfile
import com.example.model.AdaptiveParameterBounds
import com.example.model.AdaptiveSceneParameters
import com.example.model.DetailedTechnicalMetrics
import com.example.model.LearningRejectionReason
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SubjectAnalysis
import com.example.model.TestSceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdaptiveIntelligenceTest {

    @Before
    fun setUp() {
        AdaptiveProfileStore.resetToBaseline()
    }

    @Test
    fun `default adaptive profile has zero biases and baseline version`() {
        val profile = AdaptiveProfileStore.currentProfile.value
        assertEquals(1, profile.profileVersion)
        assertEquals(0.0f, profile.globalParameters.exposureBias, 0.001f)
        assertEquals(0.0f, profile.globalParameters.highlightProtectionBias, 0.001f)
        assertEquals(0.0f, profile.globalParameters.shadowRecoveryBias, 0.001f)
        assertTrue(profile.sceneParameters.isEmpty())
    }

    @Test
    fun `parameter bounds strictly clamp biases within safe operating limits`() {
        val excessiveParams = AdaptiveSceneParameters(
            exposureBias = 5.0f, // limit is +0.75f
            highlightProtectionBias = -3.0f, // limit is -1.0f
            shadowRecoveryBias = 10.0f, // limit is +1.0f
            sharpeningBias = -5.0f, // limit is -0.5f
            noiseReductionBias = 4.0f // limit is +0.5f
        )
        val clamped = excessiveParams.clamped()

        assertEquals(AdaptiveParameterBounds.MAX_EXPOSURE_BIAS, clamped.exposureBias, 0.001f)
        assertEquals(AdaptiveParameterBounds.MIN_HIGHLIGHT_PROTECTION_BIAS, clamped.highlightProtectionBias, 0.001f)
        assertEquals(AdaptiveParameterBounds.MAX_SHADOW_RECOVERY_BIAS, clamped.shadowRecoveryBias, 0.001f)
        assertEquals(AdaptiveParameterBounds.MIN_SHARPENING_BIAS, clamped.sharpeningBias, 0.001f)
        assertEquals(AdaptiveParameterBounds.MAX_NOISE_REDUCTION_BIAS, clamped.noiseReductionBias, 0.001f)
    }

    @Test
    fun `learning eligibility strictly rejects high motion`() {
        val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
            scene = TestSceneType.DAYLIGHT,
            motionScore = 0.65f // High motion > 0.40 threshold
        )
        val eligibility = LearningEligibilityEvaluator.evaluateEligibility(session, analysis)

        assertFalse(eligibility.isEligible)
        assertEquals(LearningRejectionReason.HIGH_MOTION_DURING_AB_CAPTURE, eligibility.rejectionReason)
    }

    @Test
    fun `learning eligibility strictly rejects low confidence scene analysis`() {
        val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
            scene = TestSceneType.SUNSET,
            confidence = 0.50f // Below 0.65 threshold
        )
        val eligibility = LearningEligibilityEvaluator.evaluateEligibility(session, analysis)

        assertFalse(eligibility.isEligible)
        assertEquals(LearningRejectionReason.LOW_SCENE_CONFIDENCE, eligibility.rejectionReason)
    }

    @Test
    fun `learning eligibility strictly rejects poor image quality`() {
        val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
            scene = TestSceneType.INDOOR,
            technicalScoreA = 12,
            technicalScoreB = 22 // Below 25 threshold
        )
        val eligibility = LearningEligibilityEvaluator.evaluateEligibility(session, analysis)

        assertFalse(eligibility.isEligible)
        assertEquals(LearningRejectionReason.POOR_IMAGE_QUALITY, eligibility.rejectionReason)
    }

    @Test
    fun `learning eligibility strictly rejects hardware fallback mismatch`() {
        val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
            scene = TestSceneType.PORTRAIT_DAYLIGHT,
            includeFallbackMismatch = true
        )
        val eligibility = LearningEligibilityEvaluator.evaluateEligibility(session, analysis)

        assertFalse(eligibility.isEligible)
        assertEquals(LearningRejectionReason.HARDWARE_FALLBACK_MISMATCH, eligibility.rejectionReason)
    }

    @Test
    fun `single capture sample does NOT alter parameters due to multi-sample threshold`() {
        val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
            scene = TestSceneType.SUNSET,
            smartLuma = 28.0f,
            smartShadowClip = 20.0f
        )

        // Process 1st session
        val result = AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        assertTrue(result.eligibleForLearning)
        assertFalse("1 sample must NOT update profile parameters", result.parameterUpdated)

        val profile = AdaptiveProfileStore.currentProfile.value
        val sunsetBias = profile.sceneParameters[TestSceneType.SUNSET]?.exposureBias ?: 0.0f
        assertEquals("Exposure bias must remain 0 after 1 sample", 0.0f, sunsetBias, 0.001f)
    }

    @Test
    fun `five consistent captures trigger conservative positive parameter update`() {
        // Run 5 consistent sunset sessions with heavy shadow loss
        repeat(5) {
            val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
                scene = TestSceneType.SUNSET,
                smartLuma = 28.0f,
                smartShadowClip = 22.0f
            )
            AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        }

        val updatedProfile = AdaptiveProfileStore.currentProfile.value
        val sunsetParams = updatedProfile.sceneParameters[TestSceneType.SUNSET]
        assertNotNull(sunsetParams)
        assertTrue("Exposure bias must be positive to recover shadow loss", sunsetParams!!.exposureBias > 0.0f)
        assertTrue("Shadow recovery bias must be positive", sunsetParams.shadowRecoveryBias > 0.0f)
        assertTrue("Bias must stay within safe bounds", sunsetParams.exposureBias <= AdaptiveParameterBounds.MAX_EXPOSURE_BIAS)
    }

    @Test
    fun `scene learning isolation ensures Daylight is unaffected by Sunset learning`() {
        repeat(5) {
            val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
                scene = TestSceneType.SUNSET,
                smartLuma = 28.0f,
                smartShadowClip = 22.0f
            )
            AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        }

        val profile = AdaptiveProfileStore.currentProfile.value
        val daylightParams = profile.sceneParameters[TestSceneType.DAYLIGHT]
        assertNull("Daylight parameters must not be modified when learning Sunset", daylightParams)
    }

    @Test
    fun `rollback restores previous profile state`() {
        // Step 1: Initial state (v1)
        val initialVersion = AdaptiveProfileStore.currentProfile.value.profileVersion

        // Step 2: Trigger learning to produce v2
        repeat(5) {
            val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
                scene = TestSceneType.SUNSET,
                smartLuma = 28.0f,
                smartShadowClip = 22.0f
            )
            AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        }

        val v2Profile = AdaptiveProfileStore.currentProfile.value
        assertTrue(v2Profile.profileVersion > initialVersion)

        // Step 3: Rollback
        val rollbackSuccess = AdaptiveProfileStore.rollbackLastChange()
        assertTrue(rollbackSuccess)

        val rolledBackProfile = AdaptiveProfileStore.currentProfile.value
        assertEquals(initialVersion, rolledBackProfile.profileVersion)
        assertEquals(0.0f, rolledBackProfile.sceneParameters[TestSceneType.SUNSET]?.exposureBias ?: 0.0f, 0.001f)
    }

    @Test
    fun `reset to baseline clears all learned parameters`() {
        repeat(5) {
            val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(
                scene = TestSceneType.SUNSET,
                smartLuma = 28.0f,
                smartShadowClip = 22.0f
            )
            AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)
        }

        AdaptiveProfileStore.resetToBaseline()
        val resetProfile = AdaptiveProfileStore.currentProfile.value
        assertEquals(1, resetProfile.profileVersion)
        assertTrue(resetProfile.sceneParameters.isEmpty())
        assertEquals(0.0f, resetProfile.globalParameters.exposureBias, 0.001f)
    }

    @Test
    fun `json and csv export produce structured human-readable content`() {
        val (session, analysis) = AdaptiveSimulationRunner.createSyntheticSession(TestSceneType.DAYLIGHT)
        AdaptiveIntelligenceEngine.processAbCaptureSession(session, analysis)

        val json = AdaptiveProfileStore.exportToJson()
        assertTrue(json.contains("\"profileVersion\""))
        assertTrue(json.contains("\"deviceIdentifier\""))

        val csv = AdaptiveProfileStore.exportToCsv()
        assertTrue(csv.contains("timestamp,scene,lighting,parameter,previousValue,correction,newValue,confidence,decision,rejectionReason"))
    }
}
