package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CapturedPhoto
import com.example.model.ObjectivePhotoQualityMetrics
import com.example.model.SubjectEnhancementDebugInfo
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LaserPurple
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Interactive preview dialog showcasing captured photos with:
 * - Instant BEFORE (Original Raw) vs AFTER (AI Enhanced) comparison.
 * - Prominent Subject vs Background Enhancement Summary proving targeted subject brightness & color lift.
 * - Developer Debug Overlay showing real-time subject-aware algorithmic parameters.
 * - Objective Photo Quality Metrics table comparing Raw vs Enhanced measurements.
 */
@Composable
fun PhotoComparisonDialog(
    photo: CapturedPhoto,
    onDismiss: () -> Unit
) {
    var showEnhanced by remember { mutableStateOf(true) }
    var showScoreDetails by remember { mutableStateOf(false) }
    var showDebugOverlay by remember { mutableStateOf(false) }

    val quality = photo.qualityScore ?: photo.sceneAnalysis?.photoQuality
    val score = quality?.totalScore ?: photo.photoScoreAtCapture
    val debugInfo = photo.debugInfo
    val origMetrics = photo.metricsOriginal
    val enhMetrics = photo.metricsEnhanced

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .testTag("photo_comparison_dialog"),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Photo Quality Review",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CyberCyan.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = photo.profileApplied.displayName,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = CyberCyan
                                )
                            }
                        }
                        Text(
                            text = "Score: $score/100 • Resolution: ${debugInfo?.finalOutputResolution ?: origMetrics?.resolution ?: "Sensor Native"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle Developer Debug Overlay Button
                        IconButton(
                            onClick = { showDebugOverlay = !showDebugOverlay },
                            modifier = Modifier.testTag("toggle_debug_overlay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Developer Debug Overlay",
                                tint = if (showDebugOverlay) CyberCyan else TextSecondary
                            )
                        }

                        // Toggle Score Details Button
                        IconButton(
                            onClick = { showScoreDetails = !showScoreDetails },
                            modifier = Modifier.testTag("toggle_score_details_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Score Details",
                                tint = if (showScoreDetails) ElectricGold else TextSecondary
                            )
                        }

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subject vs Background Enhancement Summary Banner
                if (debugInfo != null) {
                    SubjectEnhancementSummaryBanner(debugInfo)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Collapsible Panels (Debug Overlay OR Score Breakdown)
                AnimatedVisibility(visible = showDebugOverlay) {
                    DeveloperDebugOverlayPanel(
                        debugInfo = debugInfo,
                        metricsOriginal = origMetrics,
                        metricsEnhanced = enhMetrics
                    )
                }

                AnimatedVisibility(visible = showScoreDetails) {
                    ScoreDetailsPanel(quality = quality, score = score)
                }

                // Image Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, DarkSurfaceBorder), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmapToDisplay = if (showEnhanced) {
                        photo.enhancedBitmap ?: photo.originalBitmap ?: photo.rawBitmap
                    } else {
                        photo.rawBitmap ?: photo.originalBitmap
                    }

                    if (bitmapToDisplay != null) {
                        Image(
                            bitmap = bitmapToDisplay.asImageBitmap(),
                            contentDescription = if (showEnhanced) "AI Enhanced Photo" else "Original Raw Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Floating comparison indicator badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = DarkSurfaceGlass
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (showEnhanced) EmeraldSuccess else ElectricGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showEnhanced) "✨ AI ENHANCED (SUBJECT-AWARE)" else "📷 ORIGINAL RAW CAPTURE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (showEnhanced) EmeraldSuccess else ElectricGold
                            )
                        }
                    }

                    // Floating Resolution Badge (Proves Native Capture Resolution)
                    if (bitmapToDisplay != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = DarkSurfaceGlass
                        ) {
                            Text(
                                text = "${bitmapToDisplay.width}×${bitmapToDisplay.height}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Compare Toggle Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Raw Sensor Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (!showEnhanced) ElectricGold else DarkSurfaceBorder
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showEnhanced = false }
                            .testTag("toggle_raw_button"),
                        color = if (!showEnhanced) ElectricGold.copy(alpha = 0.18f) else DarkSurfaceElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Original (Raw)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (!showEnhanced) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (!showEnhanced) ElectricGold else TextSecondary
                            )
                        }
                    }

                    // Enhanced Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (showEnhanced) CyberCyan else DarkSurfaceBorder
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showEnhanced = true }
                            .testTag("toggle_enhanced_button"),
                        color = if (showEnhanced) CyberCyan.copy(alpha = 0.18f) else DarkSurfaceElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (showEnhanced) CyberCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Enhanced",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (showEnhanced) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (showEnhanced) CyberCyan else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary badge proving subject was enhanced independently from background.
 */
@Composable
private fun SubjectEnhancementSummaryBanner(debugInfo: SubjectEnhancementDebugInfo) {
    val subjDiff = debugInfo.enhancedSubjectLuminance - debugInfo.originalSubjectLuminance
    val bgDiff = debugInfo.enhancedBackgroundLuminance - debugInfo.originalBackgroundLuminance

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)), RoundedCornerShape(10.dp)),
        color = DarkSurfaceGlass
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SUBJECT LIFT",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = EmeraldSuccess
                )
                Text(
                    text = "${String.format("%.1f", debugInfo.originalSubjectLuminance)}% → ${String.format("%.1f", debugInfo.enhancedSubjectLuminance)}% (${if (subjDiff >= 0) "+" else ""}${String.format("%.1f", subjDiff)}%)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    color = PureWhite
                )
            }

            Column {
                Text(
                    text = "BG HIGHLIGHTS",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = CyberCyan
                )
                Text(
                    text = "${String.format("%.1f", debugInfo.originalBackgroundLuminance)}% → ${String.format("%.1f", debugInfo.enhancedBackgroundLuminance)}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    color = TextSecondary
                )
            }

            Column {
                Text(
                    text = "SUBJ/BG RATIO",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = ElectricGold
                )
                Text(
                    text = "${String.format("%.2f", debugInfo.subjectBackgroundRatioBefore)} → ${String.format("%.2f", debugInfo.subjectBackgroundRatioAfter)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = ElectricGold
                )
            }
        }
    }
}

/**
 * Developer Debug Overlay Panel displaying all 15 algorithmic diagnostic parameters
 * alongside Objective Photo Quality Metrics (Before vs After).
 */
@Composable
private fun DeveloperDebugOverlayPanel(
    debugInfo: SubjectEnhancementDebugInfo?,
    metricsOriginal: ObjectivePhotoQualityMetrics?,
    metricsEnhanced: ObjectivePhotoQualityMetrics?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)), RoundedCornerShape(12.dp)),
        color = DarkSurfaceGlass
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEVELOPER DEBUG OVERLAY",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = CyberCyan
                )
                if (debugInfo != null) {
                    Text(
                        text = debugInfo.detectionEngine,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = EmeraldSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Algorithmic Diagnostics
            if (debugInfo != null) {
                DebugParamRow("Original Subject Luminance", "${String.format("%.2f", debugInfo.originalSubjectLuminance)}%")
                DebugParamRow("Enhanced Subject Luminance", "${String.format("%.2f", debugInfo.enhancedSubjectLuminance)}%")
                DebugParamRow("Original Background Luminance", "${String.format("%.2f", debugInfo.originalBackgroundLuminance)}%")
                DebugParamRow("Enhanced Background Luminance", "${String.format("%.2f", debugInfo.enhancedBackgroundLuminance)}%")
                DebugParamRow("Subject/Background Ratio Before", String.format("%.2f", debugInfo.subjectBackgroundRatioBefore))
                DebugParamRow("Subject/Background Ratio After", String.format("%.2f", debugInfo.subjectBackgroundRatioAfter))
                DebugParamRow("Exposure Adjustment", String.format("%+.2f EV", debugInfo.exposureAdjustment))
                DebugParamRow("Shadow Recovery Strength", "${(debugInfo.shadowRecoveryStrength * 100).toInt()}%")
                DebugParamRow("Highlight Protection Strength", "${(debugInfo.highlightProtectionStrength * 100).toInt()}%")
                DebugParamRow("Saturation Adjustment", "${(debugInfo.saturationAdjustment * 100).toInt()}%")
                DebugParamRow("Contrast Adjustment", String.format("%.2fx", debugInfo.contrastAdjustment))
                DebugParamRow("Sharpening Strength", "${(debugInfo.sharpeningStrength * 100).toInt()}%")
                DebugParamRow("Enhancement Profile", debugInfo.enhancementProfile)
                DebugParamRow("Detection Confidence", "${(debugInfo.detectionConfidence * 100).toInt()}%")
                DebugParamRow("Final Output Resolution", debugInfo.finalOutputResolution)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DarkSurfaceBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // 2. Objective Photo Quality Metrics
            Text(
                text = "OBJECTIVE PHOTO QUALITY METRICS (BEFORE vs AFTER)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                color = ElectricGold
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Table Header
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text("Metric", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text("Original", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text("Enhanced", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }

            if (metricsOriginal != null && metricsEnhanced != null) {
                ObjectiveMetricRow("Resolution", metricsOriginal.resolution, metricsEnhanced.resolution)
                ObjectiveMetricRow("Sharpness (Laplacian)", String.format("%.1f", metricsOriginal.sharpnessLaplacianVar), String.format("%.1f", metricsEnhanced.sharpnessLaplacianVar))
                ObjectiveMetricRow("Highlight Clipping %", "${String.format("%.2f", metricsOriginal.highlightClippingPct)}%", "${String.format("%.2f", metricsEnhanced.highlightClippingPct)}%")
                ObjectiveMetricRow("Shadow Clipping %", "${String.format("%.2f", metricsOriginal.shadowClippingPct)}%", "${String.format("%.2f", metricsEnhanced.shadowClippingPct)}%")
                ObjectiveMetricRow("RMS Contrast", String.format("%.1f", metricsOriginal.rmsContrast), String.format("%.1f", metricsEnhanced.rmsContrast))
                ObjectiveMetricRow("Average Luminance", "${String.format("%.1f", metricsOriginal.averageLuminance)}%", "${String.format("%.1f", metricsEnhanced.averageLuminance)}%")
                ObjectiveMetricRow("Subject Luminance", "${String.format("%.1f", metricsOriginal.subjectLuminance)}%", "${String.format("%.1f", metricsEnhanced.subjectLuminance)}%")
                ObjectiveMetricRow("Background Luminance", "${String.format("%.1f", metricsOriginal.backgroundLuminance)}%", "${String.format("%.1f", metricsEnhanced.backgroundLuminance)}%")
                ObjectiveMetricRow("Subject/BG Ratio", String.format("%.2f", metricsOriginal.subjectBackgroundLuminanceRatio), String.format("%.2f", metricsEnhanced.subjectBackgroundLuminanceRatio))
                ObjectiveMetricRow("Saturation", "${String.format("%.1f", metricsOriginal.saturation)}%", "${String.format("%.1f", metricsEnhanced.saturation)}%")
                ObjectiveMetricRow("Noise Estimate", String.format("%.1f", metricsOriginal.noiseEstimate), String.format("%.1f", metricsEnhanced.noiseEstimate))
            } else if (metricsOriginal != null) {
                ObjectiveMetricRow("Resolution", metricsOriginal.resolution, "—")
                ObjectiveMetricRow("Sharpness (Laplacian)", String.format("%.1f", metricsOriginal.sharpnessLaplacianVar), "—")
                ObjectiveMetricRow("Highlight Clipping %", "${String.format("%.2f", metricsOriginal.highlightClippingPct)}%", "—")
                ObjectiveMetricRow("Shadow Clipping %", "${String.format("%.2f", metricsOriginal.shadowClippingPct)}%", "—")
                ObjectiveMetricRow("RMS Contrast", String.format("%.1f", metricsOriginal.rmsContrast), "—")
                ObjectiveMetricRow("Average Luminance", "${String.format("%.1f", metricsOriginal.averageLuminance)}%", "—")
                ObjectiveMetricRow("Subject Luminance", "${String.format("%.1f", metricsOriginal.subjectLuminance)}%", "—")
                ObjectiveMetricRow("Background Luminance", "${String.format("%.1f", metricsOriginal.backgroundLuminance)}%", "—")
                ObjectiveMetricRow("Subject/BG Ratio", String.format("%.2f", metricsOriginal.subjectBackgroundLuminanceRatio), "—")
                ObjectiveMetricRow("Saturation", "${String.format("%.1f", metricsOriginal.saturation)}%", "—")
                ObjectiveMetricRow("Noise Estimate", String.format("%.1f", metricsOriginal.noiseEstimate), "—")
            }
        }
    }
}

@Composable
private fun DebugParamRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = PureWhite)
    }
}

@Composable
private fun ObjectiveMetricRow(name: String, origVal: String, enhVal: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = origVal, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextPrimary)
        Text(text = enhVal, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = EmeraldSuccess)
    }
}

@Composable
private fun ScoreDetailsPanel(
    quality: com.example.model.PhotoQualityScore?,
    score: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, DarkSurfaceBorder), RoundedCornerShape(12.dp)),
        color = DarkSurfaceGlass
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "TECHNICAL PHOTO QUALITY BREAKDOWN",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = ElectricGold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (quality != null) {
                MetricScoreBar("Exposure", quality.exposureScore)
                MetricScoreBar("Sharpness", quality.sharpnessScore)
                MetricScoreBar("Stability", quality.stabilityScore)
                MetricScoreBar("Dynamic Range", quality.dynamicRangeScore)
                MetricScoreBar("Highlights", quality.highlightScore)
                MetricScoreBar("Shadows", quality.shadowScore)
            } else {
                MetricScoreBar("Overall Score", score)
            }
        }
    }
}

@Composable
fun MetricScoreBar(
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = "$score/100",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = if (score >= 80) EmeraldSuccess else if (score >= 60) ElectricGold else CyberCyan
        )
    }
}
