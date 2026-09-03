package com.example.model

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historical record of an adaptive learning evaluation or parameter update.
 * Stored locally on-device. Fully explainable and auditable.
 */
data class AdaptiveLearningRecord(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp)),
    val deviceProfile: String,
    val scene: TestSceneType,
    val lighting: LightingCondition,
    val lightingContext: LightingContextType,
    val motion: MotionLevel,
    val parameterName: String,
    val previousParameter: Float,
    val observedMetric: String,
    val calculatedCorrection: Float,
    val newParameter: Float,
    val confidence: Float,
    val sampleCount: Int,
    val learningDecision: String, // "ACCEPTED_UPDATE", "EVIDENCE_ACCUMULATED_NO_UPDATE", "REJECTED", "ROLLBACK", "RESET"
    val rejectionReason: LearningRejectionReason? = null,
    val explanation: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("timestamp", timestamp)
            put("formattedTime", formattedTime)
            put("deviceProfile", deviceProfile)
            put("scene", scene.name)
            put("lighting", lighting.name)
            put("lightingContext", lightingContext.name)
            put("motion", motion.name)
            put("parameterName", parameterName)
            put("previousParameter", previousParameter)
            put("observedMetric", observedMetric)
            put("calculatedCorrection", calculatedCorrection)
            put("newParameter", newParameter)
            put("confidence", confidence)
            put("sampleCount", sampleCount)
            put("learningDecision", learningDecision)
            rejectionReason?.let { put("rejectionReason", it.name) }
            put("explanation", explanation)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): AdaptiveLearningRecord {
            return AdaptiveLearningRecord(
                id = json.optString("id", java.util.UUID.randomUUID().toString().take(8)),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                formattedTime = json.optString("formattedTime", ""),
                deviceProfile = json.optString("deviceProfile", "Generic Android"),
                scene = try { TestSceneType.valueOf(json.optString("scene", "DAYLIGHT")) } catch (_: Exception) { TestSceneType.DAYLIGHT },
                lighting = try { LightingCondition.valueOf(json.optString("lighting", "NORMAL")) } catch (_: Exception) { LightingCondition.NORMAL },
                lightingContext = try { LightingContextType.valueOf(json.optString("lightingContext", "NORMAL")) } catch (_: Exception) { LightingContextType.NORMAL },
                motion = try { MotionLevel.valueOf(json.optString("motion", "STILL")) } catch (_: Exception) { MotionLevel.STILL },
                parameterName = json.optString("parameterName", "exposureBias"),
                previousParameter = json.optDouble("previousParameter", 0.0).toFloat(),
                observedMetric = json.optString("observedMetric", ""),
                calculatedCorrection = json.optDouble("calculatedCorrection", 0.0).toFloat(),
                newParameter = json.optDouble("newParameter", 0.0).toFloat(),
                confidence = json.optDouble("confidence", 0.0).toFloat(),
                sampleCount = json.optInt("sampleCount", 0),
                learningDecision = json.optString("learningDecision", "REJECTED"),
                rejectionReason = if (json.has("rejectionReason")) {
                    try { LearningRejectionReason.valueOf(json.getString("rejectionReason")) } catch (_: Exception) { null }
                } else null,
                explanation = json.optString("explanation", "")
            )
        }
    }
}
