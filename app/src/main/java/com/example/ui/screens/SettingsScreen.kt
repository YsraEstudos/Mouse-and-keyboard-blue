package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.AppSettings
import com.example.data.ControlMode
import com.example.data.HidProfileMode
import com.example.data.MouseButtonsPosition
import com.example.data.PointerAccelCurve
import com.example.data.ScreenOrientationMode
import com.example.data.ScrollbarPosition
import com.example.data.TargetOsProfile
import com.example.data.TextInputBarMode
import com.example.data.ThemeMode
import com.example.data.VolumeButtonAction
import com.example.hid.KeyboardLayouts
import com.example.system.SamsungCompatibilityInspector
import com.example.ui.components.RemoteSurface
import com.example.ui.theme.AccentPalette
import com.example.viewmodel.RemoteViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Controles", "Mouse", "Teclado", "Exibição")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        viewModel.performHaptic(false)
                        selectedTab = index
                    },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                    modifier = Modifier.testTag("settings_tab_$index"),
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTab) {
                    0 -> ControlsSettingsTab(viewModel, settings)
                    1 -> MouseSettingsTab(viewModel, settings)
                    2 -> KeyboardSettingsTab(
                        viewModel = viewModel,
                        settings = settings,
                        onOpenLanguagePicker = { showLanguageDialog = true },
                    )
                    3 -> DisplaySettingsTab(viewModel, settings)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
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
                viewModel.showBanner("Abra appground.io/keyboard no PC/TV e escaneie o QR Code")
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
private fun ControlsSettingsTab(viewModel: RemoteViewModel, s: AppSettings) {
    SettingsCard(title = "ADAPTAÇÃO DE SISTEMA OPERACIONAL") {
        Text(
            "Adapta automaticamente atalhos (Ctrl ↔ Cmd ⌘, Alt+Tab ↔ Cmd+Tab, Spotlight/Início) para o dispositivo controlado:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TargetOsProfile.entries.forEach { profile ->
                FilterChip(
                    selected = s.targetOsProfile == profile,
                    onClick = { viewModel.updateSettings { it.copy(targetOsProfile = profile) } },
                    label = { Text(profile.label) },
                    modifier = Modifier.testTag("os_profile_${profile.id}"),
                )
            }
        }

        if (s.targetOsProfile == TargetOsProfile.IOS) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ) {
                Text(
                    text = stringResource(R.string.ios_assistive_touch_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }

    SettingsCard(title = "PERFIL BLUETOOTH HID & SEGURANÇA") {
        Text(
            "Perfil HID ativo: ${s.hidProfileMode.label} — ${s.hidProfileMode.description}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HidProfileMode.entries.forEach { profile ->
                FilterChip(
                    selected = s.hidProfileMode == profile,
                    onClick = { viewModel.setHidProfileMode(profile) },
                    label = { Text(profile.shortLabel) },
                    modifier = Modifier.testTag("settings_hid_profile_${profile.id}"),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Confirmar automações externas (Links / NFC)",
            subtitle = "Exige confirmação na tela antes de executar links controlebt:// ou intents que digitem texto ou macros",
            checked = s.requireAutomationConfirmation,
            testTag = "setting_confirm_automation",
        ) { v -> viewModel.updateSettings { it.copy(requireAutomationConfirmation = v) } }
    }

    val context = LocalContext.current
    val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()
    if (deviceProfile.isSamsung || deviceProfile.isS23Ultra || deviceProfile.isDexActive) {
        SettingsCard(title = "GALAXY S23 ULTRA • ONE UI, S PEN & DEX") {
            Text(
                text = "${deviceProfile.displayTitle} • ${deviceProfile.oneUiLabel} • ${deviceProfile.activeRefreshRateHz.toInt()}Hz",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = SamsungCompatibilityInspector.buildOneUiDeviceCareGuide(deviceProfile.semPlatformInt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(SamsungCompatibilityInspector.createAppDetailsBatteryIntent(context))
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("settings_s23_battery"),
                ) {
                    Text("Bateria Irrestrita", maxLines = 1)
                }
                if (deviceProfile.hasBleSPen) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(SamsungCompatibilityInspector.createSamsungSPenSettingsIntent(context))
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("settings_s23_spen"),
                    ) {
                        Text("Ações Aéreas S Pen", maxLines = 1)
                    }
                }
            }
        }
    }

    SettingsCard(title = "TELA PRINCIPAL E ERGONOMIA") {
        SettingSwitchRow(
            title = "Botões laterais Mouse 4, Mouse 5 e Sniper DPI",
            subtitle = "Exibe Voltar/Avançar de mouse gamer e botão de precisão cirúrgica (-58% DPI)",
            checked = s.showSideMouseButtons,
            testTag = "setting_side_mouse_buttons",
        ) { v -> viewModel.updateSettings { it.copy(showSideMouseButtons = v) } }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Modo Canhoto",
            subtitle = "Espelha botões do mouse e posiciona a barra de rolagem à esquerda",
            checked = s.leftHandedMode,
            testTag = "setting_left_handed",
        ) { v -> viewModel.updateSettings { it.copy(leftHandedMode = v) } }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Modo Uma Mão (Ergonomia)",
            subtitle = "Desloca o touchpad e botões para a zona inferior de alcance do polegar",
            checked = s.oneHandedMode,
            testTag = "setting_one_handed",
        ) { v -> viewModel.updateSettings { it.copy(oneHandedMode = v) } }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Mostrar botões de controle de mídia",
            subtitle = "Adiciona barra rápida de play/pause, faixas e volume na tela principal",
            checked = s.showMediaControlsOnMain,
            testTag = "setting_show_media",
        ) { v -> viewModel.updateSettings { it.copy(showMediaControlsOnMain = v) } }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Mostrar atalhos rápidos",
            subtitle = "Exibe botões de Desfazer, Refazer, Copiar, Colar e setas ← / →",
            checked = s.showQuickShortcuts,
            testTag = "setting_show_shortcuts",
        ) { v -> viewModel.updateSettings { it.copy(showQuickShortcuts = v) } }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Mostrar botões de navegação Android",
            subtitle = "Exibe Unlock Back, Home e Menu para controlar Android TV, tablets e celulares",
            checked = s.showAndroidNavButtons,
            testTag = "setting_show_android_nav",
        ) { v -> viewModel.updateSettings { it.copy(showAndroidNavButtons = v) } }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Comandos gestuais multitoque (2D, 3D, 4D e Pinça)",
            subtitle = "Scroll H/V com 2 dedos, Pinça Zoom, clique central 3 dedos e desktops virtuais 4 dedos",
            checked = s.enableGestureCommands,
            testTag = "setting_enable_gestures",
        ) { v -> viewModel.updateSettings { it.copy(enableGestureCommands = v) } }
    }
}

@Composable
private fun MouseSettingsTab(viewModel: RemoteViewModel, s: AppSettings) {
    SettingsCard(title = "AIR MOUSE PRO (GIROSCÓPIO)") {
        SettingSwitchRow(
            title = "Mouse aéreo (Air Mouse)",
            subtitle = if (viewModel.isAirMouseSupported) "Move o cursor no PC/TV ao movimentar o celular no ar"
            else "Sensor giroscópio não detectado neste dispositivo",
            checked = s.airMouseEnabled,
            testTag = "setting_air_mouse",
        ) { v -> viewModel.updateSettings { it.copy(airMouseEnabled = v) } }

        SettingSwitchRow(
            title = "Gatilho de movimento (Segurar p/ Mover)",
            subtitle = "Move o cursor apenas enquanto você segura o botão gatilho na tela",
            checked = s.airMouseTriggerMode,
            testTag = "setting_air_mouse_trigger",
        ) { v -> viewModel.updateSettings { it.copy(airMouseTriggerMode = v) } }

        SettingSliderRow(
            title = "Sensibilidade do Air Mouse",
            valueLabel = "${(s.airMouseSensitivity * 100).roundToInt()}%",
            value = s.airMouseSensitivity,
            range = 0.4f..2.8f,
        ) { v -> viewModel.updateSettings { it.copy(airMouseSensitivity = v) } }

        OutlinedButton(
            onClick = { viewModel.recalibrateAirMouse() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Recalibrar e Centralizar Giroscópio")
        }
    }

    SettingsCard(title = "TOUCHPAD, ACELERAÇÃO E BOTÕES") {
        SettingSwitchRow(
            title = "Cliques do mouse no touchpad",
            subtitle = "Permite dar toques leves diretamente no touchpad para clicar",
            checked = s.touchpadTapToClick,
            testTag = "setting_tap_click",
        ) { v -> viewModel.updateSettings { it.copy(touchpadTapToClick = v) } }

        SettingSliderRow(
            title = "Velocidade do ponteiro",
            valueLabel = "${(s.pointerSpeed * 100).roundToInt()}%",
            value = s.pointerSpeed,
            range = 0.4f..3.0f,
        ) { v -> viewModel.updateSettings { it.copy(pointerSpeed = v) } }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Curva de aceleração do ponteiro", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PointerAccelCurve.entries.forEach { curve ->
                    FilterChip(
                        selected = s.pointerAccelCurve == curve,
                        onClick = { viewModel.updateSettings { it.copy(pointerAccelCurve = curve) } },
                        label = { Text(curve.label) },
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Posição dos botões do mouse", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MouseButtonsPosition.entries.forEach { pos ->
                    FilterChip(
                        selected = s.mouseButtonsPosition == pos,
                        onClick = { viewModel.updateSettings { it.copy(mouseButtonsPosition = pos) } },
                        label = { Text(pos.label) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Botões visíveis do mouse", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = s.showLeftMouseButton, onClick = { viewModel.updateSettings { it.copy(showLeftMouseButton = !it.showLeftMouseButton) } }, label = { Text("Esquerda") })
                FilterChip(selected = s.showMiddleMouseButton, onClick = { viewModel.updateSettings { it.copy(showMiddleMouseButton = !it.showMiddleMouseButton) } }, label = { Text("Centro") })
                FilterChip(selected = s.showRightMouseButton, onClick = { viewModel.updateSettings { it.copy(showRightMouseButton = !it.showRightMouseButton) } }, label = { Text("Direita") })
            }
        }
    }

    SettingsCard(title = "BARRA DE ROLAGEM") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Posição da barra de rolagem", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScrollbarPosition.entries.forEach { pos ->
                    FilterChip(
                        selected = s.scrollbarPosition == pos,
                        onClick = { viewModel.updateSettings { it.copy(scrollbarPosition = pos) } },
                        label = { Text(pos.label) },
                    )
                }
            }
        }

        SettingSliderRow(
            title = "Velocidade de rolamento",
            valueLabel = "${(s.scrollSpeed * 100).roundToInt()}%",
            value = s.scrollSpeed,
            range = 0.4f..3.0f,
        ) { v -> viewModel.updateSettings { it.copy(scrollSpeed = v) } }

        SettingSwitchRow("Inverter direção de rolagem", "Inverte o sentido natural do movimento da página", s.invertScrollDirection, "setting_invert_scroll") { v ->
            viewModel.updateSettings { it.copy(invertScrollDirection = v) }
        }
        SettingSwitchRow("Rolagem com inércia", "A página continua deslizando suavemente após soltar o dedo", s.inertiaScrolling, "setting_inertia_scroll") { v ->
            viewModel.updateSettings { it.copy(inertiaScrolling = v) }
        }
    }

    SettingsCard(title = "JIGGLER HUMANO, AUTO-CLICKER E CANETA") {
        SettingSwitchRow(
            title = "Mouse Jiggler (Manter PC ativo)",
            subtitle = "Impede suspensão, bloqueio de tela e status 'ausente' no PC",
            checked = s.mouseJigglerEnabled,
            testTag = "setting_mouse_jiggler",
        ) { v -> viewModel.updateSettings { it.copy(mouseJigglerEnabled = v) } }

        if (s.mouseJigglerEnabled) {
            SettingSwitchRow(
                title = "Movimento Humano (Stealth)",
                subtitle = "Trajetórias curvas orgânicas, intervalos aleatórios e tecla neutra F15",
                checked = s.mouseJigglerHumanMode,
                testTag = "setting_jiggler_human",
            ) { v -> viewModel.updateSettings { it.copy(mouseJigglerHumanMode = v) } }

            SettingSliderRow(
                title = "Intervalo médio do Jiggler",
                valueLabel = "${s.mouseJigglerIntervalSec}s",
                value = s.mouseJigglerIntervalSec.toFloat(),
                range = 10f..120f,
            ) { v -> viewModel.updateSettings { it.copy(mouseJigglerIntervalSec = v.roundToInt()) } }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Auto-Clicker Programável",
            subtitle = "Dispara cliques contínuos automaticamente (útil para tarefas repetitivas e jogos)",
            checked = s.autoClickerEnabled,
            testTag = "setting_auto_clicker",
        ) { v -> viewModel.updateSettings { it.copy(autoClickerEnabled = v) } }

        if (s.autoClickerEnabled) {
            SettingSliderRow(
                title = "Frequência (Cliques por segundo)",
                valueLabel = "${s.autoClickerCps} CPS",
                value = s.autoClickerCps.toFloat(),
                range = 1f..20f,
            ) { v -> viewModel.updateSettings { it.copy(autoClickerCps = v.roundToInt()) } }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !s.autoClickerRightButton,
                    onClick = { viewModel.updateSettings { it.copy(autoClickerRightButton = false) } },
                    label = { Text("Botão Esquerdo") },
                )
                FilterChip(
                    selected = s.autoClickerRightButton,
                    onClick = { viewModel.updateSettings { it.copy(autoClickerRightButton = true) } },
                    label = { Text("Botão Direito") },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Modo de desenho da caneta (Touchpad)",
            subtitle = "Traço contínuo rápido no touchpad (use 'Mesa Digitalizadora' para pressão e tilt HID)",
            checked = s.penDrawingMode,
            testTag = "setting_pen_drawing",
        ) { v -> viewModel.updateSettings { it.copy(penDrawingMode = v) } }
    }
}

@Composable
private fun KeyboardSettingsTab(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    onOpenLanguagePicker: () -> Unit,
) {
    val activeLang = KeyboardLayouts.getById(settings.keyboardLayoutId)

    SettingsCard(title = "IDIOMA E CONFIGURAÇÃO AUTOMÁTICA") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLanguagePicker)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Linguagem do teclado", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${activeLang.displayName} — ${activeLang.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            OutlinedButton(onClick = onOpenLanguagePicker) { Text("Alterar") }
        }

        Button(
            onClick = {
                viewModel.selectMode(ControlMode.SCANNER)
                viewModel.showBanner("Abra appground.io/keyboard no PC e escaneie o QR Code")
            },
            modifier = Modifier.fillMaxWidth().testTag("setting_auto_config_qr"),
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Configuração Automática via QR (appground.io/keyboard)")
        }
    }

    SettingsCard(title = "CORRETOR INTELIGENTE E DIGITAÇÃO") {
        SettingSwitchRow(
            title = "Corretor ortográfico em tempo real",
            subtitle = "Sugere e corrige acentos (ç, ã, é) e erros enquanto você digita",
            checked = settings.autoCorrectEnabled,
            testTag = "setting_autocorrect",
        ) { v -> viewModel.updateSettings { it.copy(autoCorrectEnabled = v) } }

        SettingSwitchRow(
            title = "Corrigir automaticamente ao pressionar Espaço",
            subtitle = "Aplica correção instantânea até no Modo Direto via Bluetooth",
            checked = settings.autoCorrectOnSpace,
            testTag = "setting_autocorrect_space",
        ) { v -> viewModel.updateSettings { it.copy(autoCorrectOnSpace = v) } }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Mostrar barra de entrada de texto", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextInputBarMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.textInputBarMode == mode,
                        onClick = { viewModel.updateSettings { it.copy(textInputBarMode = mode) } },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }

    SettingsCard(title = "BOTÕES FÍSICOS DE VOLUME DO CELULAR") {
        Text(
            "Escolha o que os botões físicos de volume (+ / -) fazem enquanto o app está aberto:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            VolumeButtonAction.entries.forEach { action ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.updateSettings { it.copy(volumeButtonAction = action) } },
                    shape = RoundedCornerShape(12.dp),
                    color = if (settings.volumeButtonAction == action) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            action.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (settings.volumeButtonAction == action) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (settings.volumeButtonAction == action) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplaySettingsTab(viewModel: RemoteViewModel, s: AppSettings) {
    var showResetConfirm by remember { mutableStateOf(false) }

    SettingsCard(title = "APARÊNCIA, TRON E ECONOMIA OLED") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cor do tema", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AccentPalette.entries.forEach { palette ->
                    val selected = s.accentPalette == palette
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.performHaptic(false)
                                    viewModel.updateSettings { it.copy(accentPalette = palette) }
                                }
                                .testTag("color_swatch_${palette.id}"),
                            shape = CircleShape,
                            color = palette.primaryDark,
                            border = BorderStroke(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                            ),
                        ) {
                            if (selected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = palette.label, tint = Color(0xFF06101B), modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(palette.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Projeto (Tema)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = s.themeMode == mode,
                        onClick = { viewModel.updateSettings { it.copy(themeMode = mode) } },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("theme_mode_${mode.id}"),
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Estilo Tron",
            subtitle = "Fundo preto absoluto com grade cibernética e contornos neon luminosos",
            checked = s.tronStyleEnabled,
            testTag = "setting_tron_style",
        ) { v -> viewModel.updateSettings { it.copy(tronStyleEnabled = v) } }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow(
            title = "Modo Economia OLED (Preto Puro + Auto-Dimming)",
            subtitle = "Pixels pretos #000000 desligados e redução automática de brilho após inatividade (evita burn-in)",
            checked = s.oledSaverEnabled,
            testTag = "setting_oled_saver",
        ) { v -> viewModel.updateSettings { it.copy(oledSaverEnabled = v) } }

        if (s.oledSaverEnabled) {
            SettingSliderRow(
                title = "Escurecer tela após inatividade",
                valueLabel = "${s.oledDimAfterSeconds}s",
                value = s.oledDimAfterSeconds.toFloat(),
                range = 10f..120f,
            ) { v -> viewModel.updateSettings { it.copy(oledDimAfterSeconds = v.roundToInt()) } }
        }
    }

    SettingsCard(title = "TELA E COMPORTAMENTO") {
        SettingSwitchRow("Inicie no modo de tela cheia", "Oculta as barras superior e inferior do Android", s.startFullscreen, "setting_fullscreen") { v ->
            viewModel.updateSettings { it.copy(startFullscreen = v) }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Orientação da tela", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScreenOrientationMode.entries.forEach { orient ->
                    FilterChip(
                        selected = s.screenOrientation == orient,
                        onClick = { viewModel.updateSettings { it.copy(screenOrientation = orient) } },
                        label = { Text(orient.label) },
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow("Mantenha a tela ligada", "Impede que a tela apague enquanto o controle estiver aberto", s.keepScreenOn, "setting_keep_screen_on") { v ->
            viewModel.updateSettings { it.copy(keepScreenOn = v) }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow("Mostrar aplicativo sobre a tela de bloqueio", "Continue controlando o PC/TV sem desbloquear o celular", s.showOverLockScreen, "setting_over_lock_screen") { v ->
            viewModel.updateSettings { it.copy(showOverLockScreen = v) }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        SettingSwitchRow("Vibrações de toque", "Retorno tátil leve ao pressionar botões, teclas e comandos gestuais", s.hapticFeedback, "setting_haptics") { v ->
            viewModel.updateSettings { it.copy(hapticFeedback = v) }
        }
    }

    SettingsCard(title = "SISTEMA, PRIVACIDADE E SOBRE") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.reopenOnboarding() },
                modifier = Modifier.weight(1f).testTag("setting_reopen_onboarding"),
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Guia Inicial")
            }
            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.weight(1f).testTag("setting_reset_defaults"),
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Restaurar Padrão")
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Text(
            text = "${stringResource(R.string.app_name)} v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.about_privacy_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Bibliotecas Open-Source: Jetpack Compose, Material 3, CameraX, ML Kit, Room, DataStore, OkHttp, Moshi e AndroidX Biometric.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.confirm_reset_defaults_title)) },
            text = { Text(stringResource(R.string.confirm_reset_defaults_msg)) },
            confirmButton = {
                Button(onClick = {
                    showResetConfirm = false
                    viewModel.resetSettingsToDefaults()
                }) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(testTag))
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
        )
    }
}
