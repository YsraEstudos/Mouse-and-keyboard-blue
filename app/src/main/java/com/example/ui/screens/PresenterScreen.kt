package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AppSettings
import com.example.hid.HidConstants
import com.example.system.SamsungCompatibilityInspector
import com.example.ui.components.RemoteSurface
import com.example.ui.components.TactileKeyButton
import com.example.ui.components.TouchpadWithScrollbar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalTronStyle
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.RemoteViewModel
import kotlinx.coroutines.delay

@Composable
fun PresenterScreen(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val airMouseHeld by viewModel.presenterAirMouseHeld.collectAsStateWithLifecycle()
    val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()
    val lastSPenAction by viewModel.lastSPenActionNotice.collectAsStateWithLifecycle()
    val tron = LocalTronStyle.current

    var timerRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showTeleprompter by remember { mutableStateOf(false) }
    var editingNotes by remember { mutableStateOf(false) }
    var notesFontSize by remember { mutableIntStateOf(16) }
    var showSPenGuide by remember { mutableStateOf(false) }

    val targetTotalSeconds = (settings.presenterTargetMinutes.coerceIn(1, 120)) * 60
    val remainingSeconds = (targetTotalSeconds - elapsedSeconds).coerceAtLeast(0)

    LaunchedEffect(timerRunning, targetTotalSeconds, settings.presenterVibrationAlerts) {
        while (timerRunning) {
            delay(1000)
            elapsedSeconds++
            val rem = targetTotalSeconds - elapsedSeconds
            if (settings.presenterVibrationAlerts) {
                when (rem) {
                    300 -> {
                        viewModel.performPresenterAlertVibration(urgent = false)
                        viewModel.showBanner("Alerta silencioso: faltam 5 minutos!")
                    }
                    60 -> {
                        viewModel.performPresenterAlertVibration(urgent = true)
                        viewModel.showBanner("Alerta silencioso: falta 1 minuto!")
                    }
                    0 -> {
                        viewModel.performPresenterAlertVibration(urgent = true)
                        viewModel.showBanner("Tempo da apresentação encerrado!")
                    }
                }
            }
        }
    }

    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val remMins = remainingSeconds / 60
    val remSecs = remainingSeconds % 60
    val formattedElapsed = "%02d:%02d".format(mins, secs)
    val formattedRemaining = "%02d:%02d".format(remMins, remSecs)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Pro Timer + Silent Vibration Countdown + Target Selector
            RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CRONÔMETRO • RESTANTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = formattedElapsed,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "(-$formattedRemaining)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 14.sp,
                                    color = if (remainingSeconds <= 60) StatusWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { timerRunning = !timerRunning }) {
                            Icon(
                                if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (timerRunning) "Pausar" else "Iniciar",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { timerRunning = false; elapsedSeconds = 0 }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Zerar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = settings.presenterVibrationAlerts,
                            onClick = { viewModel.updateSettings { it.copy(presenterVibrationAlerts = !it.presenterVibrationAlerts) } },
                            leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            label = { Text("Alertas 5m/1m") },
                        )
                        listOf(5, 10, 15, 20, 30, 45).forEach { target ->
                            FilterChip(
                                selected = settings.presenterTargetMinutes == target,
                                onClick = { viewModel.updateSettings { it.copy(presenterTargetMinutes = target) } },
                                label = { Text("${target}m") },
                            )
                        }
                    }
                }
            }

            // Collapsible Presenter Notes / Teleprompter Card
            RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).clickable { showTeleprompter = !showTeleprompter },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Notas / Teleprompter (Cola)", style = MaterialTheme.typography.titleSmall)
                        }
                        if (showTeleprompter) {
                            Row {
                                TextButton(onClick = { notesFontSize = (notesFontSize - 2).coerceAtLeast(12) }) { Text("A-") }
                                TextButton(onClick = { notesFontSize = (notesFontSize + 2).coerceAtMost(28) }) { Text("A+") }
                                TextButton(onClick = { editingNotes = !editingNotes }) {
                                    Text(if (editingNotes) "Ler" else "Editar")
                                }
                            }
                        } else {
                            TextButton(onClick = { showTeleprompter = true }) { Text("Abrir") }
                        }
                    }

                    AnimatedVisibility(visible = showTeleprompter) {
                        if (editingNotes) {
                            OutlinedTextField(
                                value = settings.presenterNotes,
                                onValueChange = { text -> viewModel.updateSettings { it.copy(presenterNotes = text) } },
                                label = { Text("Tópicos da apresentação") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            )
                        } else {
                            Text(
                                text = settings.presenterNotes.ifBlank { "Toque em Editar para adicionar seus tópicos de fala." },
                                fontSize = notesFontSize.sp,
                                lineHeight = (notesFontSize + 6).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                            )
                        }
                    }
                }
            }

            // Samsung S Pen BLE Air Actions Card (Galaxy S23 Ultra & compatible Ultra models)
            if (deviceProfile.hasBleSPen || deviceProfile.isSamsung) {
                RemoteSurface(
                    modifier = Modifier.fillMaxWidth().testTag("presenter_spen_card"),
                    shape = RoundedCornerShape(16.dp),
                    highlighted = lastSPenAction != null,
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).clickable { showSPenGuide = !showSPenGuide },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.Draw, contentDescription = null, tint = StatusConnected)
                                Column {
                                    Text("S Pen BLE (Ações Aéreas)", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = lastSPenAction ?: "Pronta: Botão da S Pen controla slides e mídia via HID",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (lastSPenAction != null) StatusConnected else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            TextButton(onClick = { showSPenGuide = !showSPenGuide }) {
                                Text(if (showSPenGuide) "Ocultar" else "Guia")
                            }
                        }

                        AnimatedVisibility(visible = showSPenGuide) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.s23_spen_guide_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(SamsungCompatibilityInspector.createSamsungSPenSettingsIntent(context))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("presenter_open_spen_settings"),
                                ) {
                                    Text("Abrir Configurações de Ações Aéreas")
                                }
                            }
                        }
                    }
                }
            }

            // Start / Stop / Black / White Screen Controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton(
                    label = "Iniciar (F5)",
                    icon = Icons.Default.Slideshow,
                    active = true,
                    onClick = {
                        viewModel.performHaptic(true)
                        timerRunning = true
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F5, "Iniciar Apresentação")
                    },
                    modifier = Modifier.weight(1.2f),
                    testTagName = "presenter_start",
                )
                TactileKeyButton(
                    label = "Encerrar",
                    icon = Icons.Default.Close,
                    onClick = {
                        viewModel.performHaptic(true)
                        timerRunning = false
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_ESC, "Encerrar Apresentação")
                    },
                    modifier = Modifier.weight(1f),
                    testTagName = "presenter_stop",
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton(
                    label = "Slide Atual",
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_LSHIFT, HidConstants.KEY_F5, "Do Slide Atual")
                    },
                    modifier = Modifier.weight(1f),
                    minHeight = 44.dp,
                )
                TactileKeyButton(
                    label = "Tela Preta",
                    icon = Icons.Default.DarkMode,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_B, "Tela Preta (B)")
                    },
                    modifier = Modifier.weight(1f),
                    minHeight = 44.dp,
                )
                TactileKeyButton(
                    label = "Branca",
                    icon = Icons.Default.LightMode,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_W, "Tela Branca (W)")
                    },
                    modifier = Modifier.weight(0.9f),
                    minHeight = 44.dp,
                )
            }

            // Giant Ergonomic Slide Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth().height(138.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            viewModel.performHaptic(true)
                            viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_LEFT, "Slide Anterior")
                        }
                        .testTag("presenter_prev_slide"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        if (tron.enabled) 1.5.dp else 1.dp,
                        if (tron.enabled) tron.glowColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Slide Anterior", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("ANTERIOR", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1.45f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            viewModel.performHaptic(true)
                            viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT, "Próximo Slide")
                        }
                        .testTag("presenter_next_slide"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo Slide", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("PRÓXIMO SLIDE", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            // Hold-to-Point Air Mouse Laser Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (viewModel.isAirMouseSupported) {
                                    viewModel.setPresenterAirMouseHold(true)
                                    tryAwaitRelease()
                                    viewModel.setPresenterAirMouseHold(false)
                                } else {
                                    viewModel.showBanner("Giroscópio indisponível — use o touchpad abaixo")
                                }
                            }
                        )
                    }
                    .testTag("presenter_air_pointer"),
                shape = RoundedCornerShape(16.dp),
                color = if (airMouseHeld) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.3.dp,
                    if (airMouseHeld) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (airMouseHeld) "PONTEIRO AÉREO ATIVO • MOVA O CELULAR" else "SEGURE PARA PONTEIRO AÉREO (GIROSCÓPIO)",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (airMouseHeld) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Built-in Pointer Touchpad
            TouchpadWithScrollbar(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                showScrollbar = false,
                tapToClick = true,
                airMouseActive = airMouseHeld || settings.airMouseEnabled,
                onMove = { dx, dy -> viewModel.onTouchpadMove(dx, dy) },
                onTapLeftClick = { viewModel.onMouseClick(HidConstants.MOUSE_LEFT) },
                onPenHoldChange = {},
                onScrollDelta = { viewModel.onScrollDelta(it) },
                onScrollFling = { viewModel.onScrollFling(it) },
                onGesture = { viewModel.onGestureCommand(it) },
            )
        }
    }
}
