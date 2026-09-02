package com.example.model

/**
 * Scene categories classified by the AI scene analysis engine.
 * Initial implementation uses local heuristics and image characteristics,
 * designed to be seamlessly replaced with an on-device ML model (e.g., TFLite / MediaPipe).
 */
enum class SceneType(val displayName: String, val iconName: String) {
    DAYLIGHT("Daylight", "wb_sunny"),
    LOW_LIGHT("Low Light", "brightness_low"),
    NIGHT("Night", "nightlight_round"),
    INDOOR("Indoor", "home"),
    SUNSET("Sunset", "wb_twilight"),
    FOREST_NATURE("Forest / Nature", "park"),
    BEACH("Beach", "beach_access"),
    PORTRAIT("Portrait", "face"),
    FOOD("Food", "restaurant"),
    ARCHITECTURE("Architecture", "apartment"),
    UNKNOWN("Unknown", "help_outline")
}
