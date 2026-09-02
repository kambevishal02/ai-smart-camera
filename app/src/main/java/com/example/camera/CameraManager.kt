package com.example.camera

import android.content.Context
import com.example.ai.CameraDecisionEngine
import com.example.ai.ISceneAnalyzer
import com.example.ai.SceneAnalyzer
import com.example.processing.IImageProcessor
import com.example.processing.LightweightImageEnhancer

/**
 * CameraManager component encapsulating camera controller creation and lifecycle management.
 */
class CameraManager(
    private val context: Context,
    private val sceneAnalyzer: ISceneAnalyzer = SceneAnalyzer(),
    private val decisionEngine: CameraDecisionEngine = CameraDecisionEngine(),
    private val imageProcessor: IImageProcessor = LightweightImageEnhancer()
) {
    val controller: ICameraController = CameraXController(
        context = context,
        sceneAnalyzer = sceneAnalyzer,
        decisionEngine = decisionEngine,
        imageProcessor = imageProcessor
    )
}
