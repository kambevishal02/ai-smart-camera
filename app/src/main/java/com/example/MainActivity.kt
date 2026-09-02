package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.camera.CameraXController
import com.example.camera.ICameraController
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.DeveloperDebugScreen
import com.example.ui.screens.PermissionRationaleScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    CAMERA,
    DEBUG
}

class MainActivity : ComponentActivity() {

    private var cameraController: ICameraController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val controller = CameraXController(applicationContext)
        cameraController = controller

        setContent {
            MyApplicationTheme {
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasCameraPermission = isGranted
                }

                LaunchedEffect(Unit) {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                if (hasCameraPermission) {
                    var currentScreen by remember { mutableStateOf(ScreenState.CAMERA) }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_nav_transition"
                    ) { screen ->
                        when (screen) {
                            ScreenState.CAMERA -> {
                                CameraScreen(
                                    cameraController = controller,
                                    onNavigateToDebug = { currentScreen = ScreenState.DEBUG }
                                )
                            }
                            ScreenState.DEBUG -> {
                                val hardwareInfo by controller.cameraHardwareInfo.collectAsState()
                                val analysisResult by controller.analysisResult.collectAsState()
                                val recommendation by controller.activeRecommendation.collectAsState()

                                DeveloperDebugScreen(
                                    hardwareInfo = hardwareInfo,
                                    analysisResult = analysisResult,
                                    recommendation = recommendation,
                                    onBack = { currentScreen = ScreenState.CAMERA }
                                )
                            }
                        }
                    }
                } else {
                    PermissionRationaleScreen(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraController?.release()
    }
}
