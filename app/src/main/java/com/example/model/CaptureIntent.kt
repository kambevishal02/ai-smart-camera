package com.example.model

/**
 * Level of highlight protection required for the capture.
 * Influenced by scene highlights, sky, bright windows, and dynamic range.
 */
enum class HighlightProtectionLevel(val label: String) {
    NONE("NONE"),
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH")
}

/**
 * Low-light / shadow recovery strategy.
 * Prevents aggressive noisy brightening while recovering shadow detail when appropriate.
 */
enum class LowLightStrategy(val label: String) {
    BALANCED("Balanced"),
    LOW_NOISE("Low Noise (Preserve Dark Floor)"),
    SHADOW_RECOVERY("Shadow Recovery")
}

/**
 * Motion freezing and shutter priority strategy.
 */
enum class MotionStrategy(val label: String) {
    STATIC("Static (Optimize Quality & Base ISO)"),
    LOW_MOTION("Low Motion (Handheld Stability)"),
    MEDIUM_MOTION("Medium Motion (Balanced Freeze)"),
    HIGH_MOTION("High Motion (Prioritize Fast Shutter)")
}

/**
 * Face exposure priority mode.
 */
enum class FacePriorityMode(val label: String) {
    NONE("No Face Priority"),
    BALANCED("Balanced Scene & Face"),
    PRIORITIZE_FACE("Face Priority (Compensate Backlight)"),
    EXTREME_BACKLIGHT("Harsh Backlight Face Rescue")
}

/**
 * CaptureIntent represents the abstract photographic decisions produced by the Smart AI Decision Engine.
 * Decoupled from Android Camera2 / CameraX HAL objects.
 *
 * Flow:
 * Scene / Lighting / Subject / Motion / Capabilities / Calibration
 *                        ↓
 *               SmartExposureEngine
 *                        ↓
 *                  CaptureIntent
 *                        ↓
 *              CameraHardwareAdapter
 *                        ↓
 *             Supported Hardware Controls
 */
data class CaptureIntent(
    val preferredExposureCompensation: Float = 0.0f,
    val exposurePriority: String = "Balanced Subject & Background",
    val highlightProtection: HighlightProtectionLevel = HighlightProtectionLevel.NONE,
    val shadowPriority: LowLightStrategy = LowLightStrategy.BALANCED,
    val facePriority: FacePriorityMode = FacePriorityMode.NONE,
    val motionPriority: MotionStrategy = MotionStrategy.STATIC,
    val preferredWhiteBalance: WhiteBalanceRecommendation = WhiteBalanceRecommendation.AUTO,
    val preferredLens: CameraLensType = CameraLensType.MAIN_WIDE,
    val preferredZoom: Float = 1.0f,
    val flashPreference: FlashRecommendation = FlashRecommendation.OFF,
    val stabilizationPreference: String = "Auto Stabilization",
    val processingProfile: ImageProcessingProfileType = ImageProcessingProfileType.NATURAL,
    val enhancementParams: EnhancementParameters = EnhancementParameters.defaultForProfile(ImageProcessingProfileType.NATURAL),
    val confidence: Float = 0.85f,
    val reasoning: String = "Standard balanced exposure",
    val requestedSettings: Map<String, String> = emptyMap(),
    val appliedSettings: Map<String, String> = emptyMap(),
    val fallbackSettings: Map<String, String> = emptyMap()
) {
    companion object {
        val DEFAULT = CaptureIntent()
    }
}
