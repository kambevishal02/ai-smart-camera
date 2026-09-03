package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AdaptiveIntelligenceEngine
import com.example.ai.AdaptiveProfileStore
import com.example.ai.AdaptiveSimulationRunner
import com.example.model.CameraCapabilities
import com.example.model.TestSceneType
import com.example.ui.components.AdaptiveExportDialog
import com.example.ui.components.AdaptiveHistoryDialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AdaptiveIntelligenceTab(
    hardware: CameraCapabilities
) {
    val context = LocalContext.current
    val currentProfile by AdaptiveProfileStore.currentProfile.collectAsState()
    val previousProfile by AdaptiveProfileStore.previousProfile.collectAsState()
    val history by AdaptiveProfileStore.learningHistory.collectAsState()
    val isPaused by AdaptiveProfileStore.isLearningPaused.collectAsState()
    val lastExplanation by AdaptiveIntelligenceEngine.lastExplanation.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var simulationResultNotice by remember { mutableStateOf<String?>(null) }

    if (showHistoryDialog) {
        AdaptiveHistoryDialog(
            history = history,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showExportDialog) {
        AdaptiveExportDialog(
            onDismiss = { showExportDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("adaptive_intelligence_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 14: ADAPTIVE INTELLIGENCE STATUS CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.5.dp, CyberCyan)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Adaptive Camera Intelligence",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyberCyan
                            )
                        }

                        val status = AdaptiveProfileStore.getLearningStatus()
                        val statusColor = when (status.name) {
                            "ACTIVE" -> HighContrastGreen
                            "PAUSED" -> SafetyOrange
                            else -> ElectricGold
                        }
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Device Profile: ${currentProfile.deviceIdentifier.manufacturer} ${currentProfile.deviceIdentifier.model} (Cam ID: ${currentProfile.deviceIdentifier.activeCameraId}, ${currentProfile.deviceIdentifier.hardwareLevel})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = PureWhite
                    )
                    Text(
                        text = "Profile Version: v${currentProfile.profileVersion} | Baseline: Generic Android V0.4",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Metrics Grid (Section 14)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AdaptiveMetricItem("Total Valid Tests", "${AdaptiveProfileStore.totalEvaluatedSessions}", Modifier.weight(1f))
                        AdaptiveMetricItem("Eligible Tests", "${AdaptiveProfileStore.learningEligibleSessions}", Modifier.weight(1f))
                        AdaptiveMetricItem("Rejected Tests", "${AdaptiveProfileStore.rejectedSessions}", Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val globalParams = currentProfile.globalParameters
                    val maxConfidence = currentProfile.sceneParameters.values.maxOfOrNull { it.confidence } ?: globalParams.confidence
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AdaptiveMetricItem("Current Confidence", "${(maxConfidence * 100).toInt()}%", Modifier.weight(1f))
                        AdaptiveMetricItem("Scenes Tuned", "${currentProfile.sceneParameters.size} / 11", Modifier.weight(1f))
                        AdaptiveMetricItem("Can Rollback", if (previousProfile != null) "YES (v${previousProfile!!.profileVersion})" else "NO", Modifier.weight(1f))
                    }
                }
            }
        }

        // Section 8: "Why did SMART AUTO change?" EXPLANATION CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, ElectricGold)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Why did SMART AUTO change? (Explainable Learning)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = ElectricGold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val exp = lastExplanation
                    if (exp != null) {
                        AdaptiveKeyValue("Scene", exp.scene)
                        AdaptiveKeyValue("Base EV", "${String.format(Locale.US, "%+.2f", exp.baseEv)} EV")
                        AdaptiveKeyValue("Adaptive bias", "${String.format(Locale.US, "%+.2f", exp.adaptiveBias)} EV")
                        AdaptiveKeyValue("Final EV", "${String.format(Locale.US, "%+.2f", exp.finalEv)} EV")
                        AdaptiveKeyValue("Reason", exp.reason)
                        AdaptiveKeyValue("Confidence", "${exp.confidencePct}%")
                        AdaptiveKeyValue("Evidence", "${exp.evidenceSamples} samples")
                    } else {
                        Text(
                            text = "Operating at calibrated baseline. To observe live changes, perform A/B test captures or run the simulation suite below.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Section 14: ACTION BUTTONS ROW
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showHistoryDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_view_learning_history")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VIEW HISTORY (${history.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighContrastGreen),
                        border = BorderStroke(1.dp, HighContrastGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_adaptive_profile")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val success = AdaptiveProfileStore.rollbackLastChange()
                            if (success) {
                                Toast.makeText(context, "Rolled back to previous profile version", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No previous profile state available to rollback", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (previousProfile != null) ElectricGold else TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (previousProfile != null) ElectricGold else Color(0x33FFFFFF)),
                        enabled = previousProfile != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_rollback_adaptive_change")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ROLLBACK LAST", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            AdaptiveProfileStore.resetToBaseline()
                            Toast.makeText(context, "Reset all adaptive parameters to V0.4 baseline", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyOrange),
                        border = BorderStroke(1.dp, SafetyOrange.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_reset_adaptive_profile")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET TO BASELINE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 14: CURRENT SCENE ADAPTIVE BIASES
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Learned Scene Biases (Safe Clamped Bounds)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentProfile.sceneParameters.isEmpty()) {
                        Text(
                            text = "No scene parameter biases deviated from baseline yet. All parameters are at 0.0 offset.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    } else {
                        currentProfile.sceneParameters.forEach { (scene, params) ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${scene.displayName} (${params.sampleCount} tests, ${(params.confidence * 100).toInt()}% conf)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = "EV: ${String.format(Locale.US, "%+.2f", params.exposureBias)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = ElectricGold
                                    )
                                }
                                Text(
                                    text = "HL Protection: ${String.format(Locale.US, "%+.2f", params.highlightProtectionBias)} | Shadow: ${String.format(Locale.US, "%+.2f", params.shadowRecoveryBias)} | Sharpness: ${String.format(Locale.US, "%+.2f", params.sharpeningBias)} | Noise: ${String.format(Locale.US, "%+.2f", params.noiseReductionBias)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = TextSecondary
                                )
                                HorizontalDivider(color = Color(0x15FFFFFF), thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 16: DETERMINISTIC SIMULATION TESTING CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Interactive Simulation Test Suite (Section 16)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = CyberCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Deterministic validation of conservative multi-sample learning, scene isolation, and rejection of bad/noisy data.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    simulationResultNotice?.let { notice ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberCyan.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = PureWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val (p1, p2) = AdaptiveSimulationRunner.runSunsetShadowLossSimulation()
                                simulationResultNotice = "${p1.successVerification}\n${p2.successVerification}"
                                Toast.makeText(context, "Sunset Simulation Finished", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricGold),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_sunset")
                        ) {
                            Text("1. Sunset (5x)", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val outcome = AdaptiveSimulationRunner.verifySceneIsolation()
                                simulationResultNotice = "${outcome.scenarioName}: ${outcome.successVerification}"
                                Toast.makeText(context, "Scene Isolation Verified", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HighContrastGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_isolation")
                        ) {
                            Text("2. Isolation", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val outMotion = AdaptiveSimulationRunner.testHighMotionRejection()
                                val outNoise = AdaptiveSimulationRunner.testNoisyDataRejection()
                                val outConf = AdaptiveSimulationRunner.testLowConfidenceRejection()
                                simulationResultNotice = "Bad Data Rejection:\n• ${outMotion.successVerification}\n• ${outNoise.successVerification}\n• ${outConf.successVerification}"
                                Toast.makeText(context, "Rejections Verified", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyOrange),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_rejections")
                        ) {
                            Text("3. Test Rejections (Motion/Noise)", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveMetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = PureWhite
        )
    }
}

@Composable
private fun AdaptiveKeyValue(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$key:",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = PureWhite
        )
    }
}
