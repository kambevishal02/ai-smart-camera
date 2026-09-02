package com.example.model

/**
 * Status of the Smart Auto camera pipeline (Requirement 13).
 */
enum class SmartCaptureStatus(
    val label: String,
    val iconName: String
) {
    ANALYZING("ANALYZING", "search"),
    READY("READY", "check_circle"),
    CAPTURING("CAPTURING", "camera"),
    AB_TESTING("A/B CAPTURING (1/2)", "compare"),
    PROCESSING("PROCESSING", "auto_awesome"),
    SAVED("PHOTO SAVED", "done_all")
}
