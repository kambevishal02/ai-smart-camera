package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraHardwareInfo
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CameraTopBar(
    isFlashOn: Boolean,
    isGridOn: Boolean,
    hardwareInfo: CameraHardwareInfo,
    currentEvIndex: Int,
    onToggleFlash: () -> Unit,
    onToggleGrid: () -> Unit,
    onSetEvIndex: (Int) -> Unit,
    onOpenDebugScreen: () -> Unit,
    onOpenLogsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEvSlider by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Button
            TopBarIconButton(
                icon = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Toggle Flash",
                isActive = isFlashOn,
                activeColor = ElectricGold,
                onClick = onToggleFlash,
                testTag = "flash_toggle_button"
            )

            // Grid Button
            TopBarIconButton(
                icon = if (isGridOn) Icons.Default.Grid3x3 else Icons.Default.GridOff,
                contentDescription = "Toggle Grid",
                isActive = isGridOn,
                activeColor = CyberCyan,
                onClick = onToggleGrid,
                testTag = "grid_toggle_button"
            )

            // EV Exposure Compensation Pill
            if (hardwareInfo.isEvCompensationSupported) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (showEvSlider || currentEvIndex != 0) CyberCyan else DarkSurfaceBorder
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { showEvSlider = !showEvSlider }
                        .testTag("ev_button"),
                    color = DarkSurfaceGlass
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "EV Control",
                            tint = if (currentEvIndex != 0) CyberCyan else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val evVal = currentEvIndex * hardwareInfo.evStep
                        val formattedEv = if (evVal >= 0) "+${String.format("%.1f", evVal)}" else String.format("%.1f", evVal)
                        Text(
                            text = "${formattedEv} EV",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (currentEvIndex != 0) CyberCyan else TextPrimary
                        )
                    }
                }
            }

            // Live Logs Sheet Button
            TopBarIconButton(
                icon = Icons.Default.ListAlt,
                contentDescription = "Developer Logs",
                isActive = false,
                activeColor = TextPrimary,
                onClick = onOpenLogsSheet,
                testTag = "open_logs_button"
            )

            // Developer / Debug Screen Button (Oppo F17 Testing requirement)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.8f)), RoundedCornerShape(16.dp))
                    .clickable { onOpenDebugScreen() }
                    .testTag("open_debug_screen"),
                color = DarkSurfaceGlass
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug Screen",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DEV DEBUG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = CyberCyan
                    )
                }
            }
        }

        // Animated EV Slider when expanded
        AnimatedVisibility(
            visible = showEvSlider,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceGlass)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Exposure Compensation",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${String.format("%.2f", currentEvIndex * hardwareInfo.evStep)} EV",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyberCyan
                    )
                }
                Slider(
                    value = currentEvIndex.toFloat(),
                    onValueChange = { onSetEvIndex(it.toInt()) },
                    valueRange = hardwareInfo.evRangeMin.toFloat()..hardwareInfo.evRangeMax.toFloat(),
                    steps = (hardwareInfo.evRangeMax - hardwareInfo.evRangeMin) - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.testTag("ev_slider")
                )
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DarkSurfaceGlass)
            .border(
                BorderStroke(1.dp, if (isActive) activeColor else DarkSurfaceBorder),
                CircleShape
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) activeColor else TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
