package com.example.model

/**
 * Resolved hardware settings execution plan.
 * Clearly separates what the AI Decision Engine requested vs. what the physical hardware applied,
 * and records explicit fallback reasons whenever a control is unsupported.
 */
data class ResolvedHardwareSettings(
    val requestedEvIndex: Int,
    val requestedEvOffset: Float,
    val appliedEvIndex: Int,
    val appliedEvOffset: Float,
    val evFallbackReason: String? = null,

    val requestedWhiteBalance: WhiteBalanceRecommendation,
    val appliedWhiteBalance: WhiteBalanceRecommendation,
    val wbFallbackReason: String? = null,

    val requestedZoom: Float,
    val appliedZoom: Float,
    val zoomFallbackReason: String? = null,

    val requestedFlash: FlashRecommendation,
    val appliedFlash: FlashRecommendation,
    val flashFallbackReason: String? = null,

    val requestedShutter: ShutterPreference,
    val appliedShutter: String,
    val shutterFallbackReason: String? = null,

    val requestedIso: IsoPreference,
    val appliedIso: String,
    val isoFallbackReason: String? = null,

    val requestedLens: CameraLensType,
    val appliedLens: CameraLensType,
    val lensFallbackReason: String? = null
) {
    fun toRequestedMap(): Map<String, String> {
        return mapOf(
            "EV Compensation" to "${String.format("%+.2f", requestedEvOffset)} EV (Index $requestedEvIndex)",
            "White Balance" to requestedWhiteBalance.label,
            "Zoom Ratio" to "${String.format("%.1f", requestedZoom)}x",
            "Flash Mode" to requestedFlash.label,
            "Shutter Strategy" to requestedShutter.label,
            "ISO Strategy" to requestedIso.label,
            "Lens Type" to requestedLens.displayName
        )
    }

    fun toAppliedMap(): Map<String, String> {
        return mapOf(
            "EV Compensation" to "${String.format("%+.2f", appliedEvOffset)} EV (Index $appliedEvIndex)",
            "White Balance" to appliedWhiteBalance.label,
            "Zoom Ratio" to "${String.format("%.1f", appliedZoom)}x",
            "Flash Mode" to appliedFlash.label,
            "Shutter Control" to appliedShutter,
            "ISO Sensitivity" to appliedIso,
            "Lens Type" to appliedLens.displayName
        )
    }

    fun toFallbackMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (evFallbackReason != null) map["EV Compensation"] = evFallbackReason
        if (wbFallbackReason != null) map["White Balance"] = wbFallbackReason
        if (zoomFallbackReason != null) map["Zoom Ratio"] = zoomFallbackReason
        if (flashFallbackReason != null) map["Flash"] = flashFallbackReason
        if (shutterFallbackReason != null) map["Shutter"] = shutterFallbackReason
        if (isoFallbackReason != null) map["ISO"] = isoFallbackReason
        if (lensFallbackReason != null) map["Lens"] = lensFallbackReason
        return map
    }
}
