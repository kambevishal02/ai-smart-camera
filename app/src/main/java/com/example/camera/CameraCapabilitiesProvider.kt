package com.example.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Range
import android.util.Rational
import android.util.Size
import com.example.model.CameraCapabilities
import com.example.model.CameraLensType
import com.example.model.DeviceCapabilityLevel
import com.example.model.PhysicalLensInfo
import com.example.util.AppLogger
import java.util.Locale
import kotlin.math.atan
import kotlin.math.sqrt

/**
 * Dynamic Camera2 capability detector for generic Android devices.
 * Probes the camera hardware layer at runtime without hardcoded device assumptions.
 */
object CameraCapabilitiesProvider {

    /**
     * Probes the active camera characteristics and discovers all multi-camera hardware on the device.
     */
    fun queryHardware(context: Context, isFront: Boolean): CameraCapabilities {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return CameraCapabilities(isFrontCamera = isFront)

            val cameraIds = cameraManager.cameraIdList.toList()
            val targetFacing = if (isFront) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }

            var matchedId: String? = null
            var matchedChars: CameraCharacteristics? = null

            // 1. Discover all physical lenses and identify the active camera
            val discoveredLenses = mutableListOf<PhysicalLensInfo>()

            for (id in cameraIds) {
                try {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
                    val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()

                    // Compute 35mm equivalent focal length if sensor size is available
                    val eq35mm = if (focalLengths.isNotEmpty() && sensorSize != null && sensorSize.width > 0f) {
                        val diag = sqrt(sensorSize.width * sensorSize.width + sensorSize.height * sensorSize.height)
                        if (diag > 0f) (focalLengths.first() * (43.27f / diag)) else null
                    } else {
                        null
                    }

                    // Dynamically categorize lens type
                    val isFrontLens = facing == CameraCharacteristics.LENS_FACING_FRONT
                    val lensType = when {
                        isFrontLens -> CameraLensType.FRONT
                        eq35mm != null && eq35mm < 22f -> CameraLensType.ULTRA_WIDE
                        eq35mm != null && eq35mm > 55f -> CameraLensType.TELEPHOTO
                        focalLengths.isNotEmpty() && focalLengths.first() < 2.5f -> CameraLensType.ULTRA_WIDE
                        focalLengths.isNotEmpty() && focalLengths.first() > 6.0f -> CameraLensType.TELEPHOTO
                        facing == CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraLensType.EXTERNAL
                        else -> CameraLensType.MAIN_WIDE
                    }

                    // Max resolution for this lens
                    val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizes = map?.getOutputSizes(ImageFormat.JPEG)
                    val maxRes = sizes?.maxByOrNull { it.width * it.height }
                    val resStr = if (maxRes != null) "${maxRes.width}x${maxRes.height}" else "Unknown"

                    discoveredLenses.add(
                        PhysicalLensInfo(
                            cameraId = id,
                            lensType = lensType,
                            focalLengths = focalLengths,
                            equivalentFocalLength35mm = eq35mm,
                            maxAperture = apertures.minOrNull(),
                            isFront = isFrontLens,
                            maxResolution = resStr
                        )
                    )

                    if (matchedId == null && facing == targetFacing) {
                        matchedId = id
                        matchedChars = chars
                    }
                } catch (e: Exception) {
                    // Skip unsupported individual camera IDs
                }
            }

            if (matchedId == null || matchedChars == null) {
                return CameraCapabilities(
                    availableCameraIds = cameraIds,
                    lensFacingName = if (isFront) "Front Camera" else "Rear Camera",
                    isFrontCamera = isFront,
                    physicalLenses = discoveredLenses
                )
            }

            // 2. Hardware Level Discovery
            val levelInt = matchedChars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            val hardwareLevel = when (levelInt) {
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> DeviceCapabilityLevel.MINIMAL
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> DeviceCapabilityLevel.LIMITED
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> DeviceCapabilityLevel.FULL
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> DeviceCapabilityLevel.LEVEL_3
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> DeviceCapabilityLevel.EXTERNAL
                else -> DeviceCapabilityLevel.UNKNOWN
            }

            // 3. Resolutions
            val streamMap = matchedChars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = streamMap?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
            val maxResolution = outputSizes.maxByOrNull { it.width * it.height }
            val resString = if (maxResolution != null) {
                val mp = (maxResolution.width * maxResolution.height) / 1_000_000.0
                "${maxResolution.width}x${maxResolution.height} (${String.format(Locale.US, "%.1f", mp)} MP)"
            } else {
                "Sensor Available"
            }
            val supportedResolutionsList = outputSizes.take(6).map { "${it.width}x${it.height}" }

            // 4. Supported FPS Ranges
            val fpsRanges = matchedChars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            val supportedFpsList = fpsRanges?.map { "[${it.lower}..${it.upper}] fps" } ?: emptyList()

            // 5. Exposure Compensation Range & Step
            val evRange = matchedChars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            val evStepRat = matchedChars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            val evStep = if (evStepRat != null && evStepRat.denominator != 0) {
                evStepRat.numerator.toFloat() / evStepRat.denominator.toFloat()
            } else {
                0.166667f
            }
            val evMin = evRange?.lower ?: 0
            val evMax = evRange?.upper ?: 0
            val isEvSupported = (evMin != 0 || evMax != 0) && (evStep > 0.001f)

            // 6. Flash Unit
            val hasFlash = matchedChars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            // 7. Supported AF Modes
            val afModes = matchedChars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
            val isManualFocusSupported = afModes.contains(CameraMetadata.CONTROL_AF_MODE_OFF)
            val afModeNames = afModes.map { mode ->
                when (mode) {
                    CameraMetadata.CONTROL_AF_MODE_OFF -> "MANUAL / OFF"
                    CameraMetadata.CONTROL_AF_MODE_AUTO -> "AF_AUTO"
                    CameraMetadata.CONTROL_AF_MODE_MACRO -> "AF_MACRO"
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "AF_CONTINUOUS_PICTURE"
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "AF_CONTINUOUS_VIDEO"
                    CameraMetadata.CONTROL_AF_MODE_EDOF -> "AF_EDOF"
                    else -> "MODE_$mode"
                }
            }

            // 8. Supported AE Modes
            val aeModes = matchedChars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES) ?: intArrayOf()
            val aeModeNames = aeModes.map { mode ->
                when (mode) {
                    CameraMetadata.CONTROL_AE_MODE_OFF -> "AE_MANUAL_OFF"
                    CameraMetadata.CONTROL_AE_MODE_ON -> "AE_AUTO_ON"
                    CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH -> "AE_AUTO_FLASH"
                    CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH -> "AE_ALWAYS_FLASH"
                    CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE -> "AE_AUTO_FLASH_REDEYE"
                    else -> "AE_MODE_$mode"
                }
            }

            // 9. Supported AWB Modes
            val awbModes = matchedChars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) ?: intArrayOf()
            val isManualWbSupported = awbModes.contains(CameraMetadata.CONTROL_AWB_MODE_OFF)
            val awbModeNames = awbModes.map { mode ->
                when (mode) {
                    CameraMetadata.CONTROL_AWB_MODE_OFF -> "AWB_MANUAL_OFF"
                    CameraMetadata.CONTROL_AWB_MODE_AUTO -> "AWB_AUTO"
                    CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT -> "AWB_INCANDESCENT"
                    CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT -> "AWB_FLUORESCENT"
                    CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT -> "AWB_WARM_FLUORESCENT"
                    CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT -> "AWB_DAYLIGHT"
                    CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "AWB_CLOUDY"
                    CameraMetadata.CONTROL_AWB_MODE_TWILIGHT -> "AWB_TWILIGHT"
                    CameraMetadata.CONTROL_AWB_MODE_SHADE -> "AWB_SHADE"
                    else -> "AWB_MODE_$mode"
                }
            }

            // 10. Device Capabilities flags (Manual Sensor, RAW)
            val capabilities = matchedChars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
            val isManualSensorSupported = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
            val isRawSupported = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)

            // 11. ISO (Sensitivity)
            val isoRange = matchedChars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val (isoString, isManualIso) = if (isManualSensorSupported && isoRange != null) {
                Pair("ISO ${isoRange.lower} - ${isoRange.upper}", true)
            } else {
                Pair("Auto (Managed by Camera ISP)", false)
            }

            // 12. Shutter Speed (Exposure Time)
            val expRange = matchedChars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val (shutterString, isManualShutter) = if (isManualSensorSupported && expRange != null) {
                val minMs = expRange.lower / 1_000_000.0
                val maxMs = expRange.upper / 1_000_000.0
                Pair("${String.format(Locale.US, "%.2f", minMs)}ms - ${String.format(Locale.US, "%.0f", maxMs)}ms", true)
            } else {
                Pair("Auto (Managed by Camera ISP)", false)
            }

            // 13. Digital Zoom Range
            val maxZoom = matchedChars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

            // 14. Multi-Camera Physical IDs (Android 9+)
            val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                matchedChars.physicalCameraIds?.toList() ?: emptyList()
            } else {
                emptyList()
            }

            val focalLengths = matchedChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()

            // Active Lens Type
            val activeLensType = discoveredLenses.find { it.cameraId == matchedId }?.lensType
                ?: if (isFront) CameraLensType.FRONT else CameraLensType.MAIN_WIDE

            val capabilitiesResult = CameraCapabilities(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                cameraApiVersion = "CameraX 1.5.0 / Camera2 HAL",
                availableCameraIds = cameraIds,
                activeCameraId = matchedId,
                lensFacingName = if (isFront) "Front Camera" else "Rear Camera (ID $matchedId)",
                isFrontCamera = isFront,
                activeLensType = activeLensType,
                hardwareLevel = hardwareLevel,
                sensorResolution = resString,
                supportedResolutions = supportedResolutionsList,
                supportedFpsRanges = supportedFpsList,
                evRangeMin = evMin,
                evRangeMax = evMax,
                evStep = evStep,
                isEvCompensationSupported = isEvSupported,
                isFlashSupported = hasFlash,
                isAutoExposureSupported = aeModes.isNotEmpty(),
                supportedFocusModes = if (afModeNames.isNotEmpty()) afModeNames else listOf("AF_CONTINUOUS_PICTURE", "AF_AUTO"),
                supportedExposureModes = if (aeModeNames.isNotEmpty()) aeModeNames else listOf("AE_AUTO_ON"),
                supportedAwbModes = if (awbModeNames.isNotEmpty()) awbModeNames else listOf("AWB_AUTO", "AWB_DAYLIGHT", "AWB_CLOUDY"),
                isoRange = isoString,
                isoRangeMin = isoRange?.lower,
                isoRangeMax = isoRange?.upper,
                isManualIsoSupported = isManualIso,
                shutterRange = shutterString,
                shutterRangeMinNanos = expRange?.lower,
                shutterRangeMaxNanos = expRange?.upper,
                isManualShutterSupported = isManualShutter,
                isManualWhiteBalanceSupported = isManualWbSupported,
                isManualFocusSupported = isManualFocusSupported,
                isManualSensorSupported = isManualSensorSupported,
                isRawSupported = isRawSupported,
                minZoomRatio = 1.0f,
                maxZoomRatio = maxZoom.coerceAtLeast(1.0f),
                availableFocalLengths = focalLengths,
                physicalLenses = discoveredLenses,
                physicalCameraIds = physicalIds
            )

            AppLogger.logCameraCapabilities(capabilitiesResult)
            return capabilitiesResult
        } catch (e: Exception) {
            e.printStackTrace()
            AppLogger.addLog("HARDWARE", "Error probing camera capabilities: ${e.message}", "WARN")
            return CameraCapabilities(isFrontCamera = isFront)
        }
    }
}
