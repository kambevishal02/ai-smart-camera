package com.example.model

/**
 * Lighting condition classifications determined by the LightingAnalyzer.
 */
enum class LightingCondition(val label: String) {
    VERY_DARK("VERY_DARK"),
    DARK("DARK"),
    NORMAL("NORMAL"),
    BRIGHT("BRIGHT"),
    VERY_BRIGHT("VERY_BRIGHT")
}

/**
 * Motion level detected across preview frames.
 */
enum class MotionLevel(val label: String) {
    STILL("Still (Stable)"),
    LOW("Slight Movement"),
    MODERATE("Moderate Motion"),
    HIGH("Fast Motion / Shake")
}

/**
 * Recommended camera autofocus strategy.
 */
enum class FocusStrategy(val label: String) {
    AUTO("Continuous Auto"),
    FACE_PRIORITY("Face Priority AF"),
    MACRO_CLOSE_UP("Close-up / Macro AF"),
    INFINITY_LANDSCAPE("Infinity Landscape AF"),
    CONTINUOUS_TRACKING("Subject Tracking AF")
}

/**
 * White balance recommendation.
 */
enum class WhiteBalanceRecommendation(val label: String) {
    AUTO("AUTO"),
    DAYLIGHT("Daylight (5500K)"),
    CLOUDY("Cloudy (6500K)"),
    SHADE("Shade (7500K)"),
    TUNGSTEN_WARM("Tungsten Warm (3200K)"),
    FLUORESCENT("Fluorescent (4000K)")
}

/**
 * Recommended flash behavior.
 */
enum class FlashRecommendation(val label: String) {
    OFF("Flash Off"),
    AUTO("Flash Auto"),
    ON("Flash Fill"),
    FILL_TORCH("Torch Assist")
}
