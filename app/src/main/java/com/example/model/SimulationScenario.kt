package com.example.model

/**
 * 15 comprehensive simulation test scenarios covering all lighting, dynamic range,
 * motion, subject, and color temperature edge cases.
 */
enum class SimulationScenario(val displayName: String, val category: String) {
    // Standard lighting
    DAYLIGHT_CLEAR("Daylight Clear", "Standard"),
    INDOOR_OFFICE("Indoor Office", "Standard"),

    // Highlight & Dynamic Range
    HARSH_BACKLIGHT("Harsh Backlight", "Dynamic Range"),
    BLOWN_SKY_LANDSCAPE("Blown Sky Landscape", "Dynamic Range"),
    SUNSET_DYNAMIC_RANGE("Sunset High Contrast", "Dynamic Range"),
    BEACH_HIGH_ALBEDO("Bright Beach Sand", "Dynamic Range"),

    // Shadow & Low Light
    LOW_LIGHT_INDOOR("Low Light Indoor", "Low Light"),
    EXTREME_NIGHT_STATIC("Extreme Night (Static)", "Low Light"),
    HIGH_CONTRAST_NIGHT_WINDOW("Night with Bright Window", "Low Light"),

    // Face & Subject
    PORTRAIT_SOFT_LIGHT("Portrait (Soft Studio)", "Portrait"),
    BACKLIT_PORTRAIT_FACE("Backlit Portrait (Dark Face)", "Portrait"),
    GROUP_PORTRAIT("Group Portrait (3 Faces)", "Portrait"),

    // Motion & Action
    RUNNING_CHILD_HIGH_MOTION("Fast Motion Action", "Motion"),
    HANDHELD_JITTER_LOW_LIGHT("Handheld Jitter in Dim Light", "Motion"),

    // Extreme Color Temp
    WARM_CANDLELIGHT_INDOOR("Warm Candlelight (2200K)", "Color Temp")
}
