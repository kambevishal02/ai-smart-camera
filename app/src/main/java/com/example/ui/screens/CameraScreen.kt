package com.example.ui.screens

import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.ICameraController
import com.example.model.CapturedPhoto
import com.example.model.ImageProcessingProfileType
import com.example.model.SmartCaptureStatus
import com.example.ui.components.AiPhotoScoreBadge
import com.example.ui.components.AiStatusPanel
import com.example.ui.components.CameraFlipButton
import com.example.ui.components.CameraTopBar
import com.example.ui.components.FaceDetectionOverlay
import com.example.ui.components.GalleryThumbnailButton
import com.example.ui.components.PhotoComparisonDialog
import com.example.ui.components.ProfileSelectorRow
import com.example.ui.components.RuleOfThirdsGrid
import com.example.ui.components.ShutterButton
import com.example.ui.components.SmartAutoModeSwitcher
import com.example.ui.components.SmartCaptureStatusIndicator
import com.example.ui.theme.DarkSurfaceElevated
import com.example.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    cameraController: ICameraController,
    onNavigateToDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val analysisResult by cameraController.analysisResult.collectAsState()
    val recommendation by cameraController.activeRecommendation.collectAsState()
    val hardwareInfo by cameraController.cameraHardwareInfo.collectAsState()
    val isFlashOn by cameraController.isFlashActive.collectAsState()
    val isCapturing by cameraController.isCapturing.collectAsState()
    val isProcessing by cameraController.isProcessing.collectAsState()
    val captureStatus by cameraController.captureStatus.collectAsState()
    val lastPhoto by cameraController.lastCapturedPhoto.collectAsState()
    val evIndex by cameraController.exposureCompensationIndex.collectAsState()
    val isAiAuto by cameraController.isAiAutoModeEnabled.collectAsState()
    val manualProfile by cameraController.manualProfileOverride.collectAsState()

    var isGridOn by remember { mutableStateOf(false) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var viewingPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var showLogsSheet by remember { mutableStateOf(false) }

    val logsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen")
    ) {
        // 1. Live Camera Preview Viewfinder
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    previewViewInstance = this
                    cameraController.startCamera(
                        lifecycleOwner = lifecycleOwner,
                        previewView = this,
                        onError = { err ->
                            Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val view = previewViewInstance ?: return@detectTapGestures
                        if (view.width > 0 && view.height > 0) {
                            val normX = offset.x / view.width
                            val normY = offset.y / view.height
                            cameraController.focusOnPoint(normX, normY, view)
                        }
                    }
                }
                .testTag("camera_preview_view")
        )

        // 2. Rule of Thirds Grid Overlay
        if (isGridOn) {
            RuleOfThirdsGrid()
        }

        // 3. Face Detection Bounding Boxes Overlay
        if (analysisResult.faceCount > 0) {
            FaceDetectionOverlay(faces = analysisResult.faces)
        }

        // 4. Flash White Screen / Capture Pulse Effect
        AnimatedVisibility(
            visible = isCapturing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.6f))
            )
        }

        // 5. Top Controls Bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            CameraTopBar(
                isFlashOn = isFlashOn,
                isGridOn = isGridOn,
                hardwareInfo = hardwareInfo,
                currentEvIndex = evIndex,
                onToggleFlash = { cameraController.toggleFlash() },
                onToggleGrid = { isGridOn = !isGridOn },
                onSetEvIndex = { cameraController.setExposureCompensation(it) },
                onOpenDebugScreen = onNavigateToDebug,
                onOpenLogsSheet = { showLogsSheet = true }
            )
        }

        // 6. Floating AI HUD Overlays
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 70.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Unobtrusive AI Status Panel (Requirement 5 & 6)
            AiStatusPanel(
                analysis = analysisResult,
                recommendation = recommendation,
                isSmartAuto = isAiAuto
            )

            // AI Photo Score Badge
            AiPhotoScoreBadge(
                score = analysisResult.photoScore,
                onClick = {
                    lastPhoto?.let { viewingPhoto = it }
                }
            )
        }

        // 7. Status Pill Indicator (e.g. ANALYZING / READY / PROCESSING / PHOTO SAVED)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 70.dp)
        ) {
            SmartCaptureStatusIndicator(
                status = captureStatus,
                lastQualityScore = lastPhoto?.qualityScore
            )
        }

        // 8. Bottom Controls: Mode Switcher, Profile Selector, Shutter
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prominent SMART AUTO vs AUTO Switcher (Requirement 1 & 4)
            SmartAutoModeSwitcher(
                isSmartAuto = isAiAuto,
                onModeChanged = { enabled ->
                    cameraController.setAiAutoMode(enabled)
                    if (enabled) {
                        cameraController.setManualProfile(null)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scene Profile Horizontal Pill Selector
            ProfileSelectorRow(
                activeProfile = recommendation.imageProcessingProfile,
                manualOverride = manualProfile,
                isAiAuto = isAiAuto,
                onSelectProfile = { profile ->
                    cameraController.setManualProfile(profile)
                    if (profile != null) {
                        cameraController.setAiAutoMode(false)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Controls: Gallery Thumbnail, Shutter Button, Switch Camera Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last Captured Thumbnail
                GalleryThumbnailButton(
                    lastPhoto = lastPhoto,
                    onClick = {
                        lastPhoto?.let { viewingPhoto = it }
                    }
                )

                // Large Shutter Button with Processing animation
                ShutterButton(
                    isCapturing = isCapturing,
                    isProcessing = isProcessing,
                    isSmartAuto = isAiAuto,
                    onClick = {
                        cameraController.capturePhoto(
                            onSuccess = { photo ->
                                viewingPhoto = photo
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )

                // Flip Camera (Front <-> Rear)
                CameraFlipButton(
                    onClick = { cameraController.switchCamera() }
                )
            }
        }

        // 9. Photo Review / Compare Dialog
        viewingPhoto?.let { photo ->
            PhotoComparisonDialog(
                photo = photo,
                onDismiss = { viewingPhoto = null }
            )
        }

        // 10. Quick Logs Bottom Sheet
        if (showLogsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLogsSheet = false },
                sheetState = logsSheetState,
                containerColor = DarkSurfaceElevated
            ) {
                val logs by AppLogger.logs.collectAsState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(16.dp)
                ) {
                    DeveloperDebugScreenLogsContent(logs = logs)
                }
            }
        }
    }
}

@Composable
private fun DeveloperDebugScreenLogsContent(logs: List<AppLogger.LogEntry>) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(logs.size) { index ->
            val log = logs[logs.size - 1 - index]
            Row(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.Text(
                    text = "${log.timestamp} [${log.tag}] ${log.message}",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = com.example.ui.theme.TextPrimary
                )
            }
        }
    }
}
