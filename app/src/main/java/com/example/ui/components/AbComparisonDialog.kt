package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AbCaptureSession
import com.example.model.DetailedTechnicalMetrics
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.HighAlertRed
import com.example.ui.theme.LaserPurple
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Visual Comparison Modes for A/B Testing:
 * 0: Swipe / Split Slider
 * 1: Side-by-Side
 * 2: Single Toggle (Instant A/B switch)
 */
enum class ComparisonViewMode(val title: String) {
    SWIPE_SLIDER("Swipe Slider"),
    SIDE_BY_SIDE("Side-by-Side"),
    TOGGLE("Toggle")
}

@Composable
fun AbComparisonDialog(
    session: AbCaptureSession,
    onDismiss: () -> Unit,
    onOpenCalibration: () -> Unit = {}
) {
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf(ComparisonViewMode.SWIPE_SLIDER) }
    var singleToggleIsSmart by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSurface),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
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
                                text = "A/B TEST COMPARISON",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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
                        }
                        Text(
                            text = "${session.deviceName} • ${session.formattedTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val report = session.toJsonObject().toString(2)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, report)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export A/B Test Session")
                                context.startActivity(shareIntent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Report",
                                tint = CyberCyan
                            )
                        }

                        IconButton(
                            onClick = {
                                onDismiss()
                                onOpenCalibration()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Calibrate Parameters",
                                tint = ElectricGold
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Switcher Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkCardBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ComparisonViewMode.values().forEach { mode ->
                        val isSelected = viewMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) CyberCyan else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(vertical = 6.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { _, _ -> }
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { viewMode = mode },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) CyberCyan else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content: Visual Viewport + Scores + Technical Metrics
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Visual Comparison Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, Color(0xFF2E384D), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (viewMode) {
                            ComparisonViewMode.SWIPE_SLIDER -> {
                                SwipeComparisonView(
                                    bitmapA = session.photoA_Bitmap,
                                    bitmapB = session.photoB_Bitmap
                                )
                            }
                            ComparisonViewMode.SIDE_BY_SIDE -> {
                                SideBySideComparisonView(
                                    bitmapA = session.photoA_Bitmap,
                                    bitmapB = session.photoB_Bitmap
                                )
                            }
                            ComparisonViewMode.TOGGLE -> {
                                SingleToggleComparisonView(
                                    bitmapA = session.photoA_Bitmap,
                                    bitmapB = session.photoB_Bitmap,
                                    isSmart = singleToggleIsSmart,
                                    onToggle = { singleToggleIsSmart = !singleToggleIsSmart }
                                )
                            }
                        }
                    }

                    // 2. Scores Overview Card
                    ScoresOverviewCard(
                        metricsA = session.photoA_Metrics,
                        metricsB = session.photoB_Metrics
                    )

                    // 2.5 Subject-Aware Enhancement Diagnostics (V0.6 Quality Upgrade)
                    if (session.subjectDebugInfo != null) {
                        SubjectDiagnosticsCard(session.subjectDebugInfo)
                    }

                    // 2.6 Objective Photo Quality Metrics (V0.6 Quality Upgrade)
                    if (session.photoA_ObjectiveMetrics != null && session.photoB_ObjectiveMetrics != null) {
                        ObjectiveQualityMetricsCard(
                            metricsA = session.photoA_ObjectiveMetrics,
                            metricsB = session.photoB_ObjectiveMetrics
                        )
                    }

                    // 3. Detailed Technical Metrics Comparison Table (Requirement 4 & 5)
                    TechnicalMetricsTable(
                        metricsA = session.photoA_Metrics,
                        metricsB = session.photoB_Metrics
                    )

                    // 4. Applied Settings & Calibration Summary Card (Requirement 6)
                    AppliedSettingsCard(session)

                    // 5. Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val report = session.toJsonObject().toString(2)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("A/B Test Report", report))
                                Toast.makeText(context, "A/B Test JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                        ) {
                            Text("Copy JSON Report", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenCalibration()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricGold),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGold.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Calibrate Engine", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Draggable Split/Swipe Comparison View.
 */
@Composable
private fun SwipeComparisonView(
    bitmapA: Bitmap?,
    bitmapB: Bitmap?
) {
    if (bitmapA == null || bitmapB == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Preview frame unavailable", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        var splitFraction by remember { mutableFloatStateOf(0.50f) }

        // Bottom Layer: Photo B (SMART AUTO)
        Image(
            bitmap = bitmapB.asImageBitmap(),
            contentDescription = "SMART AUTO (B)",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Top Layer: Photo A (Standard AUTO) clipped to left side
        Image(
            bitmap = bitmapA.asImageBitmap(),
            contentDescription = "Standard AUTO (A)",
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = widthPx * splitFraction) {
                        this@drawWithContent.drawContent()
                    }
                },
            contentScale = ContentScale.Crop
        )

        // Draggable Divider Line & Knob
        val dividerX = widthPx * splitFraction
        Box(
            modifier = Modifier
                .offset { IntOffset(dividerX.roundToInt() - 1, 0) }
                .fillMaxHeight()
                .width(2.dp)
                .background(PureWhite)
        )

        // Central Handle Knob with Drag Gesture
        Box(
            modifier = Modifier
                .offset { IntOffset(dividerX.roundToInt() - 20, (constraints.maxHeight / 2) - 20) }
                .size(40.dp)
                .clip(CircleShape)
                .background(CyberCyan)
                .border(2.dp, PureWhite, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        splitFraction = ((splitFraction * widthPx + dragAmount.x) / widthPx).coerceIn(0.05f, 0.95f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Compare,
                contentDescription = "Drag to compare",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        // Overlay Labels
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("AUTO (A)", color = PureWhite, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("SMART (B)", color = CyberCyan, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

/**
 * Side-by-side Dual Viewport with zoom/pan inspection.
 */
@Composable
private fun SideBySideComparisonView(
    bitmapA: Bitmap?,
    bitmapB: Bitmap?
) {
    if (bitmapA == null || bitmapB == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Preview frames unavailable", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left: Photo A (Standard AUTO)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(0.5.dp, Color(0xFF2E384D))
        ) {
            Image(
                bitmap = bitmapA.asImageBitmap(),
                contentDescription = "AUTO",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("AUTO", color = PureWhite, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Right: Photo B (SMART AUTO)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(0.5.dp, Color(0xFF2E384D))
        ) {
            Image(
                bitmap = bitmapB.asImageBitmap(),
                contentDescription = "SMART",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("SMART", color = CyberCyan, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

/**
 * Single Toggle View with Zoom & Pan capability.
 */
@Composable
private fun SingleToggleComparisonView(
    bitmapA: Bitmap?,
    bitmapB: Bitmap?,
    isSmart: Boolean,
    onToggle: () -> Unit
) {
    val currentBitmap = if (isSmart) bitmapB else bitmapA

    if (currentBitmap == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Preview frame unavailable", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + offsetChange else Offset.Zero
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = if (isSmart) "SMART (B)" else "AUTO (A)",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformState),
            contentScale = ContentScale.Crop
        )

        // Toggle Switch Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .border(1.dp, if (isSmart) CyberCyan else PureWhite, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, _ -> }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.TextButton(onClick = onToggle) {
                    Text(
                        text = if (isSmart) "VIEWING: SMART (B)  [TAP FOR AUTO]" else "VIEWING: AUTO (A)  [TAP FOR SMART]",
                        color = if (isSmart) CyberCyan else PureWhite,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * Scores Overview Card with explicit non-artistic quality disclaimer.
 */
@Composable
private fun ScoresOverviewCard(
    metricsA: DetailedTechnicalMetrics,
    metricsB: DetailedTechnicalMetrics
) {
    val delta = metricsB.totalTechnicalScore - metricsA.totalTechnicalScore

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AUTO Score
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("AUTO SCORE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = "${metricsA.totalTechnicalScore}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = PureWhite
                    )
                    Text(metricsA.ratingLabel, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }

                // Delta Indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (delta >= 0) "+$delta pts" else "$delta pts",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (delta >= 0) NeonEmerald else HighAlertRed
                        )
                    }
                    Text("Delta", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }

                // SMART Score
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("SMART SCORE", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                    Text(
                        text = "${metricsB.totalTechnicalScore}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = CyberCyan
                    )
                    Text(metricsB.ratingLabel, style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Non-artistic disclaimer badge (Requirement 4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E2638))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scores measure quantifiable physical signal metrics (exposure accuracy, sharpness, highlight/shadow retention, and noise). They do not represent artistic or subjective aesthetic preference.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextTertiary
                )
            }
        }
    }
}

/**
 * Detailed Technical Metrics Comparison Table (Requirement 4 & 5).
 */
@Composable
private fun TechnicalMetricsTable(
    metricsA: DetailedTechnicalMetrics,
    metricsB: DetailedTechnicalMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Technical Metrics Breakdown",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Metric", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("AUTO (A)", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("SMART (B)", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                Text("Difference", modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, color = ElectricGold)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2E384D)))
            Spacer(modifier = Modifier.height(4.dp))

            // Exposure
            MetricRow(
                name = "Exposure Score",
                valA = "${metricsA.exposureScore}/100",
                valB = "${metricsB.exposureScore}/100",
                diff = "${if (metricsB.exposureScore >= metricsA.exposureScore) "+" else ""}${metricsB.exposureScore - metricsA.exposureScore}",
                isBetter = metricsB.exposureScore >= metricsA.exposureScore
            )

            // Brightness (Mean Luma)
            MetricRow(
                name = "Brightness (Luma)",
                valA = "${String.format(Locale.US, "%.1f", metricsA.brightnessLuma)}%",
                valB = "${String.format(Locale.US, "%.1f", metricsB.brightnessLuma)}%",
                diff = "${String.format(Locale.US, "%+.1f", metricsB.brightnessLuma - metricsA.brightnessLuma)}%",
                isBetter = null
            )

            // Contrast RMS
            MetricRow(
                name = "Contrast (RMS)",
                valA = "${String.format(Locale.US, "%.1f", metricsA.contrastRms)}",
                valB = "${String.format(Locale.US, "%.1f", metricsB.contrastRms)}",
                diff = "${String.format(Locale.US, "%+.1f", metricsB.contrastRms - metricsA.contrastRms)}",
                isBetter = metricsB.contrastRms >= metricsA.contrastRms
            )

            // Highlight Clipping %
            MetricRow(
                name = "Highlight Clipping",
                valA = "${String.format(Locale.US, "%.1f", metricsA.highlightClippingPct)}%",
                valB = "${String.format(Locale.US, "%.1f", metricsB.highlightClippingPct)}%",
                diff = "${String.format(Locale.US, "%+.1f", metricsB.highlightClippingPct - metricsA.highlightClippingPct)}%",
                isBetter = metricsB.highlightClippingPct <= metricsA.highlightClippingPct
            )

            // Shadow Clipping %
            MetricRow(
                name = "Shadow Clipping",
                valA = "${String.format(Locale.US, "%.1f", metricsA.shadowClippingPct)}%",
                valB = "${String.format(Locale.US, "%.1f", metricsB.shadowClippingPct)}%",
                diff = "${String.format(Locale.US, "%+.1f", metricsB.shadowClippingPct - metricsA.shadowClippingPct)}%",
                isBetter = metricsB.shadowClippingPct <= metricsA.shadowClippingPct
            )

            // Sharpness
            MetricRow(
                name = "Sharpness (Laplacian)",
                valA = "${metricsA.sharpnessScore}/100",
                valB = "${metricsB.sharpnessScore}/100",
                diff = "${if (metricsB.sharpnessScore >= metricsA.sharpnessScore) "+" else ""}${metricsB.sharpnessScore - metricsA.sharpnessScore}",
                isBetter = metricsB.sharpnessScore >= metricsA.sharpnessScore
            )

            // Noise Estimate
            MetricRow(
                name = "Noise Estimate",
                valA = "${String.format(Locale.US, "%.1f", metricsA.noiseEstimate)}",
                valB = "${String.format(Locale.US, "%.1f", metricsB.noiseEstimate)}",
                diff = "${String.format(Locale.US, "%+.1f", metricsB.noiseEstimate - metricsA.noiseEstimate)}",
                isBetter = metricsB.noiseEstimate <= metricsA.noiseEstimate
            )

            // Dynamic Range
            MetricRow(
                name = "Dynamic Range",
                valA = "${String.format(Locale.US, "%.1f", metricsA.dynamicRangeStops)} EV",
                valB = "${String.format(Locale.US, "%.1f", metricsB.dynamicRangeStops)} EV",
                diff = "${String.format(Locale.US, "%+.1f", metricsB.dynamicRangeStops - metricsA.dynamicRangeStops)} EV",
                isBetter = metricsB.dynamicRangeStops >= metricsA.dynamicRangeStops
            )

            // Color Cast
            MetricRow(
                name = "Color Cast Bias",
                valA = "${String.format(Locale.US, "%.1f", metricsA.colorCastOffset)}",
                valB = "${String.format(Locale.US, "%.1f", metricsB.colorCastOffset)}",
                diff = "${String.format(Locale.US, "%+.1f", metricsB.colorCastOffset - metricsA.colorCastOffset)}",
                isBetter = null
            )

            // Face Exposure (if available)
            if (metricsA.faceExposureLuma != null || metricsB.faceExposureLuma != null) {
                MetricRow(
                    name = "Face Exposure",
                    valA = metricsA.faceExposureLuma?.let { "${String.format(Locale.US, "%.1f", it)}%" } ?: "N/A",
                    valB = metricsB.faceExposureLuma?.let { "${String.format(Locale.US, "%.1f", it)}%" } ?: "N/A",
                    diff = if (metricsA.faceExposureLuma != null && metricsB.faceExposureLuma != null) {
                        "${String.format(Locale.US, "%+.1f", metricsB.faceExposureLuma - metricsA.faceExposureLuma)}%"
                    } else "N/A",
                    isBetter = null
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    name: String,
    valA: String,
    valB: String,
    diff: String,
    isBetter: Boolean?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Text(valA, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextSecondary)
        Text(valB, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CyberCyan)
        Text(
            text = diff,
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = when (isBetter) {
                true -> NeonEmerald
                false -> HighAlertRed
                null -> ElectricGold
            }
        )
    }
}

/**
 * Engine Decisions & Applied Settings Card (Requirement 6).
 */
@Composable
private fun AppliedSettingsCard(session: AbCaptureSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Engine Decisions & Calibration Log",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Scene / Intent:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(session.testScene.displayName, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Processing Profile:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(session.processingProfile.displayName, style = MaterialTheme.typography.bodySmall, color = CyberCyan)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Applied Calibration:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(session.appliedCalibrationSummary, style = MaterialTheme.typography.bodySmall, color = ElectricGold)
            }

            session.photoB_AppliedSettings.forEach { (k, v) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SMART $k:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(v, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }

            if (session.fallbackSettings.isNotEmpty()) {
                session.fallbackSettings.forEach { (k, v) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fallback $k:", style = MaterialTheme.typography.labelSmall, color = HighAlertRed)
                        Text(v, style = MaterialTheme.typography.bodySmall, color = HighAlertRed)
                    }
                }
            }
        }
    }
}

/**
 * Developer Diagnostics Card displaying the 15 subject-aware enhancement parameters.
 */
@Composable
private fun SubjectDiagnosticsCard(debugInfo: com.example.model.SubjectEnhancementDebugInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject-Aware Enhancement Diagnostics",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyberCyan
                )
                Text(
                    text = debugInfo.detectionEngine,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = NeonEmerald
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ParamLine("Original Subject Luminance", "${String.format("%.2f", debugInfo.originalSubjectLuminance)}%")
            ParamLine("Enhanced Subject Luminance", "${String.format("%.2f", debugInfo.enhancedSubjectLuminance)}%")
            ParamLine("Original BG Luminance", "${String.format("%.2f", debugInfo.originalBackgroundLuminance)}%")
            ParamLine("Enhanced BG Luminance", "${String.format("%.2f", debugInfo.enhancedBackgroundLuminance)}%")
            ParamLine("Subject/BG Ratio Before", String.format("%.2f", debugInfo.subjectBackgroundRatioBefore))
            ParamLine("Subject/BG Ratio After", String.format("%.2f", debugInfo.subjectBackgroundRatioAfter))
            ParamLine("Exposure Adjustment", String.format("%+.2f EV", debugInfo.exposureAdjustment))
            ParamLine("Shadow Recovery Strength", "${(debugInfo.shadowRecoveryStrength * 100).toInt()}%")
            ParamLine("Highlight Protection Strength", "${(debugInfo.highlightProtectionStrength * 100).toInt()}%")
            ParamLine("Saturation Adjustment", "${(debugInfo.saturationAdjustment * 100).toInt()}%")
            ParamLine("Contrast Adjustment", String.format("%.2fx", debugInfo.contrastAdjustment))
            ParamLine("Sharpening Strength", "${(debugInfo.sharpeningStrength * 100).toInt()}%")
            ParamLine("Enhancement Profile", debugInfo.enhancementProfile)
            ParamLine("Detection Confidence", "${(debugInfo.detectionConfidence * 100).toInt()}%")
            ParamLine("Final Output Resolution", debugInfo.finalOutputResolution)
        }
    }
}

@Composable
private fun ParamLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = PureWhite)
    }
}

/**
 * Objective Photo Quality Metrics Comparison Card (Requirement 3).
 */
@Composable
private fun ObjectiveQualityMetricsCard(
    metricsA: com.example.model.ObjectivePhotoQualityMetrics,
    metricsB: com.example.model.ObjectivePhotoQualityMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Objective Photo Quality Metrics (AUTO vs SMART)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = ElectricGold
            )

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                Text("Metric", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text("AUTO", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text("SMART", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text("Change", modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }

            ObjectiveMetricComparisonRow("Resolution", metricsA.resolution, metricsB.resolution, "Native", true)
            ObjectiveMetricComparisonRow("Sharpness (Laplacian)", String.format("%.1f", metricsA.sharpnessLaplacianVar), String.format("%.1f", metricsB.sharpnessLaplacianVar), String.format("%+.1f", metricsB.sharpnessLaplacianVar - metricsA.sharpnessLaplacianVar), metricsB.sharpnessLaplacianVar >= metricsA.sharpnessLaplacianVar)
            ObjectiveMetricComparisonRow("Highlight Clip %", "${String.format("%.2f", metricsA.highlightClippingPct)}%", "${String.format("%.2f", metricsB.highlightClippingPct)}%", String.format("%+.2f%%", metricsB.highlightClippingPct - metricsA.highlightClippingPct), metricsB.highlightClippingPct <= metricsA.highlightClippingPct + 0.5f)
            ObjectiveMetricComparisonRow("Shadow Clip %", "${String.format("%.2f", metricsA.shadowClippingPct)}%", "${String.format("%.2f", metricsB.shadowClippingPct)}%", String.format("%+.2f%%", metricsB.shadowClippingPct - metricsA.shadowClippingPct), metricsB.shadowClippingPct <= metricsA.shadowClippingPct)
            ObjectiveMetricComparisonRow("RMS Contrast", String.format("%.1f", metricsA.rmsContrast), String.format("%.1f", metricsB.rmsContrast), String.format("%+.1f", metricsB.rmsContrast - metricsA.rmsContrast), true)
            ObjectiveMetricComparisonRow("Average Luma", "${String.format("%.1f", metricsA.averageLuminance)}%", "${String.format("%.1f", metricsB.averageLuminance)}%", String.format("%+.1f%%", metricsB.averageLuminance - metricsA.averageLuminance), null)
            ObjectiveMetricComparisonRow("Subject Luma", "${String.format("%.1f", metricsA.subjectLuminance)}%", "${String.format("%.1f", metricsB.subjectLuminance)}%", String.format("%+.1f%%", metricsB.subjectLuminance - metricsA.subjectLuminance), metricsB.subjectLuminance >= metricsA.subjectLuminance)
            ObjectiveMetricComparisonRow("BG Luma", "${String.format("%.1f", metricsA.backgroundLuminance)}%", "${String.format("%.1f", metricsB.backgroundLuminance)}%", String.format("%+.1f%%", metricsB.backgroundLuminance - metricsA.backgroundLuminance), null)
            ObjectiveMetricComparisonRow("Subject/BG Ratio", String.format("%.2f", metricsA.subjectBackgroundLuminanceRatio), String.format("%.2f", metricsB.subjectBackgroundLuminanceRatio), String.format("%+.2f", metricsB.subjectBackgroundLuminanceRatio - metricsA.subjectBackgroundLuminanceRatio), metricsB.subjectBackgroundLuminanceRatio >= metricsA.subjectBackgroundLuminanceRatio)
            ObjectiveMetricComparisonRow("Saturation", "${String.format("%.1f", metricsA.saturation)}%", "${String.format("%.1f", metricsB.saturation)}%", String.format("%+.1f%%", metricsB.saturation - metricsA.saturation), metricsB.saturation >= metricsA.saturation)
            ObjectiveMetricComparisonRow("Noise Est.", String.format("%.1f", metricsA.noiseEstimate), String.format("%.1f", metricsB.noiseEstimate), String.format("%+.1f", metricsB.noiseEstimate - metricsA.noiseEstimate), metricsB.noiseEstimate <= metricsA.noiseEstimate * 1.3f)
        }
    }
}

@Composable
private fun ObjectiveMetricComparisonRow(
    name: String,
    valA: String,
    valB: String,
    diff: String,
    isBetter: Boolean?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Text(valA, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextSecondary)
        Text(valB, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CyberCyan)
        Text(
            text = diff,
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = when (isBetter) {
                true -> NeonEmerald
                false -> HighAlertRed
                null -> ElectricGold
            }
        )
    }
}
