package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.example.ai.AdaptiveIntelligenceEngine
import com.example.ai.CalibrationEngine
import com.example.ai.CameraDecisionEngine
import com.example.camera.CameraHardwareAdapter
import com.example.model.AbCaptureSession
import com.example.model.AbTestStore
import com.example.model.CalibrationParameters
import com.example.model.CalibrationProfilesRepository
import com.example.model.CameraCapabilities
import com.example.model.CameraRecommendation
import com.example.model.CapabilityStatus
import com.example.model.DeviceCalibrationProfile
import com.example.model.LightingAnalysis
import com.example.model.LightingCondition
import com.example.model.MotionAnalysis
import com.example.model.MotionLevel
import com.example.model.PhotoQualityScore
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SimulationScenario
import com.example.model.SimulationScenariosProvider
import com.example.model.SubjectAnalysis
import com.example.model.TestSceneType
import com.example.ui.components.AbComparisonDialog
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HighAlertRed
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.AppLogger
import java.util.Locale

/**
 * Generic Android AI Smart Camera Developer & Diagnostics Screen.
 * Provides dynamic hardware discovery, compatibility checklists, AI scene telemetry,
 * recommendation audits, test simulations, and live structured event logs.
 */
@Composable
fun DeveloperDebugScreen(
    hardwareInfo: CameraCapabilities,
    analysisResult: SceneAnalysis,
    recommendation: CameraRecommendation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val logs by AppLogger.logs.collectAsState()
    val metadataLogs by com.example.model.DeveloperMetadataStore.metadataLogs.collectAsState()
    val abSessions by AbTestStore.sessions.collectAsState()
    val testedScenes by AbTestStore.testedScenes.collectAsState()
    var selectedAbSessionForDialog by remember { mutableStateOf<AbCaptureSession?>(null) }

    val tabs = listOf(
        "📱 Compatibility",
        "📷 Camera Hardware",
        "🧠 Live Scene",
        "⚡ Recommendation",
        "🔬 A/B Testing (${abSessions.size})",
        "⚙️ Calibration",
        "🧬 Adaptive Intelligence",
        "📸 Captures (${metadataLogs.size})",
        "🧪 Test Simulation",
        "📝 Logs (${logs.size})"
    )

    if (selectedAbSessionForDialog != null) {
        AbComparisonDialog(
            session = selectedAbSessionForDialog!!,
            onDismiss = { selectedAbSessionForDialog = null },
            onOpenCalibration = {
                selectedAbSessionForDialog = null
                selectedTab = 5 // Switch to Calibration tab
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("developer_debug_screen"),
        containerColor = DarkBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("debug_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Camera",
                                tint = CyberCyan
                            )
                        }
                        Column {
                            Text(
                                text = "Camera Developer Suite",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "${hardwareInfo.deviceName} • ${hardwareInfo.hardwareLevel.label} HAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    // Copy Diagnostic Report Button
                    IconButton(
                        onClick = {
                            val report = buildDiagnosticReport(hardwareInfo, analysisResult, recommendation)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Camera Diagnostics", report))
                            Toast.makeText(context, "Diagnostics report copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Report",
                            tint = CyberCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = CyberCyan,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyberCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == index) CyberCyan else TextSecondary
                                )
                            },
                            modifier = Modifier.testTag("debug_tab_$index")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DeviceCompatibilityTab(hardwareInfo, analysisResult)
                1 -> CameraHardwareTab(hardwareInfo)
                2 -> LiveSceneTab(analysisResult)
                3 -> EngineDecisionsTab(recommendation, analysisResult, hardwareInfo)
                4 -> AbTestingStudioTab(
                    abSessions = abSessions,
                    testedScenes = testedScenes,
                    onSelectSession = { selectedAbSessionForDialog = it }
                )
                5 -> ParameterCalibrationTab()
                6 -> AdaptiveIntelligenceTab(hardwareInfo)
                7 -> CapturedMetadataTab(metadataLogs)
                8 -> SimulationTestTab(hardwareInfo)
                9 -> LogsTab(logs)
            }
        }
    }
}

/**
 * Requirement 8: Device Compatibility Screen
 * Displays dynamic device specs, HAL level, supported features checklist,
 * and unavailable features dynamically queried from Android Camera2 APIs.
 */
@Composable
private fun DeviceCompatibilityTab(
    hardware: CameraCapabilities,
    analysis: SceneAnalysis
) {
    val auditMap = remember(hardware) { CameraHardwareAdapter.getHardwareFeatureAudit(hardware) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. DEVICE IDENTIFICATION
        item {
            DebugCard(title = "DEVICE & PLATFORM SPECIFICATION", icon = Icons.Default.Devices) {
                DebugItem("Manufacturer", hardware.manufacturer)
                DebugItem("Model", hardware.model)
                DebugItem("Android Version", hardware.androidVersion)
                DebugItem("Camera API Layer", hardware.cameraApiVersion)
                DebugItem("Camera2 Hardware Level", "${hardware.hardwareLevel.label} (${hardware.hardwareLevel.description})")
                DebugItem("Total Discovered Cameras", "${hardware.availableCameraIds.size} (${hardware.availableCameraIds.joinToString(", ")})")
                DebugItem("Active Camera Facing", "${hardware.lensFacingName} (Lens Type: ${hardware.activeLensType.displayName})")
            }
        }

        // 2. MULTI-CAMERA PHYSICAL LENSES
        if (hardware.physicalLenses.isNotEmpty()) {
            item {
                DebugCard(title = "MULTI-CAMERA HARDWARE DISCOVERY", icon = Icons.Default.Camera) {
                    hardware.physicalLenses.forEach { lens ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.55f)) {
                                Text(
                                    text = "Camera ID ${lens.cameraId} (${lens.lensType.displayName})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                val focalStr = if (lens.focalLengths.isNotEmpty()) {
                                    "${lens.focalLengths.joinToString("/") { String.format(Locale.US, "%.1fmm", it) }}" +
                                    (lens.equivalentFocalLength35mm?.let { " (${String.format(Locale.US, "%.0fmm", it)} 35mm eq)" } ?: "")
                                } else "Fixed"
                                Text(
                                    text = "Focal: $focalStr • Aperture: ${lens.maxAperture?.let { "f/$it" } ?: "Auto"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = lens.maxResolution,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                color = CyberCyan,
                                modifier = Modifier.weight(0.45f)
                            )
                        }
                    }
                }
            }
        }

        // 3. SUPPORTED HARDWARE CAPABILITIES (Dynamic Checklist)
        item {
            DebugCard(title = "SUPPORTED CAPABILITIES (LIVE AUDIT)", icon = Icons.Default.Check) {
                val supportedFeatures = auditMap.filter { it.value == CapabilityStatus.SUPPORTED }
                supportedFeatures.forEach { (feature, status) ->
                    VerificationItem(feature, true, status.label)
                }
            }
        }

        // 4. UNAVAILABLE CAPABILITIES & FALLBACK BEHAVIOR
        item {
            DebugCard(title = "UNAVAILABLE ON THIS DEVICE (AUTO FALLBACKS)", icon = Icons.Default.Memory) {
                val unavailableFeatures = auditMap.filter { it.value == CapabilityStatus.UNSUPPORTED }
                if (unavailableFeatures.isEmpty()) {
                    Text(
                        text = "All manual sensor and pipeline controls are supported on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSuccess
                    )
                } else {
                    unavailableFeatures.forEach { (feature, status) ->
                        val fallbackDesc = when (feature) {
                            "Manual ISO Sensitivity" -> "Auto-managed by Camera ISP"
                            "Manual Shutter Exposure" -> "Auto-managed by Camera ISP"
                            "Manual White Balance" -> "AWB Auto / Presets active"
                            "Manual Focus Distance" -> "Continuous Autofocus (CAF)"
                            "RAW Image Output" -> "Direct JPEG Zero-Lag Pipeline"
                            "Manual Sensor Capabilities" -> "Hardware AE / Auto Exposure"
                            else -> "Gracefully degraded"
                        }
                        VerificationItem(feature, false, "$fallbackDesc (${status.label})")
                    }
                }
            }
        }

        // 5. PRIVACY & ARCHITECTURE GUARANTEES
        item {
            DebugCard(title = "OFFLINE & PRIVACY GUARANTEES", icon = Icons.Default.BugReport) {
                VerificationItem("100% On-Device Processing", true, "Zero network or cloud connectivity required")
                VerificationItem("Frame Privacy Protection", true, "No camera frames uploaded to cloud")
                VerificationItem("Local ML Models", true, "Google ML Kit offline face & composition inference")
                VerificationItem("Platform-Independent AI", true, "Clean decoupled domain models ready for Android/iOS")
            }
        }
    }
}

@Composable
private fun VerificationItem(
    title: String,
    isSupported: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(0.48f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSupported) "✓ " else "✗ ",
                color = if (isSupported) EmeraldSuccess else CrimsonAlert,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSupported) FontWeight.Normal else FontWeight.Bold
            ),
            color = if (isSupported) TextSecondary else CrimsonAlert,
            modifier = Modifier.weight(0.52f)
        )
    }
}

/**
 * Camera Hardware Tab:
 * Sensor Resolution, Zoom, Flash support, Exposure compensation range, ISO support, Shutter support, White balance support
 */
@Composable
private fun CameraHardwareTab(hardware: CameraCapabilities) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DebugCard(title = "Sensor & Optical Characteristics", icon = Icons.Default.Camera) {
                DebugItem("Active Camera ID", hardware.activeCameraId)
                DebugItem("Lens Facing", hardware.lensFacingName)
                DebugItem("Lens Classification", hardware.activeLensType.displayName)
                DebugItem("HAL Support Level", "${hardware.hardwareLevel.label} (${hardware.hardwareLevel.description})")
                DebugItem("Max Sensor Resolution", hardware.sensorResolution)
                DebugItem("Supported Resolutions", hardware.supportedResolutions.joinToString(", "))
                DebugItem("Zoom Max Ratio", "${hardware.maxZoomRatio}x Digital")
                DebugItem("Flash Unit", if (hardware.isFlashSupported) "SUPPORTED" else "UNSUPPORTED")
            }
        }

        item {
            DebugCard(title = "Exposure & Manual Sensor Controls", icon = Icons.Default.Speed) {
                val evMinEv = hardware.evRangeMin * hardware.evStep
                val evMaxEv = hardware.evRangeMax * hardware.evStep
                DebugItem(
                    "Exposure Compensation Range",
                    if (hardware.isEvCompensationSupported) "[${hardware.evRangeMin}..+${hardware.evRangeMax}] (${String.format(Locale.US, "%.2f", evMinEv)} to +${String.format(Locale.US, "%.2f", evMaxEv)} EV)" else "UNSUPPORTED"
                )
                DebugItem("EV Step Size", if (hardware.isEvCompensationSupported) "${String.format(Locale.US, "%.4f", hardware.evStep)} EV" else "N/A")
                DebugItem("ISO Sensitivity Range", if (hardware.isManualIsoSupported) hardware.isoRange else "AUTO ONLY (${hardware.isoRange})")
                DebugItem("Shutter Exposure Range", if (hardware.isManualShutterSupported) hardware.shutterRange else "AUTO ONLY (${hardware.shutterRange})")
                DebugItem("White Balance Modes", if (hardware.supportedAwbModes.isNotEmpty()) hardware.supportedAwbModes.joinToString(", ") else "AWB_AUTO ONLY")
                DebugItem("Supported FPS Ranges", if (hardware.supportedFpsRanges.isNotEmpty()) hardware.supportedFpsRanges.joinToString(", ") else "Standard 30fps")
            }
        }

        item {
            DebugCard(title = "Available Camera2 Modes", icon = Icons.Default.Memory) {
                DebugItem("Supported Focus Modes", hardware.supportedFocusModes.joinToString(", "))
                DebugItem("Supported AE Modes", hardware.supportedExposureModes.joinToString(", "))
                if (hardware.physicalCameraIds.isNotEmpty()) {
                    DebugItem("Physical Camera IDs", hardware.physicalCameraIds.joinToString(", "))
                }
            }
        }
    }
}

/**
 * Live Scene Tab:
 * Lighting, Brightness, Faces, Person detected, Motion estimate, Confidence
 */
@Composable
private fun LiveSceneTab(analysis: SceneAnalysis) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DebugCard(title = "Live Scene Telemetry", icon = Icons.Default.Psychology) {
                DebugItem("Detected Scene", analysis.scene.displayName)
                DebugItem("Confidence", "${(analysis.confidence * 100).toInt()}%")
                DebugItem("Lighting Condition", analysis.lighting.condition.label)
                DebugItem("Brightness", "${String.format(Locale.US, "%.1f", analysis.lighting.brightness)} / 100")
                DebugItem("Darkness", "${String.format(Locale.US, "%.1f", analysis.lighting.darkness)} / 100")
                DebugItem("Contrast", "${String.format(Locale.US, "%.1f", analysis.lighting.contrast)} / 100")
                DebugItem("Highlight Clipping", "${String.format(Locale.US, "%.1f", analysis.lighting.highlightClipping)}%")
                DebugItem("Shadow Level", "${String.format(Locale.US, "%.1f", analysis.lighting.shadowLevel)}%")
                DebugItem("Faces Detected (ML Kit)", "${analysis.subject.numberOfFaces}")
                DebugItem("Person Present", if (analysis.subject.isPersonPresent) "YES" else "NO")
                DebugItem("Subject Size", analysis.subject.approximateSubjectSize)
                DebugItem("Likely Portrait", if (analysis.subject.isLikelyPortrait) "YES" else "NO")
                DebugItem("Motion Estimate", "${analysis.motion.motionLevel.label} (${String.format(Locale.US, "%.2f", analysis.motion.motionScore)})")
                DebugItem("Motion Blur Risk", if (analysis.motion.isBlurRisk) "HIGH (Hold Steady)" else "LOW / SAFE")
                DebugItem("Photo Quality Score", "${analysis.photoQuality.totalScore}/100 (${analysis.photoQuality.ratingLabel})")
            }
        }

        if (analysis.subject.isPersonPresent && analysis.subject.detectedFaces.isNotEmpty()) {
            item {
                DebugCard(title = "Face Exposure Telemetry (V0.4)", icon = Icons.Default.Camera) {
                    DebugItem("Primary Face Brightness", "${String.format(Locale.US, "%.1f", analysis.subject.primaryFaceBrightness)}%")
                    DebugItem("Face Relative Exposure", "${String.format(Locale.US, "%+.1f", analysis.subject.primaryFaceExposureRelativeToScene)}% vs Scene")
                    DebugItem("Face Highlight Clipping", "${String.format(Locale.US, "%.1f", analysis.subject.primaryFaceClipping)}%")
                    DebugItem("Face Shadow Ratio", "${String.format(Locale.US, "%.1f", analysis.subject.primaryFaceShadowLevel)}%")
                    DebugItem("Face Count", "${analysis.subject.numberOfFaces}")
                    analysis.subject.detectedFaces.forEachIndexed { idx, face ->
                        DebugItem("Face #${idx + 1} Bounds", "[${String.format(Locale.US, "%.2f", face.bounds.left)}, ${String.format(Locale.US, "%.2f", face.bounds.top)}] (${String.format(Locale.US, "%.1f", face.faceBrightness)}% Luma)")
                    }
                }
            }
        }
    }
}

/**
 * Recommendation & Engine Decisions Tab:
 * Displays abstract AI preferences alongside hardware-adapted implementations.
 */
@Composable
private fun EngineDecisionsTab(
    recommendation: CameraRecommendation,
    analysis: SceneAnalysis,
    hardware: CameraCapabilities
) {
    val resolvedSettings = remember(recommendation.captureIntent, hardware) {
        CameraHardwareAdapter.resolveIntent(recommendation.captureIntent, hardware)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // V0.4 "Why SMART AUTO?" Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.5.dp, CyberCyan)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Why SMART AUTO? (AI Intent Explanation)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recommendation.captureIntent.reasoning,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val intent = recommendation.captureIntent
                    DebugItem("Target Strategy", intent.exposurePriority)
                    DebugItem("Highlight Protection", intent.highlightProtection.label)
                    DebugItem("Shadow Priority", intent.shadowPriority.label)
                    DebugItem("Face Priority Mode", intent.facePriority.label)
                    DebugItem("Motion Priority", intent.motionPriority.label)
                    DebugItem("Target Sensor EV Offset", "${String.format(Locale.US, "%+.2f", intent.preferredExposureCompensation)} EV")
                }
            }
        }

        // V0.5 "Why did SMART AUTO change?" Adaptive Explanation Panel
        item {
            val lastAdaptiveExplanation by AdaptiveIntelligenceEngine.lastExplanation.collectAsState()
            lastAdaptiveExplanation?.let { exp ->
                DebugCard(title = "Why did SMART AUTO change? (Adaptive Intelligence)", icon = Icons.Default.Psychology) {
                    DebugItem("Scene Context", exp.scene)
                    DebugItem("Base Intent EV Target", "${String.format(Locale.US, "%+.2f", exp.baseEv)} EV")
                    DebugItem("Learned Adaptive Bias", "${String.format(Locale.US, "%+.2f", exp.adaptiveBias)} EV")
                    DebugItem("Final Adjusted EV", "${String.format(Locale.US, "%+.2f", exp.finalEv)} EV")
                    DebugItem("Valid A/B Evidence", "${exp.evidenceSamples} test samples")
                    DebugItem("Statistical Confidence", "${exp.confidencePct}%")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Empirical Rationale: ${exp.reason}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = ElectricGold
                    )
                }
            }
        }

        // V0.4 Hardware Resolution & Fallback Audit
        item {
            DebugCard(title = "Hardware Translation & Fallback Audit", icon = Icons.Default.Tune) {
                DebugItem("Hardware Device", "${hardware.deviceName} (${hardware.hardwareLevel.label})")
                DebugItem("Requested EV Index", "${resolvedSettings.requestedEvIndex} (${String.format(Locale.US, "%+.2f", resolvedSettings.requestedEvOffset)} EV)")
                DebugItem("Applied EV Index", "${resolvedSettings.appliedEvIndex} (${String.format(Locale.US, "%+.2f", resolvedSettings.appliedEvOffset)} EV)")
                DebugItem("Requested Zoom", "${resolvedSettings.requestedZoom}x")
                DebugItem("Applied Zoom", "${resolvedSettings.appliedZoom}x")
                DebugItem("Flash Control", "${resolvedSettings.requestedFlash.label} → ${resolvedSettings.appliedFlash.label}")

                if (resolvedSettings.toFallbackMap().isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active Hardware Fallbacks / Graceful Mitigations:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ElectricGold
                    )
                    resolvedSettings.toFallbackMap().forEach { (k, v) ->
                        DebugItem("⚠ $k", v)
                    }
                } else {
                    DebugItem("HAL Status", "All requested controls natively supported")
                }
            }
        }

        item {
            DebugCard(title = "AI Decision & Recommendations", icon = Icons.Default.Speed) {
                DebugItem("Primary Action", recommendation.primaryActionText)
                DebugItem("Reasoning", recommendation.secondaryReasonText)
                DebugItem("Enhancement Profile", recommendation.imageProcessingProfile.displayName)
                DebugItem("Recommended Lens", "${recommendation.recommendedLensType.displayName} (${recommendation.recommendedLens})")
                DebugItem("Target Exposure (EV)", "${recommendation.exposureCompensationIndex} index (${String.format(Locale.US, "%.2f", recommendation.exposureCompensationEv)} EV)")
                DebugItem("Focus Strategy", recommendation.focusStrategy.label)
                DebugItem("Flash Recommendation", recommendation.flashRecommendation.label)
                DebugItem("White Balance", recommendation.whiteBalance.label)
                DebugItem("ISO Intent", "${recommendation.isoPreference.label} → ${recommendation.isoRecommendation}")
                DebugItem("Shutter Intent", "${recommendation.shutterPreference.label} → ${recommendation.shutterRecommendation}")
                DebugItem("Zoom Multiplier", "${recommendation.zoomRecommendation}x")
            }
        }

        item {
            DebugCard(title = "Post-Capture Enhancement Parameters", icon = Icons.Default.Science) {
                val p = recommendation.enhancementParams
                DebugItem("Exposure Offset", "${String.format(Locale.US, "%+.2f", p.exposureOffset)} EV")
                DebugItem("Contrast Multiplier", "${String.format(Locale.US, "%.2f", p.contrastMultiplier)}x")
                DebugItem("Highlight Compression", "${(p.highlightCompression * 100).toInt()}%")
                DebugItem("Shadow Lift", "${(p.shadowLift * 100).toInt()}%")
                DebugItem("Saturation Multiplier", "${String.format(Locale.US, "%.2f", p.saturationMultiplier)}x")
                DebugItem("Sharpness Strength", "${(p.sharpnessStrength * 100).toInt()}%")
                DebugItem("Noise Reduction", "${(p.noiseReductionStrength * 100).toInt()}%")
                DebugItem("Warm Tint Shift", "${String.format(Locale.US, "%+.2f", p.warmTint)}")
                DebugItem("Vibrance Boost", "${(p.vibranceBoost * 100).toInt()}%")
            }
        }
    }
}

/**
 * Test Simulation Suite Tab (V0.4):
 * Simulates all 15 photography scenarios against device capabilities offline,
 * showing the CaptureIntent, Why SMART AUTO reasoning, and Hardware Translation fallback audit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimulationTestTab(hardwareInfo: CameraCapabilities) {
    var selectedScenario by remember { mutableStateOf(SimulationScenario.HARSH_BACKLIGHT) }
    val decisionEngine = remember { CameraDecisionEngine() }

    val simulatedAnalysis = remember(selectedScenario) {
        SimulationScenariosProvider.getAnalysisForScenario(selectedScenario)
    }

    val simulatedRec = remember(simulatedAnalysis, hardwareInfo) {
        decisionEngine.evaluate(simulatedAnalysis, hardwareInfo)
    }

    val simulatedResolvedSettings = remember(simulatedRec.captureIntent, hardwareInfo) {
        CameraHardwareAdapter.resolveIntent(simulatedRec.captureIntent, hardwareInfo)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = ElectricGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "15 Test Simulation Scenarios (V0.4)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricGold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simulates edge cases across dynamic range, backlighting, faces, motion, and color temp. Verifies CaptureIntent and hardware adaptation offline.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SimulationScenario.values().forEach { scenario ->
                            FilterChip(
                                selected = selectedScenario == scenario,
                                onClick = { selectedScenario = scenario },
                                label = { Text(scenario.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.5.dp, CyberCyan)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Why SMART AUTO? (${selectedScenario.displayName})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = simulatedRec.captureIntent.reasoning,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val intent = simulatedRec.captureIntent
                    DebugItem("Target Strategy", intent.exposurePriority)
                    DebugItem("Highlight Protection", intent.highlightProtection.label)
                    DebugItem("Shadow Priority", intent.shadowPriority.label)
                    DebugItem("Face Priority Mode", intent.facePriority.label)
                    DebugItem("Motion Priority", intent.motionPriority.label)
                    DebugItem("Target Sensor EV Offset", "${String.format(Locale.US, "%+.2f", intent.preferredExposureCompensation)} EV")
                }
            }
        }

        item {
            DebugCard(title = "Hardware Translation & Fallback Audit", icon = Icons.Default.Tune) {
                DebugItem("Hardware Device", "${hardwareInfo.deviceName} (${hardwareInfo.hardwareLevel.label})")
                DebugItem("Requested EV Index", "${simulatedResolvedSettings.requestedEvIndex} (${String.format(Locale.US, "%+.2f", simulatedResolvedSettings.requestedEvOffset)} EV)")
                DebugItem("Applied EV Index", "${simulatedResolvedSettings.appliedEvIndex} (${String.format(Locale.US, "%+.2f", simulatedResolvedSettings.appliedEvOffset)} EV)")
                DebugItem("Flash Control", "${simulatedResolvedSettings.requestedFlash.label} → ${simulatedResolvedSettings.appliedFlash.label}")

                if (simulatedResolvedSettings.toFallbackMap().isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hardware Fallbacks / Graceful Mitigations:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CrimsonAlert
                    )
                    simulatedResolvedSettings.toFallbackMap().forEach { (k, v) ->
                        DebugItem("⚠ $k", v)
                    }
                } else {
                    DebugItem("HAL Status", "All requested controls natively supported")
                }
            }
        }

        item {
            DebugCard(title = "Simulated Scene Metrics (${selectedScenario.displayName})", icon = Icons.Default.Psychology) {
                DebugItem("Lighting Condition", simulatedAnalysis.lighting.condition.label)
                DebugItem("Luma Brightness", "${simulatedAnalysis.lighting.brightness}/100")
                DebugItem("Contrast", "${simulatedAnalysis.lighting.contrast}/100")
                DebugItem("Highlight Clipping", "${simulatedAnalysis.lighting.highlightClipping}%")
                DebugItem("Shadow Level", "${simulatedAnalysis.lighting.shadowLevel}%")
                DebugItem("Faces Detected", "${simulatedAnalysis.subject.numberOfFaces}")
                if (simulatedAnalysis.subject.numberOfFaces > 0) {
                    DebugItem("Face Brightness", "${String.format(Locale.US, "%.1f", simulatedAnalysis.subject.primaryFaceBrightness)}% Luma")
                    DebugItem("Face vs Scene Offset", "${String.format(Locale.US, "%+.1f", simulatedAnalysis.subject.primaryFaceExposureRelativeToScene)}%")
                }
                DebugItem("Motion State", simulatedAnalysis.motion.motionLevel.label)
                DebugItem("Estimated Kelvin", "${simulatedAnalysis.estimatedKelvin}K")
                DebugItem("Photo Quality Score", "${simulatedAnalysis.photoQuality.totalScore}/100 (${simulatedAnalysis.photoQuality.ratingLabel})")
            }
        }

        item {
            DebugCard(title = "Engine Output for Simulation", icon = Icons.Default.Speed) {
                DebugItem("Primary Action", simulatedRec.primaryActionText)
                DebugItem("Profile Applied", simulatedRec.imageProcessingProfile.displayName)
                DebugItem("Target Exposure (EV)", "${simulatedRec.exposureCompensationIndex} index (${String.format(Locale.US, "%.2f", simulatedRec.exposureCompensationEv)} EV)")
                DebugItem("Focus Strategy", simulatedRec.focusStrategy.label)
                DebugItem("Flash Recommendation", simulatedRec.flashRecommendation.label)
                DebugItem("White Balance", simulatedRec.whiteBalance.label)
                DebugItem("ISO Advice", simulatedRec.isoRecommendation)
                DebugItem("Shutter Advice", simulatedRec.shutterRecommendation)
                DebugItem("Recommended Lens", "${simulatedRec.recommendedLensType.displayName} (${simulatedRec.recommendedLens})")
            }
        }
    }
}

/**
 * Requirement 2 & 3: A/B Testing Studio Tab (V0.3)
 * Checklist of 11 standard test scenes, capture session history, and local JSON/CSV export.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbTestingStudioTab(
    abSessions: List<AbCaptureSession>,
    testedScenes: Set<TestSceneType>,
    onSelectSession: (AbCaptureSession) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Checklist Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Standard Test Scenes Checklist",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${testedScenes.size} / ${TestSceneType.values().size} Scenes Tested",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (testedScenes.size == TestSceneType.values().size) NeonEmerald else CyberCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (testedScenes.size == TestSceneType.values().size) NeonEmerald.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${((testedScenes.size.toFloat() / TestSceneType.values().size.toFloat()) * 100).toInt()}% Done",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (testedScenes.size == TestSceneType.values().size) NeonEmerald else CyberCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TestSceneType.values().forEach { scene ->
                        val isDone = testedScenes.contains(scene)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDone) NeonEmerald.copy(alpha = 0.15f) else Color(0xFF222B3D),
                            border = BorderStroke(1.dp, if (isDone) NeonEmerald.copy(alpha = 0.5f) else Color(0xFF333E54))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isDone) NeonEmerald else TextTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = scene.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isDone) NeonEmerald else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Data Export Options Card (Requirement 8)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Export Test Data (Developer Option)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Export objective physical metrics for all test sessions locally. All data remains strictly on-device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val jsonExport = AbTestStore.exportToJson()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, jsonExport)
                                type = "application/json"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export A/B Test JSON Data"))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = abSessions.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            val csvExport = AbTestStore.exportToCsv()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csvExport)
                                type = "text/csv"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export A/B Test CSV Data"))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = abSessions.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricGold),
                        border = BorderStroke(1.dp, ElectricGold.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 3. Captured A/B Sessions List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recorded A/B Sessions (${abSessions.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            if (abSessions.isNotEmpty()) {
                IconButton(onClick = { AbTestStore.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = TextSecondary)
                }
            }
        }

        if (abSessions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No A/B test captures recorded yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switch to 'A/B TEST' mode on the camera screen and press shutter to record an objective comparison.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            abSessions.forEach { session ->
                val delta = session.photoB_Metrics.totalTechnicalScore - session.photoA_Metrics.totalTechnicalScore
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSession(session) },
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E384D))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = session.testScene.displayName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyberCyan
                                    )
                                }
                                Text(
                                    text = session.formattedTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            delta > 0 -> NeonEmerald.copy(alpha = 0.2f)
                                            delta < 0 -> HighAlertRed.copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (delta >= 0) "+$delta pts" else "$delta pts",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (delta >= 0) NeonEmerald else HighAlertRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("AUTO SCORE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${session.photoA_Metrics.totalTechnicalScore}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = PureWhite
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PROFILE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = session.processingProfile.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = ElectricGold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("SMART SCORE", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                                Text(
                                    text = "${session.photoB_Metrics.totalTechnicalScore}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = CyberCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap to open full side-by-side & swipe comparison »",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = CyberCyan
                        )
                    }
                }
            }
        }
    }
}

/**
 * Requirement 7 & Device Profiles: Parameter Calibration Engine Tab
 * Allows developers to calibrate exposure bias, recovery, contrast, sharpness, denoise, and white balance.
 */
@Composable
private fun ParameterCalibrationTab() {
    val activeProfile by CalibrationEngine.activeProfile.collectAsState()
    val activeParams by CalibrationEngine.activeParameters.collectAsState()

    var expBias by remember(activeParams) { mutableFloatStateOf(activeParams.exposureBias) }
    var hlRecovery by remember(activeParams) { mutableFloatStateOf(activeParams.highlightRecovery) }
    var shRecovery by remember(activeParams) { mutableFloatStateOf(activeParams.shadowRecovery) }
    var contrast by remember(activeParams) { mutableFloatStateOf(activeParams.contrast) }
    var saturation by remember(activeParams) { mutableFloatStateOf(activeParams.saturation) }
    var sharpness by remember(activeParams) { mutableFloatStateOf(activeParams.sharpness) }
    var denoise by remember(activeParams) { mutableFloatStateOf(activeParams.noiseReduction) }
    var wbBias by remember(activeParams) { mutableFloatStateOf(activeParams.whiteBalanceBias) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Device Profile Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Device-Specific Calibration Profiles",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Optional profiles adjust hardware limits and processing multipliers based on sensor characteristics.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )

                Spacer(modifier = Modifier.height(10.dp))

                CalibrationProfilesRepository.ALL_PROFILES.forEach { profile ->
                    val isSelected = activeProfile.profileId == profile.profileId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { CalibrationEngine.selectProfile(profile) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color(0xFF1E2638),
                        border = BorderStroke(1.dp, if (isSelected) CyberCyan else Color(0xFF2E384D))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) CyberCyan else TextPrimary
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyberCyan)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Safe EV Steps: ±${profile.maxSafeEvCompensationSteps} | Denoise Multiplier: ${profile.noiseReductionMultiplier}x | Sharpening: ${profile.sharpeningStrengthMultiplier}x",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = ElectricGold
                            )
                        }
                    }
                }
            }
        }

        // 2. Manual Calibration Sliders Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manual Parameter Calibration",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = {
                            CalibrationEngine.resetToProfileDefaults()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Defaults",
                            tint = CyberCyan
                        )
                    }
                }

                Text(
                    text = "Adjust parameters to tune SMART AUTO decisions and image enhancement without rewriting AI engine code.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Exposure Bias
                CalibrationSliderItem(
                    label = "Exposure Bias",
                    value = expBias,
                    valueText = "${String.format(Locale.US, "%+.2f", expBias)} EV",
                    range = -2.0f..2.0f,
                    onValueChange = {
                        expBias = it
                        CalibrationEngine.updateParameters(activeParams.copy(exposureBias = it))
                    }
                )

                // Highlight Recovery
                CalibrationSliderItem(
                    label = "Highlight Recovery",
                    value = hlRecovery,
                    valueText = "${String.format(Locale.US, "%.2f", hlRecovery)}x",
                    range = 0.0f..2.0f,
                    onValueChange = {
                        hlRecovery = it
                        CalibrationEngine.updateParameters(activeParams.copy(highlightRecovery = it))
                    }
                )

                // Shadow Recovery
                CalibrationSliderItem(
                    label = "Shadow Recovery",
                    value = shRecovery,
                    valueText = "${String.format(Locale.US, "%.2f", shRecovery)}x",
                    range = 0.0f..2.0f,
                    onValueChange = {
                        shRecovery = it
                        CalibrationEngine.updateParameters(activeParams.copy(shadowRecovery = it))
                    }
                )

                // Contrast
                CalibrationSliderItem(
                    label = "Contrast",
                    value = contrast,
                    valueText = "${String.format(Locale.US, "%.2f", contrast)}x",
                    range = 0.5f..2.0f,
                    onValueChange = {
                        contrast = it
                        CalibrationEngine.updateParameters(activeParams.copy(contrast = it))
                    }
                )

                // Saturation
                CalibrationSliderItem(
                    label = "Saturation",
                    value = saturation,
                    valueText = "${String.format(Locale.US, "%.2f", saturation)}x",
                    range = 0.5f..2.0f,
                    onValueChange = {
                        saturation = it
                        CalibrationEngine.updateParameters(activeParams.copy(saturation = it))
                    }
                )

                // Sharpness
                CalibrationSliderItem(
                    label = "Sharpness",
                    value = sharpness,
                    valueText = "${String.format(Locale.US, "%.2f", sharpness)}x",
                    range = 0.0f..2.0f,
                    onValueChange = {
                        sharpness = it
                        CalibrationEngine.updateParameters(activeParams.copy(sharpness = it))
                    }
                )

                // Noise Reduction
                CalibrationSliderItem(
                    label = "Noise Reduction",
                    value = denoise,
                    valueText = "${String.format(Locale.US, "%.2f", denoise)}x",
                    range = 0.0f..2.0f,
                    onValueChange = {
                        denoise = it
                        CalibrationEngine.updateParameters(activeParams.copy(noiseReduction = it))
                    }
                )

                // White Balance Bias
                CalibrationSliderItem(
                    label = "White Balance Bias",
                    value = wbBias,
                    valueText = if (wbBias < 0) "${String.format(Locale.US, "%.2f", wbBias)} (Cool)" else "${String.format(Locale.US, "%+.2f", wbBias)} (Warm)",
                    range = -1.0f..1.0f,
                    onValueChange = {
                        wbBias = it
                        CalibrationEngine.updateParameters(activeParams.copy(whiteBalanceBias = it))
                    }
                )
            }
        }

        // 3. Live Calibration State Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Live Active Calibration Configuration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF131926)
                ) {
                    Text(
                        text = CalibrationEngine.getSummaryString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = ElectricGold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationSliderItem(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(
                valueText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = CyberCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = Color(0xFF2E384D)
            )
        )
    }
}

/**
 * Requirement 14: Captured Shots Technical Metadata Tab
 * Displays recorded developer metadata for recent captures.
 */
@Composable
private fun CapturedMetadataTab(metadataLogs: List<com.example.model.CaptureMetadata>) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Captured Shots Metadata (${metadataLogs.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Row {
                IconButton(
                    onClick = {
                        val allLogs = metadataLogs.joinToString("\n\n") { it.toFormattedLog() }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Capture Metadata", allLogs))
                        Toast.makeText(context, "Metadata copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    enabled = metadataLogs.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Metadata",
                        tint = if (metadataLogs.isNotEmpty()) CyberCyan else TextSecondary
                    )
                }
                IconButton(onClick = { com.example.model.DeveloperMetadataStore.clear() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Metadata",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (metadataLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No captured photos recorded yet.\nTake a photo in SMART AUTO or AUTO mode to view technical diagnostic metadata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(metadataLogs) { item ->
                    DebugCard(
                        title = "${item.captureMode} • ${item.scene.displayName} (${item.formattedTime})",
                        icon = Icons.Default.Camera
                    ) {
                        DebugItem("Capture ID", item.id)
                        DebugItem("Device", "${item.device} (Camera ID ${item.cameraId})")
                        DebugItem("Scene Detected", "${item.scene.displayName} (${item.sceneDetectionType})")
                        DebugItem("Lighting", "${item.lighting.label} (Luma: ${String.format(Locale.US, "%.1f", item.brightnessLuma)}%)")
                        DebugItem("Subject", "${item.faceCount} Faces Detected | Motion: ${item.motionLevel.label}")
                        DebugItem("Processing Profile", item.processingProfile.displayName)
                        DebugItem("Total Photo Score", "${item.qualityScore} / 100")
                        DebugItem("Highlight Protection", item.highlightProtection)
                        DebugItem("Shadow Strategy", item.shadowStrategy)
                        DebugItem("Face Priority Mode", item.facePriorityMode)
                        DebugItem("Target Exposure (EV)", "${String.format(Locale.US, "%+.2f", item.targetEv)} EV")
                        DebugItem("AI Decision Reasoning", item.decisionReasoning)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Applied Settings:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricGold
                        )
                        item.appliedSettings.forEach { (k, v) ->
                            DebugItem(k, v)
                        }
                        if (item.hardwareFallbackSettings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hardware Fallbacks / Graceful Mitigations:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CrimsonAlert
                            )
                            item.hardwareFallbackSettings.forEach { (k, v) ->
                                DebugItem("⚠ $k", v)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Structured Logs Tab:
 * Displays diagnostic log events chronologically.
 */
@Composable
private fun LogsTab(logs: List<AppLogger.LogEntry>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Diagnostic Logs (${logs.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            IconButton(onClick = { AppLogger.clearLogs() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Logs",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No log events recorded yet.\nStart camera viewfinder to generate events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs.reversed()) { entry ->
                    LogItemRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(entry: AppLogger.LogEntry) {
    val categoryColor = when (entry.tag) {
        "SCENE" -> CyberCyan
        "LIGHTING" -> ElectricGold
        "HARDWARE" -> Color(0xFFBB86FC)
        "RECOMMENDATION" -> EmeraldSuccess
        "CAPTURE" -> Color(0xFF03DAC5)
        "PROCESSING" -> Color(0xFFFF7597)
        "FOCUS" -> Color(0xFFFFB74D)
        "CAMERA" -> Color(0xFF64B5F6)
        else -> TextSecondary
    }

    val levelColor = when (entry.level) {
        "ERROR" -> CrimsonAlert
        "WARN" -> ElectricGold
        else -> TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.tag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = categoryColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextTertiary
                    )
                }
                Text(
                    text = entry.level,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = levelColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun DebugCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyberCyan
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DebugItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = TextSecondary,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            modifier = Modifier.weight(0.55f)
        )
    }
}

private fun generateSimulatedSceneAnalysis(scene: SceneType): SceneAnalysis {
    return when (scene) {
        SceneType.DAYLIGHT -> SceneAnalysis(
            scene = SceneType.DAYLIGHT,
            confidence = 0.92f,
            lighting = LightingAnalysis(brightness = 65.0f, darkness = 35.0f, contrast = 55.0f, highlightClipping = 2.0f, shadowLevel = 1.5f, condition = LightingCondition.NORMAL),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "None", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.05f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 93, exposureScore = 95, sharpnessScore = 90, stabilityScore = 95, dynamicRangeScore = 92, ratingLabel = "EXCELLENT"),
            sharpnessMetric = 65.0f,
            estimatedKelvin = 5500
        )
        SceneType.NIGHT -> SceneAnalysis(
            scene = SceneType.NIGHT,
            confidence = 0.95f,
            lighting = LightingAnalysis(brightness = 10.0f, darkness = 90.0f, contrast = 70.0f, highlightClipping = 5.0f, shadowLevel = 35.0f, condition = LightingCondition.VERY_DARK),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "None", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.08f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 78, exposureScore = 70, sharpnessScore = 75, stabilityScore = 88, dynamicRangeScore = 72, ratingLabel = "GOOD"),
            sharpnessMetric = 40.0f,
            estimatedKelvin = 4000
        )
        SceneType.FOREST_NATURE -> SceneAnalysis(
            scene = SceneType.FOREST_NATURE,
            confidence = 0.94f,
            lighting = LightingAnalysis(brightness = 48.0f, darkness = 52.0f, contrast = 60.0f, highlightClipping = 8.0f, shadowLevel = 12.0f, condition = LightingCondition.NORMAL),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "None", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.04f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 91, exposureScore = 90, sharpnessScore = 88, stabilityScore = 95, dynamicRangeScore = 89, ratingLabel = "EXCELLENT"),
            sharpnessMetric = 58.0f,
            greenVegetationRatio = 0.45f,
            estimatedKelvin = 6000
        )
        SceneType.BEACH -> SceneAnalysis(
            scene = SceneType.BEACH,
            confidence = 0.90f,
            lighting = LightingAnalysis(brightness = 82.0f, darkness = 18.0f, contrast = 65.0f, highlightClipping = 14.0f, shadowLevel = 1.0f, condition = LightingCondition.BRIGHT),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "None", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.10f, motionLevel = MotionLevel.LOW, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 88, exposureScore = 84, sharpnessScore = 86, stabilityScore = 90, dynamicRangeScore = 85, ratingLabel = "GREAT"),
            sharpnessMetric = 55.0f,
            coolBlueRatio = 0.40f,
            skyDetected = true,
            estimatedKelvin = 5600
        )
        SceneType.PORTRAIT -> SceneAnalysis(
            scene = SceneType.PORTRAIT,
            confidence = 0.96f,
            lighting = LightingAnalysis(brightness = 54.0f, darkness = 46.0f, contrast = 45.0f, highlightClipping = 3.0f, shadowLevel = 2.0f, condition = LightingCondition.NORMAL),
            subject = SubjectAnalysis(numberOfFaces = 1, isPersonPresent = true, approximateSubjectSize = "Large", isLikelyPortrait = true, skinRatio = 0.18f),
            motion = MotionAnalysis(motionScore = 0.05f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 94, exposureScore = 95, sharpnessScore = 92, stabilityScore = 96, dynamicRangeScore = 92, ratingLabel = "EXCELLENT"),
            sharpnessMetric = 62.0f,
            estimatedKelvin = 5200
        )
        SceneType.SUNSET -> SceneAnalysis(
            scene = SceneType.SUNSET,
            confidence = 0.93f,
            lighting = LightingAnalysis(brightness = 40.0f, darkness = 60.0f, contrast = 75.0f, highlightClipping = 10.0f, shadowLevel = 18.0f, condition = LightingCondition.NORMAL),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "None", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.03f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 89, exposureScore = 88, sharpnessScore = 85, stabilityScore = 96, dynamicRangeScore = 84, ratingLabel = "GREAT"),
            sharpnessMetric = 50.0f,
            warmColorRatio = 0.48f,
            skyDetected = true,
            estimatedKelvin = 3200
        )
        SceneType.FOOD -> SceneAnalysis(
            scene = SceneType.FOOD,
            confidence = 0.88f,
            lighting = LightingAnalysis(brightness = 56.0f, darkness = 44.0f, contrast = 58.0f, highlightClipping = 4.0f, shadowLevel = 3.0f, condition = LightingCondition.NORMAL),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "Medium", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.04f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 90, exposureScore = 92, sharpnessScore = 91, stabilityScore = 95, dynamicRangeScore = 90, ratingLabel = "EXCELLENT"),
            sharpnessMetric = 68.0f,
            warmColorRatio = 0.35f,
            estimatedKelvin = 4200
        )
        SceneType.ARCHITECTURE -> SceneAnalysis(
            scene = SceneType.ARCHITECTURE,
            confidence = 0.91f,
            lighting = LightingAnalysis(brightness = 60.0f, darkness = 40.0f, contrast = 68.0f, highlightClipping = 6.0f, shadowLevel = 4.0f, condition = LightingCondition.NORMAL),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "None", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.05f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 92, exposureScore = 92, sharpnessScore = 94, stabilityScore = 94, dynamicRangeScore = 91, ratingLabel = "EXCELLENT"),
            sharpnessMetric = 75.0f,
            edgeDensity = 0.28f,
            estimatedKelvin = 5400
        )
        SceneType.LOW_LIGHT -> SceneAnalysis(
            scene = SceneType.LOW_LIGHT,
            confidence = 0.89f,
            lighting = LightingAnalysis(brightness = 25.0f, darkness = 75.0f, contrast = 50.0f, highlightClipping = 2.0f, shadowLevel = 22.0f, condition = LightingCondition.DARK),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "None", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.06f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 80, exposureScore = 78, sharpnessScore = 78, stabilityScore = 90, dynamicRangeScore = 78, ratingLabel = "GREAT"),
            sharpnessMetric = 42.0f,
            estimatedKelvin = 4500
        )
        SceneType.INDOOR -> SceneAnalysis(
            scene = SceneType.INDOOR,
            confidence = 0.85f,
            lighting = LightingAnalysis(brightness = 45.0f, darkness = 55.0f, contrast = 48.0f, highlightClipping = 3.0f, shadowLevel = 5.0f, condition = LightingCondition.NORMAL),
            subject = SubjectAnalysis(numberOfFaces = 0, isPersonPresent = false, approximateSubjectSize = "None", isLikelyPortrait = false),
            motion = MotionAnalysis(motionScore = 0.05f, motionLevel = MotionLevel.STILL, isBlurRisk = false),
            photoQuality = PhotoQualityScore(totalScore = 86, exposureScore = 88, sharpnessScore = 82, stabilityScore = 92, dynamicRangeScore = 86, ratingLabel = "GREAT"),
            sharpnessMetric = 48.0f,
            warmColorRatio = 0.28f,
            estimatedKelvin = 3400
        )
        SceneType.UNKNOWN -> SceneAnalysis.INITIAL
    }
}

private fun buildDiagnosticReport(
    hardware: CameraCapabilities,
    analysis: SceneAnalysis,
    rec: CameraRecommendation
): String {
    return """
        ==================================================
        AI SMART CAMERA - DIAGNOSTIC REPORT (${hardware.deviceName})
        ==================================================
        DEVICE SPECIFICATIONS:
        - Device: ${hardware.deviceName}
        - Android: ${hardware.androidVersion}
        - Camera API: ${hardware.cameraApiVersion}
        - Camera2 HAL Level: ${hardware.hardwareLevel.label}
        - Active Camera ID: ${hardware.activeCameraId} (${hardware.lensFacingName})
        - Active Lens Type: ${hardware.activeLensType.displayName}
        - Max Resolution: ${hardware.sensorResolution}
        - Multi-Camera Lenses Discovered: ${hardware.physicalLenses.size}
        - EV Range: [${hardware.evRangeMin}..+${hardware.evRangeMax}], Step: ${hardware.evStep}
        - Flash Unit: ${if (hardware.isFlashSupported) "SUPPORTED" else "UNSUPPORTED"}
        - ISO Support: ${if (hardware.isManualIsoSupported) hardware.isoRange else "AUTO ONLY (${hardware.isoRange})"}
        - Shutter Support: ${if (hardware.isManualShutterSupported) hardware.shutterRange else "AUTO ONLY (${hardware.shutterRange})"}
        - Supported AF: ${hardware.supportedFocusModes}
        - Supported AE: ${hardware.supportedExposureModes}
        - Supported AWB: ${hardware.supportedAwbModes}
        
        AI SCENE ANALYSIS:
        - Detected Scene: ${analysis.scene.displayName} (${(analysis.confidence * 100).toInt()}%)
        - Lighting: ${analysis.lighting.condition.label} (Brightness: ${analysis.lighting.brightness}/100, Darkness: ${analysis.lighting.darkness}/100)
        - Motion: ${analysis.motion.motionLevel.label} (${analysis.motion.motionScore})
        - Face Count: ${analysis.subject.numberOfFaces} (Person: ${analysis.subject.isPersonPresent})
        - Sharpness: ${analysis.sharpnessMetric}
        - Highlight Clipping: ${analysis.lighting.highlightClipping}%
        - Shadow Level: ${analysis.lighting.shadowLevel}%
        - Sky Detected: ${analysis.skyDetected}
        - Photo Score: ${analysis.photoQuality.totalScore}/100 (${analysis.photoQuality.ratingLabel})
        
        ENGINE RECOMMENDATIONS:
        - Action: ${rec.primaryActionText}
        - Reason: ${rec.secondaryReasonText}
        - Profile: ${rec.imageProcessingProfile.displayName}
        - Recommended Lens: ${rec.recommendedLensType.displayName} (${rec.recommendedLens})
        - EV Target: ${rec.exposureCompensationIndex} (${rec.exposureCompensationEv} EV)
        - Focus Strategy: ${rec.focusStrategy.label}
        - Flash Rec: ${rec.flashRecommendation.label}
        - White Balance: ${rec.whiteBalance.label}
        - ISO Intent: ${rec.isoPreference.label} (${rec.isoRecommendation})
        - Shutter Intent: ${rec.shutterPreference.label} (${rec.shutterRecommendation})
        ==================================================
    """.trimIndent()
}
