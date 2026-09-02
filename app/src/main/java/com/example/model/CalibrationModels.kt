package com.example.model

import android.os.Build

/**
 * Manual calibration parameters configured by developers to tune camera decision and image processing.
 * Changes to these values modify capture behavior without needing to rewrite core AI engine code.
 */
data class CalibrationParameters(
    val exposureBias: Float = 0.0f,              // -2.0 to +2.0 EV bias
    val highlightRecovery: Float = 1.0f,          // 0.0 to 2.0x multiplier
    val shadowRecovery: Float = 1.0f,             // 0.0 to 2.0x multiplier
    val contrast: Float = 1.0f,                   // 0.5 to 2.0x multiplier
    val saturation: Float = 1.0f,                 // 0.5 to 2.0x multiplier
    val sharpness: Float = 1.0f,                  // 0.0 to 2.0x multiplier
    val noiseReduction: Float = 1.0f,             // 0.0 to 2.0x multiplier
    val whiteBalanceBias: Float = 0.0f            // -1.0 (cool) to +1.0 (warm)
) {
    companion object {
        val DEFAULT = CalibrationParameters()
    }
}

/**
 * Device-specific calibration profile.
 * Identified by manufacturer + model + camera characteristics.
 * Adjusts safe hardware limits and processing multipliers while keeping the AI engine generic.
 */
data class DeviceCalibrationProfile(
    val profileId: String,
    val displayName: String,
    val description: String,
    val matchManufacturer: String? = null,
    val matchModelKeyword: String? = null,
    val maxSafeEvCompensationSteps: Int = 18,
    val processingStrengthMultiplier: Float = 1.0f,
    val sharpeningStrengthMultiplier: Float = 1.0f,
    val noiseReductionMultiplier: Float = 1.0f,
    val defaultParams: CalibrationParameters = CalibrationParameters.DEFAULT
)

/**
 * Catalog of calibration profiles and automated profile matching.
 */
object CalibrationProfilesRepository {

    val GENERIC_ANDROID = DeviceCalibrationProfile(
        profileId = "generic_android",
        displayName = "Generic Android (Baseline)",
        description = "Universal conservative calibration profile for all standard Android Camera2/CameraX devices.",
        matchManufacturer = "*",
        maxSafeEvCompensationSteps = 18,
        processingStrengthMultiplier = 1.0f,
        sharpeningStrengthMultiplier = 1.0f,
        noiseReductionMultiplier = 1.0f,
        defaultParams = CalibrationParameters()
    )

    val MODEL_A_FLAGSHIP = DeviceCalibrationProfile(
        profileId = "model_a_flagship",
        displayName = "Device Profile A (High Dynamic Sensor)",
        description = "Optimized for large-sensor devices with wide dynamic range and clean base ISO signal-to-noise ratio.",
        matchManufacturer = "Google,Samsung",
        maxSafeEvCompensationSteps = 24,
        processingStrengthMultiplier = 0.90f,
        sharpeningStrengthMultiplier = 0.85f,
        noiseReductionMultiplier = 0.75f,
        defaultParams = CalibrationParameters(
            exposureBias = 0.0f,
            highlightRecovery = 1.15f,
            shadowRecovery = 0.90f,
            contrast = 1.02f,
            saturation = 1.0f,
            sharpness = 0.90f,
            noiseReduction = 0.80f,
            whiteBalanceBias = 0.0f
        )
    )

    val MODEL_B_BUDGET = DeviceCalibrationProfile(
        profileId = "model_b_budget",
        displayName = "Device Profile B (Budget Sensor)",
        description = "Optimized for budget/entry sensors with higher shadow sensor noise and limited highlight headroom.",
        matchManufacturer = null,
        maxSafeEvCompensationSteps = 12,
        processingStrengthMultiplier = 1.10f,
        sharpeningStrengthMultiplier = 1.05f,
        noiseReductionMultiplier = 1.35f,
        defaultParams = CalibrationParameters(
            exposureBias = 0.0f,
            highlightRecovery = 1.25f,
            shadowRecovery = 0.85f,
            contrast = 0.96f,
            saturation = 1.0f,
            sharpness = 1.0f,
            noiseReduction = 1.30f,
            whiteBalanceBias = 0.02f
        )
    )

    val MODEL_C_ULTRAWIDE_NOISY = DeviceCalibrationProfile(
        profileId = "model_c_ultrawide",
        displayName = "Device Profile C (High Noise / Vignette)",
        description = "Optimized for secondary ultra-wide or high-noise auxiliary sensors requiring edge clarity and shadow lift.",
        matchManufacturer = null,
        maxSafeEvCompensationSteps = 12,
        processingStrengthMultiplier = 1.15f,
        sharpeningStrengthMultiplier = 1.20f,
        noiseReductionMultiplier = 1.25f,
        defaultParams = CalibrationParameters(
            exposureBias = 0.05f,
            highlightRecovery = 1.10f,
            shadowRecovery = 1.20f,
            contrast = 1.05f,
            saturation = 1.02f,
            sharpness = 1.15f,
            noiseReduction = 1.20f,
            whiteBalanceBias = 0.0f
        )
    )

    val ALL_PROFILES = listOf(
        GENERIC_ANDROID,
        MODEL_A_FLAGSHIP,
        MODEL_B_BUDGET,
        MODEL_C_ULTRAWIDE_NOISY
    )

    /**
     * Auto-detects the most suitable calibration profile using device manufacturer, model, and hardware level.
     * Note: strictly generic heuristic, does not hard-code vendor-specific assumptions.
     */
    fun autoDetectProfile(
        manufacturer: String = Build.MANUFACTURER,
        model: String = Build.MODEL,
        hardwareLevel: DeviceCapabilityLevel = DeviceCapabilityLevel.LIMITED
    ): DeviceCalibrationProfile {
        val mfgUpper = manufacturer.uppercase()
        val modelUpper = model.uppercase()

        // 1. High-tier devices with LEVEL_3 or FULL hardware support
        if (hardwareLevel == DeviceCapabilityLevel.FULL || hardwareLevel == DeviceCapabilityLevel.LEVEL_3) {
            return MODEL_A_FLAGSHIP
        }

        // 2. Legacy or limited devices
        if (hardwareLevel == DeviceCapabilityLevel.LEGACY) {
            return MODEL_B_BUDGET
        }

        // Default to Generic Android baseline
        return GENERIC_ANDROID
    }
}
