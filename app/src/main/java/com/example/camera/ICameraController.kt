package com.example.camera

import android.graphics.Bitmap
import android.net.Uri
import android.view.Surface
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.example.model.CameraCapabilities
import com.example.model.CameraHardwareInfo
import com.example.model.CameraRecommendation
import com.example.model.CapturedPhoto
import com.example.model.FlashRecommendation
import com.example.model.FrameAnalysisResult
import com.example.model.ImageProcessingProfileType
import com.example.model.SmartCaptureStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic camera controller interface.
 * Decouples the UI and smart engine from Android CameraX/Camera2 hardware layer,
 * enabling future multi-platform ports (such as iOS AVFoundation).
 */
interface ICameraController {
    val analysisResult: StateFlow<FrameAnalysisResult>
    val activeRecommendation: StateFlow<CameraRecommendation>
    val cameraHardwareInfo: StateFlow<CameraHardwareInfo>
    val cameraCapabilities: StateFlow<CameraCapabilities>
    val isFrontCamera: StateFlow<Boolean>
    val isFlashActive: StateFlow<Boolean>
    val isCapturing: StateFlow<Boolean>
    val isProcessing: StateFlow<Boolean>
    val captureStatus: StateFlow<SmartCaptureStatus>
    val lastCapturedPhoto: StateFlow<CapturedPhoto?>
    val exposureCompensationIndex: StateFlow<Int>
    val isAiAutoModeEnabled: StateFlow<Boolean>
    val manualProfileOverride: StateFlow<ImageProcessingProfileType?>
    val isAbTestModeEnabled: StateFlow<Boolean>
    val selectedTestScene: StateFlow<com.example.model.TestSceneType>
    val lastAbCaptureSession: StateFlow<com.example.model.AbCaptureSession?>

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onError: (String) -> Unit
    )

    fun switchCamera()
    fun toggleFlash()
    fun setFlashMode(flashRec: FlashRecommendation)
    fun setExposureCompensation(index: Int)
    fun setAiAutoMode(enabled: Boolean)
    fun setManualProfile(profile: ImageProcessingProfileType?)
    fun setAbTestMode(enabled: Boolean)
    fun setSelectedTestScene(scene: com.example.model.TestSceneType)
    fun focusOnPoint(xNorm: Float, yNorm: Float, previewView: PreviewView)
    fun setZoomRatio(ratio: Float)

    fun capturePhoto(
        onSuccess: (CapturedPhoto) -> Unit,
        onError: (String) -> Unit
    )

    fun captureAbTest(
        testScene: com.example.model.TestSceneType,
        onSuccess: (com.example.model.AbCaptureSession) -> Unit,
        onError: (String) -> Unit
    )

    fun release()
}
