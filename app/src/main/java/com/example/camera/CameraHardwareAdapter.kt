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
     * Resolves an abstract CaptureIntent against the device's concrete CameraCapabilities.
     * Accurately tracks requested vs. applied values and documents fallbacks whenever unsupported.
     */
    fun resolveIntent(
        intent: com.example.model.CaptureIntent,
        capabilities: CameraCapabilities
    ): com.example.model.ResolvedHardwareSettings {
        val step = if (capabilities.evStep > 0.001f) capabilities.evStep else 0.33f
        val requestedEvIndex = (intent.preferredExposureCompensation / step).roundToInt()

        val (appliedEvIndex, appliedEvOffset, evFallback) = if (!capabilities.isEvCompensationSupported) {
            Triple(0, 0.0f, "Hardware lacks EV compensation; falling back to 3A auto metering")
        } else {
            val clampedIndex = requestedEvIndex.coerceIn(capabilities.evRangeMin, capabilities.evRangeMax)
            val clampedEv = clampedIndex * step
            val fallback = if (clampedIndex != requestedEvIndex) {
                "Requested EV index $requestedEvIndex clamped to hardware bounds [${capabilities.evRangeMin}..${capabilities.evRangeMax}]"
            } else null
            Triple(clampedIndex, clampedEv, fallback)
        }

        // White balance resolution
        val targetAwbString = when (intent.preferredWhiteBalance) {
            WhiteBalanceRecommendation.DAYLIGHT -> "AWB_DAYLIGHT"
            WhiteBalanceRecommendation.CLOUDY -> "AWB_CLOUDY"
            WhiteBalanceRecommendation.SHADE -> "AWB_SHADE"
            WhiteBalanceRecommendation.TUNGSTEN_WARM -> "AWB_INCANDESCENT"
            WhiteBalanceRecommendation.FLUORESCENT -> "AWB_FLUORESCENT"
            WhiteBalanceRecommendation.AUTO -> "AWB_AUTO"
        }
        val (appliedWb, wbFallback) = if (intent.preferredWhiteBalance == WhiteBalanceRecommendation.AUTO) {
            Pair(WhiteBalanceRecommendation.AUTO, null)
        } else if (capabilities.supportedAwbModes.contains(targetAwbString) || capabilities.isManualWhiteBalanceSupported) {
            Pair(intent.preferredWhiteBalance, null)
        } else {
            Pair(WhiteBalanceRecommendation.AUTO, "Preset ${intent.preferredWhiteBalance.label} unsupported by device HAL; falling back to AUTO WB")
        }

        // Zoom resolution
        val requestedZoom = intent.preferredZoom
        val (appliedZoom, zoomFallback) = if (capabilities.maxZoomRatio <= 1.0f) {
            Pair(1.0f, if (requestedZoom > 1.0f) "Digital zoom unsupported by device sensor" else null)
        } else {
            val clampedZoom = requestedZoom.coerceIn(capabilities.minZoomRatio, capabilities.maxZoomRatio)
            val fallback = if (clampedZoom != requestedZoom) "Zoom clamped to physical sensor limits [${capabilities.minZoomRatio}x..${capabilities.maxZoomRatio}x]" else null
            Pair(clampedZoom, fallback)
        }

        // Flash resolution
        val (appliedFlash, flashFallback) = if (!capabilities.isFlashSupported) {
            Pair(FlashRecommendation.OFF, if (intent.flashPreference != FlashRecommendation.OFF) "No physical flash unit on this camera; falling back to OFF" else null)
        } else {
            Pair(intent.flashPreference, null)
        }

        // Shutter & ISO resolution
        val (appliedShutter, shutterFallback) = if (capabilities.isManualShutterSupported) {
            Pair("Manual Shutter Active", null)
        } else {
            Pair("AUTO (AE Controlled)", "Manual shutter exposure unsupported; HAL 3A auto-metering active")
        }

        val (appliedIso, isoFallback) = if (capabilities.isManualIsoSupported) {
            Pair("Manual ISO Active", null)
        } else {
            Pair("AUTO (AE Controlled)", "Manual ISO gain unsupported; HAL 3A auto-gain active")
        }

        // Lens selection resolution
        val isLensPhysicallyAvailable = capabilities.physicalLenses.any { it.lensType == intent.preferredLens }
        val (appliedLens, lensFallback) = if (isLensPhysicallyAvailable || capabilities.activeLensType == intent.preferredLens) {
            Pair(intent.preferredLens, null)
        } else {
            Pair(capabilities.activeLensType, "Preferred lens ${intent.preferredLens.displayName} unavailable; routed to ${capabilities.activeLensType.displayName}")
        }

        return com.example.model.ResolvedHardwareSettings(
            requestedEvIndex = requestedEvIndex,
            requestedEvOffset = intent.preferredExposureCompensation,
            appliedEvIndex = appliedEvIndex,
            appliedEvOffset = appliedEvOffset,
            evFallbackReason = evFallback,
            requestedWhiteBalance = intent.preferredWhiteBalance,
            appliedWhiteBalance = appliedWb,
            wbFallbackReason = wbFallback,
            requestedZoom = requestedZoom,
            appliedZoom = appliedZoom,
            zoomFallbackReason = zoomFallback,
            requestedFlash = intent.flashPreference,
            appliedFlash = appliedFlash,
            flashFallbackReason = flashFallback,
            requestedShutter = com.example.model.ShutterPreference.AUTO,
            appliedShutter = appliedShutter,
            shutterFallbackReason = shutterFallback,
            requestedIso = com.example.model.IsoPreference.AUTO,
            appliedIso = appliedIso,
            isoFallbackReason = isoFallback,
            requestedLens = intent.preferredLens,
            appliedLens = appliedLens,
            lensFallbackReason = lensFallback
        )
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
