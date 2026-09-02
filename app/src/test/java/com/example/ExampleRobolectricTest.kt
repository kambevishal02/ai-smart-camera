package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.CameraDecisionEngine
import com.example.model.CameraCapabilities
import com.example.model.DeviceCapabilityLevel
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SubjectAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI Smart Camera", appName)
  }

  @Test
  fun `test camera decision engine evaluates portrait scene correctly`() {
    val engine = CameraDecisionEngine()
    val analysis = SceneAnalysis.INITIAL.copy(
      scene = SceneType.PORTRAIT,
      lighting = LightingAnalysis(condition = LightingCondition.NORMAL),
      subject = SubjectAnalysis(numberOfFaces = 1, isPersonPresent = true, isLikelyPortrait = true),
      confidence = 0.92f
    )
    val hardware = CameraCapabilities(
      activeCameraId = "0",
      hardwareLevel = DeviceCapabilityLevel.LIMITED,
      isEvCompensationSupported = true,
      evStep = 0.166667f,
      evRangeMin = -12,
      evRangeMax = 12
    )

    val recommendation = engine.evaluate(analysis, hardware)
    assertNotNull(recommendation)
    assertEquals("Portrait & Skin Tone Optimization", recommendation.primaryActionText)
  }
}
