package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CapturedPhoto
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Interactive preview dialog showcasing the captured photo with
 * instant Before (Raw) / After (AI Enhanced) comparison toggle and
 * detailed Photo Quality Score breakdown.
 */
@Composable
fun PhotoComparisonDialog(
    photo: CapturedPhoto,
    onDismiss: () -> Unit
) {
    var showEnhanced by remember { mutableStateOf(true) }
    var showScoreDetails by remember { mutableStateOf(false) }

    val quality = photo.qualityScore ?: photo.sceneAnalysis?.photoQuality
    val score = quality?.totalScore ?: photo.photoScoreAtCapture

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
                    .padding(16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Photo Review",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Profile: ${photo.profileApplied.displayName} • Quality Score: $score/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                    }

                    Row {
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

                Spacer(modifier = Modifier.height(10.dp))

                // Score Breakdown Panel (Collapsible)
                AnimatedVisibility(visible = showScoreDetails) {
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
                            contentDescription = if (showEnhanced) "Enhanced Photo" else "Original Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Floating comparison indicator badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp)),
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
                                text = if (showEnhanced) "✨ AI ENHANCED (${photo.profileApplied.displayName.uppercase()})" else "📷 RAW SENSOR CAPTURE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (showEnhanced) EmeraldSuccess else ElectricGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Compare Toggle Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Raw Sensor Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (!showEnhanced) ElectricGold else DarkSurfaceBorder
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { showEnhanced = false }
                            .testTag("toggle_raw_button"),
                        color = if (!showEnhanced) ElectricGold.copy(alpha = 0.2f) else DarkSurfaceElevated
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
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (showEnhanced) CyberCyan else DarkSurfaceBorder
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { showEnhanced = true }
                            .testTag("toggle_enhanced_button"),
                        color = if (showEnhanced) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated
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
