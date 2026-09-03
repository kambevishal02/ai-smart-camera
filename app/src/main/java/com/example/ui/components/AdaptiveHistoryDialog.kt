package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AdaptiveProfileStore
import com.example.model.AdaptiveLearningRecord
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

enum class HistoryFilter {
    ALL, UPDATES_ONLY, REJECTED_ONLY
}

@Composable
fun AdaptiveHistoryDialog(
    history: List<AdaptiveLearningRecord>,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    val filteredList = remember(history, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL -> history
            HistoryFilter.UPDATES_ONLY -> history.filter { it.learningDecision == "ACCEPTED_UPDATE" || it.learningDecision == "ROLLBACK" || it.learningDecision == "RESET" }
            HistoryFilter.REJECTED_ONLY -> history.filter { it.learningDecision == "REJECTED" }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("adaptive_history_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Adaptive Learning History",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "${history.size} total audit logs on-device",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_history_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.ALL,
                        onClick = { selectedFilter = HistoryFilter.ALL },
                        label = { Text("All (${history.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan,
                            labelColor = TextSecondary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.UPDATES_ONLY,
                        onClick = { selectedFilter = HistoryFilter.UPDATES_ONLY },
                        label = {
                            val count = history.count { it.learningDecision == "ACCEPTED_UPDATE" || it.learningDecision == "ROLLBACK" || it.learningDecision == "RESET" }
                            Text("Updates ($count)", fontSize = 11.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HighContrastGreen.copy(alpha = 0.2f),
                            selectedLabelColor = HighContrastGreen,
                            labelColor = TextSecondary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.REJECTED_ONLY,
                        onClick = { selectedFilter = HistoryFilter.REJECTED_ONLY },
                        label = {
                            val count = history.count { it.learningDecision == "REJECTED" }
                            Text("Rejected ($count)", fontSize = 11.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SafetyOrange.copy(alpha = 0.2f),
                            selectedLabelColor = SafetyOrange,
                            labelColor = TextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Records list
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No learning history logs match filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { record ->
                            LearningRecordCard(record)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { AdaptiveProfileStore.clearHistory() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyOrange),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SafetyOrange.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("clear_learning_history_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                    ) {
                        Text("Done", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningRecordCard(record: AdaptiveLearningRecord) {
    val badgeColor = when (record.learningDecision) {
        "ACCEPTED_UPDATE" -> HighContrastGreen
        "EVIDENCE_ACCUMULATED_NO_UPDATE" -> CyberCyan
        "REJECTED" -> SafetyOrange
        "ROLLBACK", "RESET" -> Color(0xFFCE93D8)
        else -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Time & Decision Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = record.learningDecision,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scene & Parameter Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${record.scene.displayName} (${record.lightingContext.label})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
                Text(
                    text = record.parameterName,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Parameter shift values
            if (record.learningDecision == "ACCEPTED_UPDATE" || record.learningDecision == "EVIDENCE_ACCUMULATED_NO_UPDATE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Value: ${String.format(Locale.US, "%.3f", record.previousParameter)} → ${String.format(Locale.US, "%.3f", record.newParameter)} (${String.format(Locale.US, "%+.3f", record.calculatedCorrection)})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = ElectricGold
                    )
                    Text(
                        text = "Samples: ${record.sampleCount} | Conf: ${(record.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )
                }
            }

            // Rejection reason badge if rejected
            if (record.rejectionReason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(SafetyOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Rejection Reason: ${record.rejectionReason.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = SafetyOrange
                    )
                }
            }

            // Explanation / Observation text
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.explanation,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = TextSecondary
            )
        }
    }
}
