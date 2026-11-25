package com.example.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.res.stringResource
import androidx.input.motionprediction.MotionEventPredictor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AppSettings
import com.example.data.ControlMode
import com.example.data.DigitizerMonitorMapping
import com.example.data.HidProfileMode
import com.example.data.TargetOsProfile
import com.example.ui.components.RemoteSurface
import com.example.ui.components.TactileKeyButton
import com.example.ui.theme.LocalTronStyle
import com.example.viewmodel.RemoteViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class InkSegment(
    val start: Offset,
    val end: Offset,
    val pressure: Float,
    val isEraser: Boolean,
)

/**
 * Professional HID Digitizer Graphics Tablet (Report ID 5):
 * - Absolute coordinate mapping (0..32767) with Multi-Monitor region selection
 * - Real Stylus / S Pen Pressure sensitivity, Tilt X/Y, Hover In-Range cursor, Barrel Button & Eraser
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DigitizerScreen(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    var eraserMode by remember { mutableStateOf(false) }
    var barrelButtonHeld by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var hoverPoint by remember { mutableStateOf<Offset?>(null) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }
    var predictedPoint by remember { mutableStateOf<Offset?>(null) }
    var livePressure by remember { mutableFloatStateOf(0f) }
    var liveTiltX by remember { mutableIntStateOf(0) }
    var liveTiltY by remember { mutableIntStateOf(0) }
    var liveToolLabel by remember { mutableStateOf("Toque ou passe a S Pen") }

    val view = LocalView.current
    val predictor = remember(view) { runCatching { MotionEventPredictor.newInstance(view) }.getOrNull() }
    val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()

    val segments = remember { mutableStateListOf<InkSegment>() }
    val monitor = settings.digitizerMonitorMapping
    val primary = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
    val tron = LocalTronStyle.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (settings.targetOsProfile == TargetOsProfile.IOS) {
            RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), highlighted = true) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ios_digitizer_warning),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { viewModel.selectMode(ControlMode.MOUSE_KEYBOARD) }) {
                        Text("Touchpad")
                    }
                }
            }
        } else if (settings.hidProfileMode != HidProfileMode.PRO_COMPLETE &&
            settings.hidProfileMode != HidProfileMode.DIGITIZER_ONLY
        ) {
            RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Perfil HID atual (${settings.hidProfileMode.shortLabel}) não inclui Mesa Digitalizadora.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { viewModel.setHidProfileMode(HidProfileMode.DIGITIZER_ONLY) }) {
                        Text("Ativar Mesa")
                    }
                }
            }
        }

        // Multi-Monitor Mapping Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Monitor, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
            DigitizerMonitorMapping.entries.forEach { mapOption ->
                FilterChip(
                    selected = monitor == mapOption,
                    onClick = { viewModel.updateSettings { it.copy(digitizerMonitorMapping = mapOption) } },
                    label = { Text(mapOption.label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        // Stylus Tool Controls (Pen / Eraser / Barrel / Undo / Clear)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TactileKeyButton(
                label = "Caneta",
                icon = Icons.Default.Draw,
                active = !eraserMode,
                onClick = { eraserMode = false },
                modifier = Modifier.weight(1f),
                minHeight = 44.dp,
                testTagName = "digitizer_pen_btn",
            )
            TactileKeyButton(
                label = "Borracha",
                icon = Icons.Default.CleaningServices,
                active = eraserMode,
                onClick = { eraserMode = true },
                modifier = Modifier.weight(1f),
                minHeight = 44.dp,
                testTagName = "digitizer_eraser_btn",
            )
            TactileKeyButton(
                label = "Botão S Pen",
                active = barrelButtonHeld,
                onClick = { barrelButtonHeld = !barrelButtonHeld },
                modifier = Modifier.weight(1.1f),
                minHeight = 44.dp,
            )
            TactileKeyButton(
                label = "",
                icon = Icons.AutoMirrored.Filled.Undo,
                onClick = {
                    viewModel.performHaptic(false)
                    viewModel.hidManager.sendAdaptiveShortcut("CTRL+Z", settings.targetOsProfile, "Desfazer")
                },
                minHeight = 44.dp,
            )
            TactileKeyButton(
                label = "",
                icon = Icons.Default.DeleteSweep,
                onClick = { segments.clear() },
                minHeight = 44.dp,
            )
        }

        // Active Absolute Digitizer Drawing Surface
        RemoteSurface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("digitizer_canvas"),
            shape = RoundedCornerShape(20.dp),
            highlighted = true,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInteropFilter { event ->
                        val w = canvasSize.width.coerceAtLeast(1).toFloat()
                        val h = canvasSize.height.coerceAtLeast(1).toFloat()
                        val rawX = (event.x / w).coerceIn(0f, 1f)
                        val rawY = (event.y / h).coerceIn(0f, 1f)

                        // Map into selected monitor range (e.g. Monitor 2 of 2 = 0.5..1.0)
                        val mappedX = monitor.xStart + rawX * (monitor.xEnd - monitor.xStart)
                        val toolType = event.getToolType(0)
                        val isHardwareEraser = toolType == MotionEvent.TOOL_TYPE_ERASER
                        val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS || isHardwareEraser
                        val isBarrel = barrelButtonHeld ||
                            (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                        val activeEraser = eraserMode || isHardwareEraser

                        val pressure = event.pressure.coerceIn(0f, 1f)
                        val tiltRad = event.getAxisValue(MotionEvent.AXIS_TILT)
                        val orientRad = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)
                        val tiltDeg = Math.toDegrees(tiltRad.toDouble()).toFloat()
                        val tx = (sin(orientRad.toDouble()) * tiltDeg).roundToInt().coerceIn(-90, 90)
                        val ty = (cos(orientRad.toDouble()) * tiltDeg).roundToInt().coerceIn(-90, 90)

                        livePressure = pressure
                        liveTiltX = tx
                        liveTiltY = ty
                        liveToolLabel = if (isStylus) {
                            if (activeEraser) "Caneta Ativa (Borracha)" else "Caneta Ativa (S Pen)"
                        } else {
                            if (activeEraser) "Toque (Borracha)" else "Toque Absoluto"
                        }

                        val pt = Offset(event.x, event.y)
                        predictor?.record(event)
                        when (event.actionMasked) {
                            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                                hoverPoint = pt
                                predictedPoint = null
                                viewModel.hidManager.sendDigitizerReport(
                                    tipDown = false,
                                    barrelPressed = isBarrel,
                                    eraserActive = activeEraser,
                                    inRangeHover = true,
                                    normX = mappedX,
                                    normY = rawY,
                                    pressure = 0f,
                                    tiltXDeg = tx,
                                    tiltYDeg = ty,
                                )
                            }
                            MotionEvent.ACTION_HOVER_EXIT -> {
                                hoverPoint = null
                                predictedPoint = null
                                viewModel.hidManager.sendDigitizerReport(
                                    tipDown = false,
                                    barrelPressed = false,
                                    eraserActive = false,
                                    inRangeHover = false,
                                    normX = mappedX,
                                    normY = rawY,
                                    pressure = 0f,
                                )
                            }
                            MotionEvent.ACTION_DOWN -> {
                                runCatching { view.requestUnbufferedDispatch(event) }
                                hoverPoint = pt
                                lastPoint = pt
                                predictedPoint = null
                                viewModel.hidManager.sendDigitizerReport(
                                    tipDown = true,
                                    barrelPressed = isBarrel,
                                    eraserActive = activeEraser,
                                    inRangeHover = true,
                                    normX = mappedX,
                                    normY = rawY,
                                    pressure = pressure.coerceAtLeast(0.15f),
                                    tiltXDeg = tx,
                                    tiltYDeg = ty,
                                )
                            }
                            MotionEvent.ACTION_MOVE -> {
                                hoverPoint = pt
                                lastPoint?.let { prev ->
                                    segments.add(InkSegment(prev, pt, pressure.coerceAtLeast(0.2f), activeEraser))
                                    if (segments.size > 600) segments.removeAt(0)
                                }
                                lastPoint = pt
                                val predictedEvent = runCatching { predictor?.predict() }.getOrNull()
                                predictedPoint = if (predictedEvent != null && predictedEvent.pointerCount > 0) {
                                    Offset(predictedEvent.x, predictedEvent.y)
                                } else {
                                    null
                                }
                                viewModel.hidManager.sendDigitizerReport(
                                    tipDown = true,
                                    barrelPressed = isBarrel,
                                    eraserActive = activeEraser,
                                    inRangeHover = true,
                                    normX = mappedX,
                                    normY = rawY,
                                    pressure = pressure.coerceAtLeast(0.15f),
                                    tiltXDeg = tx,
                                    tiltYDeg = ty,
                                )
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                lastPoint = null
                                hoverPoint = null
                                predictedPoint = null
                                viewModel.hidManager.sendDigitizerReport(
                                    tipDown = false,
                                    barrelPressed = false,
                                    eraserActive = activeEraser,
                                    inRangeHover = false,
                                    normX = mappedX,
                                    normY = rawY,
                                    pressure = 0f,
                                )
                            }
                        }
                        true
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Subtle proportion grid (16:9 canvas reference)
                    val step = 42.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        drawLine(if (tron.enabled) tron.gridColor else gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                        x += step
                    }
                    var y = step
                    while (y < size.height) {
                        drawLine(if (tron.enabled) tron.gridColor else gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                        y += step
                    }

                    // Pressure-variable ink strokes
                    segments.forEach { seg ->
                        drawLine(
                            color = if (seg.isEraser) bgColor else primary,
                            start = seg.start,
                            end = seg.end,
                            strokeWidth = (if (seg.isEraser) 18f else 2.5f + seg.pressure * 10f).dp.toPx() * 0.45f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Transient predicted tip segment (discarded cleanly on next actual event)
                    val tip = lastPoint
                    val pred = predictedPoint
                    if (tip != null && pred != null && !eraserMode) {
                        drawLine(
                            color = primary.copy(alpha = 0.55f),
                            start = tip,
                            end = pred,
                            strokeWidth = (2.5f + livePressure * 10f).dp.toPx() * 0.45f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Hover / Stylus live indicator ring
                    hoverPoint?.let { hp ->
                        drawCircle(
                            color = primary.copy(alpha = 0.25f),
                            radius = (14f + livePressure * 18f).dp.toPx() * 0.5f,
                            center = hp,
                        )
                        drawCircle(
                            color = primary,
                            radius = 4.dp.toPx(),
                            center = hp,
                        )
                    }
                }

                // Live Stylus Telemetry HUD
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = buildString {
                            append("$liveToolLabel • ${monitor.label}")
                            if (deviceProfile.isLtpo120HzCapable) append(" • ${deviceProfile.activeRefreshRateHz.toInt()}Hz")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Pressão: ${(livePressure * 100).roundToInt()}%  Tilt: ${liveTiltX}°/${liveTiltY}°",
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                    )
                }
            }
        }
    }
}
