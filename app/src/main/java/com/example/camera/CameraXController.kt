package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.ai.CameraDecisionEngine
import com.example.ai.ISceneAnalyzer
import com.example.ai.SceneAnalyzer
import com.example.model.CameraCapabilities
import com.example.model.CameraHardwareInfo
import com.example.model.CameraRecommendation
import com.example.model.CapturedPhoto
import com.example.model.EnhancementParameters
import com.example.model.FlashRecommendation
import com.example.model.FrameAnalysisResult
import com.example.model.ImageProcessingProfileType
import com.example.model.SceneType
import com.example.model.SmartCaptureStatus
import com.example.processing.IImageProcessor
import com.example.processing.LightweightImageEnhancer
import com.example.util.AppLogger
import com.example.util.MediaSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Robust CameraX controller implementing full real preview, frame analysis,
 * hardware capability detection, and V0.2 Smart Auto capture pipeline.
 */
class CameraXController(
    private val context: Context,
    private val sceneAnalyzer: ISceneAnalyzer = SceneAnalyzer(),
    private val decisionEngine: CameraDecisionEngine = CameraDecisionEngine(),
    private val imageProcessor: IImageProcessor = LightweightImageEnhancer()
) : ICameraController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val captureExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val smartCaptureController = SmartCaptureController(
        context = context,
        cameraExecutor = captureExecutor,
        decisionEngine = decisionEngine,
        imageProcessor = imageProcessor
    )

    // State Flows
    private val _analysisResult = MutableStateFlow(FrameAnalysisResult.INITIAL)
    override val analysisResult: StateFlow<FrameAnalysisResult> = _analysisResult.asStateFlow()

    private val _activeRecommendation = MutableStateFlow(CameraRecommendation())
    override val activeRecommendation: StateFlow<CameraRecommendation> = _activeRecommendation.asStateFlow()

    private val _cameraHardwareInfo = MutableStateFlow(CameraHardwareInfo())
    override val cameraHardwareInfo: StateFlow<CameraHardwareInfo> = _cameraHardwareInfo.asStateFlow()

    override val cameraCapabilities: StateFlow<CameraCapabilities> = _cameraHardwareInfo

    private val _isFrontCamera = MutableStateFlow(false)
    override val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isFlashActive = MutableStateFlow(false)
    override val isFlashActive: StateFlow<Boolean> = _isFlashActive.asStateFlow()

    override val captureStatus: StateFlow<SmartCaptureStatus> = smartCaptureController.captureStatus

    override val isCapturing: StateFlow<Boolean> = smartCaptureController.captureStatus
        .map { it == SmartCaptureStatus.CAPTURING }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override val isProcessing: StateFlow<Boolean> = smartCaptureController.captureStatus
        .map { it == SmartCaptureStatus.PROCESSING }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _lastCapturedPhoto = MutableStateFlow<CapturedPhoto?>(null)
    override val lastCapturedPhoto: StateFlow<CapturedPhoto?> = _lastCapturedPhoto.asStateFlow()

    private val _exposureCompensationIndex = MutableStateFlow(0)
    override val exposureCompensationIndex: StateFlow<Int> = _exposureCompensationIndex.asStateFlow()

    private val _isAiAutoModeEnabled = MutableStateFlow(true)
    override val isAiAutoModeEnabled: StateFlow<Boolean> = _isAiAutoModeEnabled.asStateFlow()

    private val _manualProfileOverride = MutableStateFlow<ImageProcessingProfileType?>(null)
    override val manualProfileOverride: StateFlow<ImageProcessingProfileType?> = _manualProfileOverride.asStateFlow()

    // Analysis throttling control
    private var lastAnalysisTimestamp = 0L
    private val analysisIntervalMs = 350L

    // Track last applied EV to prevent redundant cameraControl calls
    private var lastAppliedEvIndex = 0

    override fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onError: (String) -> Unit
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
            } catch (e: Exception) {
                e.printStackTrace()
                AppLogger.addLog("CAMERA", "Failed to get CameraProvider: ${e.message}", "ERROR")
                onError(e.message ?: "Failed to initialize camera")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        // 1. Query Hardware Capabilities dynamically for current lens facing
        val hardware = CameraCapabilitiesProvider.queryHardware(context, _isFrontCamera.value)
        _cameraHardwareInfo.value = hardware

        // 2. Camera Selector
        val lensFacing = if (_isFrontCamera.value) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // 3. Preview UseCase
        preview = Preview.Builder()
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        // 4. Image Capture UseCase
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(if (_isFlashActive.value) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()

        // 5. Image Analysis UseCase
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    processAnalysisFrame(imageProxy)
                }
            }

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            // Initialize EV compensation state
            val exposureState = cameraInfo?.exposureState
            if (exposureState != null && exposureState.isExposureCompensationSupported) {
                _exposureCompensationIndex.value = exposureState.exposureCompensationIndex
            }

            AppLogger.addLog("CAMERA", "Camera bound successfully (${hardware.lensFacingName})")
        } catch (e: Exception) {
            e.printStackTrace()
            AppLogger.addLog("CAMERA", "UseCases binding failed: ${e.message}", "ERROR")
        }
    }

    private fun processAnalysisFrame(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalysisTimestamp < analysisIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalysisTimestamp = now
        smartCaptureController.updateAnalysisState(true)

        try {
            // Run analysis pipeline
            val result = sceneAnalyzer.analyzeFrame(imageProxy)
            val hardware = _cameraHardwareInfo.value

            // Compute smart camera decision recommendation
            val recommendation = decisionEngine.evaluate(result, hardware)

            _analysisResult.value = result
            _activeRecommendation.value = recommendation

            // Log diagnostic info when scene or lighting changes
            AppLogger.logSceneDetected(result.scene, result.confidence)
            AppLogger.logLightingDetected(result.lightingLevel, result.brightnessLuma)
            AppLogger.logRecommendationSelected(recommendation)

            // If AI Auto mode is enabled and hardware supports EV compensation, apply optimal EV
            if (_isAiAutoModeEnabled.value && hardware.isEvCompensationSupported) {
                val targetEv = recommendation.exposureCompensationIndex
                if (targetEv != lastAppliedEvIndex) {
                    lastAppliedEvIndex = targetEv
                    cameraControl?.setExposureCompensationIndex(targetEv)
                    _exposureCompensationIndex.value = targetEv
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
            smartCaptureController.updateAnalysisState(false)
        }
    }

    override fun switchCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
        sceneAnalyzer.resetHistory()
        val owner = currentLifecycleOwner ?: return
        val view = currentPreviewView ?: return
        bindCameraUseCases(owner, view)
        AppLogger.addLog("CAMERA", "Switched camera to: ${if (_isFrontCamera.value) "Front" else "Rear"}")
    }

    override fun toggleFlash() {
        _isFlashActive.value = !_isFlashActive.value
        val flashMode = if (_isFlashActive.value) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        imageCapture?.flashMode = flashMode
        cameraControl?.enableTorch(_isFlashActive.value)
        AppLogger.addLog("CAMERA", "Flash set to: ${if (_isFlashActive.value) "ON" else "OFF"}")
    }

    override fun setFlashMode(flashRec: FlashRecommendation) {
        when (flashRec) {
            FlashRecommendation.ON -> {
                _isFlashActive.value = true
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                cameraControl?.enableTorch(false)
            }
            FlashRecommendation.FILL_TORCH -> {
                _isFlashActive.value = true
                cameraControl?.enableTorch(true)
            }
            FlashRecommendation.AUTO -> {
                _isFlashActive.value = false
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
                cameraControl?.enableTorch(false)
            }
            FlashRecommendation.OFF -> {
                _isFlashActive.value = false
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                cameraControl?.enableTorch(false)
            }
        }
    }

    override fun setExposureCompensation(index: Int) {
        val hardware = _cameraHardwareInfo.value
        val clamped = index.coerceIn(hardware.evRangeMin, hardware.evRangeMax)
        _exposureCompensationIndex.value = clamped
        lastAppliedEvIndex = clamped
        cameraControl?.setExposureCompensationIndex(clamped)
        AppLogger.addLog("CAMERA", "Manual EV Compensation set to index: $clamped (${clamped * hardware.evStep} EV)")
    }

    override fun setAiAutoMode(enabled: Boolean) {
        _isAiAutoModeEnabled.value = enabled
        AppLogger.addLog("AI_ENGINE", "Camera Mode set to: ${if (enabled) "SMART AUTO" else "AUTO"}")
    }

    override fun setManualProfile(profile: ImageProcessingProfileType?) {
        _manualProfileOverride.value = profile
        AppLogger.addLog("PROCESSING", "Manual profile override: ${profile?.displayName ?: "Auto AI Selected"}")
    }

    override fun focusOnPoint(xNorm: Float, yNorm: Float, previewView: PreviewView) {
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(xNorm * previewView.width, yNorm * previewView.height)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            cameraControl?.startFocusAndMetering(action)
            AppLogger.addLog("FOCUS", "Focus triggered at point: (${String.format("%.2f", xNorm)}, ${String.format("%.2f", yNorm)})")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setZoomRatio(ratio: Float) {
        val hardware = _cameraHardwareInfo.value
        val clamped = ratio.coerceIn(1.0f, hardware.maxZoomRatio)
        cameraControl?.setZoomRatio(clamped)
    }

    override fun capturePhoto(
        onSuccess: (CapturedPhoto) -> Unit,
        onError: (String) -> Unit
    ) {
        smartCaptureController.capture(
            imageCapture = imageCapture,
            cameraControl = cameraControl,
            cameraInfo = cameraInfo,
            capabilities = _cameraHardwareInfo.value,
            currentAnalysis = _analysisResult.value,
            isSmartAuto = _isAiAutoModeEnabled.value,
            manualProfileOverride = _manualProfileOverride.value,
            isFrontCamera = _isFrontCamera.value,
            scope = scope,
            onSuccess = { photo ->
                _lastCapturedPhoto.value = photo
                onSuccess(photo)
            },
            onError = onError
        )
    }

    override fun release() {
        analysisExecutor.shutdown()
        captureExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
