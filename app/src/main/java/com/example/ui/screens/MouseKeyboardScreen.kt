package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppSettings
import com.example.data.ControlMode
import com.example.data.MouseButtonsPosition
import com.example.data.ScrollbarPosition
import com.example.data.TextInputBarMode
import com.example.hid.HidConstants
import com.example.hid.KeyboardLayouts
import com.example.ui.components.GestureGuideDialog
import com.example.ui.components.RemoteSurface
import com.example.ui.components.TactileKeyButton
import com.example.ui.components.TouchpadWithScrollbar
import com.example.ui.theme.LocalTronStyle
import com.example.viewmodel.RemoteViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MouseKeyboardScreen(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val gestureHud by viewModel.gestureHudText.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val autoCorrectNotice by viewModel.lastAutoCorrectionNotice.collectAsStateWithLifecycle()
    val lastAction by viewModel.lastActionLabel.collectAsStateWithLifecycle()
    val sniperActive by viewModel.sniperPrecisionActive.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    var showGestureGuide by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var leftDragLocked by remember { mutableStateOf(false) }
    var airTriggerHeld by remember { mutableStateOf(false) }
    var forceShowTextBar by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                viewModel.sendPastedOrVoiceText(spoken)
            }
        }
    }

    val isImeOpen = WindowInsets.isImeVisible
    val showTextInputPanel = when (settings.textInputBarMode) {
        TextInputBarMode.ALWAYS -> true
        TextInputBarMode.KEYBOARD_ONLY -> isImeOpen || forceShowTextBar
        TextInputBarMode.HIDDEN -> forceShowTextBar
    }

    // Left-Handed mode automatically moves scrollbar to Left if set to Right
    val effectiveScrollbarPos = if (settings.leftHandedMode && settings.scrollbarPosition == ScrollbarPosition.RIGHT) {
        ScrollbarPosition.LEFT
    } else {
        settings.scrollbarPosition
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // One-Handed Ergonomic Mode pushes controls comfortably into lower thumb zone
        if (settings.oneHandedMode && !isImeOpen) {
            Spacer(Modifier.height(92.dp))
        }

        // Optional Quick Media Controls Bar
        AnimatedVisibility(visible = settings.showMediaControlsOnMain) {
            MediaMiniStrip(viewModel)
        }

        // Air Mouse Pro Action Strip (Trigger hold-to-move, Recalibrate, Air Drag-and-Hold)
        AnimatedVisibility(visible = settings.airMouseEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (settings.airMouseTriggerMode) {
                    Surface(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        airTriggerHeld = true
                                        viewModel.setAirMouseTriggerHeld(true)
                                        tryAwaitRelease()
                                        airTriggerHeld = false
                                        viewModel.setAirMouseTriggerHeld(false)
                                    }
                                )
                            }
                            .testTag("air_mouse_trigger_hold"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (airTriggerHeld) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (airTriggerHeld) "MOVENDO NO AR..." else "SEGURE P/ MOVER",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (airTriggerHeld) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                TactileKeyButton(
                    label = "Recalibrar",
                    icon = Icons.Default.CenterFocusStrong,
                    onClick = { viewModel.recalibrateAirMouse() },
                    modifier = Modifier.weight(1f),
                    minHeight = 42.dp,
                    testTagName = "air_mouse_recalibrate",
                )
                TactileKeyButton(
                    label = if (leftDragLocked) "Soltar" else "Arrastar Ar",
                    active = leftDragLocked,
                    onClick = {
                        leftDragLocked = !leftDragLocked
                        viewModel.onMouseButtonHold(HidConstants.MOUSE_LEFT, leftDragLocked)
                    },
                    modifier = Modifier.weight(1f),
                    minHeight = 42.dp,
                    testTagName = "air_mouse_drag_lock",
                )
            }
        }

        // Mouse Buttons on TOP if configured
        if (settings.mouseButtonsPosition == MouseButtonsPosition.TOP) {
            MouseButtonsRow(
                settings = settings,
                leftDragLocked = leftDragLocked,
                onLeftClick = {
                    if (leftDragLocked) {
                        leftDragLocked = false
                        viewModel.onMouseButtonHold(HidConstants.MOUSE_LEFT, false)
                    } else {
                        viewModel.onMouseClick(HidConstants.MOUSE_LEFT)
                    }
                },
                onLeftLongPress = {
                    leftDragLocked = !leftDragLocked
                    viewModel.onMouseButtonHold(HidConstants.MOUSE_LEFT, leftDragLocked)
                },
                onMiddleClick = { viewModel.onMouseClick(HidConstants.MOUSE_MIDDLE) },
                onRightClick = { viewModel.onMouseClick(HidConstants.MOUSE_RIGHT) },
            )
        }

        // Main Expansive Multi-Touch Trackpad + Scrollbar (2D Scroll, Pinch Zoom, 3D/4D Gestures)
        TouchpadWithScrollbar(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            scrollbarPosition = effectiveScrollbarPos,
            tapToClick = settings.touchpadTapToClick,
            penDrawingMode = settings.penDrawingMode,
            airMouseActive = settings.airMouseEnabled,
            gestureHudText = gestureHud,
            onMove = { dx, dy -> viewModel.onTouchpadMove(dx, dy) },
            onTapLeftClick = { viewModel.onMouseClick(HidConstants.MOUSE_LEFT) },
            onPenHoldChange = { held -> viewModel.onMouseButtonHold(HidConstants.MOUSE_LEFT, held) },
            onScrollDelta = { dy -> viewModel.onScrollDelta(dy, horizontal = false) },
            onHorizontalScrollDelta = { dx -> viewModel.onScrollDelta(dx, horizontal = true) },
            onScrollFling = { vel -> viewModel.onScrollFling(vel) },
            onPinchZoom = { zoomIn -> viewModel.onPinchZoom(zoomIn) },
            onGesture = { g -> viewModel.onGestureCommand(g) },
            overlayContent = {
                // Quick floating utility pills at top-right of touchpad
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    PadMiniChip(
                        icon = Icons.Default.ScreenRotation,
                        label = "Air",
                        active = settings.airMouseEnabled,
                        onClick = {
                            if (!viewModel.isAirMouseSupported) {
                                viewModel.showBanner("Giroscópio indisponível neste aparelho")
                            } else {
                                viewModel.performHaptic(false)
                                viewModel.updateSettings { it.copy(airMouseEnabled = !it.airMouseEnabled) }
                            }
                        },
                        testTagName = "chip_air_mouse",
                    )
                    PadMiniChip(
                        icon = Icons.Default.AdsClick,
                        label = "Auto",
                        active = settings.autoClickerEnabled,
                        onClick = {
                            viewModel.performHaptic(true)
                            viewModel.updateSettings { it.copy(autoClickerEnabled = !it.autoClickerEnabled) }
                        },
                        testTagName = "chip_auto_clicker",
                    )
                    PadMiniChip(
                        icon = Icons.Default.Draw,
                        label = "Caneta",
                        active = settings.penDrawingMode,
                        onClick = {
                            viewModel.performHaptic(false)
                            viewModel.updateSettings { it.copy(penDrawingMode = !it.penDrawingMode) }
                        },
                        testTagName = "chip_pen_mode",
                    )
                    PadMiniChip(
                        icon = Icons.Default.TouchApp,
                        label = "Gestos",
                        active = false,
                        onClick = { showGestureGuide = true },
                        testTagName = "chip_gesture_guide",
                    )
                }

                // Discreet live HID telemetry chip at bottom-left of touchpad
                Text(
                    text = if (sniperActive) "MODO SNIPER (-58% DPI) • $lastAction" else lastAction,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sniperActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
        )

        // Mouse 4 (Back), Mouse 5 (Forward) & Sniper DPI Precision Hold Row
        AnimatedVisibility(visible = settings.showSideMouseButtons) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TactileKeyButton(
                    label = "M4 Voltar",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { viewModel.onMouseClick(HidConstants.MOUSE_BACK) },
                    modifier = Modifier.weight(1f),
                    minHeight = 40.dp,
                    testTagName = "mouse_btn_m4",
                )
                // Holdable Sniper / Precision DPI button
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.setSniperPrecision(true)
                                    tryAwaitRelease()
                                    viewModel.setSniperPrecision(false)
                                }
                            )
                        }
                        .testTag("mouse_btn_sniper"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (sniperActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (sniperActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = "Precisão Sniper",
                            tint = if (sniperActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (sniperActive) "PRECISÃO ●" else "Sniper DPI",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (sniperActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                TactileKeyButton(
                    label = "M5 Avançar",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = { viewModel.onMouseClick(HidConstants.MOUSE_FORWARD) },
                    modifier = Modifier.weight(1f),
                    minHeight = 40.dp,
                    testTagName = "mouse_btn_m5",
                )
            }
        }

        // Mouse Buttons on BOTTOM if configured
        if (settings.mouseButtonsPosition == MouseButtonsPosition.BOTTOM) {
            MouseButtonsRow(
                settings = settings,
                leftDragLocked = leftDragLocked,
                onLeftClick = {
                    if (leftDragLocked) {
                        leftDragLocked = false
                        viewModel.onMouseButtonHold(HidConstants.MOUSE_LEFT, false)
                    } else {
                        viewModel.onMouseClick(HidConstants.MOUSE_LEFT)
                    }
                },
                onLeftLongPress = {
                    leftDragLocked = !leftDragLocked
                    viewModel.onMouseButtonHold(HidConstants.MOUSE_LEFT, leftDragLocked)
                },
                onMiddleClick = { viewModel.onMouseClick(HidConstants.MOUSE_MIDDLE) },
                onRightClick = { viewModel.onMouseClick(HidConstants.MOUSE_RIGHT) },
            )
        }

        // Optional Quick Shortcuts Bar (OS-Adaptive!)
        AnimatedVisibility(visible = settings.showQuickShortcuts) {
            val os = settings.targetOsProfile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TactileKeyButton(
                    label = "Desfazer",
                    icon = Icons.AutoMirrored.Filled.Undo,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendAdaptiveShortcut("CTRL+Z", os, "Desfazer")
                    },
                    minHeight = 40.dp,
                    testTagName = "shortcut_undo",
                )
                TactileKeyButton(
                    label = "Refazer",
                    icon = Icons.AutoMirrored.Filled.Redo,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendAdaptiveShortcut("CTRL+Y", os, "Refazer")
                    },
                    minHeight = 40.dp,
                    testTagName = "shortcut_redo",
                )
                TactileKeyButton(
                    label = "Copiar",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendAdaptiveShortcut("CTRL+C", os, "Copiar")
                    },
                    minHeight = 40.dp,
                    testTagName = "shortcut_copy",
                )
                TactileKeyButton(
                    label = "Colar",
                    icon = Icons.Default.ContentPaste,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendAdaptiveShortcut("CTRL+V", os, "Colar")
                    },
                    minHeight = 40.dp,
                    testTagName = "shortcut_paste",
                )
                TactileKeyButton(
                    label = "",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_LEFT, "Seta ←")
                    },
                    minHeight = 40.dp,
                    testTagName = "shortcut_left",
                )
                TactileKeyButton(
                    label = "",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT, "Seta →")
                    },
                    minHeight = 40.dp,
                    testTagName = "shortcut_right",
                )
            }
        }

        // Optional Android Navigation Buttons (Unlock/Back, Home, Menu)
        AnimatedVisibility(visible = settings.showAndroidNavButtons) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TactileKeyButton(
                    label = "Unlock / Voltar",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_BACK, "Android Voltar")
                    },
                    modifier = Modifier.weight(1.2f),
                    minHeight = 42.dp,
                    testTagName = "android_nav_back",
                )
                TactileKeyButton(
                    label = "Home",
                    icon = Icons.Default.Home,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_HOME, "Android Home")
                    },
                    modifier = Modifier.weight(1f),
                    minHeight = 42.dp,
                    testTagName = "android_nav_home",
                )
                TactileKeyButton(
                    label = "Menu",
                    icon = Icons.Default.Apps,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_MENU, "Android Menu")
                    },
                    modifier = Modifier.weight(1f),
                    minHeight = 42.dp,
                    testTagName = "android_nav_menu",
                )
            }
        }

        // Smart Typing & Autocorrect Input Dock
        if (showTextInputPanel) {
            RemoteSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AnimatedVisibility(visible = !settings.passwordMode && (suggestions.isNotEmpty() || autoCorrectNotice != null)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Corretor inteligente",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            if (suggestions.isNotEmpty()) {
                                suggestions.forEach { sug ->
                                    Surface(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { viewModel.applySuggestion(sug) },
                                        shape = CircleShape,
                                        color = if (sug.isPrimaryCorrection) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (sug.isPrimaryCorrection) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                    ) {
                                        Text(
                                            text = sug.replacement,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (sug.isPrimaryCorrection) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            } else if (autoCorrectNotice != null) {
                                Text(
                                    text = autoCorrectNotice.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = settings.directTypingMode,
                            onClick = {
                                viewModel.clearInputText()
                                viewModel.updateSettings { it.copy(directTypingMode = !it.directTypingMode) }
                            },
                            label = {
                                Text(
                                    if (settings.directTypingMode) "Modo Direto" else "Campo de Texto",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            modifier = Modifier.testTag("chip_direct_mode"),
                        )
                        FilterChip(
                            selected = settings.passwordMode,
                            onClick = {
                                viewModel.clearInputText()
                                viewModel.updateSettings { it.copy(passwordMode = !it.passwordMode) }
                            },
                            leadingIcon = {
                                Icon(
                                    if (settings.passwordMode) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                            label = { Text("Senha", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("chip_password_mode"),
                        )
                        FilterChip(
                            selected = settings.autoCorrectEnabled && !settings.passwordMode,
                            onClick = {
                                viewModel.updateSettings { it.copy(autoCorrectEnabled = !it.autoCorrectEnabled) }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text("Corretor", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("chip_autocorrect"),
                        )
                        val activeLang = KeyboardLayouts.getById(settings.keyboardLayoutId)
                        FilterChip(
                            selected = false,
                            onClick = { showLanguageDialog = true },
                            leadingIcon = {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text(activeLang.regionTag, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("chip_keyboard_lang"),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { viewModel.onInputTextChanged(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("main_text_input"),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = when {
                                        settings.passwordMode -> "Digite a senha com privacidade..."
                                        settings.directTypingMode -> "Digite aqui (envio em tempo real)..."
                                        else -> "Escreva ou cole para enviar..."
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            visualTransformation = if (settings.passwordMode) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            keyboardOptions = KeyboardOptions(
                                autoCorrectEnabled = !settings.passwordMode && settings.autoCorrectEnabled,
                                keyboardType = if (settings.passwordMode) KeyboardType.Password else KeyboardType.Text,
                                imeAction = ImeAction.Send,
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (settings.directTypingMode) {
                                        viewModel.hidManager.sendKeyPress(
                                            HidConstants.MOD_NONE,
                                            HidConstants.KEY_ENTER,
                                            "Enter",
                                        )
                                        viewModel.clearInputText()
                                    } else {
                                        viewModel.sendBatchInputText(appendEnter = true)
                                    }
                                }
                            ),
                            trailingIcon = {
                                if (inputText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearInputText() }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpar texto")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                        )

                        IconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text.orEmpty()
                                if (clipText.isNotBlank()) {
                                    viewModel.sendPastedOrVoiceText(clipText)
                                } else {
                                    viewModel.showBanner("Área de transferência vazia")
                                }
                            },
                            modifier = Modifier.testTag("button_paste_clipboard"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Colar da área de transferência",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR").toLanguageTag())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale o texto para enviar ao PC/TV")
                                }
                                runCatching { voiceLauncher.launch(intent) }.onFailure {
                                    viewModel.showBanner("Ditado por voz indisponível neste dispositivo")
                                }
                            },
                            modifier = Modifier.testTag("button_voice_dictation"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Ditado por voz",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (settings.directTypingMode) {
                                        viewModel.performHaptic(false)
                                        viewModel.hidManager.sendKeyPress(
                                            HidConstants.MOD_NONE,
                                            HidConstants.KEY_ENTER,
                                            "Enter",
                                        )
                                        viewModel.clearInputText()
                                    } else {
                                        viewModel.sendBatchInputText(appendEnter = false)
                                    }
                                }
                                .testTag("button_send_text"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar texto",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            TactileKeyButton(
                label = "Abrir Barra de Digitação",
                icon = Icons.Default.AutoFixHigh,
                onClick = { forceShowTextBar = true },
                modifier = Modifier.fillMaxWidth(),
                minHeight = 44.dp,
            )
        }
    }

    if (showGestureGuide) {
        GestureGuideDialog(onDismiss = { showGestureGuide = false })
    }

    if (showLanguageDialog) {
        KeyboardLanguagePickerDialog(
            selectedId = settings.keyboardLayoutId,
            onSelect = { id ->
                viewModel.updateSettings { it.copy(keyboardLayoutId = id) }
                showLanguageDialog = false
            },
            onOpenAutoConfig = {
                showLanguageDialog = false
                viewModel.selectMode(ControlMode.SCANNER)
                viewModel.showBanner("Abra appground.io/keyboard no PC e escaneie o QR Code")
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MouseButtonsRow(
    settings: AppSettings,
    leftDragLocked: Boolean,
    onLeftClick: () -> Unit,
    onLeftLongPress: () -> Unit,
    onMiddleClick: () -> Unit,
    onRightClick: () -> Unit,
) {
    val showAny = settings.showLeftMouseButton || settings.showMiddleMouseButton || settings.showRightMouseButton
    if (!showAny) return

    val tron = LocalTronStyle.current
    val leftBtn: @Composable (Modifier) -> Unit = { mod ->
        Surface(
            modifier = mod
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onLeftClick,
                    onLongClick = onLeftLongPress,
                )
                .testTag("mouse_btn_left"),
            shape = RoundedCornerShape(14.dp),
            color = if (leftDragLocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                if (leftDragLocked || tron.enabled) 1.3.dp else 0.8.dp,
                if (leftDragLocked) MaterialTheme.colorScheme.primary
                else if (tron.enabled) tron.glowColor.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (leftDragLocked) "ARRASTANDO" else if (settings.leftHandedMode) "PRIMÁRIO (E)" else "ESQUERDO",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (leftDragLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    val rightBtn: @Composable (Modifier) -> Unit = { mod ->
        TactileKeyButton(
            label = if (settings.leftHandedMode) "SECUND. (D)" else "DIREITO",
            onClick = onRightClick,
            modifier = mod,
            minHeight = 48.dp,
            testTagName = "mouse_btn_right",
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (settings.leftHandedMode) {
            // Mirrored for Left-Handed ergonomics
            if (settings.showRightMouseButton) rightBtn(Modifier.weight(1.35f))
            if (settings.showMiddleMouseButton) {
                TactileKeyButton(
                    label = "MEIO",
                    onClick = onMiddleClick,
                    modifier = Modifier.weight(0.7f),
                    minHeight = 48.dp,
                    testTagName = "mouse_btn_middle",
                )
            }
            if (settings.showLeftMouseButton) leftBtn(Modifier.weight(1.35f))
        } else {
            if (settings.showLeftMouseButton) leftBtn(Modifier.weight(1.35f))
            if (settings.showMiddleMouseButton) {
                TactileKeyButton(
                    label = "MEIO",
                    onClick = onMiddleClick,
                    modifier = Modifier.weight(0.7f),
                    minHeight = 48.dp,
                    testTagName = "mouse_btn_middle",
                )
            }
            if (settings.showRightMouseButton) rightBtn(Modifier.weight(1.35f))
        }
    }
}

@Composable
private fun MediaMiniStrip(viewModel: RemoteViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TactileKeyButton(
            label = "",
            icon = Icons.Default.SkipPrevious,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_PREV_TRACK, "Faixa Anterior")
            },
            modifier = Modifier.weight(1f),
            minHeight = 40.dp,
        )
        TactileKeyButton(
            label = "",
            icon = Icons.Default.PlayArrow,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_PLAY_PAUSE, "Play / Pause")
            },
            modifier = Modifier.weight(1.2f),
            active = true,
            minHeight = 40.dp,
        )
        TactileKeyButton(
            label = "",
            icon = Icons.Default.SkipNext,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_NEXT_TRACK, "Próxima Faixa")
            },
            modifier = Modifier.weight(1f),
            minHeight = 40.dp,
        )
        TactileKeyButton(
            label = "",
            icon = Icons.AutoMirrored.Filled.VolumeDown,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_DOWN, "Volume -")
            },
            modifier = Modifier.weight(1f),
            minHeight = 40.dp,
        )
        TactileKeyButton(
            label = "",
            icon = Icons.AutoMirrored.Filled.VolumeOff,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_MUTE, "Mudo")
            },
            modifier = Modifier.weight(0.9f),
            minHeight = 40.dp,
        )
        TactileKeyButton(
            label = "",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_UP, "Volume +")
            },
            modifier = Modifier.weight(1f),
            minHeight = 40.dp,
        )
    }
}

@Composable
private fun PadMiniChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTagName: String,
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTagName),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun KeyboardLanguagePickerDialog(
    selectedId: String,
    onSelect: (String) -> Unit,
    onOpenAutoConfig: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Idioma do Teclado", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenAutoConfig),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Configuração Automática (QR Code)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Abra appground.io/keyboard no PC/TV e escaneie o QR pelo app para detectar acentos e @ automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(KeyboardLayouts.allLayouts.size) { index ->
                        val lang = KeyboardLayouts.allLayouts[index]
                        val isSelected = lang.id == selectedId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelect(lang.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        lang.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        lang.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    lang.regionTag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
    )
}
