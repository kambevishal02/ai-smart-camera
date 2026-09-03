package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CapturedPhoto
import com.example.model.DetectedFace
import com.example.model.FlashRecommendation
import com.example.model.ImageProcessingProfileType
import com.example.model.SmartCaptureStatus
import com.example.model.TestSceneType
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Mode selector switcher between SMART AUTO (AI scene + optimization), AUTO (standard camera behavior),
 * and A/B TEST (V0.3 dual comparative evaluation).
 */
@Composable
fun SmartAutoModeSwitcher(
    isSmartAuto: Boolean,
    isAbTest: Boolean = false,
    onSelectMode: (isSmartAuto: Boolean, isAbTest: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(24.dp))
            .testTag("mode_switcher"),
        color = DarkSurfaceGlass
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SMART AUTO pill
            val isSmartActive = isSmartAuto && !isAbTest
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSmartActive) CyberCyan else Color.Transparent)
                    .clickable { onSelectMode(true, false) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("mode_smart_auto"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Smart Auto",
                        tint = if (isSmartActive) Color.Black else TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "SMART AUTO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSmartActive) FontWeight.ExtraBold else FontWeight.Normal,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isSmartActive) Color.Black else TextSecondary
                    )
                }
            }

            // STANDARD AUTO pill
            val isAutoActive = !isSmartAuto && !isAbTest
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isAutoActive) ElectricGold else Color.Transparent)
                    .clickable { onSelectMode(false, false) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("mode_standard_auto"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Standard Auto",
                        tint = if (isAutoActive) Color.Black else TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "AUTO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isAutoActive) FontWeight.ExtraBold else FontWeight.Normal,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isAutoActive) Color.Black else TextSecondary
                    )
                }
            }

            // A/B TEST pill (Requirement 2 & 3)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isAbTest) NeonEmerald else Color.Transparent)
                    .clickable { onSelectMode(true, true) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("mode_ab_test"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "A/B Test",
                        tint = if (isAbTest) Color.Black else TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "A/B TEST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isAbTest) FontWeight.ExtraBold else FontWeight.Normal,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isAbTest) Color.Black else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Backward-compatible overload for SmartAutoModeSwitcher.
 */
@Composable
fun SmartAutoModeSwitcher(
    isSmartAuto: Boolean,
    onModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SmartAutoModeSwitcher(
        isSmartAuto = isSmartAuto,
        isAbTest = false,
        onSelectMode = { smart, _ -> onModeChanged(smart) },
        modifier = modifier
    )
}

/**
 * Standard test scene selector row for A/B Testing Studio (Requirement 2 & 3).
 * Allows developers to choose among the 11 standardized test scenes directly in the camera viewfinder.
 */
@Composable
fun TestSceneSelectorRow(
    selectedScene: TestSceneType,
    onSelectScene: (TestSceneType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TestSceneType.values().forEach { scene ->
            val isSelected = selectedScene == scene
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectScene(scene) }
                    .testTag("test_scene_${scene.name.lowercase()}"),
                color = if (isSelected) NeonEmerald.copy(alpha = 0.25f) else DarkSurfaceGlass,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) NeonEmerald else Color(0x33FFFFFF)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = scene.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    color = if (isSelected) NeonEmerald else TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Modern tactile Shutter Button with outer halo and processing indicator.
 */
@Composable
fun ShutterButton(
    isCapturing: Boolean,
    isProcessing: Boolean,
    isSmartAuto: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed || isCapturing) 0.88f else 1.0f,
        animationSpec = tween(120),
        label = "shutter_scale"
    )

    val haloGradient = if (isSmartAuto) {
        listOf(CyberCyan, ElectricGold, CyberCyan)
    } else {
        listOf(ElectricGold, Color.White, ElectricGold)
    }

    Box(
        modifier = modifier
            .size(84.dp)
            .scale(scale)
            .pointerInput(isCapturing, isProcessing) {
                if (!isCapturing && !isProcessing) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                            onClick()
                        }
                    )
                }
            }
            .testTag("shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    BorderStroke(4.dp, Brush.sweepGradient(haloGradient)),
                    CircleShape
                )
        )

        // Inner solid shutter core
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isProcessing) CyberCyan.copy(alpha = 0.3f) else Color.White
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = CyberCyan,
                    strokeWidth = 3.dp
                )
            } else if (isCapturing) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan)
                )
            }
        }
    }
}

/**
 * Camera switch button with 180° rotation animation.
 */
@Composable
fun CameraFlipButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "flip_rotation"
    )

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(DarkSurfaceGlass)
            .border(BorderStroke(1.dp, DarkSurfaceBorder), CircleShape)
            .clickable {
                rotationAngle += 180f
                onClick()
            }
            .testTag("switch_camera_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = "Switch Camera",
            tint = TextPrimary,
            modifier = Modifier
                .size(26.dp)
                .rotate(animatedRotation)
        )
    }
}

/**
 * Thumbnail button displaying the latest enhanced photo captured.
 */
@Composable
fun GalleryThumbnailButton(
    lastPhoto: CapturedPhoto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceGlass)
            .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)), RoundedCornerShape(14.dp))
            .clickable(enabled = lastPhoto != null) { onClick() }
            .testTag("gallery_thumbnail_button"),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = lastPhoto?.enhancedBitmap ?: lastPhoto?.originalBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Last Captured Photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Scene Profile Selector Pill Bar (Auto, Natural, Portrait, Landscape, Night, Low Light, Beach, Sunset, Forest, Food, Architecture).
 */
@Composable
fun ProfileSelectorRow(
    activeProfile: ImageProcessingProfileType,
    manualOverride: ImageProcessingProfileType?,
    isAiAuto: Boolean,
    onSelectProfile: (ImageProcessingProfileType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI AUTO pill
        val isAutoSelected = manualOverride == null
        ProfileChip(
            title = "✨ AI AUTO",
            isSelected = isAutoSelected,
            badgeColor = CyberCyan,
            onClick = { onSelectProfile(null) },
            testTag = "profile_chip_ai_auto"
        )

        // All 10 Profiles
        ImageProcessingProfileType.values().forEach { profile ->
            val isSelected = manualOverride == profile || (isAutoSelected && activeProfile == profile)
            ProfileChip(
                title = profile.displayName.uppercase(),
                isSelected = isSelected,
                badgeColor = if (isSelected) ElectricGold else TextSecondary,
                onClick = { onSelectProfile(profile) },
                testTag = "profile_chip_${profile.name.lowercase()}"
            )
        }
    }
}

@Composable
private fun ProfileChip(
    title: String,
    isSelected: Boolean,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) badgeColor else Color(0x33FFFFFF)
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        color = if (isSelected) badgeColor.copy(alpha = 0.22f) else DarkSurfaceGlass
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            ),
            color = if (isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/**
 * Rule of thirds grid overlay.
 */
@Composable
fun RuleOfThirdsGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val gridColor = Color(0x33FFFFFF)
        val strokeWidth = 1.dp.toPx()

        // Vertical lines
        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
        drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth)

        // Horizontal lines
        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
        drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth)
    }
}

/**
 * Visual face detection reticle box.
 */
@Composable
fun FaceDetectionOverlay(
    faces: List<DetectedFace>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cornerLen = 16.dp.toPx()
        val strokeW = 2.dp.toPx()

        faces.forEach { face ->
            val left = face.bounds.left * w
            val top = face.bounds.top * h
            val right = face.bounds.right * w
            val bottom = face.bounds.bottom * h

            val boxColor = CyberCyan

            // Top-left corner
            drawLine(boxColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
            drawLine(boxColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

            // Top-right corner
            drawLine(boxColor, Offset(right, top), Offset(right - cornerLen, top), strokeW)
            drawLine(boxColor, Offset(right, top), Offset(right, top + cornerLen), strokeW)

            // Bottom-left corner
            drawLine(boxColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
            drawLine(boxColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)

            // Bottom-right corner
            drawLine(boxColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
            drawLine(boxColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)
        }
    }
}
