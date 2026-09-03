package com.example.model

import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/**
 * Safe bounds for all adaptive camera parameters.
 * Strictly prevents runaway, unlimited, or destructive self-learning.
 */
object AdaptiveParameterBounds {
    const val MIN_EXPOSURE_BIAS = -1.0f
    const val MAX_EXPOSURE_BIAS = 1.0f

    const val MIN_HIGHLIGHT_PROTECTION_BIAS = -1.0f
    const val MAX_HIGHLIGHT_PROTECTION_BIAS = 1.0f

    const val MIN_SHADOW_RECOVERY_BIAS = -0.5f
    const val MAX_SHADOW_RECOVERY_BIAS = 0.5f

    const val MIN_FACE_EXPOSURE_BIAS = -0.5f
    const val MAX_FACE_EXPOSURE_BIAS = 0.5f

    const val MIN_LOW_LIGHT_BIAS = -0.5f
    const val MAX_LOW_LIGHT_BIAS = 0.5f

    const val MIN_MOTION_BIAS = -0.5f
    const val MAX_MOTION_BIAS = 0.5f

    const val MIN_WHITE_BALANCE_BIAS = -0.5f
    const val MAX_WHITE_BALANCE_BIAS = 0.5f

    const val MIN_SHARPENING_BIAS = -0.3f
    const val MAX_SHARPENING_BIAS = 0.3f

    const val MIN_NOISE_REDUCTION_BIAS = -0.3f
    const val MAX_NOISE_REDUCTION_BIAS = 0.3f

    const val MIN_PROCESSING_STRENGTH_BIAS = -0.3f
    const val MAX_PROCESSING_STRENGTH_BIAS = 0.3f
}

/**
 * Lighting context categories for lighting-specific calibration learning.
 */
enum class LightingContextType(val label: String) {
    BRIGHT("Bright Natural"),
    NORMAL("Standard Ambient"),
    LOW_LIGHT("Low Light"),
    EXTREME_LOW_LIGHT("Extreme Low Light"),
    BACKLIGHT("High Backlight"),
    HIGH_DYNAMIC_RANGE("High Dynamic Range");

    companion object {
        fun fromAnalysis(lighting: LightingAnalysis): LightingContextType {
            return when {
                lighting.contrast > 75.0f && lighting.highlightClipping > 10.0f -> HIGH_DYNAMIC_RANGE
                lighting.highlightClipping > 12.0f && lighting.darkness > 40.0f -> BACKLIGHT
                lighting.condition == LightingCondition.VERY_DARK -> EXTREME_LOW_LIGHT
                lighting.condition == LightingCondition.DARK -> LOW_LIGHT
                lighting.condition == LightingCondition.BRIGHT || lighting.condition == LightingCondition.VERY_BRIGHT -> BRIGHT
                else -> NORMAL
            }
        }
    }
}

/**
 * Parameter container holding bounded learned biases, evidence count, and confidence.
 */
typealias AdaptiveSceneParameters = AdaptiveParameters

data class AdaptiveParameters(
    val exposureBias: Float = 0.0f,
    val highlightProtectionBias: Float = 0.0f,
    val shadowRecoveryBias: Float = 0.0f,
    val faceExposureBias: Float = 0.0f,
    val lowLightBias: Float = 0.0f,
    val motionBias: Float = 0.0f,
    val whiteBalanceBias: Float = 0.0f,
    val sharpeningBias: Float = 0.0f,
    val noiseReductionBias: Float = 0.0f,
    val processingStrengthBias: Float = 0.0f,
    val sampleCount: Int = 0,
    val confidence: Float = 0.0f,
    val lastUpdatedTimestamp: Long = 0L
) {
    fun clampToBounds(): AdaptiveParameters {
        return copy(
            exposureBias = exposureBias.coerceIn(AdaptiveParameterBounds.MIN_EXPOSURE_BIAS, AdaptiveParameterBounds.MAX_EXPOSURE_BIAS),
            highlightProtectionBias = highlightProtectionBias.coerceIn(AdaptiveParameterBounds.MIN_HIGHLIGHT_PROTECTION_BIAS, AdaptiveParameterBounds.MAX_HIGHLIGHT_PROTECTION_BIAS),
            shadowRecoveryBias = shadowRecoveryBias.coerceIn(AdaptiveParameterBounds.MIN_SHADOW_RECOVERY_BIAS, AdaptiveParameterBounds.MAX_SHADOW_RECOVERY_BIAS),
            faceExposureBias = faceExposureBias.coerceIn(AdaptiveParameterBounds.MIN_FACE_EXPOSURE_BIAS, AdaptiveParameterBounds.MAX_FACE_EXPOSURE_BIAS),
            lowLightBias = lowLightBias.coerceIn(AdaptiveParameterBounds.MIN_LOW_LIGHT_BIAS, AdaptiveParameterBounds.MAX_LOW_LIGHT_BIAS),
            motionBias = motionBias.coerceIn(AdaptiveParameterBounds.MIN_MOTION_BIAS, AdaptiveParameterBounds.MAX_MOTION_BIAS),
            whiteBalanceBias = whiteBalanceBias.coerceIn(AdaptiveParameterBounds.MIN_WHITE_BALANCE_BIAS, AdaptiveParameterBounds.MAX_WHITE_BALANCE_BIAS),
            sharpeningBias = sharpeningBias.coerceIn(AdaptiveParameterBounds.MIN_SHARPENING_BIAS, AdaptiveParameterBounds.MAX_SHARPENING_BIAS),
            noiseReductionBias = noiseReductionBias.coerceIn(AdaptiveParameterBounds.MIN_NOISE_REDUCTION_BIAS, AdaptiveParameterBounds.MAX_NOISE_REDUCTION_BIAS),
            processingStrengthBias = processingStrengthBias.coerceIn(AdaptiveParameterBounds.MIN_PROCESSING_STRENGTH_BIAS, AdaptiveParameterBounds.MAX_PROCESSING_STRENGTH_BIAS),
            confidence = confidence.coerceIn(0.0f, 1.0f)
        )
    }

    fun clamped(): AdaptiveParameters = clampToBounds()

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("exposureBias", exposureBias)
            put("highlightProtectionBias", highlightProtectionBias)
            put("shadowRecoveryBias", shadowRecoveryBias)
            put("faceExposureBias", faceExposureBias)
            put("lowLightBias", lowLightBias)
            put("motionBias", motionBias)
            put("whiteBalanceBias", whiteBalanceBias)
            put("sharpeningBias", sharpeningBias)
            put("noiseReductionBias", noiseReductionBias)
            put("processingStrengthBias", processingStrengthBias)
            put("sampleCount", sampleCount)
            put("confidence", confidence)
            put("lastUpdatedTimestamp", lastUpdatedTimestamp)
        }
    }

    companion object {
        val DEFAULT = AdaptiveParameters()

        fun fromJsonObject(json: JSONObject): AdaptiveParameters {
            return AdaptiveParameters(
                exposureBias = json.optDouble("exposureBias", 0.0).toFloat(),
                highlightProtectionBias = json.optDouble("highlightProtectionBias", 0.0).toFloat(),
                shadowRecoveryBias = json.optDouble("shadowRecoveryBias", 0.0).toFloat(),
                faceExposureBias = json.optDouble("faceExposureBias", 0.0).toFloat(),
                lowLightBias = json.optDouble("lowLightBias", 0.0).toFloat(),
                motionBias = json.optDouble("motionBias", 0.0).toFloat(),
                whiteBalanceBias = json.optDouble("whiteBalanceBias", 0.0).toFloat(),
                sharpeningBias = json.optDouble("sharpeningBias", 0.0).toFloat(),
                noiseReductionBias = json.optDouble("noiseReductionBias", 0.0).toFloat(),
                processingStrengthBias = json.optDouble("processingStrengthBias", 0.0).toFloat(),
                sampleCount = json.optInt("sampleCount", 0),
                confidence = json.optDouble("confidence", 0.0).toFloat(),
                lastUpdatedTimestamp = json.optLong("lastUpdatedTimestamp", 0L)
            ).clampToBounds()
        }
    }
}

/**
 * Dynamically discovered device and camera hardware identity.
 * Strictly no hardcoding of specific device vendors or models.
 */
data class DeviceProfileIdentifier(
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val activeCameraId: String = "0",
    val lensFacing: String = "BACK",
    val hardwareLevel: String = "LIMITED",
    val evStep: Float = 0.166667f,
    val evRangeMin: Int = -12,
    val evRangeMax: Int = 12,
    val zoomRange: String = "1.0x - 8.0x"
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("manufacturer", manufacturer)
            put("model", model)
            put("activeCameraId", activeCameraId)
            put("lensFacing", lensFacing)
            put("hardwareLevel", hardwareLevel)
            put("evStep", evStep)
            put("evRangeMin", evRangeMin)
            put("evRangeMax", evRangeMax)
            put("zoomRange", zoomRange)
        }
    }

    companion object {
        fun fromCapabilities(caps: CameraCapabilities): DeviceProfileIdentifier {
            return DeviceProfileIdentifier(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                activeCameraId = caps.activeCameraId,
                lensFacing = if (caps.isFrontCamera) "FRONT" else "BACK",
                hardwareLevel = caps.hardwareLevel.name,
                evStep = caps.evStep,
                evRangeMin = caps.evRangeMin,
                evRangeMax = caps.evRangeMax,
                zoomRange = "${String.format("%.1f", caps.minZoomRatio)}x - ${String.format("%.1f", caps.maxZoomRatio)}x"
            )
        }

        fun fromJsonObject(json: JSONObject): DeviceProfileIdentifier {
            return DeviceProfileIdentifier(
                manufacturer = json.optString("manufacturer", Build.MANUFACTURER),
                model = json.optString("model", Build.MODEL),
                activeCameraId = json.optString("activeCameraId", "0"),
                lensFacing = json.optString("lensFacing", "BACK"),
                hardwareLevel = json.optString("hardwareLevel", "LIMITED"),
                evStep = json.optDouble("evStep", 0.166667).toFloat(),
                evRangeMin = json.optInt("evRangeMin", -12),
                evRangeMax = json.optInt("evRangeMax", 12),
                zoomRange = json.optString("zoomRange", "1.0x - 8.0x")
            )
        }
    }
}

/**
 * Adaptive Camera Profile.
 * Stores bounded learned parameter biases segmented by scene category and lighting context.
 */
data class AdaptiveCameraProfile(
    val profileId: String = "adaptive_${System.currentTimeMillis()}",
    val profileVersion: Int = 1,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val deviceIdentifier: DeviceProfileIdentifier = DeviceProfileIdentifier(),
    val globalParameters: AdaptiveParameters = AdaptiveParameters.DEFAULT,
    val sceneParameters: Map<TestSceneType, AdaptiveParameters> = emptyMap(),
    val lightingParameters: Map<LightingContextType, AdaptiveParameters> = emptyMap()
) {
    /**
     * Resolves effective adaptive parameters for a given scene and lighting context.
     * Prioritizes scene-specific parameters, blends lighting context where relevant,
     * and strictly clamps results to safe physical bounds.
     */
    fun resolveEffectiveParameters(
        scene: TestSceneType,
        lightingContext: LightingContextType
    ): AdaptiveParameters {
        val sceneParams = sceneParameters[scene] ?: AdaptiveParameters.DEFAULT
        val lightParams = lightingParameters[lightingContext] ?: AdaptiveParameters.DEFAULT

        // Weighted blend between scene-specific learning and lighting-context learning
        val combinedExposure = (sceneParams.exposureBias + lightParams.exposureBias * 0.4f + globalParameters.exposureBias * 0.2f)
            .coerceIn(AdaptiveParameterBounds.MIN_EXPOSURE_BIAS, AdaptiveParameterBounds.MAX_EXPOSURE_BIAS)

        val combinedHighlight = (sceneParams.highlightProtectionBias + lightParams.highlightProtectionBias * 0.5f)
            .coerceIn(AdaptiveParameterBounds.MIN_HIGHLIGHT_PROTECTION_BIAS, AdaptiveParameterBounds.MAX_HIGHLIGHT_PROTECTION_BIAS)

        val combinedShadow = (sceneParams.shadowRecoveryBias + lightParams.shadowRecoveryBias * 0.5f)
            .coerceIn(AdaptiveParameterBounds.MIN_SHADOW_RECOVERY_BIAS, AdaptiveParameterBounds.MAX_SHADOW_RECOVERY_BIAS)

        val combinedFace = (sceneParams.faceExposureBias + lightParams.faceExposureBias * 0.3f)
            .coerceIn(AdaptiveParameterBounds.MIN_FACE_EXPOSURE_BIAS, AdaptiveParameterBounds.MAX_FACE_EXPOSURE_BIAS)

        val combinedLowLight = (sceneParams.lowLightBias + lightParams.lowLightBias * 0.5f)
            .coerceIn(AdaptiveParameterBounds.MIN_LOW_LIGHT_BIAS, AdaptiveParameterBounds.MAX_LOW_LIGHT_BIAS)

        val combinedMotion = sceneParams.motionBias
            .coerceIn(AdaptiveParameterBounds.MIN_MOTION_BIAS, AdaptiveParameterBounds.MAX_MOTION_BIAS)

        val combinedWb = (sceneParams.whiteBalanceBias + lightParams.whiteBalanceBias * 0.3f)
            .coerceIn(AdaptiveParameterBounds.MIN_WHITE_BALANCE_BIAS, AdaptiveParameterBounds.MAX_WHITE_BALANCE_BIAS)

        val combinedSharpness = sceneParams.sharpeningBias
            .coerceIn(AdaptiveParameterBounds.MIN_SHARPENING_BIAS, AdaptiveParameterBounds.MAX_SHARPENING_BIAS)

        val combinedNoise = (sceneParams.noiseReductionBias + lightParams.noiseReductionBias * 0.4f)
            .coerceIn(AdaptiveParameterBounds.MIN_NOISE_REDUCTION_BIAS, AdaptiveParameterBounds.MAX_NOISE_REDUCTION_BIAS)

        val combinedProcessing = sceneParams.processingStrengthBias
            .coerceIn(AdaptiveParameterBounds.MIN_PROCESSING_STRENGTH_BIAS, AdaptiveParameterBounds.MAX_PROCESSING_STRENGTH_BIAS)

        val maxConfidence = maxOf(sceneParams.confidence, lightParams.confidence, globalParameters.confidence)
        val totalSamples = sceneParams.sampleCount + lightParams.sampleCount

        return AdaptiveParameters(
            exposureBias = combinedExposure,
            highlightProtectionBias = combinedHighlight,
            shadowRecoveryBias = combinedShadow,
            faceExposureBias = combinedFace,
            lowLightBias = combinedLowLight,
            motionBias = combinedMotion,
            whiteBalanceBias = combinedWb,
            sharpeningBias = combinedSharpness,
            noiseReductionBias = combinedNoise,
            processingStrengthBias = combinedProcessing,
            sampleCount = totalSamples,
            confidence = maxConfidence,
            lastUpdatedTimestamp = lastModifiedTimestamp
        )
    }

    fun toJsonObject(): JSONObject {
        val root = JSONObject()
        root.put("profileId", profileId)
        root.put("profileVersion", profileVersion)
        root.put("createdTimestamp", createdTimestamp)
        root.put("lastModifiedTimestamp", lastModifiedTimestamp)
        root.put("deviceIdentifier", deviceIdentifier.toJsonObject())
        root.put("globalParameters", globalParameters.toJsonObject())

        val scenesObj = JSONObject()
        sceneParameters.forEach { (scene, params) ->
            scenesObj.put(scene.name, params.toJsonObject())
        }
        root.put("sceneParameters", scenesObj)

        val lightingObj = JSONObject()
        lightingParameters.forEach { (light, params) ->
            lightingObj.put(light.name, params.toJsonObject())
        }
        root.put("lightingParameters", lightingObj)

        return root
    }

    companion object {
        fun createDefault(caps: CameraCapabilities): AdaptiveCameraProfile {
            return AdaptiveCameraProfile(
                profileId = "adaptive_profile_${caps.activeCameraId}",
                profileVersion = 1,
                createdTimestamp = System.currentTimeMillis(),
                lastModifiedTimestamp = System.currentTimeMillis(),
                deviceIdentifier = DeviceProfileIdentifier.fromCapabilities(caps),
                globalParameters = AdaptiveParameters.DEFAULT,
                sceneParameters = emptyMap(),
                lightingParameters = emptyMap()
            )
        }

        fun fromJsonObject(json: JSONObject): AdaptiveCameraProfile {
            val deviceId = if (json.has("deviceIdentifier")) {
                DeviceProfileIdentifier.fromJsonObject(json.getJSONObject("deviceIdentifier"))
            } else {
                DeviceProfileIdentifier()
            }

            val global = if (json.has("globalParameters")) {
                AdaptiveParameters.fromJsonObject(json.getJSONObject("globalParameters"))
            } else {
                AdaptiveParameters.DEFAULT
            }

            val sceneMap = mutableMapOf<TestSceneType, AdaptiveParameters>()
            if (json.has("sceneParameters")) {
                val scenesJson = json.getJSONObject("sceneParameters")
                scenesJson.keys().forEach { key ->
                    try {
                        val sceneType = TestSceneType.valueOf(key)
                        sceneMap[sceneType] = AdaptiveParameters.fromJsonObject(scenesJson.getJSONObject(key))
                    } catch (_: Exception) {}
                }
            }

            val lightMap = mutableMapOf<LightingContextType, AdaptiveParameters>()
            if (json.has("lightingParameters")) {
                val lightJson = json.getJSONObject("lightingParameters")
                lightJson.keys().forEach { key ->
                    try {
                        val lightType = LightingContextType.valueOf(key)
                        lightMap[lightType] = AdaptiveParameters.fromJsonObject(lightJson.getJSONObject(key))
                    } catch (_: Exception) {}
                }
            }

            return AdaptiveCameraProfile(
                profileId = json.optString("profileId", "adaptive_profile"),
                profileVersion = json.optInt("profileVersion", 1),
                createdTimestamp = json.optLong("createdTimestamp", System.currentTimeMillis()),
                lastModifiedTimestamp = json.optLong("lastModifiedTimestamp", System.currentTimeMillis()),
                deviceIdentifier = deviceId,
                globalParameters = global,
                sceneParameters = sceneMap,
                lightingParameters = lightMap
            )
        }
    }
}
