package com.example.camera

import com.example.model.CameraCapabilities
import com.example.model.CameraRecommendation
import com.example.model.CapabilityStatus
import com.example.model.DeviceCapabilityLevel
import com.example.model.FlashRecommendation
import com.example.model.FocusStrategy
import com.example.model.WhiteBalanceRecommendation
import kotlin.math.roundToInt

/**
 * CameraHardwareAdapter decouples the AI Decision Engine from physical Android Camera hardware.
 *
 * Architecture:
 * AI Decision Engine
 *        ↓
 * CameraRecommendation (Platform-Independent)
 *        ↓
 * CameraHardwareAdapter
 *        ↓
 * Device Capabilities & HAL
 *        ↓
 * Best Supported Implementation (with graceful fallbacks)
 */
object CameraHardwareAdapter {

    /**
     * Resolves the effective exposure compensation index based on device capabilities.
     * Falls back to 0 if EV compensation is not supported.
     */
    fun resolveExposureCompensation(
        recommendation: CameraRecommendation,
        capabilities: CameraCapabilities
    ): Int {
        if (!capabilities.isEvCompensationSupported || capabilities.evStep <= 0.001f) {
            return 0
        }
        val targetIndex = (recommendation.exposureCompensationEv / capabilities.evStep).roundToInt()
        return targetIndex.coerceIn(capabilities.evRangeMin, capabilities.evRangeMax)
    }

    /**
     * Resolves the effective zoom ratio clamped to the physical sensor's zoom range.
     */
    fun resolveZoomRatio(
        recommendation: CameraRecommendation,
        capabilities: CameraCapabilities
    ): Float {
        return recommendation.zoomRecommendation.coerceIn(
            capabilities.minZoomRatio,
            capabilities.maxZoomRatio
        )
    }

    /**
     * Resolves the effective flash mode. Falls back to OFF if the hardware lacks a flash unit.
     */
    fun resolveFlashMode(
        recommendation: CameraRecommendation,
        capabilities: CameraCapabilities
    ): FlashRecommendation {
        if (!capabilities.isFlashSupported) {
            return FlashRecommendation.OFF
        }
        return recommendation.flashRecommendation
    }

    /**
     * Resolves white balance setting. Falls back to AUTO WB if the recommended preset is not supported.
     */
    fun resolveWhiteBalance(
        recommendation: CameraRecommendation,
        capabilities: CameraCapabilities
    ): WhiteBalanceRecommendation {
        if (!capabilities.isManualWhiteBalanceSupported && capabilities.supportedAwbModes.isEmpty()) {
            return WhiteBalanceRecommendation.AUTO
        }
        val targetAwbString = when (recommendation.whiteBalance) {
            WhiteBalanceRecommendation.DAYLIGHT -> "AWB_DAYLIGHT"
            WhiteBalanceRecommendation.CLOUDY -> "AWB_CLOUDY"
            WhiteBalanceRecommendation.SHADE -> "AWB_SHADE"
            WhiteBalanceRecommendation.TUNGSTEN_WARM -> "AWB_INCANDESCENT"
            WhiteBalanceRecommendation.FLUORESCENT -> "AWB_FLUORESCENT"
            WhiteBalanceRecommendation.AUTO -> "AWB_AUTO"
        }
        return if (capabilities.supportedAwbModes.contains(targetAwbString) || recommendation.whiteBalance == WhiteBalanceRecommendation.AUTO) {
            recommendation.whiteBalance
        } else {
            WhiteBalanceRecommendation.AUTO
        }
    }

    /**
     * Resolves focus mode. Returns fallback autofocus if manual/macro mode is unsupported.
     */
    fun resolveFocusStrategy(
        recommendation: CameraRecommendation,
        capabilities: CameraCapabilities
    ): FocusStrategy {
        if (recommendation.focusStrategy == FocusStrategy.MACRO_CLOSE_UP && !capabilities.supportedFocusModes.contains("AF_MACRO")) {
            return FocusStrategy.AUTO
        }
        return recommendation.focusStrategy
    }

    /**
     * Formats an exhaustive audit report of hardware capabilities vs. AI recommendations.
     */
    fun getHardwareFeatureAudit(capabilities: CameraCapabilities): Map<String, CapabilityStatus> {
        return mapOf(
            "Camera Preview" to CapabilityStatus.SUPPORTED,
            "Photo Capture (JPEG)" to CapabilityStatus.SUPPORTED,
            "Image Analysis (YUV_420_888)" to CapabilityStatus.SUPPORTED,
            "On-Device Face Detection" to CapabilityStatus.SUPPORTED,
            "Exposure Compensation" to if (capabilities.isEvCompensationSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "Autofocus & Metering" to if (capabilities.supportedFocusModes.isNotEmpty()) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "Flash / Torch Unit" to if (capabilities.isFlashSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "Digital Zoom" to if (capabilities.maxZoomRatio > 1.0f) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "Manual ISO Sensitivity" to if (capabilities.isManualIsoSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "Manual Shutter Exposure" to if (capabilities.isManualShutterSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "Manual White Balance" to if (capabilities.isManualWhiteBalanceSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "Manual Focus Distance" to if (capabilities.isManualFocusSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "Manual Sensor Capabilities" to if (capabilities.isManualSensorSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            "RAW Image Output" to if (capabilities.isRawSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
        )
    }
}
