package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.hid.HidConstants
import com.example.ui.components.RemoteSurface
import com.example.ui.components.RepeatableTactileKeyButton
import com.example.ui.components.TactileKeyButton
import com.example.ui.theme.LocalTronStyle
import com.example.viewmodel.RemoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MultimediaScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Multimídia", "TV Completa", "Web e Energia")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = {
                        viewModel.performHaptic(false)
                        selectedTab = idx
                    },
                    text = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                    modifier = Modifier.testTag("media_tab_$idx"),
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (selectedTab) {
                    0 -> MediaPrimaryRemoteTab(viewModel)
                    1 -> FullTvRemoteTab(viewModel)
                    2 -> WebAndSystemPowerTab(viewModel)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MediaPrimaryRemoteTab(viewModel: RemoteViewModel) {
    val tron = LocalTronStyle.current
    val companionConnected by viewModel.companionConnected.collectAsStateWithLifecycle()
    val pcStatus by viewModel.pcStatus.collectAsStateWithLifecycle()

    // Live PC Now-Playing Feedback + Scrubbing when Companion Agent is active
    if (companionConnected && pcStatus.mediaTitle.isNotBlank()) {
        RemoteSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            highlighted = true,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pcStatus.mediaTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (pcStatus.mediaArtist.isNotBlank()) {
                            Text(pcStatus.mediaArtist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    Text(
                        "CPU ${pcStatus.cpuPercent}% • RAM ${pcStatus.ramPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (pcStatus.mediaDurationSec > 0) {
                    Slider(
                        value = pcStatus.mediaPositionSec.toFloat().coerceIn(0f, pcStatus.mediaDurationSec.toFloat()),
                        onValueChange = { viewModel.seekCompanionMedia(it.roundToInt()) },
                        valueRange = 0f..pcStatus.mediaDurationSec.toFloat(),
                    )
                }
            }
        }
    }

    // Top Utility Row: Power, Search, Home, Menu
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TactileKeyButton(
            label = "",
            icon = Icons.Default.PowerSettingsNew,
            onClick = {
                viewModel.performHaptic(true)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_POWER, "Power / Suspender")
            },
            modifier = Modifier.weight(1f),
            minHeight = 50.dp,
            testTagName = "media_power",
        )
        TactileKeyButton(
            label = "Buscar",
            icon = Icons.Default.Search,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_SEARCH, "Buscar")
            },
            modifier = Modifier.weight(1.4f),
            minHeight = 50.dp,
            testTagName = "media_search",
        )
        TactileKeyButton(
            label = "",
            icon = Icons.Default.Home,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_HOME, "Início")
            },
            modifier = Modifier.weight(1f),
            minHeight = 50.dp,
            testTagName = "media_home",
        )
        TactileKeyButton(
            label = "",
            icon = Icons.Default.Menu,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_MENU, "Menu")
            },
            modifier = Modifier.weight(1f),
            minHeight = 50.dp,
            testTagName = "media_menu",
        )
    }

    // Sculptural Circular D-Pad with OK Center
    Surface(
        modifier = Modifier.size(228.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (tron.enabled) 1.8.dp else 1.2.dp,
            color = if (tron.enabled) tron.glowColor else MaterialTheme.colorScheme.outlineVariant,
        ),
        tonalElevation = if (tron.enabled) 0.dp else 4.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DPadArrowButton(Icons.Default.KeyboardArrowUp, "Cima", Modifier.align(Alignment.TopCenter).padding(top = 8.dp), "media_dpad_up") {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_UP, "Cima")
            }
            DPadArrowButton(Icons.Default.KeyboardArrowDown, "Baixo", Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), "media_dpad_down") {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_DOWN, "Baixo")
            }
            DPadArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Esquerda", Modifier.align(Alignment.CenterStart).padding(start = 8.dp), "media_dpad_left") {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_LEFT, "Esquerda")
            }
            DPadArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Direita", Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), "media_dpad_right") {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT, "Direita")
            }
            Surface(
                modifier = Modifier
                    .size(84.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .clickable {
                        viewModel.performHaptic(true)
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_ENTER, "OK")
                    }
                    .testTag("media_dpad_ok"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.6.dp, MaterialTheme.colorScheme.primary),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("OK", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Back + Stop Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TactileKeyButton(
            label = "Voltar",
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_BACK, "Voltar")
            },
            modifier = Modifier.weight(1f),
            minHeight = 50.dp,
            testTagName = "media_back",
        )
        TactileKeyButton(
            label = "Parar",
            icon = Icons.Default.Stop,
            onClick = {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_STOP, "Parar")
            },
            modifier = Modifier.weight(1f),
            minHeight = 50.dp,
            testTagName = "media_stop",
        )
    }

    // Playback Transport Deck
    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportIconButton(Icons.Default.FastRewind, "Retroceder", "media_rew") {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_REWIND, "Retroceder")
            }
            TransportIconButton(Icons.Default.SkipPrevious, "Anterior", "media_prev") {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_PREV_TRACK, "Anterior")
            }
            Surface(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .clickable {
                        viewModel.performHaptic(true)
                        viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_PLAY_PAUSE, "Play / Pause")
                    }
                    .testTag("media_play_pause"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproduzir ou Pausar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                }
            }
            TransportIconButton(Icons.Default.SkipNext, "Avançar", "media_next") {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_NEXT_TRACK, "Próximo")
            }
            TransportIconButton(Icons.Default.FastForward, "Avançar rápido", "media_ff") {
                viewModel.performHaptic(false)
                viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_FAST_FORWARD, "Avançar Rápido")
            }
        }
    }

    // Twin Rockers: Volume & Channel/Page
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RemoteSurface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("VOLUME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RepeatableTactileKeyButton("Vol +", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_UP, "Volume +") }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Filled.VolumeUp, minHeight = 50.dp, testTagName = "media_vol_up")
                TactileKeyButton("Mudo", { viewModel.performHaptic(true); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_MUTE, "Mudo") }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Filled.VolumeOff, minHeight = 46.dp, testTagName = "media_mute")
                RepeatableTactileKeyButton("Vol -", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_DOWN, "Volume -") }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Filled.VolumeDown, minHeight = 50.dp, testTagName = "media_vol_down")
            }
        }
        RemoteSurface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("CANAL / PÁGINA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RepeatableTactileKeyButton("CH +", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_CH_UP, "Canal +") }, Modifier.fillMaxWidth(), Icons.Default.KeyboardArrowUp, minHeight = 50.dp, testTagName = "media_ch_up")
                TactileKeyButton("Tela Cheia", { viewModel.performHaptic(false); viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F, "Tela Cheia (F)") }, Modifier.fillMaxWidth(), minHeight = 46.dp, testTagName = "media_fullscreen")
                RepeatableTactileKeyButton("CH -", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_CH_DOWN, "Canal -") }, Modifier.fillMaxWidth(), Icons.Default.KeyboardArrowDown, minHeight = 50.dp, testTagName = "media_ch_down")
            }
        }
    }
}

@Composable
private fun FullTvRemoteTab(viewModel: RemoteViewModel) {
    // Power / Sleep / Source / Guide
    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CONTROLE DE TV COMPLETO (HID)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Power", { viewModel.performHaptic(true); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_POWER, "TV Power") }, Modifier.weight(1f), Icons.Default.PowerSettingsNew, active = true)
                TactileKeyButton("Sleep", { viewModel.performHaptic(true); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_SLEEP, "TV Sleep") }, Modifier.weight(1f), Icons.Default.Bedtime)
                TactileKeyButton("Source", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_TV_INPUT, "Entrada HDMI / Source") }, Modifier.weight(1.1f), Icons.Default.Input)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Guia TV", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_TV_GUIDE, "Guia de Programação") }, Modifier.weight(1f))
                RepeatableTactileKeyButton("CH +", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_CH_UP, "Canal +") }, Modifier.weight(1f), Icons.Default.KeyboardArrowUp)
                RepeatableTactileKeyButton("CH -", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_CH_DOWN, "Canal -") }, Modifier.weight(1f), Icons.Default.KeyboardArrowDown)
            }
        }
    }

    // Numeric Channel Keypad (0-9)
    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TECLADO NUMÉRICO DE CANAIS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val rows = listOf(
                listOf("1" to HidConstants.KEY_1, "2" to HidConstants.KEY_2, "3" to HidConstants.KEY_3),
                listOf("4" to HidConstants.KEY_4, "5" to HidConstants.KEY_5, "6" to HidConstants.KEY_6),
                listOf("7" to HidConstants.KEY_7, "8" to HidConstants.KEY_8, "9" to HidConstants.KEY_9),
            )
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (digit, code) ->
                        TactileKeyButton(
                            label = digit,
                            onClick = {
                                viewModel.performHaptic(false)
                                viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, code, "Canal $digit")
                            },
                            modifier = Modifier.weight(1f),
                            minHeight = 52.dp,
                            testTagName = "tv_digit_$digit",
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Voltar", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_BACK, "Voltar") }, Modifier.weight(1f), minHeight = 52.dp)
                TactileKeyButton("0", { viewModel.performHaptic(false); viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_0, "Canal 0") }, Modifier.weight(1f), minHeight = 52.dp, testTagName = "tv_digit_0")
                TactileKeyButton("OK / Ir", { viewModel.performHaptic(true); viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_ENTER, "Confirmar Canal") }, Modifier.weight(1f), active = true, minHeight = 52.dp)
            }
        }
    }

    // TV 4 Color Function Keys (Red, Green, Yellow, Blue)
    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TECLAS DE COR DA TV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvColorButton("A", Color(0xFFE53935), Modifier.weight(1f)) {
                    viewModel.performHaptic(false)
                    viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_TV_RED, "Tecla Vermelha TV")
                }
                TvColorButton("B", Color(0xFF43A047), Modifier.weight(1f)) {
                    viewModel.performHaptic(false)
                    viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_TV_GREEN, "Tecla Verde TV")
                }
                TvColorButton("C", Color(0xFFFDD835), Modifier.weight(1f)) {
                    viewModel.performHaptic(false)
                    viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F3, "Tecla Amarela TV")
                }
                TvColorButton("D", Color(0xFF1E88E5), Modifier.weight(1f)) {
                    viewModel.performHaptic(false)
                    viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F4, "Tecla Azul TV")
                }
            }
        }
    }
}

@Composable
private fun TvColorButton(
    label: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.22f),
        border = BorderStroke(1.5.dp, color),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun WebAndSystemPowerTab(viewModel: RemoteViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val os = settings.targetOsProfile
    var pendingPowerAction by remember { mutableStateOf<Int?>(null) }

    // Web Browser Remote Panel
    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CONTROLE DE NAVEGADOR (WEB REMOTE • ${os.shortTag})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Nova Aba", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("CTRL+T", os, "Nova Aba") }, Modifier.weight(1f), Icons.Default.AddBox, active = true, testTagName = "web_new_tab")
                TactileKeyButton("Fechar Aba", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("CTRL+W", os, "Fechar Aba") }, Modifier.weight(1f), Icons.Default.Close, testTagName = "web_close_tab")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Reabrir Aba", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("CTRL+SHIFT+T", os, "Reabrir Aba Fechada") }, Modifier.weight(1f), Icons.Default.Restore)
                TactileKeyButton("Barra URL", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("CTRL+L", os, "Barra de Endereços") }, Modifier.weight(1f), Icons.Default.Search)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Aba Ant.", { viewModel.performHaptic(false); viewModel.hidManager.sendShortcutString("CTRL+SHIFT+TAB", "Aba Anterior") }, Modifier.weight(1f), Icons.Default.Tab)
                TactileKeyButton("Próx. Aba", { viewModel.performHaptic(false); viewModel.hidManager.sendShortcutString("CTRL+TAB", "Próxima Aba") }, Modifier.weight(1f), Icons.Default.Tab)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Voltar", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("ALT+LEFT", os, "Voltar Página") }, Modifier.weight(1f), Icons.AutoMirrored.Filled.ArrowBack)
                TactileKeyButton("Atualizar", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("CTRL+R", os, "Atualizar Página") }, Modifier.weight(1f), Icons.Default.Refresh)
                TactileKeyButton("Avançar", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("ALT+RIGHT", os, "Avançar Página") }, Modifier.weight(1f), Icons.AutoMirrored.Filled.ArrowForward)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Zoom -", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("CTRL+MINUS", os, "Zoom -") }, Modifier.weight(1f), Icons.Default.ZoomOut)
                TactileKeyButton("100%", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("CTRL+0", os, "Zoom Padrão") }, Modifier.weight(0.8f))
                TactileKeyButton("Zoom +", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("CTRL+EQUAL", os, "Zoom +") }, Modifier.weight(1f), Icons.Default.ZoomIn)
            }
        }
    }

    // Native HID System & Power Control Panel
    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SISTEMA, BRILHO E ENERGIA VIA HID (SEM SERVIDOR)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatableTactileKeyButton("Brilho -", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_BRIGHTNESS_DOWN, "Brilho -") }, Modifier.weight(1f), Icons.Default.BrightnessLow)
                RepeatableTactileKeyButton("Brilho +", { viewModel.performHaptic(false); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_BRIGHTNESS_UP, "Brilho +") }, Modifier.weight(1f), Icons.Default.BrightnessHigh)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Print Screen", { viewModel.performHaptic(true); viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_PRINT_SCREEN, "Captura de Tela") }, Modifier.weight(1f), Icons.Default.ScreenshotMonitor)
                TactileKeyButton("Trocar Janela", { viewModel.performHaptic(false); viewModel.hidManager.sendAdaptiveShortcut("ALT+TAB", os, "Alternar Janelas") }, Modifier.weight(1f), Icons.Default.SwapHoriz)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TactileKeyButton("Bloquear", { viewModel.performHaptic(true); viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_LOCK, "Bloquear Tela") }, Modifier.weight(1f), Icons.Default.Lock, testTagName = "sys_lock")
                TactileKeyButton("Suspender", { viewModel.performHaptic(true); pendingPowerAction = HidConstants.SYS_SLEEP }, Modifier.weight(1f), Icons.Default.Bedtime, testTagName = "sys_sleep")
                TactileKeyButton("Desligar", { viewModel.performHaptic(true); pendingPowerAction = HidConstants.SYS_POWER_DOWN }, Modifier.weight(1f), Icons.Default.PowerSettingsNew, testTagName = "sys_power_down")
            }
        }
    }

    pendingPowerAction?.let { mask ->
        val isShutdown = mask == HidConstants.SYS_POWER_DOWN
        AlertDialog(
            onDismissRequest = { pendingPowerAction = null },
            title = {
                Text(stringResource(if (isShutdown) R.string.confirm_power_down_title else R.string.confirm_sleep_title))
            },
            text = {
                Text(stringResource(if (isShutdown) R.string.confirm_power_down_msg else R.string.confirm_sleep_msg))
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.performHaptic(true)
                    viewModel.hidManager.sendSystemPowerControl(
                        mask,
                        if (isShutdown) "Desligar PC" else "Suspender PC",
                    )
                    pendingPowerAction = null
                }) {
                    Text(if (isShutdown) "Desligar" else "Suspender")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPowerAction = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun DPadArrowButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier,
    testTag: String,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val latestClick by rememberUpdatedState(onClick)

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    latestClick()
                    val repeatJob = scope.launch {
                        delay(380L)
                        while (isActive) {
                            latestClick()
                            delay(95L)
                        }
                    }
                    waitForUpOrCancellation()
                    repeatJob.cancel()
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun TransportIconButton(
    icon: ImageVector,
    description: String,
    testTag: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp),
        )
    }
}
