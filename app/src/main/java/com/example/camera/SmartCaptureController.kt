package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import com.example.ai.AdaptiveIntelligenceEngine
import com.example.ai.AdaptiveProfileStore
import com.example.ai.CalibrationEngine
import com.example.ai.CameraDecisionEngine
import com.example.ai.ImageTechnicalMetricCalculator
import com.example.ai.PhotoQualityAnalyzer
import com.example.model.AbCaptureSession
import com.example.model.AbTestStore
import com.example.model.CameraCapabilities
import com.example.model.CameraRecommendation
import com.example.model.CaptureMetadata
import com.example.model.CapturedPhoto
import com.example.model.DeveloperMetadataStore
import com.example.model.EnhancementParameters
import com.example.model.FlashRecommendation
import com.example.model.FocusStrategy
import com.example.model.ImageProcessingProfileType
import com.example.model.PhotoQualityScore
import com.example.model.SceneAnalysis
import com.example.model.SmartCaptureStatus
import com.example.model.TestSceneType
import com.example.processing.IImageProcessor
import com.example.processing.LightweightImageEnhancer
import com.example.util.AppLogger
import com.example.util.MediaSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SmartCaptureController encapsulates the end-to-end capture pipelines:
 *
 * 1. SMART AUTO:
 *    - Live scene evaluation & recommendation.
 *    - Applies calibrated hardware controls with graceful fallbacks.
 *    - Captures frame & applies calibrated conservative enhancement profile.
 *    - Calculates PhotoQualityScore & DetailedTechnicalMetrics.
 *    - Saves to MediaStore & local developer metadata store.
 *
 * 2. STANDARD AUTO:
 *    - Standard 3A capture without computational offsets.
 *
 * 3. A/B TEST MODE (V0.3):
 *    - Sequential dual capture under identical framing (same camera, lens, zoom, orientation).
 *    - PHOTO A: Standard AUTO capture.
 *    - PHOTO B: SMART AUTO capture with calibrated parameters.
 *    - Computes objective technical metrics for both photos.
 *    - Stores session locally in AbTestStore for developer comparison & parameter calibration.
 */
class SmartCaptureController(
    private val context: Context,
    private val cameraExecutor: ExecutorService,
    private val decisionEngine: CameraDecisionEngine = CameraDecisionEngine(),
    private val imageProcessor: IImageProcessor = LightweightImageEnhancer(),
    private val photoQualityAnalyzer: PhotoQualityAnalyzer = PhotoQualityAnalyzer(),
    private val metricCalculator: ImageTechnicalMetricCalculator = ImageTechnicalMetricCalculator()
) {
    private val _captureStatus = MutableStateFlow(SmartCaptureStatus.READY)
    val captureStatus: StateFlow<SmartCaptureStatus> = _captureStatus.asStateFlow()

    private val _lastQualityScore = MutableStateFlow<PhotoQualityScore?>(null)
    val lastQualityScore: StateFlow<PhotoQualityScore?> = _lastQualityScore.asStateFlow()

    private val _abProgress = MutableStateFlow<String?>(null)
    val abProgress: StateFlow<String?> = _abProgress.asStateFlow()

    fun updateAnalysisState(isAnalyzing: Boolean) {
        if (!isAnalyzing && _captureStatus.value == SmartCaptureStatus.ANALYZING) {
            _captureStatus.value = SmartCaptureStatus.READY
        }
    }

    fun resetToAnalyzing() {
        if (_captureStatus.value == SmartCaptureStatus.READY) {
            _captureStatus.value = SmartCaptureStatus.ANALYZING
        }
    }

    /**
     * Executes the complete Smart Auto or Standard Auto single capture pipeline.
     */
    fun capture(
        imageCapture: ImageCapture?,
        cameraControl: CameraControl?,
        cameraInfo: CameraInfo?,
        capabilities: CameraCapabilities,
        currentAnalysis: SceneAnalysis,
        isSmartAuto: Boolean,
        manualProfileOverride: ImageProcessingProfileType?,
        isFrontCamera: Boolean,
        scope: CoroutineScope,
        onSuccess: (CapturedPhoto) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Camera capture pipeline is not initialized")
            return
        }

        scope.launch {
            try {
                _captureStatus.value = SmartCaptureStatus.CAPTURING

                // 1. Evaluate Scene & Recommendations
                val recommendation = if (isSmartAuto) {
                    decisionEngine.evaluate(currentAnalysis, capabilities)
                } else {
                    CameraRecommendation(
                        exposureCompensationIndex = 0,
                        exposureCompensationEv = 0.0f,
                        focusStrategy = FocusStrategy.AUTO,
                        flashRecommendation = FlashRecommendation.OFF,
                        imageProcessingProfile = manualProfileOverride ?: ImageProcessingProfileType.NATURAL,
                        confidence = 1.0f
                    )
                }

                // 2. Resolve CaptureIntent via CameraHardwareAdapter (requested vs. applied vs. fallbacks)
                val resolvedSettings = CameraHardwareAdapter.resolveIntent(recommendation.captureIntent, capabilities)
                val requestedSettingsMap = resolvedSettings.toRequestedMap()
                val fallbackSettingsMap = resolvedSettings.toFallbackMap()

                var needsStabilizationDelay = false
                val appliedSettingsMap = mutableMapOf<String, String>()

                if (cameraControl != null) {
                    if (isSmartAuto && capabilities.isEvCompensationSupported) {
                        val targetEvIndex = resolvedSettings.appliedEvIndex
                        cameraControl.setExposureCompensationIndex(targetEvIndex)
                        appliedSettingsMap["EV Index"] = "$targetEvIndex (${resolvedSettings.appliedEvOffset} EV)"
                        if (targetEvIndex != 0) {
                            needsStabilizationDelay = true
                        }
                    } else {
                        appliedSettingsMap["EV Index"] = "0 (Auto Default)"
                    }

                    if (isSmartAuto && currentAnalysis.subject.isPersonPresent && currentAnalysis.subject.detectedFaces.isNotEmpty()) {
                        val primaryFace = currentAnalysis.subject.detectedFaces.first()
                        val centerX = primaryFace.bounds.centerX()
                        val centerY = primaryFace.bounds.centerY()
                        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                        val point = factory.createPoint(centerX, centerY)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        cameraControl.startFocusAndMetering(action)
                        appliedSettingsMap["Focus/AE"] = "Face Priority Metering (${String.format("%.2f", centerX)}, ${String.format("%.2f", centerY)})"
                    } else {
                        appliedSettingsMap["Focus/AE"] = "Center Area 3A Auto"
                    }

                    if (isSmartAuto && resolvedSettings.appliedZoom > 1.05f) {
                        cameraControl.setZoomRatio(resolvedSettings.appliedZoom)
                        appliedSettingsMap["Zoom"] = "${String.format("%.1f", resolvedSettings.appliedZoom)}x"
                    } else {
                        appliedSettingsMap["Zoom"] = "1.0x"
                    }
                }

                val targetFlashMode = when (resolvedSettings.appliedFlash) {
                    FlashRecommendation.ON, FlashRecommendation.FILL_TORCH -> ImageCapture.FLASH_MODE_ON
                    FlashRecommendation.AUTO -> ImageCapture.FLASH_MODE_AUTO
                    FlashRecommendation.OFF -> ImageCapture.FLASH_MODE_OFF
                }
                capture.flashMode = targetFlashMode
                appliedSettingsMap["Flash Mode"] = when (targetFlashMode) {
                    ImageCapture.FLASH_MODE_ON -> "ON"
                    ImageCapture.FLASH_MODE_AUTO -> "AUTO"
                    else -> "OFF"
                }

                if (needsStabilizationDelay) {
                    delay(60)
                }

                // 3. Capture frame
                val rawBitmap = takePictureAsync(capture, isFrontCamera)

                _captureStatus.value = SmartCaptureStatus.PROCESSING

                val activeProfile = if (isSmartAuto) {
                    manualProfileOverride ?: recommendation.imageProcessingProfile
                } else {
                    manualProfileOverride ?: ImageProcessingProfileType.NATURAL
                }
                appliedSettingsMap["Profile Applied"] = activeProfile.displayName

                // 4. Apply Calibrated Image Processing
                var finalBitmap = rawBitmap
                var debugInfo: com.example.model.SubjectEnhancementDebugInfo? = null
                var metricsOriginal: com.example.model.ObjectivePhotoQualityMetrics? = null
                var metricsEnhanced: com.example.model.ObjectivePhotoQualityMetrics? = null

                if (isSmartAuto || manualProfileOverride != null) {
                    val baseParams = if (manualProfileOverride != null) {
                        EnhancementParameters.defaultForProfile(manualProfileOverride)
                    } else {
                        recommendation.enhancementParams
                    }
                    val calibratedParams = CalibrationEngine.applyCalibration(baseParams, activeProfile)
                    val result = imageProcessor.enhanceImageWithDetails(rawBitmap, activeProfile, calibratedParams, currentAnalysis)
                    finalBitmap = result.bitmap
                    debugInfo = result.debugInfo
                    metricsOriginal = result.metricsOriginal
                    metricsEnhanced = result.metricsEnhanced
                } else {
                    val primaryFaceBox = currentAnalysis.subject.detectedFaces.firstOrNull()?.bounds
                    metricsOriginal = metricCalculator.calculateObjectiveMetrics(rawBitmap, primaryFaceBox)
                }

                // 5. Compute Post-Capture Photo Quality Score
                val postQualityScore = photoQualityAnalyzer.calculate(
                    lighting = currentAnalysis.lighting,
                    subject = currentAnalysis.subject,
                    motion = currentAnalysis.motion,
                    sharpnessMetric = currentAnalysis.sharpnessMetric,
                    scene = currentAnalysis.scene
                )
                _lastQualityScore.value = postQualityScore

                // 6. Save to Gallery
                val savedUri = MediaSaver.saveBitmapToGallery(
                    context = context,
                    bitmap = finalBitmap,
                    sceneName = if (isSmartAuto) currentAnalysis.scene.name.lowercase() else "auto",
                    isEnhanced = isSmartAuto
                )

                // 7. Record Diagnostic Metadata
                val detectionEngine = if (currentAnalysis.subject.isPersonPresent && currentAnalysis.subject.detectedFaces.isNotEmpty()) {
                    "ML_KIT_AND_HEURISTICS"
                } else {
                    "HEURISTIC"
                }
                val intent = recommendation.captureIntent
                val metadata = CaptureMetadata(
                    device = capabilities.deviceName,
                    cameraId = capabilities.activeCameraId,
                    captureMode = if (isSmartAuto) "SMART AUTO" else "AUTO",
                    scene = currentAnalysis.scene,
                    sceneDetectionType = detectionEngine,
                    lighting = currentAnalysis.lighting.condition,
                    brightnessLuma = currentAnalysis.lighting.brightness,
                    faceCount = currentAnalysis.subject.numberOfFaces,
                    motionLevel = currentAnalysis.motion.motionLevel,
                    recommendationSummary = recommendation.primaryActionText,
                    appliedSettings = appliedSettingsMap,
                    processingProfile = activeProfile,
                    qualityScore = postQualityScore.totalScore,
                    qualityBreakdown = mapOf(
                        "Exposure" to postQualityScore.exposureScore,
                        "Sharpness" to postQualityScore.sharpnessScore,
                        "Stability" to postQualityScore.stabilityScore,
                        "Highlight Protection" to postQualityScore.highlightScore,
                        "Shadow Preservation" to postQualityScore.shadowScore,
                        "Dynamic Range" to postQualityScore.dynamicRangeScore
                    ),
                    highlightProtection = intent.highlightProtection.label,
                    shadowStrategy = intent.shadowPriority.label,
                    facePriorityMode = intent.facePriority.label,
                    targetEv = resolvedSettings.appliedEvOffset,
                    hardwareRequestedSettings = requestedSettingsMap,
                    hardwareFallbackSettings = fallbackSettingsMap,
                    decisionReasoning = intent.reasoning
                )
                DeveloperMetadataStore.record(metadata)

                val capturedPhoto = CapturedPhoto(
                    rawBitmap = rawBitmap,
                    enhancedBitmap = finalBitmap,
                    sceneAnalysis = currentAnalysis,
                    recommendation = recommendation,
                    savedUri = savedUri,
                    qualityScore = postQualityScore,
                    debugInfo = debugInfo,
                    metricsOriginal = metricsOriginal,
                    metricsEnhanced = metricsEnhanced
                )

                withContext(Dispatchers.Main) {
                    _captureStatus.value = SmartCaptureStatus.SAVED
                    onSuccess(capturedPhoto)
                }

                delay(2000)
                if (_captureStatus.value == SmartCaptureStatus.SAVED) {
                    _captureStatus.value = SmartCaptureStatus.READY
                }
            } catch (e: Exception) {
                AppLogger.e("SmartCaptureController", "Capture error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _captureStatus.value = SmartCaptureStatus.READY
                    onError("Capture failed: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * V0.3: Sequential A/B Test Capture Mode.
     * Captures PHOTO A (Standard AUTO) and PHOTO B (SMART AUTO) under identical framing.
     */
    fun captureAbTest(
        imageCapture: ImageCapture?,
        cameraControl: CameraControl?,
        cameraInfo: CameraInfo?,
        capabilities: CameraCapabilities,
        currentAnalysis: SceneAnalysis,
        testScene: TestSceneType,
        isFrontCamera: Boolean,
        scope: CoroutineScope,
        onSuccess: (AbCaptureSession) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Camera capture pipeline is not initialized")
            return
        }

        scope.launch {
            try {
                _captureStatus.value = SmartCaptureStatus.AB_TESTING
                _abProgress.value = "1/2 Capturing PHOTO A (Standard AUTO)..."

                // --- PHASE 1: PHOTO A (STANDARD AUTO) ---
                if (cameraControl != null && capabilities.isEvCompensationSupported) {
                    cameraControl.setExposureCompensationIndex(0)
                }
                capture.flashMode = ImageCapture.FLASH_MODE_OFF

                val appliedSettingsA = mutableMapOf(
                    "EV Index" to "0 (Default Auto)",
                    "Focus/AE" to "Center 3A Auto",
                    "Flash Mode" to "OFF",
                    "Profile" to "Original True Capture"
                )

                delay(60)
                val photoA_RawBitmap = takePictureAsync(capture, isFrontCamera)

                // Compute Photo A Metrics
                val primaryFaceBox = currentAnalysis.subject.detectedFaces.firstOrNull()?.bounds
                val photoA_Metrics = metricCalculator.calculateMetrics(photoA_RawBitmap, primaryFaceBox)
                val photoA_ObjectiveMetrics = metricCalculator.calculateObjectiveMetrics(photoA_RawBitmap, primaryFaceBox)

                // Save Photo A (Original unenhanced)
                val photoA_Uri = MediaSaver.saveBitmapToGallery(
                    context = context,
                    bitmap = photoA_RawBitmap,
                    sceneName = "ab_test_auto_${testScene.name.lowercase()}",
                    isEnhanced = false
                )

                // --- PHASE 2: PHOTO B (SMART AUTO) ---
                _abProgress.value = "2/2 Capturing PHOTO B (SMART AUTO)..."
                val recommendation = decisionEngine.evaluate(currentAnalysis, capabilities)
                val appliedSettingsB = mutableMapOf<String, String>()
                val fallbackSettings = mutableMapOf<String, String>()

                if (cameraControl != null) {
                    if (capabilities.isEvCompensationSupported) {
                        val targetEvIndex = recommendation.exposureCompensationIndex
                        cameraControl.setExposureCompensationIndex(targetEvIndex)
                        appliedSettingsB["EV Index"] = "$targetEvIndex (${recommendation.exposureCompensationEv} EV)"
                    } else {
                        fallbackSettings["EV Compensation"] = "Fallback to Software Tone Curve"
                    }

                    if (currentAnalysis.subject.isPersonPresent && currentAnalysis.subject.detectedFaces.isNotEmpty()) {
                        val primaryFace = currentAnalysis.subject.detectedFaces.first()
                        val centerX = primaryFace.bounds.centerX()
                        val centerY = primaryFace.bounds.centerY()
                        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                        val point = factory.createPoint(centerX, centerY)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        cameraControl.startFocusAndMetering(action)
                        appliedSettingsB["Focus/AE"] = "Face Priority Metering (${String.format("%.2f", centerX)}, ${String.format("%.2f", centerY)})"
                    } else {
                        appliedSettingsB["Focus/AE"] = "Scene 3A Optimization"
                    }
                }

                delay(120) // Stabilization delay
                val photoB_RawBitmap = takePictureAsync(capture, isFrontCamera)

                _abProgress.value = "Processing & Calculating Metrics..."
                _captureStatus.value = SmartCaptureStatus.PROCESSING

                // Apply Calibrated Subject-Aware Image Enhancement
                val profile = recommendation.imageProcessingProfile
                val calibratedParams = CalibrationEngine.applyCalibration(recommendation.enhancementParams, profile)
                appliedSettingsB["Profile Applied"] = profile.displayName
                appliedSettingsB["Calibration"] = CalibrationEngine.getSummaryString()

                val enhancementResult = imageProcessor.enhanceImageWithDetails(
                    source = photoB_RawBitmap,
                    profile = profile,
                    params = calibratedParams,
                    sceneAnalysis = currentAnalysis
                )
                val photoB_EnhancedBitmap = enhancementResult.bitmap
                val photoB_ObjectiveMetrics = enhancementResult.metricsEnhanced
                val subjectDebugInfo = enhancementResult.debugInfo

                // Compute Photo B Metrics
                val photoB_Metrics = metricCalculator.calculateMetrics(photoB_EnhancedBitmap, primaryFaceBox)

                // Save Photo B (Enhanced)
                val photoB_Uri = MediaSaver.saveBitmapToGallery(
                    context = context,
                    bitmap = photoB_EnhancedBitmap,
                    sceneName = "ab_test_smart_${testScene.name.lowercase()}",
                    isEnhanced = true
                )

                // Create and Store A/B Session
                val session = AbCaptureSession(
                    testScene = testScene,
                    deviceName = capabilities.deviceName,
                    cameraId = capabilities.activeCameraId,
                    hardwareLevel = capabilities.hardwareLevel.name,
                    photoA_Uri = photoA_Uri,
                    photoA_Bitmap = photoA_RawBitmap,
                    photoA_Metrics = photoA_Metrics,
                    photoA_AppliedSettings = appliedSettingsA,
                    photoB_Uri = photoB_Uri,
                    photoB_Bitmap = photoB_EnhancedBitmap,
                    photoB_Metrics = photoB_Metrics,
                    photoB_AppliedSettings = appliedSettingsB,
                    recommendation = recommendation,
                    fallbackSettings = fallbackSettings,
                    processingProfile = profile,
                    appliedCalibrationSummary = CalibrationEngine.getSummaryString(),
                    photoA_ObjectiveMetrics = photoA_ObjectiveMetrics,
                    photoB_ObjectiveMetrics = photoB_ObjectiveMetrics,
                    subjectDebugInfo = subjectDebugInfo
                )

                AbTestStore.recordSession(session)
                val adaptiveEval = AdaptiveIntelligenceEngine.processAbCaptureSession(session, currentAnalysis)
                AppLogger.i("SmartCaptureController", "A/B Test recorded for ${testScene.displayName}: Auto=${photoA_Metrics.totalTechnicalScore} vs Smart=${photoB_Metrics.totalTechnicalScore} | V0.5 Adaptive Eligible=${adaptiveEval.eligibleForLearning}")

                withContext(Dispatchers.Main) {
                    _captureStatus.value = SmartCaptureStatus.SAVED
                    _abProgress.value = null
                    onSuccess(session)
                }

                delay(2000)
                if (_captureStatus.value == SmartCaptureStatus.SAVED) {
                    _captureStatus.value = SmartCaptureStatus.READY
                }
            } catch (e: Exception) {
                AppLogger.e("SmartCaptureController", "A/B test capture failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _captureStatus.value = SmartCaptureStatus.READY
                    _abProgress.value = null
                    onError("A/B capture failed: ${e.localizedMessage}")
                }
            }
        }
    }

    private suspend fun takePictureAsync(
        imageCapture: ImageCapture,
        isFrontCamera: Boolean
    ): Bitmap = suspendCancellableCoroutine { cont ->
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val bitmap = decodeBitmapFromImageProxy(imageProxy, rotationDegrees, isFrontCamera)
                    imageProxy.close()
                    if (bitmap != null) {
                        cont.resume(bitmap)
                    } else {
                        cont.resumeWithException(IllegalStateException("Failed to decode image frame"))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
        )
    }

    private fun decodeBitmapFromImageProxy(
        imageProxy: ImageProxy,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ): Bitmap? {
        val plane = imageProxy.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        if (rotationDegrees == 0 && !isFrontCamera) {
            return decoded
        }

        val matrix = Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f, decoded.width / 2f, decoded.height / 2f)
        }

        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }
}
