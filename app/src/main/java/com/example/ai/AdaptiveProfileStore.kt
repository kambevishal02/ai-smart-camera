package com.example.ai

import com.example.model.AdaptiveCameraProfile
import com.example.model.AdaptiveLearningPolicy
import com.example.model.AdaptiveLearningRecord
import com.example.model.AdaptiveParameterBounds
import com.example.model.AdaptiveParameters
import com.example.model.CameraCapabilities
import com.example.model.DeviceCapabilityLevel
import com.example.model.DeviceProfileIdentifier
import com.example.model.LearningRejectionReason
import com.example.model.LightingCondition
import com.example.model.LightingContextType
import com.example.model.MotionLevel
import com.example.model.TestSceneType
import com.example.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Learning status for developer diagnostics.
 */
enum class AdaptiveLearningStatus(val label: String) {
    ACTIVE("ACTIVE"),
    INSUFFICIENT_DATA("INSUFFICIENT DATA"),
    PAUSED("PAUSED")
}

/**
 * AdaptiveProfileStore (Sections 9, 10, 15).
 *
 * Manages the active AdaptiveCameraProfile, single-step rollback history,
 * full learning records log, and JSON/CSV export formatting.
 * Strictly local, offline, bounded, and reversible.
 */
object AdaptiveProfileStore {

    private val _currentProfile = MutableStateFlow(
        AdaptiveCameraProfile(
            profileId = "adaptive_baseline",
            profileVersion = 1,
            createdTimestamp = System.currentTimeMillis(),
            lastModifiedTimestamp = System.currentTimeMillis(),
            deviceIdentifier = DeviceProfileIdentifier()
        )
    )
    val currentProfile: StateFlow<AdaptiveCameraProfile> = _currentProfile.asStateFlow()

    private val _previousProfile = MutableStateFlow<AdaptiveCameraProfile?>(null)
    val previousProfile: StateFlow<AdaptiveCameraProfile?> = _previousProfile.asStateFlow()

    private val _learningHistory = MutableStateFlow<List<AdaptiveLearningRecord>>(emptyList())
    val learningHistory: StateFlow<List<AdaptiveLearningRecord>> = _learningHistory.asStateFlow()

    private val _isLearningPaused = MutableStateFlow(false)
    val isLearningPaused: StateFlow<Boolean> = _isLearningPaused.asStateFlow()

    // Telemetry counters
    var totalEvaluatedSessions: Int = 0
        private set
    var learningEligibleSessions: Int = 0
        private set
    var rejectedSessions: Int = 0
        private set

    fun initializeWithCapabilities(capabilities: CameraCapabilities) {
        val defaultProfile = AdaptiveCameraProfile.createDefault(capabilities)
        _currentProfile.value = defaultProfile
        _previousProfile.value = null
        AppLogger.i("AdaptiveProfileStore", "Initialized adaptive profile: ${defaultProfile.profileId} for ${capabilities.deviceName}")
    }

    fun getLearningStatus(): AdaptiveLearningStatus {
        if (_isLearningPaused.value) return AdaptiveLearningStatus.PAUSED
        val totalSamples = _currentProfile.value.sceneParameters.values.sumOf { it.sampleCount } +
                _currentProfile.value.globalParameters.sampleCount
        return if (totalSamples >= AdaptiveLearningPolicy.MIN_SAMPLE_COUNT_FOR_UPDATE) {
            AdaptiveLearningStatus.ACTIVE
        } else {
            AdaptiveLearningStatus.INSUFFICIENT_DATA
        }
    }

    fun setLearningPaused(paused: Boolean) {
        _isLearningPaused.value = paused
    }

    /**
     * Appends an audit record to the learning history.
     */
    fun recordLearningEvent(record: AdaptiveLearningRecord) {
        totalEvaluatedSessions++
        if (record.learningDecision == "REJECTED") {
            rejectedSessions++
        } else {
            learningEligibleSessions++
        }

        val updated = _learningHistory.value.toMutableList()
        updated.add(0, record)
        if (updated.size > 200) {
            _learningHistory.value = updated.take(200)
        } else {
            _learningHistory.value = updated
        }
    }

    /**
     * Updates an existing profile safely.
     * Saves the current profile to `previousProfile` enabling single-click rollback.
     */
    fun updateProfile(newProfile: AdaptiveCameraProfile) {
        _previousProfile.value = _currentProfile.value
        _currentProfile.value = newProfile.copy(
            profileVersion = _currentProfile.value.profileVersion + 1,
            lastModifiedTimestamp = System.currentTimeMillis()
        )
        AppLogger.i("AdaptiveProfileStore", "Adaptive profile updated to v${_currentProfile.value.profileVersion}")
    }

    /**
     * Reverts to the previous profile state (Section 10: ROLLBACK SYSTEM).
     */
    fun rollbackLastChange(): Boolean {
        val prev = _previousProfile.value ?: return false
        val rolledBackProfile = prev.copy(
            profileVersion = _currentProfile.value.profileVersion + 1,
            lastModifiedTimestamp = System.currentTimeMillis()
        )
        _currentProfile.value = rolledBackProfile
        _previousProfile.value = null

        val record = AdaptiveLearningRecord(
            deviceProfile = rolledBackProfile.deviceIdentifier.model,
            scene = TestSceneType.DAYLIGHT,
            lighting = LightingCondition.NORMAL,
            lightingContext = LightingContextType.NORMAL,
            motion = MotionLevel.STILL,
            parameterName = "ROLLBACK",
            previousParameter = 0f,
            observedMetric = "User Triggered Rollback",
            calculatedCorrection = 0f,
            newParameter = 0f,
            confidence = 1.0f,
            sampleCount = 0,
            learningDecision = "ROLLBACK",
            rejectionReason = null,
            explanation = "Restored previous profile state v${prev.profileVersion}"
        )
        recordLearningEvent(record)
        AppLogger.i("AdaptiveProfileStore", "Rolled back to previous profile version")
        return true
    }

    /**
     * Resets all learned parameters to baseline (Section 10: RESET ADAPTIVE PROFILE).
     * Strictly preserves historical learning records.
     */
    fun resetToBaseline() {
        val current = _currentProfile.value
        _previousProfile.value = current
        val baseline = AdaptiveCameraProfile(
            profileId = current.profileId,
            profileVersion = current.profileVersion + 1,
            createdTimestamp = current.createdTimestamp,
            lastModifiedTimestamp = System.currentTimeMillis(),
            deviceIdentifier = current.deviceIdentifier,
            globalParameters = AdaptiveParameters.DEFAULT,
            sceneParameters = emptyMap(),
            lightingParameters = emptyMap()
        )
        _currentProfile.value = baseline

        val record = AdaptiveLearningRecord(
            deviceProfile = current.deviceIdentifier.model,
            scene = TestSceneType.DAYLIGHT,
            lighting = LightingCondition.NORMAL,
            lightingContext = LightingContextType.NORMAL,
            motion = MotionLevel.STILL,
            parameterName = "RESET",
            previousParameter = 0f,
            observedMetric = "User Triggered Reset",
            calculatedCorrection = 0f,
            newParameter = 0f,
            confidence = 1.0f,
            sampleCount = 0,
            learningDecision = "RESET",
            rejectionReason = null,
            explanation = "Reset all scene and lighting adaptive parameters to V0.4 baseline"
        )
        recordLearningEvent(record)
        AppLogger.i("AdaptiveProfileStore", "Reset adaptive profile to baseline")
    }

    fun clearHistory() {
        _learningHistory.value = emptyList()
        totalEvaluatedSessions = 0
        learningEligibleSessions = 0
        rejectedSessions = 0
    }

    /**
     * Exports the adaptive profile and learning history to a structured JSON document.
     */
    fun exportToJson(): String {
        val root = JSONObject()
        root.put("reportType", "ADAPTIVE_CAMERA_PROFILE_EXPORT")
        root.put("version", "0.5.0")
        root.put("generatedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("learningStatus", getLearningStatus().name)
        root.put("totalEvaluatedSessions", totalEvaluatedSessions)
        root.put("learningEligibleSessions", learningEligibleSessions)
        root.put("rejectedSessions", rejectedSessions)

        // Active Profile
        root.put("currentProfile", _currentProfile.value.toJsonObject())

        // Previous Profile (if available)
        _previousProfile.value?.let { prev ->
            root.put("previousProfile", prev.toJsonObject())
        }

        // Learning History Records
        val historyArray = JSONArray()
        _learningHistory.value.forEach { record ->
            historyArray.put(record.toJsonObject())
        }
        root.put("learningHistory", historyArray)

        return root.toString(2)
    }

    /**
     * Exports the adaptive learning history to CSV format for external analysis.
     */
    fun exportToCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("RecordID,Timestamp,Device,Scene,Lighting,Motion,Parameter,PrevValue,Correction,NewValue,Confidence,Samples,Decision,RejectionReason,Explanation")
        _learningHistory.value.forEach { r ->
            sb.appendLine(
                "\"${r.id}\",\"${r.formattedTime}\",\"${r.deviceProfile}\",\"${r.scene.name}\",\"${r.lighting.name}\",\"${r.motion.name}\",\"${r.parameterName}\",${String.format(Locale.US, "%.3f", r.previousParameter)},${String.format(Locale.US, "%.3f", r.calculatedCorrection)},${String.format(Locale.US, "%.3f", r.newParameter)},${String.format(Locale.US, "%.2f", r.confidence)},${r.sampleCount},\"${r.learningDecision}\",\"${r.rejectionReason?.name ?: "NONE"}\",\"${r.explanation.replace("\"", "'")}\""
            )
        }
        return sb.toString()
    }
}
