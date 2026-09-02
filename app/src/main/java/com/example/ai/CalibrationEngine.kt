package com.example.ai

import com.example.model.CalibrationParameters
import com.example.model.CalibrationProfilesRepository
import com.example.model.DeviceCalibrationProfile
import com.example.model.DeviceCapabilityLevel
import com.example.model.EnhancementParameters
import com.example.model.ImageProcessingProfileType
import com.example.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CalibrationEngine: Manages developer parameter calibration and device-specific profiles.
 *
 * NOTE ON AI & MACHINE LEARNING SCOPE:
 * This engine does NOT automatically alter production parameters based on a single photo.
 * It provides a structured calibration system where developers review objective technical
 * metrics from A/B captures and manually tune parameters.
 */
object CalibrationEngine {

    private val _activeProfile = MutableStateFlow(CalibrationProfilesRepository.GENERIC_ANDROID)
    val activeProfile: StateFlow<DeviceCalibrationProfile> = _activeProfile.asStateFlow()

    private val _activeParameters = MutableStateFlow(CalibrationParameters.DEFAULT)
    val activeParameters: StateFlow<CalibrationParameters> = _activeParameters.asStateFlow()

    fun initializeWithDevice(
        manufacturer: String,
        model: String,
        hardwareLevel: DeviceCapabilityLevel
    ) {
        val detected = CalibrationProfilesRepository.autoDetectProfile(manufacturer, model, hardwareLevel)
        _activeProfile.value = detected
        _activeParameters.value = detected.defaultParams
        AppLogger.i("CalibrationEngine", "Initialized profile: ${detected.displayName} for $manufacturer $model ($hardwareLevel)")
    }

    fun selectProfile(profile: DeviceCalibrationProfile) {
        _activeProfile.value = profile
        _activeParameters.value = profile.defaultParams
        AppLogger.i("CalibrationEngine", "Switched profile to: ${profile.displayName}")
    }

    fun updateParameters(params: CalibrationParameters) {
        _activeParameters.value = params
        AppLogger.i("CalibrationEngine", "Updated calibration parameters: $params")
    }

    fun resetToProfileDefaults() {
        _activeParameters.value = _activeProfile.value.defaultParams
        AppLogger.i("CalibrationEngine", "Reset parameters to default for ${_activeProfile.value.displayName}")
    }

    /**
     * Applies calibrated developer parameters and active device profile multipliers to base enhancement settings.
     */
    fun applyCalibration(
        baseParams: EnhancementParameters,
        profileType: ImageProcessingProfileType
    ): EnhancementParameters {
        val profile = _activeProfile.value
        val cal = _activeParameters.value

        val calibratedExposure = (baseParams.exposureOffset + cal.exposureBias * 0.12f).coerceIn(-1.0f, 1.0f)
        val calibratedContrast = (baseParams.contrastMultiplier * cal.contrast).coerceIn(0.6f, 1.8f)
        val calibratedHighlights = (baseParams.highlightCompression * cal.highlightRecovery).coerceIn(0.0f, 1.0f)
        val calibratedShadows = (baseParams.shadowLift * cal.shadowRecovery).coerceIn(0.0f, 1.0f)
        val calibratedSaturation = (baseParams.saturationMultiplier * cal.saturation).coerceIn(0.5f, 1.8f)
        val calibratedSharpness = (baseParams.sharpnessStrength * cal.sharpness * profile.sharpeningStrengthMultiplier).coerceIn(0.0f, 1.0f)
        val calibratedDenoise = (baseParams.noiseReductionStrength * cal.noiseReduction * profile.noiseReductionMultiplier).coerceIn(0.0f, 1.0f)
        val calibratedTint = (baseParams.warmTint + cal.whiteBalanceBias * 0.15f).coerceIn(-0.5f, 0.5f)

        return baseParams.copy(
            exposureOffset = calibratedExposure,
            contrastMultiplier = calibratedContrast,
            highlightCompression = calibratedHighlights,
            shadowLift = calibratedShadows,
            saturationMultiplier = calibratedSaturation,
            sharpnessStrength = calibratedSharpness,
            noiseReductionStrength = calibratedDenoise,
            warmTint = calibratedTint
        )
    }

    fun getSummaryString(): String {
        val p = _activeProfile.value
        val c = _activeParameters.value
        return "${p.displayName} [ExpBias: ${String.format("%.2f", c.exposureBias)}, HL: ${String.format("%.2f", c.highlightRecovery)}x, Shd: ${String.format("%.2f", c.shadowRecovery)}x, Shp: ${String.format("%.2f", c.sharpness)}x, Dns: ${String.format("%.2f", c.noiseReduction)}x]"
    }
}
