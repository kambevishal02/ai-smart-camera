package com.example.model

/**
 * High-level abstract ISO sensitivity preference.
 */
enum class IsoPreference(val label: String) {
    AUTO("AUTO"),
    LOW_CLEAN("Low (Clean Base)"),
    BALANCED("Balanced Mid-Range"),
    HIGH_SPEED("High Speed Action"),
    NIGHT_BOOST("High Sensitivity Night")
}

/**
 * High-level abstract shutter speed preference.
 */
enum class ShutterPreference(val label: String) {
    AUTO("AUTO"),
    HIGH_SPEED_FREEZE("High Speed (Freeze Motion)"),
    ACTION_PRIORITY("Action Priority"),
    BALANCED("Balanced Handheld"),
    NIGHT_LONG("Night Extended")
}

/**
 * Platform-independent output of the AI Decision Engine.
 * Formulates recommendations in terms of abstract photography intent,
 * which are subsequently translated by CameraHardwareAdapter to physical device HAL controls.
 */
data class CameraRecommendation(
    val exposureCompensationIndex: Int = 0,
    val exposureCompensationEv: Float = 0.0f,
    val focusStrategy: FocusStrategy = FocusStrategy.AUTO,
    val flashRecommendation: FlashRecommendation = FlashRecommendation.OFF,
    val zoomRecommendation: Float = 1.0f,
    val whiteBalance: WhiteBalanceRecommendation = WhiteBalanceRecommendation.AUTO,
    val isoPreference: IsoPreference = IsoPreference.AUTO,
    val shutterPreference: ShutterPreference = ShutterPreference.AUTO,
    val isoRecommendation: String = "AUTO ISO",
    val shutterRecommendation: String = "AUTO Shutter",
    val imageProcessingProfile: ImageProcessingProfileType = ImageProcessingProfileType.NATURAL,
    val enhancementParams: EnhancementParameters = EnhancementParameters.defaultForProfile(ImageProcessingProfileType.NATURAL),
    val recommendedLensType: CameraLensType = CameraLensType.MAIN_WIDE,
    val recommendedLens: String = "Main Wide Lens",
    val primaryActionText: String = "Optimizing scene exposure",
    val secondaryReasonText: String = "Standard balanced profile applied",
    val confidence: Float = 0.9f,
    val captureIntent: CaptureIntent = CaptureIntent.DEFAULT
)
