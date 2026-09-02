package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.CameraCapabilities
import com.example.model.CameraRecommendation
import com.example.model.SceneAnalysis
import com.example.ui.screens.DeveloperDebugScreen

/**
 * Standard DebugScreen entry point forwarding to the full DeveloperDebugScreen.
 */
@Composable
fun DebugScreen(
    hardwareInfo: CameraCapabilities,
    analysisResult: SceneAnalysis,
    recommendation: CameraRecommendation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    DeveloperDebugScreen(
        hardwareInfo = hardwareInfo,
        analysisResult = analysisResult,
        recommendation = recommendation,
        onBack = onBack,
        modifier = modifier
    )
}
