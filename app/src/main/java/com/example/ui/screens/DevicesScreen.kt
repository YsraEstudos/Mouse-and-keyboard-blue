package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.DeviceCategory
import com.example.bluetooth.PairedDeviceUi
import com.example.data.EasySwitchSlot
import com.example.data.HidProfileMode
import com.example.data.TargetOsProfile
import com.example.system.SamsungCompatibilityInspector
import com.example.system.ThermalHealthLevel
import com.example.ui.components.RemoteSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.RemoteViewModel

@Composable
fun DevicesScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val isHidReady by viewModel.isHidProfileReady.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()
    val isQosActive by viewModel.isLowLatencyQosActive.collectAsStateWithLifecycle()

    var showMacDialog by remember { mutableStateOf(false) }
    var macInput by remember { mutableStateOf("") }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showDeviceCareGuide by remember { mutableStateOf(!deviceProfile.isIgnoringBatteryOptimizations) }
    var showAppleGuide by remember(settings.targetOsProfile) {
        mutableStateOf(settings.targetOsProfile == TargetOsProfile.IOS || settings.targetOsProfile == TargetOsProfile.MACOS)
    }

    val requiredPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.hidManager.initialize(settings.hidProfileMode)
        viewModel.runBluetoothDiagnostics()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDeviceProfile()
        viewModel.hidManager.initialize(settings.hidProfileMode)
        viewModel.runBluetoothDiagnostics()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero Banner Card
            item {
                RemoteSurface(
                    modifier = Modifier.fillMaxWidth().height(142.dp),
                    shape = RoundedCornerShape(20.dp),
                    highlighted = connectionState is BluetoothConnectionState.Connected,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_bluetooth_hero_1795575970162),
                            contentDescription = "Conexão Bluetooth HID direta sem servidor",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.25f), Color(0xFF070B14).copy(alpha = 0.88f))
                                )
                            )
                        )
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text("Conexão Direta Sem Servidor", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                "Bluetooth HID nativo (Android 9–16) para PC, Mac, iPhone, iPad, Linux e Smart TVs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.84f),
                            )
                        }
                    }
                }
            }

            // Easy-Switch Multi-Device Slots (1 | 2 | 3)
            item {
                val slots = settings.easySwitchSlots()
                val connectedAddr = (connectionState as? BluetoothConnectionState.Connected)?.address
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("EASY-SWITCH (TROCA RÁPIDA 1 | 2 | 3)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            slots.forEach { slot ->
                                val isActive = connectedAddr != null && slot.address.equals(connectedAddr, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.activateEasySwitchSlot(slot.slotNumber) }
                                        .testTag("easy_switch_card_${slot.slotNumber}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isActive) StatusConnected.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isActive) StatusConnected else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            "SLOT ${slot.slotNumber}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isActive) StatusConnected else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            slot.name.ifBlank { "Livre" },
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selectable HID Profile Card (Universal Apple/TV vs Complete Pro vs Gamepad vs Digitizer)
            item {
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("PERFIL BLUETOOTH HID (COMPATIBILIDADE)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "Escolha o perfil mais compatível com seu dispositivo alvo. Use \"Padrão Universal\" para iPhone, iPad, Mac e Smart TVs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HidProfileMode.entries.chunked(2).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { profile ->
                                        FilterChip(
                                            selected = settings.hidProfileMode == profile,
                                            onClick = { viewModel.setHidProfileMode(profile) },
                                            label = { Text(profile.shortLabel, maxLines = 1) },
                                            modifier = Modifier.weight(1f).testTag("hid_profile_${profile.id}"),
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = "${settings.hidProfileMode.label}: ${settings.hidProfileMode.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Galaxy S23 Ultra / One UI / FastConnect 7800 / Device Care Guard Card
            if (deviceProfile.isSamsung || deviceProfile.isS23Ultra || deviceProfile.isDexActive) {
                item {
                    RemoteSurface(
                        modifier = Modifier.fillMaxWidth().testTag("samsung_s23_ultra_card"),
                        shape = RoundedCornerShape(16.dp),
                        highlighted = deviceProfile.isS23Ultra || !deviceProfile.isIgnoringBatteryOptimizations,
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(
                                            text = if (deviceProfile.isS23Ultra) "PERFIL GALAXY S23 ULTRA ATIVO" else "OTIMIZAÇÃO SAMSUNG ONE UI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(deviceProfile.displayTitle, style = MaterialTheme.typography.titleSmall)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                ) {
                                    Text(
                                        text = "${deviceProfile.activeRefreshRateHz.toInt()}Hz",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            Text(
                                text = "${deviceProfile.oneUiLabel} • ${deviceProfile.chipSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = if (isQosActive) StatusConnected else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = if (isQosActive) "HID QoS 8ms (125Hz)" else "HID Universal",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.DesktopWindows,
                                            contentDescription = null,
                                            tint = if (deviceProfile.thermalLevel == ThermalHealthLevel.THROTTLED) StatusWarning else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = if (deviceProfile.isDexActive) "DeX Ativo" else deviceProfile.thermalLevel.label,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }

                            // Device Care / Deep Sleep Protection Status
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeviceCareGuide = !showDeviceCareGuide },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = if (deviceProfile.isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (deviceProfile.isIgnoringBatteryOptimizations) StatusConnected else StatusWarning,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Text(
                                        text = if (deviceProfile.isIgnoringBatteryOptimizations) {
                                            "Device Care: Bateria sem restrições"
                                        } else {
                                            "Proteger contra Suspensão Profunda"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                TextButton(onClick = { showDeviceCareGuide = !showDeviceCareGuide }) {
                                    Text(if (showDeviceCareGuide) "Ocultar" else "Guia")
                                }
                            }

                            if (showDeviceCareGuide) {
                                Text(
                                    text = SamsungCompatibilityInspector.buildOneUiDeviceCareGuide(deviceProfile.semPlatformInt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(SamsungCompatibilityInspector.createAppDetailsBatteryIntent(context))
                                        }.onFailure {
                                            viewModel.showBanner("Abra Configurações → Apps → Controle BT → Bateria")
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("s23_device_care_app_battery"),
                                ) {
                                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Bateria do App", maxLines = 1)
                                }
                                if (deviceProfile.hasBleSPen) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(SamsungCompatibilityInspector.createSamsungSPenSettingsIntent(context))
                                            }
                                        },
                                        modifier = Modifier.weight(1f).testTag("s23_spen_settings_btn"),
                                    ) {
                                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ações S Pen", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // iPhone / iPad / Mac Compatibility Guide
            item {
                RemoteSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    highlighted = settings.targetOsProfile == TargetOsProfile.IOS,
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showAppleGuide = !showAppleGuide },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Guia iPhone, iPad e Mac (Apple)", style = MaterialTheme.typography.titleSmall)
                            }
                            TextButton(onClick = { showAppleGuide = !showAppleGuide }) {
                                Text(if (showAppleGuide) "Ocultar" else "Ver dicas")
                            }
                        }
                        if (showAppleGuide) {
                            Text(
                                text = stringResource(R.string.ios_assistive_touch_tip),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "• iPadOS e macOS exibem o ponteiro automaticamente ao conectar.\n• Em iPhone/iPad, o perfil \"Padrão Universal\" evita rejeições de descritores compostos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Status, Permissions, Smart Auto-Reconnect & Manual MAC Connect Card
            item {
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val (title, subtitle, dotColor) = when (val st = connectionState) {
                            is BluetoothConnectionState.Connected -> Triple("Conectado a ${st.deviceName}", "Notificação ativa na barra superior • HID Pronto", StatusConnected)
                            is BluetoothConnectionState.Connecting -> Triple("Conectando a ${st.deviceName}...", "Aguardando resposta HID do dispositivo remoto", StatusWarning)
                            is BluetoothConnectionState.PermissionRequired -> Triple("Permissão Bluetooth necessária", "Autorize o acesso ao Bluetooth para listar e controlar aparelhos pareados.", StatusWarning)
                            is BluetoothConnectionState.BluetoothOff -> Triple("Bluetooth desativado", "Ative o Bluetooth do celular para conectar ao seu PC ou TV.", MaterialTheme.colorScheme.error)
                            is BluetoothConnectionState.Unsupported -> Triple("Hardware Bluetooth HID indisponível", "Modo demonstração local ativo — todos os controles funcionam na tela.", MaterialTheme.colorScheme.onSurfaceVariant)
                            is BluetoothConnectionState.Disconnected -> Triple(if (isHidReady) "Perfil HID Ativo • Desconectado" else "Pronto para parear", "Selecione um aparelho pareado abaixo ou conecte por endereço MAC.", MaterialTheme.colorScheme.primary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Smart Auto-Reconnect Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reconexão automática inteligente", style = MaterialTheme.typography.titleSmall)
                                Text("Reconecta ao último aparelho ao abrir o app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.autoReconnectEnabled,
                                onCheckedChange = { v -> viewModel.updateSettings { it.copy(autoReconnectEnabled = v) } },
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (connectionState) {
                                is BluetoothConnectionState.PermissionRequired -> {
                                    Button(onClick = { permLauncher.launch(requiredPermissions) }, modifier = Modifier.weight(1f).testTag("bt_request_permissions")) {
                                        Text("Permitir Bluetooth")
                                    }
                                }
                                is BluetoothConnectionState.BluetoothOff -> {
                                    Button(onClick = { runCatching { context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) } }, modifier = Modifier.weight(1f).testTag("bt_enable_button")) {
                                        Text("Ativar Bluetooth")
                                    }
                                }
                                is BluetoothConnectionState.Connected -> {
                                    OutlinedButton(onClick = { viewModel.performHaptic(true); viewModel.hidManager.disconnect() }, modifier = Modifier.weight(1f).testTag("bt_disconnect_button")) {
                                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Desconectar")
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = {
                                            viewModel.performHaptic(false)
                                            runCatching {
                                                val discIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180)
                                                }
                                                context.startActivity(discIntent)
                                            }.onFailure {
                                                runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).testTag("bt_setup_remote_device"),
                                    ) {
                                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Configurar remoto")
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.performHaptic(false)
                                    viewModel.hidManager.initialize(settings.hidProfileMode)
                                    viewModel.hidManager.refreshPairedDevices()
                                    viewModel.runBluetoothDiagnostics()
                                },
                                modifier = Modifier.testTag("bt_refresh_devices"),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Atualizar lista Bluetooth")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { showMacDialog = true },
                                modifier = Modifier.weight(1f).testTag("bt_manual_mac_button"),
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Conectar por MAC")
                            }
                            OutlinedButton(
                                onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) } },
                                modifier = Modifier.weight(1f).testTag("bt_open_system_settings"),
                            ) {
                                Text("Parear Android")
                            }
                        }
                    }
                }
            }

            // Paired Devices Header
            item {
                Text(
                    text = "DISPOSITIVOS PAREADOS (${pairedDevices.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }

            if (pairedDevices.isEmpty()) {
                item {
                    RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Text("Nenhum dispositivo pareado encontrado", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Toque em \"Configurar remoto\" ou \"Conectar por MAC\" para conectar ao seu PC, Mac, iPhone, iPad ou Android TV.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(pairedDevices, key = { it.address }) { device ->
                    PairedDeviceCard(
                        device = device,
                        easySwitchSlots = settings.easySwitchSlots(),
                        onAssignSlot = { slotNum -> viewModel.assignEasySwitchSlot(slotNum, device.name, device.address) },
                        onUseDevice = {
                            viewModel.performHaptic(true)
                            viewModel.hidManager.connectToDevice(device.address)
                        },
                        onDisconnect = {
                            viewModel.performHaptic(false)
                            viewModel.hidManager.disconnect()
                        },
                        onRemoveDevice = {
                            if (device.isConnected) viewModel.hidManager.disconnect()
                            runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                            viewModel.showBanner("Remova ${device.name} nas configurações de Bluetooth")
                        },
                    )
                }
            }

            // Bluetooth HID Diagnostics Tool + Export Report + Battery Optimization Shortcut
            item {
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showDiagnostics = !showDiagnostics
                                if (showDiagnostics) viewModel.runBluetoothDiagnostics()
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Diagnóstico Bluetooth HID", style = MaterialTheme.typography.titleSmall)
                            }
                            TextButton(onClick = {
                                showDiagnostics = !showDiagnostics
                                viewModel.runBluetoothDiagnostics()
                            }) {
                                Text(if (showDiagnostics) "Ocultar" else "Executar")
                            }
                        }

                        if (showDiagnostics) {
                            diagnostics.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = if (item.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (item.isHealthy) StatusConnected else StatusWarning,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${item.label}: ${item.status}", style = MaterialTheme.typography.labelLarge)
                                        if (item.recommendation.isNotBlank()) {
                                            Text(item.recommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(viewModel.buildDiagnosticsReport()))
                                        viewModel.showBanner("Relatório de diagnóstico copiado!")
                                    },
                                    modifier = Modifier.weight(1f).testTag("bt_copy_diagnostics"),
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copiar Relatório")
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                        }.onFailure {
                                            viewModel.showBanner("Abra Configurações → Bateria para desativar restrições")
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("bt_battery_optimization"),
                                ) {
                                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Bateria")
                                }
                            }
                        }
                    }
                }
            }

            // Tasker / MacroDroid / NFC Tags & Wear OS Automation Card
            item {
                val sampleUri = "controlebt://connect?mac=${settings.lastDeviceAddress.ifBlank { "AA:BB:CC:DD:EE:FF" }}&mode=presenter"
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Automação (Tasker, MacroDroid, Tags NFC)", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            "Grave a URI abaixo em uma Tag NFC ou dispare via Intent (com.example.action.AUTOMATION). Links com texto ou macro pedem confirmação antes de digitar:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    sampleUri,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(sampleUri))
                                    viewModel.showBanner("URI de automação/NFC copiada!")
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar URI NFC/Tasker", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Text(
                            "Suporte também via Widget na Tela Inicial, Bloco nas Configurações Rápidas (Quick Settings) e Janela Flutuante (PiP).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showMacDialog) {
        AlertDialog(
            onDismissRequest = { showMacDialog = false },
            title = { Text("Conectar por Endereço MAC") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Útil para dispositivos sem descoberta ativa ou servidores headless.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = macInput,
                        onValueChange = { macInput = it.uppercase() },
                        label = { Text("MAC Bluetooth (ex: AA:BB:CC:DD:EE:FF)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (macInput.isNotBlank()) {
                        viewModel.hidManager.connectToDevice(macInput.trim())
                        showMacDialog = false
                    }
                }) { Text("Conectar") }
            },
            dismissButton = { TextButton(onClick = { showMacDialog = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PairedDeviceCard(
    device: PairedDeviceUi,
    easySwitchSlots: List<EasySwitchSlot>,
    onAssignSlot: (Int) -> Unit,
    onUseDevice: () -> Unit,
    onDisconnect: () -> Unit,
    onRemoveDevice: () -> Unit,
) {
    val icon = when (device.category) {
        DeviceCategory.COMPUTER -> Icons.Default.Computer
        DeviceCategory.TV_MEDIA -> Icons.Default.Tv
        DeviceCategory.PHONE_TABLET -> Icons.Default.Smartphone
        DeviceCategory.OTHER -> Icons.Default.DevicesOther
    }

    RemoteSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        highlighted = device.isConnected,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = if (device.isConnected) StatusConnected.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (device.isConnected) StatusConnected else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (device.isConnected) Icons.Default.BluetoothConnected else icon,
                            contentDescription = device.name,
                            tint = if (device.isConnected) StatusConnected else MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (device.isConnected) "Conectado agora • ${device.address}" else device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (device.isConnected) StatusConnected else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (device.isConnected) {
                    OutlinedButton(onClick = onDisconnect) { Text("Sair") }
                } else {
                    FilledTonalButton(onClick = onUseDevice) { Text("Usar") }
                }

                IconButton(onClick = onRemoveDevice) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remover conexão", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Easy-Switch Slot Assignment Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Easy-Switch:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                easySwitchSlots.forEach { slot ->
                    val assignedHere = slot.address.equals(device.address, ignoreCase = true)
                    FilterChip(
                        selected = assignedHere,
                        onClick = { onAssignSlot(slot.slotNumber) },
                        label = { Text("Slot ${slot.slotNumber}") },
                    )
                }
            }
        }
    }
}
