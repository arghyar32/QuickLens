package com.dolphin.quicklens.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dolphin.quicklens.data.LensPreferencesRepository
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onDelayChanged: (Int) -> Unit,
    onHapticsToggled: (Boolean) -> Unit,
    onNativeCtsToggled: (Boolean) -> Unit,
    onLauncherTriggerToggled: (Boolean) -> Unit,
    onNavBarGestureToggled: (Boolean) -> Unit,
    onNavBarGestureIndicatorToggled: (Boolean) -> Unit,
    onNavBarGestureWidthChanged: (Int) -> Unit,
    onNavBarGestureOffsetXChanged: (Int) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAutostartSettings: () -> Unit,
    onTestTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection()

            AccessibilityStatusCard(
                isActive = state.isAccessibilityActive,
                onOpenSettings = onOpenAccessibilitySettings
            )

            // HyperOS / MIUI RAM & Autostart Protection Card
            MiuiBackgroundProtectionCard(
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenAutostartSettings = onOpenAutostartSettings
            )

            TriggerMethodsCard(
                navBarGestureEnabled = state.navBarGestureEnabled,
                navBarGestureIndicator = state.navBarGestureIndicator,
                navBarGestureWidthDp = state.navBarGestureWidthDp,
                navBarGestureOffsetX = state.navBarGestureOffsetX,
                launcherTriggerEnabled = state.launcherDirectTrigger,
                onNavBarGestureToggled = onNavBarGestureToggled,
                onNavBarGestureIndicatorToggled = onNavBarGestureIndicatorToggled,
                onNavBarGestureWidthChanged = onNavBarGestureWidthChanged,
                onNavBarGestureOffsetXChanged = onNavBarGestureOffsetXChanged,
                onLauncherTriggerToggled = onLauncherTriggerToggled
            )

            DelayPreferenceCard(
                currentDelayMs = state.tileTriggerDelayMs,
                onDelayChanged = onDelayChanged
            )

            PreferencesTogglesCard(
                nativeCtsEnabled = state.useNativeCTS,
                hapticEnabled = state.hapticFeedbackEnabled,
                onNativeCtsToggled = onNativeCtsToggled,
                onHapticsToggled = onHapticsToggled
            )

            TestTriggerCard(onTestTrigger = onTestTrigger)

            TileSetupGuideCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "QuickLens",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Instant Visual Search Enabler",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "v2.5 (Persistent)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun AccessibilityStatusCard(
    isActive: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                Color(0xFF0F3822).copy(alpha = 0.15f)
            } else {
                Color(0xFF8A5300).copy(alpha = 0.15f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Color(0xFF1B874B) else Color(0xFFE28A00)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Outlined.CheckCircle else Icons.Outlined.AccessibilityNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isActive) "Zero-Permission Lens Armed" else "Enable Accessibility Service",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isActive) {
                            "Ready! No permission prompts or recording dialogs will appear."
                        } else {
                            "Required once to allow instant screen capture for Google Lens."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isActive) {
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE28A00)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turn On in Accessibility Settings")
                }
            } else {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Accessibility Service Is Active")
                }
            }
        }
    }
}

@Composable
private fun MiuiBackgroundProtectionCard(
    onOpenBatterySettings: () -> Unit,
    onOpenAutostartSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "RAM & Background Lock (MIUI / HyperOS)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "MIUI / HyperOS turns off services when swiping apps from Recent Apps. To keep QuickLens permanently active:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "No Restrictions",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedButton(
                    onClick = onOpenAutostartSettings,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Enable Autostart",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DelayPreferenceCard(
    currentDelayMs: Int,
    onDelayChanged: (Int) -> Unit
) {
    var sliderPosition by remember(currentDelayMs) {
        mutableFloatStateOf(currentDelayMs.toFloat())
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shade Retreat Delay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Delay after tile click so the notification shade retracts cleanly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = "${sliderPosition.roundToInt()} ms",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Slider(
                value = sliderPosition,
                onValueChange = { newValue ->
                    val stepped = ((newValue / LensPreferencesRepository.DELAY_STEP_MS).roundToInt() * LensPreferencesRepository.DELAY_STEP_MS).toFloat()
                    sliderPosition = stepped
                },
                onValueChangeFinished = {
                    onDelayChanged(sliderPosition.roundToInt())
                },
                valueRange = LensPreferencesRepository.MIN_DELAY_MS.toFloat()..LensPreferencesRepository.MAX_DELAY_MS.toFloat(),
                steps = (LensPreferencesRepository.MAX_DELAY_MS - LensPreferencesRepository.MIN_DELAY_MS) / LensPreferencesRepository.DELAY_STEP_MS - 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${LensPreferencesRepository.MIN_DELAY_MS} ms (Fast)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Recommended: 300 ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${LensPreferencesRepository.MAX_DELAY_MS} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TriggerMethodsCard(
    navBarGestureEnabled: Boolean,
    navBarGestureIndicator: Boolean,
    navBarGestureWidthDp: Int,
    navBarGestureOffsetX: Int,
    launcherTriggerEnabled: Boolean,
    onNavBarGestureToggled: (Boolean) -> Unit,
    onNavBarGestureIndicatorToggled: (Boolean) -> Unit,
    onNavBarGestureWidthChanged: (Int) -> Unit,
    onNavBarGestureOffsetXChanged: (Int) -> Unit,
    onLauncherTriggerToggled: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Trigger Methods",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            PreferenceToggleItem(
                icon = Icons.Outlined.Gesture,
                title = "Navigation Bar / Home Long-Press",
                subtitle = "Hold bottom center bar or Home button to search. Works automatically with Gestures and 3-Button Navigation.",
                checked = navBarGestureEnabled,
                onCheckedChange = onNavBarGestureToggled
            )

            if (navBarGestureEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                PreferenceToggleItem(
                    icon = Icons.Outlined.Info,
                    title = "Show Navigation Bar Pill",
                    subtitle = "Shows a pill guide for Gesture navigation (hidden automatically in 3-Button mode)",
                    checked = navBarGestureIndicator,
                    onCheckedChange = onNavBarGestureIndicatorToggled
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pill / Trigger Width Slider
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pill / Trigger Width",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$navBarGestureWidthDp dp",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Make the pill and touch area smaller or larger",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    var sliderWidth by remember(navBarGestureWidthDp) {
                        mutableFloatStateOf(navBarGestureWidthDp.toFloat())
                    }
                    Slider(
                        value = sliderWidth,
                        onValueChange = { sliderWidth = it },
                        onValueChangeFinished = {
                            val stepped = (sliderWidth / 10).roundToInt() * 10
                            onNavBarGestureWidthChanged(stepped)
                        },
                        valueRange = LensPreferencesRepository.MIN_NAV_BAR_GESTURE_WIDTH_DP.toFloat()..
                                LensPreferencesRepository.MAX_NAV_BAR_GESTURE_WIDTH_DP.toFloat(),
                        steps = 17,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Position (Left / Center / Right)
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Horizontal Position",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val posLabel = when {
                            navBarGestureOffsetX < -20 -> "Left (${abs(navBarGestureOffsetX)}%)"
                            navBarGestureOffsetX > 20 -> "Right (${navBarGestureOffsetX}%)"
                            else -> "Center"
                        }
                        Text(
                            text = posLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Place the pill left, center, right, or anywhere in between",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset buttons [ Left ] [ Center ] [ Right ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionPresetButton(
                            text = "Left",
                            selected = navBarGestureOffsetX <= -70,
                            onClick = { onNavBarGestureOffsetXChanged(-100) },
                            modifier = Modifier.weight(1f)
                        )
                        PositionPresetButton(
                            text = "Center",
                            selected = abs(navBarGestureOffsetX) < 20,
                            onClick = { onNavBarGestureOffsetXChanged(0) },
                            modifier = Modifier.weight(1f)
                        )
                        PositionPresetButton(
                            text = "Right",
                            selected = navBarGestureOffsetX >= 70,
                            onClick = { onNavBarGestureOffsetXChanged(100) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    var sliderOffset by remember(navBarGestureOffsetX) {
                        mutableFloatStateOf(navBarGestureOffsetX.toFloat())
                    }
                    Slider(
                        value = sliderOffset,
                        onValueChange = { sliderOffset = it },
                        onValueChangeFinished = {
                            val stepped = (sliderOffset / 5).roundToInt() * 5
                            onNavBarGestureOffsetXChanged(stepped)
                        },
                        valueRange = -100f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            PreferenceToggleItem(
                icon = Icons.Outlined.TouchApp,
                title = "Launcher Tap to Search",
                subtitle = "Tap app icon to search directly; if permissions are missing, opens settings",
                checked = launcherTriggerEnabled,
                onCheckedChange = onLauncherTriggerToggled
            )

            if (launcherTriggerEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Tip: Long-press the QuickLens app icon on your home screen and select 'Settings' to open this screen at any time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionPresetButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreferencesTogglesCard(
    nativeCtsEnabled: Boolean,
    hapticEnabled: Boolean,
    onNativeCtsToggled: (Boolean) -> Unit,
    onHapticsToggled: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            PreferenceToggleItem(
                icon = Icons.Outlined.Search,
                title = "Try Circle to Search First",
                subtitle = "Try system CTS first, with immediate Google Lens fallback",
                checked = nativeCtsEnabled,
                onCheckedChange = onNativeCtsToggled
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            PreferenceToggleItem(
                icon = Icons.Outlined.Vibration,
                title = "Haptic Feedback",
                subtitle = "Vibrate subtly when triggering from Quick Settings tile",
                checked = hapticEnabled,
                onCheckedChange = onHapticsToggled
            )
        }
    }
}

@Composable
private fun PreferenceToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TestTriggerCard(onTestTrigger: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Test Trigger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Minimizes QuickLens and captures the screen directly into Google Lens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onTestTrigger,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Launch,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Trigger Now")
            }
        }
    }
}

@Composable
private fun TileSetupGuideCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Quick Settings Tile Setup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "1. Swipe down twice from the top of the screen to open Quick Settings.\n" +
                        "2. Tap the Edit (pencil) button to view available inactive tiles.\n" +
                        "3. Find 'QuickLens' and drag it to your active tiles.\n" +
                        "4. Tap QuickLens anytime to search anything on screen with zero permission prompts!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
            )
        }
    }
}
