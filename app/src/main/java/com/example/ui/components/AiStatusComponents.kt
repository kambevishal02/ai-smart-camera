package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraRecommendation
import com.example.model.LightingCondition
import com.example.model.PhotoQualityScore
import com.example.model.SceneAnalysis
import com.example.model.SceneType
import com.example.model.SmartCaptureStatus
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ScoreGold
import com.example.ui.theme.ScoreOrange
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Requirement 13: Smart Capture Status Indicator
 * Displays state in real-time:
 * - ANALYZING
 * - READY
 * - CAPTURING
 * - PROCESSING
 * - SAVED
 */
@Composable
fun SmartCaptureStatusIndicator(
    status: SmartCaptureStatus,
    lastQualityScore: PhotoQualityScore? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        SmartCaptureStatus.ANALYZING -> CyberCyan
        SmartCaptureStatus.READY -> EmeraldSuccess
        SmartCaptureStatus.CAPTURING -> ElectricGold
        SmartCaptureStatus.PROCESSING -> CyberCyan
        SmartCaptureStatus.SAVED -> EmeraldSuccess
    }

    val icon: ImageVector = when (status) {
        SmartCaptureStatus.ANALYZING -> Icons.Default.Search
        SmartCaptureStatus.READY -> Icons.Default.CheckCircle
        SmartCaptureStatus.CAPTURING -> Icons.Default.Camera
        SmartCaptureStatus.PROCESSING -> Icons.Default.AutoAwesome
        SmartCaptureStatus.SAVED -> Icons.Default.DoneAll
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_anim"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                BorderStroke(1.dp, statusColor.copy(alpha = if (status == SmartCaptureStatus.CAPTURING || status == SmartCaptureStatus.PROCESSING) alphaAnim else 0.8f)),
                RoundedCornerShape(20.dp)
            )
            .testTag("smart_capture_status_indicator"),
        color = DarkSurfaceGlass
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (status == SmartCaptureStatus.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = CyberCyan,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = status.label,
                    tint = statusColor,
                    modifier = Modifier.size(13.dp)
                )
            }

            Text(
                text = if (status == SmartCaptureStatus.SAVED && lastQualityScore != null) {
                    "PHOTO SAVED • QUALITY ${lastQualityScore.totalScore}"
                } else {
                    status.label
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = if (status == SmartCaptureStatus.SAVED) EmeraldSuccess else TextPrimary
            )
        }
    }
}

/**
 * Requirement 6 & 13: Real-time small AI status overlay on camera preview.
 * Structure:
 * --------------------------------
 * AI SMART CAMERA
 *
 * Scene: FOREST (HEURISTIC)
 * Lighting: LOW (28%)
 * Subject: PERSON (1 Face)
 *
 * Recommendation:
 * Shadow Recovery & Face Priority
 *
 * PHOTO SCORE: 91
 * --------------------------------
 */
@Composable
fun AiStatusPanel(
    analysis: SceneAnalysis,
    recommendation: CameraRecommendation,
    isSmartAuto: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.5f), DarkSurfaceBorder))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .testTag("ai_status_panel"),
        color = DarkSurfaceGlass,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .width(260.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isSmartAuto) CyberCyan.copy(alpha = 0.2f) else TextSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Active",
                            tint = if (isSmartAuto) CyberCyan else TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSmartAuto) "AI SMART CAMERA" else "STANDARD AUTO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isSmartAuto) CyberCyan else TextSecondary
                    )
                }

                Text(
                    text = "${(analysis.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Scene line
            val sceneText = analysis.scene.displayName.uppercase()
            val engineType = if (analysis.subject.isPersonPresent && analysis.subject.detectedFaces.isNotEmpty()) "ML KIT" else "HEURISTIC"
            StatusRow(
                label = "Scene:",
                value = "$sceneText ($engineType)",
                accentColor = when (analysis.scene) {
                    SceneType.SUNSET, SceneType.BEACH -> ElectricGold
                    SceneType.FOREST_NATURE -> EmeraldSuccess
                    SceneType.PORTRAIT -> CyberCyan
                    SceneType.NIGHT, SceneType.LOW_LIGHT -> ElectricGold
                    else -> TextPrimary
                }
            )

            // Lighting line
            val lightingText = when (analysis.lighting.condition) {
                LightingCondition.VERY_DARK -> "EXTREME LOW"
                LightingCondition.DARK -> "LOW"
                LightingCondition.NORMAL -> "BALANCED"
                LightingCondition.BRIGHT -> "BRIGHT"
                LightingCondition.VERY_BRIGHT -> "HARSH SUNLIGHT"
            }
            StatusRow(
                label = "Lighting:",
                value = "$lightingText (${analysis.lighting.brightness.toInt()}%)",
                accentColor = when (analysis.lighting.condition) {
                    LightingCondition.VERY_DARK, LightingCondition.DARK -> ElectricGold
                    LightingCondition.VERY_BRIGHT -> ScoreOrange
                    else -> TextPrimary
                }
            )

            // Subject line
            val subjectText = when {
                analysis.subject.numberOfFaces > 1 -> "${analysis.subject.numberOfFaces} PEOPLE"
                analysis.subject.numberOfFaces == 1 || analysis.subject.isPersonPresent -> "PERSON"
                analysis.scene == SceneType.FOOD -> "FOOD"
                analysis.scene == SceneType.ARCHITECTURE -> "ARCHITECTURE"
                analysis.scene == SceneType.FOREST_NATURE -> "NATURE"
                analysis.skyDetected -> "LANDSCAPE"
                else -> "ENVIRONMENT"
            }
            StatusRow(
                label = "Subject:",
                value = subjectText,
                accentColor = if (analysis.subject.isPersonPresent) CyberCyan else TextPrimary
            )

            // Recommendation block
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF131C29))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Column {
                    Text(
                        text = "Recommendation:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = ElectricGold
                    )
                    Text(
                        text = recommendation.primaryActionText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // PHOTO SCORE: 91
            val score = analysis.photoQuality.totalScore
            val scoreColor = when {
                score >= 90 -> EmeraldSuccess
                score >= 78 -> CyberCyan
                score >= 60 -> ScoreGold
                else -> ScoreOrange
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PHOTO SCORE: $score",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = scoreColor
                )

                Text(
                    text = if (isExpanded) "Tap to collapse ▲" else "Tap for details ▼",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextSecondary
                )
            }

            // Expanded Diagnostic Details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    MetricScoreBar("Exposure", analysis.photoQuality.exposureScore)
                    MetricScoreBar("Sharpness", analysis.photoQuality.sharpnessScore)
                    MetricScoreBar("Stability", analysis.photoQuality.stabilityScore)
                    MetricScoreBar("Dynamic Range", analysis.photoQuality.dynamicRangeScore)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Profile: ${recommendation.imageProcessingProfile.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                    Text(
                        text = "Focus: ${recommendation.focusStrategy.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = accentColor
        )
    }
}

@Composable
fun MetricScoreBar(name: String, score: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondary,
            modifier = Modifier.width(90.dp)
        )
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (score >= 80) CyberCyan else ScoreGold,
            trackColor = Color(0x33FFFFFF)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$score",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary
        )
    }
}

/**
 * Minimal top-right AI photo score badge.
 */
@Composable
fun AiPhotoScoreBadge(
    score: PhotoQualityScore,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score.totalScore >= 90 -> EmeraldSuccess
        score.totalScore >= 78 -> CyberCyan
        score.totalScore >= 60 -> ScoreGold
        else -> ScoreOrange
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, scoreColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("ai_photo_score_badge"),
        color = DarkSurfaceGlass
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = "Score",
                tint = scoreColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${score.totalScore}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = TextPrimary
            )
            Text(
                text = score.ratingLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = scoreColor
            )
        }
    }
}
