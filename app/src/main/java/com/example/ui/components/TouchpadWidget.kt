package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ScrollbarPosition
import com.example.ui.theme.LocalTronStyle
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Advanced Multi-Touch Precision Trackpad supporting:
 * - 1-finger move + tap-to-click + Pen ink trail
 * - 2-finger tap (right-click), 2-finger vertical & horizontal scroll, 2-finger Pinch-to-Zoom
 * - 3-finger tap (middle-click), 3-finger directional swipes (Back, Forward, Task View, Desktop)
 * - 4-finger horizontal swipe (Virtual Desktop switching)
 * - Dedicated Vertical or Horizontal Scrollbar with inertia fling
 */
@Composable
fun TouchpadWithScrollbar(
    modifier: Modifier = Modifier,
    scrollbarPosition: ScrollbarPosition = ScrollbarPosition.RIGHT,
    showScrollbar: Boolean = true,
    tapToClick: Boolean = true,
    penDrawingMode: Boolean = false,
    airMouseActive: Boolean = false,
    gestureHudText: String? = null,
    onMove: (dx: Float, dy: Float) -> Unit,
    onTapLeftClick: () -> Unit,
    onPenHoldChange: (Boolean) -> Unit,
    onScrollDelta: (Float) -> Unit,
    onHorizontalScrollDelta: (Float) -> Unit = {},
    onScrollFling: (Float) -> Unit,
    onPinchZoom: (zoomIn: Boolean) -> Unit = {},
    onGesture: (String) -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit = {},
) {
    when {
        !showScrollbar -> {
            TouchpadSurface(
                modifier = modifier,
                tapToClick = tapToClick,
                penDrawingMode = penDrawingMode,
                airMouseActive = airMouseActive,
                gestureHudText = gestureHudText,
                onMove = onMove,
                onTapLeftClick = onTapLeftClick,
                onPenHoldChange = onPenHoldChange,
                onScrollDelta = onScrollDelta,
                onHorizontalScrollDelta = onHorizontalScrollDelta,
                onScrollFling = onScrollFling,
                onPinchZoom = onPinchZoom,
                onGesture = onGesture,
                overlayContent = overlayContent,
            )
        }
        scrollbarPosition == ScrollbarPosition.BOTTOM -> {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TouchpadSurface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    tapToClick = tapToClick,
                    penDrawingMode = penDrawingMode,
                    airMouseActive = airMouseActive,
                    gestureHudText = gestureHudText,
                    onMove = onMove,
                    onTapLeftClick = onTapLeftClick,
                    onPenHoldChange = onPenHoldChange,
                    onScrollDelta = onScrollDelta,
                    onHorizontalScrollDelta = onHorizontalScrollDelta,
                    onScrollFling = onScrollFling,
                    onPinchZoom = onPinchZoom,
                    onGesture = onGesture,
                    overlayContent = overlayContent,
                )
                HorizontalScrollbarStrip(
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    onScrollDelta = onScrollDelta,
                    onScrollFling = onScrollFling,
                )
            }
        }
        scrollbarPosition == ScrollbarPosition.LEFT -> {
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VerticalScrollbarStrip(
                    modifier = Modifier.width(34.dp).fillMaxHeight(),
                    onScrollDelta = onScrollDelta,
                    onScrollFling = onScrollFling,
                )
                TouchpadSurface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    tapToClick = tapToClick,
                    penDrawingMode = penDrawingMode,
                    airMouseActive = airMouseActive,
                    gestureHudText = gestureHudText,
                    onMove = onMove,
                    onTapLeftClick = onTapLeftClick,
                    onPenHoldChange = onPenHoldChange,
                    onScrollDelta = onScrollDelta,
                    onHorizontalScrollDelta = onHorizontalScrollDelta,
                    onScrollFling = onScrollFling,
                    onPinchZoom = onPinchZoom,
                    onGesture = onGesture,
                    overlayContent = overlayContent,
                )
            }
        }
        else -> {
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TouchpadSurface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    tapToClick = tapToClick,
                    penDrawingMode = penDrawingMode,
                    airMouseActive = airMouseActive,
                    gestureHudText = gestureHudText,
                    onMove = onMove,
                    onTapLeftClick = onTapLeftClick,
                    onPenHoldChange = onPenHoldChange,
                    onScrollDelta = onScrollDelta,
                    onHorizontalScrollDelta = onHorizontalScrollDelta,
                    onScrollFling = onScrollFling,
                    onPinchZoom = onPinchZoom,
                    onGesture = onGesture,
                    overlayContent = overlayContent,
                )
                VerticalScrollbarStrip(
                    modifier = Modifier.width(34.dp).fillMaxHeight(),
                    onScrollDelta = onScrollDelta,
                    onScrollFling = onScrollFling,
                )
            }
        }
    }
}

@Composable
private fun TouchpadSurface(
    modifier: Modifier,
    tapToClick: Boolean,
    penDrawingMode: Boolean,
    airMouseActive: Boolean,
    gestureHudText: String?,
    onMove: (dx: Float, dy: Float) -> Unit,
    onTapLeftClick: () -> Unit,
    onPenHoldChange: (Boolean) -> Unit,
    onScrollDelta: (Float) -> Unit,
    onHorizontalScrollDelta: (Float) -> Unit,
    onScrollFling: (Float) -> Unit,
    onPinchZoom: (Boolean) -> Unit,
    onGesture: (String) -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit,
) {
    val tron = LocalTronStyle.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
    val inkTrail = remember { mutableStateListOf<Offset>() }
    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }

    RemoteSurface(
        modifier = modifier.testTag("touchpad_surface"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        highlighted = penDrawingMode || airMouseActive,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tapToClick, penDrawingMode) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        var maxPointers = 1
                        var totalDistance = 0f
                        var multiDx = 0f
                        var multiDy = 0f
                        var multiTriggered = false
                        var lastTwoFingerDy = 0f
                        var lastPinchDistance = -1f
                        var pinchAccum = 0f
                        var lastEventTime = downTime

                        activeTouchPoint = firstDown.position
                        if (penDrawingMode) {
                            inkTrail.clear()
                            inkTrail.add(firstDown.position)
                            onPenHoldChange(true)
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                if (pressed.size > maxPointers) maxPointers = pressed.size
                                val now = System.currentTimeMillis()
                                val dtMs = (now - lastEventTime).coerceAtLeast(1L)
                                lastEventTime = now

                                when {
                                    pressed.size >= 4 -> {
                                        val avgDelta = pressed.fold(Offset.Zero) { acc, c ->
                                            acc + c.positionChange()
                                        } / pressed.size.toFloat()
                                        multiDx += avgDelta.x
                                        totalDistance += hypot(avgDelta.x, avgDelta.y)
                                        if (!multiTriggered && abs(multiDx) > 85f) {
                                            multiTriggered = true
                                            onGesture(if (multiDx > 0) "FOUR_FINGER_RIGHT" else "FOUR_FINGER_LEFT")
                                        }
                                        pressed.forEach { it.consume() }
                                    }

                                    pressed.size == 3 -> {
                                        val avgDelta = pressed.fold(Offset.Zero) { acc, c ->
                                            acc + c.positionChange()
                                        } / 3f
                                        multiDx += avgDelta.x
                                        multiDy += avgDelta.y
                                        totalDistance += hypot(avgDelta.x, avgDelta.y)
                                        if (!multiTriggered && (abs(multiDx) > 85f || abs(multiDy) > 85f)) {
                                            multiTriggered = true
                                            if (abs(multiDx) > abs(multiDy)) {
                                                onGesture(if (multiDx > 0) "THREE_FINGER_RIGHT" else "THREE_FINGER_LEFT")
                                            } else {
                                                onGesture(if (multiDy > 0) "THREE_FINGER_DOWN" else "THREE_FINGER_UP")
                                            }
                                        }
                                        pressed.forEach { it.consume() }
                                    }

                                    pressed.size == 2 -> {
                                        val p0 = pressed[0].position
                                        val p1 = pressed[1].position
                                        val curDist = hypot(p1.x - p0.x, p1.y - p0.y)
                                        val avgDelta = (pressed[0].positionChange() + pressed[1].positionChange()) / 2f
                                        totalDistance += hypot(avgDelta.x, avgDelta.y)

                                        if (lastPinchDistance > 0f) {
                                            val distDelta = curDist - lastPinchDistance
                                            val moveMag = hypot(avgDelta.x, avgDelta.y)
                                            // Detect pinch-to-zoom when finger spacing changes faster than translation
                                            if (abs(distDelta) > moveMag * 1.15f && abs(distDelta) > 2.5f) {
                                                pinchAccum += distDelta
                                                if (abs(pinchAccum) >= 42f) {
                                                    onPinchZoom(pinchAccum > 0f)
                                                    pinchAccum = 0f
                                                }
                                            } else {
                                                if (abs(avgDelta.y) >= abs(avgDelta.x) && abs(avgDelta.y) > 0.5f) {
                                                    onScrollDelta(avgDelta.y)
                                                    lastTwoFingerDy = (avgDelta.y / dtMs) * 1000f
                                                } else if (abs(avgDelta.x) > abs(avgDelta.y) && abs(avgDelta.x) > 0.8f) {
                                                    onHorizontalScrollDelta(avgDelta.x)
                                                }
                                            }
                                        }
                                        lastPinchDistance = curDist
                                        activeTouchPoint = (p0 + p1) / 2f
                                        pressed.forEach { it.consume() }
                                    }

                                    else -> {
                                        lastPinchDistance = -1f
                                        val change = pressed.first()
                                        val delta = change.positionChange()
                                        activeTouchPoint = change.position
                                        if (delta != Offset.Zero) {
                                            totalDistance += hypot(delta.x, delta.y)
                                            onMove(delta.x, delta.y)
                                            if (penDrawingMode) {
                                                inkTrail.add(change.position)
                                                if (inkTrail.size > 160) inkTrail.removeAt(0)
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        } finally {
                            activeTouchPoint = null
                            if (penDrawingMode) {
                                onPenHoldChange(false)
                            }
                            val elapsed = System.currentTimeMillis() - downTime
                            when {
                                maxPointers == 3 && !multiTriggered && totalDistance < 34f && elapsed < 340L -> {
                                    onGesture("THREE_FINGER_TAP")
                                }
                                maxPointers == 2 && totalDistance < 28f && elapsed < 320L -> {
                                    onGesture("TWO_FINGER_TAP")
                                }
                                maxPointers == 2 && abs(lastTwoFingerDy) > 120f -> {
                                    onScrollFling(lastTwoFingerDy)
                                }
                                maxPointers == 1 && !penDrawingMode && tapToClick &&
                                    totalDistance < 18f && elapsed < 260L -> {
                                    onTapLeftClick()
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (tron.enabled) {
                    val step = 36.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        drawLine(
                            color = tron.gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f,
                        )
                        x += step
                    }
                    var y = step
                    while (y < size.height) {
                        drawLine(
                            color = tron.gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                        y += step
                    }
                } else {
                    val step = 28.dp.toPx()
                    var y = step
                    while (y < size.height) {
                        var x = step
                        while (x < size.width) {
                            drawCircle(color = dotColor, radius = 1.6f, center = Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                }

                activeTouchPoint?.let { pt ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0f)),
                            center = pt,
                            radius = 52.dp.toPx(),
                        ),
                        radius = 52.dp.toPx(),
                        center = pt,
                    )
                }

                if (penDrawingMode && inkTrail.size > 1) {
                    val path = Path().apply {
                        moveTo(inkTrail.first().x, inkTrail.first().y)
                        for (i in 1 until inkTrail.size) {
                            lineTo(inkTrail[i].x, inkTrail[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val hint = when {
                    airMouseActive -> "AIR MOUSE ATIVO • MOVA O CELULAR"
                    penDrawingMode -> "MODO CANETA • DESENHE NA ÁREA"
                    else -> "TRACKPAD MULTITOQUE • 2D SCROLL/ZOOM • 3D/4D GESTOS"
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }

            AnimatedVisibility(
                visible = gestureHudText != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = gestureHudText.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            overlayContent()
        }
    }
}

@Composable
private fun VerticalScrollbarStrip(
    modifier: Modifier,
    onScrollDelta: (Float) -> Unit,
    onScrollFling: (Float) -> Unit,
) {
    var lastDy by remember { mutableStateOf(0f) }
    var lastTime by remember { mutableStateOf(0L) }
    val primary = MaterialTheme.colorScheme.primary

    RemoteSurface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .testTag("vertical_scrollbar"),
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (abs(lastDy) > 80f) onScrollFling(lastDy)
                            lastDy = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val now = System.currentTimeMillis()
                            val dt = (now - lastTime).coerceAtLeast(1L)
                            lastTime = now
                            lastDy = (dragAmount / dt) * 1000f
                            onScrollDelta(dragAmount)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 18.dp, horizontal = 10.dp)) {
                val ticks = 9
                val gap = size.height / (ticks + 1)
                for (i in 1..ticks) {
                    val y = gap * i
                    drawLine(
                        color = primary.copy(alpha = if (i == 5) 0.85f else 0.35f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (i == 5) 3.dp.toPx() else 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalScrollbarStrip(
    modifier: Modifier,
    onScrollDelta: (Float) -> Unit,
    onScrollFling: (Float) -> Unit,
) {
    var lastDx by remember { mutableStateOf(0f) }
    var lastTime by remember { mutableStateOf(0L) }
    val primary = MaterialTheme.colorScheme.primary

    RemoteSurface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .testTag("horizontal_scrollbar"),
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(lastDx) > 80f) onScrollFling(lastDx)
                            lastDx = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val now = System.currentTimeMillis()
                            val dt = (now - lastTime).coerceAtLeast(1L)
                            lastTime = now
                            lastDx = (dragAmount / dt) * 1000f
                            onScrollDelta(dragAmount)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 10.dp)) {
                val ticks = 11
                val gap = size.width / (ticks + 1)
                for (i in 1..ticks) {
                    val x = gap * i
                    drawLine(
                        color = primary.copy(alpha = if (i == 6) 0.85f else 0.35f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = if (i == 6) 3.dp.toPx() else 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
