package com.example.model

/**
 * Image enhancement profiles applied after capture.
 * All profiles use conservative adjustments to deliver a "better version of the original scene",
 * strictly avoiding heavy, artificial, or Instagram-style filters.
 */
enum class ImageProcessingProfileType(
    val displayName: String,
    val description: String
) {
    NATURAL("Natural", "True-to-life color, balanced dynamic range, clean midtones"),
    PORTRAIT("Portrait", "Natural skin tone preservation, gentle highlight roll-off, moderate sharpening"),
    LANDSCAPE("Landscape", "Balanced sky & foliage contrast, crisp clarity, natural earth tones"),
    NIGHT("Night", "Shadow detail recovery, highlight protection, chroma noise reduction"),
    LOW_LIGHT("Low Light", "Smooth shadow lift, controlled noise reduction, natural color fidelity"),
    BEACH("Beach", "Sky & sand highlight protection, natural blue tones, shadow preservation"),
    SUNSET("Sunset", "Warm golden-hour shift, highlight roll-off, foreground shadow recovery"),
    FOREST("Forest", "Shadow detail in foliage, natural green tones, canopy highlight protection"),
    FOOD("Food", "Warm appetizing saturation, micro-contrast for rich textures"),
    ARCHITECTURE("Architecture", "Edge clarity, linear contrast, balanced architectural highlights")
}

/**
 * Quantifiable image enhancement parameters.
 */
data class EnhancementParameters(
    val exposureOffset: Float = 0.0f,          // -1.0 to +1.0
    val contrastMultiplier: Float = 1.0f,       // 0.7 to 1.5
    val highlightCompression: Float = 0.0f,     // 0.0 to 1.0 (recovers blown highlights)
    val shadowLift: Float = 0.0f,               // 0.0 to 1.0 (brightens deep shadows)
    val saturationMultiplier: Float = 1.0f,     // 0.5 to 1.8
    val sharpnessStrength: Float = 0.0f,        // 0.0 to 1.0
    val noiseReductionStrength: Float = 0.0f,   // 0.0 to 1.0
    val warmTint: Float = 0.0f,                 // -0.5 (cool) to +0.5 (warm)
    val vibranceBoost: Float = 0.0f             // selective boost for muted colors
) {
    companion object {
        fun defaultForProfile(profile: ImageProcessingProfileType): EnhancementParameters {
            return when (profile) {
                ImageProcessingProfileType.NATURAL -> EnhancementParameters(
                    exposureOffset = 0.03f,
                    contrastMultiplier = 1.03f,
                    highlightCompression = 0.08f,
                    shadowLift = 0.12f,
                    saturationMultiplier = 1.03f,
                    sharpnessStrength = 0.15f,
                    noiseReductionStrength = 0.08f,
                    warmTint = 0.0f,
                    vibranceBoost = 0.06f
                )
                ImageProcessingProfileType.PORTRAIT -> EnhancementParameters(
                    exposureOffset = 0.08f,
                    contrastMultiplier = 0.98f,
                    highlightCompression = 0.18f,
                    shadowLift = 0.20f,
                    saturationMultiplier = 1.02f, // Subtle, natural skin tones
                    sharpnessStrength = 0.14f,     // Moderate sharpening, no harsh facial edges
                    noiseReductionStrength = 0.20f,
                    warmTint = 0.08f,             // Gentle flattering skin warmth
                    vibranceBoost = 0.05f
                )
                ImageProcessingProfileType.NIGHT -> EnhancementParameters(
                    exposureOffset = 0.20f,
                    contrastMultiplier = 1.08f,
                    highlightCompression = 0.35f, // Protect light bulbs/street lamps
                    shadowLift = 0.38f,           // Pull details from deep shadows
                    saturationMultiplier = 1.05f, // Keep natural, avoid oversaturating dark noise
                    sharpnessStrength = 0.15f,
                    noiseReductionStrength = 0.38f, // Clean sensor chroma noise
                    warmTint = 0.03f,
                    vibranceBoost = 0.10f
                )
                ImageProcessingProfileType.LOW_LIGHT -> EnhancementParameters(
                    exposureOffset = 0.14f,
                    contrastMultiplier = 1.04f,
                    highlightCompression = 0.25f,
                    shadowLift = 0.28f,
                    saturationMultiplier = 1.04f,
                    sharpnessStrength = 0.14f,
                    noiseReductionStrength = 0.26f,
                    warmTint = 0.02f,
                    vibranceBoost = 0.08f
                )
                ImageProcessingProfileType.LANDSCAPE -> EnhancementParameters(
                    exposureOffset = 0.0f,
                    contrastMultiplier = 1.10f,
                    highlightCompression = 0.22f, // Protect sky highlights
                    shadowLift = 0.18f,
                    saturationMultiplier = 1.12f, // Natural saturation, not neon
                    sharpnessStrength = 0.25f,
                    noiseReductionStrength = 0.08f,
                    warmTint = -0.02f,            // Crisp clear sky
                    vibranceBoost = 0.15f
                )
                ImageProcessingProfileType.BEACH -> EnhancementParameters(
                    exposureOffset = -0.04f,      // Protect bright sand highlights
                    contrastMultiplier = 1.08f,
                    highlightCompression = 0.30f, // Recover sky and water reflections
                    shadowLift = 0.16f,
                    saturationMultiplier = 1.14f, // Natural blue and cyan vibrance
                    sharpnessStrength = 0.20f,
                    noiseReductionStrength = 0.08f,
                    warmTint = 0.03f,             // Gentle sand warmth
                    vibranceBoost = 0.16f
                )
                ImageProcessingProfileType.SUNSET -> EnhancementParameters(
                    exposureOffset = -0.08f,      // Protect bright sun/horizon without washing out
                    contrastMultiplier = 1.14f,
                    highlightCompression = 0.28f,
                    shadowLift = 0.12f,           // Foreground shadow recovery while keeping silhouettes
                    saturationMultiplier = 1.20f, // Preserve rich warm sunset hues
                    sharpnessStrength = 0.16f,
                    noiseReductionStrength = 0.12f,
                    warmTint = 0.25f,             // Golden hour warmth - do not neutralize
                    vibranceBoost = 0.20f
                )
                ImageProcessingProfileType.FOREST -> EnhancementParameters(
                    exposureOffset = 0.0f,
                    contrastMultiplier = 1.06f,   // Moderate contrast
                    highlightCompression = 0.24f, // Protect sky through canopy
                    shadowLift = 0.24f,           // Shadow detail in deep foliage
                    saturationMultiplier = 1.10f, // Natural foliage green, not artificial
                    sharpnessStrength = 0.22f,
                    noiseReductionStrength = 0.08f,
                    warmTint = 0.02f,
                    vibranceBoost = 0.14f
                )
                ImageProcessingProfileType.FOOD -> EnhancementParameters(
                    exposureOffset = 0.08f,
                    contrastMultiplier = 1.10f,
                    highlightCompression = 0.14f,
                    shadowLift = 0.18f,
                    saturationMultiplier = 1.16f, // Appetizing color
                    sharpnessStrength = 0.24f,    // Crispy texture
                    noiseReductionStrength = 0.10f,
                    warmTint = 0.12f,             // Warm appetizing glow
                    vibranceBoost = 0.15f
                )
                ImageProcessingProfileType.ARCHITECTURE -> EnhancementParameters(
                    exposureOffset = 0.0f,
                    contrastMultiplier = 1.12f,   // Strong linear contrast
                    highlightCompression = 0.26f, // Window/sky highlight protection
                    shadowLift = 0.15f,
                    saturationMultiplier = 1.06f,
                    sharpnessStrength = 0.28f,    // Edge clarity
                    noiseReductionStrength = 0.08f,
                    warmTint = 0.0f,
                    vibranceBoost = 0.10f
                )
            }
        }
    }
}
