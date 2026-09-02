package com.example.model

import android.os.Build

/**
 * Capability support status for camera hardware features.
 * Prevents false claims by strictly distinguishing SUPPORTED, UNSUPPORTED, and UNKNOWN.
 */
enum class CapabilityStatus(val label: String) {
    SUPPORTED("SUPPORTED"),
    UNSUPPORTED("UNSUPPORTED"),
    UNKNOWN("UNKNOWN")
}

/**
 * Camera2 Hardware Level classifications.
 * Determines manual sensor control depth and processing pipeline capabilities across all Android OEMs.
 */
enum class DeviceCapabilityLevel(val label: String, val description: String) {
    FULL(
        label = "FULL",
        description = "Full manual sensor, exposure timing, and high-framerate per-frame control support."
    ),
    LIMITED(
        label = "LIMITED",
        description = "Standard Camera2 API with basic AE/AF and hardware ISP exposure management."
    ),
    MINIMAL(
        label = "MINIMAL (LEGACY)",
        description = "Legacy HAL compatibility mode with basic preview and capture capabilities."
    ),
    LEVEL_3(
        label = "LEVEL_3",
        description = "Advanced manual YUV reprocessing and RAW sensor capabilities."
    ),
    EXTERNAL(
        label = "EXTERNAL",
        description = "External USB or peripheral camera stream."
    ),
    UNKNOWN(
        label = "UNKNOWN",
        description = "Unspecified or custom OEM HAL capability level."
    )
}

/**
 * Physical lens categorization dynamically computed from focal length and sensor dimensions.
 */
enum class CameraLensType(val displayName: String) {
    MAIN_WIDE("Main Wide"),
    ULTRA_WIDE("Ultra Wide"),
    TELEPHOTO("Telephoto"),
    MACRO("Macro"),
    FRONT("Front Selfie"),
    EXTERNAL("External Lens"),
    UNKNOWN("Standard Lens")
}

/**
 * Information on an individual physical camera lens discovered dynamically.
 */
data class PhysicalLensInfo(
    val cameraId: String,
    val lensType: CameraLensType,
    val focalLengths: List<Float> = emptyList(),
    val equivalentFocalLength35mm: Float? = null,
    val maxAperture: Float? = null,
    val isFront: Boolean = false,
    val maxResolution: String = ""
)

/**
 * Device-independent camera capabilities discovered dynamically at runtime via Android Camera2 / CameraX.
 * Contains ZERO hardcoded manufacturer or model values.
 */
data class CameraCapabilities(
    val manufacturer: String = runCatching { Build.MANUFACTURER }.getOrNull() ?: "Generic",
    val model: String = runCatching { Build.MODEL }.getOrNull() ?: "Android Device",
    val deviceName: String = runCatching { "${Build.MANUFACTURER} ${Build.MODEL}" }.getOrNull() ?: "Generic Android Camera",
    val androidVersion: String = runCatching { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }.getOrNull() ?: "Android",
    val cameraApiVersion: String = "CameraX 1.5.0 / Camera2 HAL",
    val availableCameraIds: List<String> = emptyList(),
    val activeCameraId: String = "0",
    val cameraId: String = activeCameraId,
    val lensFacingName: String = "Rear Camera",
    val isFrontCamera: Boolean = false,
    val activeLensType: CameraLensType = CameraLensType.MAIN_WIDE,
    val hardwareLevel: DeviceCapabilityLevel = DeviceCapabilityLevel.LIMITED,
    val sensorResolution: String = "Unknown",
    val supportedResolutions: List<String> = emptyList(),
    val supportedFpsRanges: List<String> = emptyList(),
    val evRangeMin: Int = 0,
    val evRangeMax: Int = 0,
    val evStep: Float = 0.0f,
    val isEvCompensationSupported: Boolean = false,
    val isFlashSupported: Boolean = false,
    val isAutoExposureSupported: Boolean = true,
    val supportedFocusModes: List<String> = listOf("AF_CONTINUOUS_PICTURE", "AF_AUTO"),
    val supportedExposureModes: List<String> = listOf("AE_AUTO_ON"),
    val supportedAwbModes: List<String> = listOf("AWB_AUTO", "AWB_DAYLIGHT", "AWB_CLOUDY", "AWB_INCANDESCENT", "AWB_FLUORESCENT"),
    val isoRange: String = "Auto (Managed by ISP)",
    val isoRangeMin: Int? = null,
    val isoRangeMax: Int? = null,
    val isManualIsoSupported: Boolean = false,
    val shutterRange: String = "Auto (Managed by ISP)",
    val shutterRangeMinNanos: Long? = null,
    val shutterRangeMaxNanos: Long? = null,
    val isManualShutterSupported: Boolean = false,
    val isManualWhiteBalanceSupported: Boolean = false,
    val isManualFocusSupported: Boolean = false,
    val isManualSensorSupported: Boolean = false,
    val isRawSupported: Boolean = false,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f,
    val availableFocalLengths: List<Float> = emptyList(),
    val physicalLenses: List<PhysicalLensInfo> = emptyList(),
    val physicalCameraIds: List<String> = emptyList()
)

// Backward compatibility alias
typealias CameraHardwareInfo = CameraCapabilities
