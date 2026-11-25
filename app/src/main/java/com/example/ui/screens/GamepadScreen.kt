package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedButton
import com.example.data.AppSettings
import com.example.data.HidProfileMode
import com.example.hid.HidConstants
import com.example.ui.components.RemoteSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalTronStyle
import com.example.viewmodel.RemoteViewModel
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Native Bluetooth HID Gamepad (Report ID 4).
 * Recognized natively by Windows, Linux, Steam Deck, macOS, and Android with dual analog sticks,
 * D-Pad, ABXY/PlayStation buttons, L1/L2/R1/R2 triggers, and live deadzone calibration.
 */
@Composable
fun GamepadScreen(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    var buttonsMask by remember { mutableIntStateOf(0) }
    var hatSwitch by remember { mutableIntStateOf(HidConstants.GP_HAT_CENTER) }
    var leftStickX by remember { mutableFloatStateOf(0f) }
    var leftStickY by remember { mutableFloatStateOf(0f) }
    var rightStickX by remember { mutableFloatStateOf(0f) }
    var rightStickY by remember { mutableFloatStateOf(0f) }
    var l2Pressed by remember { mutableStateOf(false) }
    var r2Pressed by remember { mutableStateOf(false) }

    val deadzone = settings.gamepadDeadzone

    fun applyDeadzone(v: Float): Int {
        val absV = kotlin.math.abs(v)
        if (absV < deadzone) return 0
        val rescaled = ((absV - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)
        return (rescaled * 127f * if (v < 0) -1f else 1f).roundToInt()
    }

    LaunchedEffect(buttonsMask, hatSwitch, leftStickX, leftStickY, rightStickX, rightStickY, l2Pressed, r2Pressed) {
        viewModel.hidManager.sendGamepadReport(
            buttonsMask = buttonsMask,
            hatSwitch = hatSwitch,
            leftX = applyDeadzone(leftStickX),
            leftY = applyDeadzone(leftStickY),
            rightX = applyDeadzone(rightStickX),
            rightY = applyDeadzone(rightStickY),
            l2Trigger = if (l2Pressed) 255 else 0,
            r2Trigger = if (r2Pressed) 255 else 0,
        )
    }

    fun setButton(bit: Int, pressed: Boolean) {
        if (pressed) viewModel.performHaptic(false)
        buttonsMask = if (pressed) buttonsMask or bit else buttonsMask and bit.inv()
    }

    val ps = settings.gamepadPlayStationLabels
    val labelA = if (ps) "✕" else "A"
    val labelB = if (ps) "○" else "B"
    val labelX = if (ps) "□" else "X"
    val labelY = if (ps) "△" else "Y"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (settings.hidProfileMode != HidProfileMode.PRO_COMPLETE &&
            settings.hidProfileMode != HidProfileMode.GAMEPAD_ONLY
        ) {
            RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Perfil HID (${settings.hidProfileMode.shortLabel}) sem Gamepad ativo.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { viewModel.setHidProfileMode(HidProfileMode.GAMEPAD_ONLY) }) {
                        Text("Ativar Gamepad")
                    }
                }
            }
        }

        // Top Triggers Row: L2, L1, Deadzone / Layout Switch, R1, R2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HoldGamepadPill("L2", l2Pressed, Modifier.weight(1f).testTag("gp_l2")) {
                l2Pressed = it
                setButton(HidConstants.GP_BTN_L2, it)
            }
            HoldGamepadPill("L1", (buttonsMask and HidConstants.GP_BTN_L1) != 0, Modifier.weight(1f).testTag("gp_l1")) {
                setButton(HidConstants.GP_BTN_L1, it)
            }
            FilterChip(
                selected = ps,
                onClick = { viewModel.updateSettings { it.copy(gamepadPlayStationLabels = !it.gamepadPlayStationLabels) } },
                label = { Text(if (ps) "PS" else "Xbox", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("gp_toggle_style"),
            )
            HoldGamepadPill("R1", (buttonsMask and HidConstants.GP_BTN_R1) != 0, Modifier.weight(1f).testTag("gp_r1")) {
                setButton(HidConstants.GP_BTN_R1, it)
            }
            HoldGamepadPill("R2", r2Pressed, Modifier.weight(1f).testTag("gp_r2")) {
                r2Pressed = it
                setButton(HidConstants.GP_BTN_R2, it)
            }
        }

        // Main Dual-Zone Controller Deck
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // LEFT SIDE: Left Analog Stick + D-Pad
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                AnalogStickPad(
                    label = "L",
                    modifier = Modifier.size(134.dp).testTag("gp_left_stick"),
                    onStickChange = { x, y ->
                        leftStickX = x
                        leftStickY = y
                    },
                )
                Spacer(Modifier.height(10.dp))
                // Compact D-Pad
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HoldGamepadSquare("↑", hatSwitch == HidConstants.GP_HAT_UP) {
                        hatSwitch = if (it) HidConstants.GP_HAT_UP else HidConstants.GP_HAT_CENTER
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        HoldGamepadSquare("←", hatSwitch == HidConstants.GP_HAT_LEFT) {
                            hatSwitch = if (it) HidConstants.GP_HAT_LEFT else HidConstants.GP_HAT_CENTER
                        }
                        HoldGamepadSquare("L3", (buttonsMask and HidConstants.GP_BTN_L3) != 0) {
                            setButton(HidConstants.GP_BTN_L3, it)
                        }
                        HoldGamepadSquare("→", hatSwitch == HidConstants.GP_HAT_RIGHT) {
                            hatSwitch = if (it) HidConstants.GP_HAT_RIGHT else HidConstants.GP_HAT_CENTER
                        }
                    }
                    HoldGamepadSquare("↓", hatSwitch == HidConstants.GP_HAT_DOWN) {
                        hatSwitch = if (it) HidConstants.GP_HAT_DOWN else HidConstants.GP_HAT_CENTER
                    }
                }
            }

            // CENTER: Select / Guide / Start
            Column(
                modifier = Modifier.weight(0.65f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HoldGamepadPill("SELECT", (buttonsMask and HidConstants.GP_BTN_SELECT) != 0, Modifier.fillMaxWidth()) {
                    setButton(HidConstants.GP_BTN_SELECT, it)
                }
                HoldGamepadPill("GUIDE", (buttonsMask and HidConstants.GP_BTN_HOME) != 0, Modifier.fillMaxWidth()) {
                    setButton(HidConstants.GP_BTN_HOME, it)
                }
                HoldGamepadPill("START", (buttonsMask and HidConstants.GP_BTN_START) != 0, Modifier.fillMaxWidth()) {
                    setButton(HidConstants.GP_BTN_START, it)
                }
            }

            // RIGHT SIDE: ABXY Diamond + Right Analog Stick
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                // ABXY / PlayStation Diamond
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HoldGamepadCircle(labelY, Color(0xFFFBBF24), (buttonsMask and HidConstants.GP_BTN_Y) != 0, "gp_btn_y") {
                        setButton(HidConstants.GP_BTN_Y, it)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        HoldGamepadCircle(labelX, Color(0xFF38BDF8), (buttonsMask and HidConstants.GP_BTN_X) != 0, "gp_btn_x") {
                            setButton(HidConstants.GP_BTN_X, it)
                        }
                        HoldGamepadCircle(labelB, Color(0xFFFB7185), (buttonsMask and HidConstants.GP_BTN_B) != 0, "gp_btn_b") {
                            setButton(HidConstants.GP_BTN_B, it)
                        }
                    }
                    HoldGamepadCircle(labelA, Color(0xFF34D399), (buttonsMask and HidConstants.GP_BTN_A) != 0, "gp_btn_a") {
                        setButton(HidConstants.GP_BTN_A, it)
                    }
                }
                Spacer(Modifier.height(10.dp))
                AnalogStickPad(
                    label = "R",
                    modifier = Modifier.size(134.dp).testTag("gp_right_stick"),
                    onStickChange = { x, y ->
                        rightStickX = x
                        rightStickY = y
                    },
                )
            }
        }

        // Bottom Deadzone Adjustment Bar
        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Zona morta: ${(deadzone * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = deadzone,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(gamepadDeadzone = v) } },
                    valueRange = 0.04f..0.35f,
                    modifier = Modifier.weight(1f),
                )
                HoldGamepadPill("R3", (buttonsMask and HidConstants.GP_BTN_R3) != 0, Modifier.width(54.dp)) {
                    setButton(HidConstants.GP_BTN_R3, it)
                }
            }
        }
    }
}

@Composable
private fun AnalogStickPad(
    label: String,
    modifier: Modifier,
    onStickChange: (x: Float, y: Float) -> Unit,
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outlineVariant
    val tron = LocalTronStyle.current

    Box(
        modifier = modifier
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val radius = size.width / 2f
                    val center = Offset(radius, radius)
                    val maxTravel = radius * 0.62f

                    fun updateFrom(pos: Offset) {
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxTravel && dist > 0f) {
                            Offset(delta.x / dist * maxTravel, delta.y / dist * maxTravel)
                        } else delta
                        knobOffset = clamped
                        onStickChange((clamped.x / maxTravel).coerceIn(-1f, 1f), (clamped.y / maxTravel).coerceIn(-1f, 1f))
                    }

                    updateFrom(down.position)
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val active = event.changes.firstOrNull { it.pressed } ?: break
                            updateFrom(active.position)
                            active.consume()
                        }
                    } finally {
                        knobOffset = Offset.Zero
                        onStickChange(0f, 0f)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(r, r)
            drawCircle(color = surfaceColor, radius = r - 2f, center = c)
            drawCircle(
                color = if (tron.enabled) tron.glowColor else outline,
                radius = r - 2f,
                center = c,
                style = Stroke(width = 2.dp.toPx()),
            )
            // Center crosshair
            drawLine(outline.copy(alpha = 0.4f), Offset(c.x - 12f, c.y), Offset(c.x + 12f, c.y), 1.5f)
            drawLine(outline.copy(alpha = 0.4f), Offset(c.x, c.y - 12f), Offset(c.x, c.y + 12f), 1.5f)
            // Movable thumbstick cap
            val capCenter = c + knobOffset
            drawCircle(color = primary.copy(alpha = 0.28f), radius = r * 0.42f, center = capCenter)
            drawCircle(
                color = primary,
                radius = r * 0.40f,
                center = capCenter,
                style = Stroke(width = 2.2.dp.toPx()),
            )
        }
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp, color = primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HoldGamepadCircle(
    label: String,
    accentColor: Color,
    pressed: Boolean,
    tag: String,
    onHoldChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onHoldChange(true)
                    tryAwaitRelease()
                    onHoldChange(false)
                })
            }
            .testTag(tag),
        shape = CircleShape,
        color = if (pressed) accentColor else accentColor.copy(alpha = 0.16f),
        border = BorderStroke(1.5.dp, accentColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (pressed) Color.Black else accentColor,
            )
        }
    }
}

@Composable
private fun HoldGamepadSquare(
    label: String,
    pressed: Boolean,
    onHoldChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onHoldChange(true)
                    tryAwaitRelease()
                    onHoldChange(false)
                })
            },
        shape = RoundedCornerShape(10.dp),
        color = if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                color = if (pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HoldGamepadPill(
    label: String,
    pressed: Boolean,
    modifier: Modifier = Modifier,
    onHoldChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onHoldChange(true)
                    tryAwaitRelease()
                    onHoldChange(false)
                })
            },
        shape = RoundedCornerShape(12.dp),
        color = if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
