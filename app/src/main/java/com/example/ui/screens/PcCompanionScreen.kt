package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppSettings
import com.example.hid.HidConstants
import com.example.network.PcCompanionClient
import com.example.ui.components.RemoteSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusConnected
import com.example.viewmodel.RemoteViewModel

/**
 * Parte 2 — Optional Wi-Fi / USB Companion Hub:
 * 1. Wake-on-LAN (Magic Packet UDP) + Local Web Layout Editor Server (PC Browser)
 * 2. Bidirectional Clipboard (PC <-> Phone) + Live PC Telemetry & Media Scrubbing + Auto-Layout Switching
 * 3. Live Screen Magnifier ("Lupa" around cursor) + Remote File Explorer & File Upload
 */
@Composable
fun PcCompanionScreen(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val companionConnected by viewModel.companionConnected.collectAsStateWithLifecycle()
    val companionPolling by viewModel.companionPolling.collectAsStateWithLifecycle()
    val pcStatus by viewModel.pcStatus.collectAsStateWithLifecycle()
    val magnifierBitmap by viewModel.magnifierBitmap.collectAsStateWithLifecycle()
    val remoteFiles by viewModel.remoteFiles.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentRemotePath.collectAsStateWithLifecycle()
    val webEditorUrl by viewModel.webEditorUrl.collectAsStateWithLifecycle()
    val webEditorPin by viewModel.webEditorPin.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var hostInput by remember(settings.companionHost) { mutableStateOf(settings.companionHost) }
    var portInput by remember(settings.companionPort) { mutableStateOf(settings.companionPort.toString()) }
    var wolMacInput by remember(settings.wolMacAddress) { mutableStateOf(settings.wolMacAddress) }
    var wolBroadcastInput by remember(settings.wolBroadcastIp) { mutableStateOf(settings.wolBroadcastIp) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                var fileName = "arquivo_controle_bt"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) fileName = cursor.getString(nameIdx)
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    viewModel.uploadFileToPc(fileName, bytes)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            listOf("Conexão & WoL", "Lupa & Clip", "Arquivos").forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("companion_tab_$idx"),
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
                    0 -> {
                        // 1. Wake-on-LAN Card
                        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Wake-on-LAN (Ligar PC pela Rede)", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    "Envia um Magic Packet UDP para acordar um computador desligado ou hibernando antes mesmo do Bluetooth conectar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedTextField(
                                    value = wolMacInput,
                                    onValueChange = { wolMacInput = it },
                                    label = { Text("Endereço MAC da placa de rede (AA:BB:CC:DD:EE:FF)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("wol_mac_input"),
                                )
                                OutlinedTextField(
                                    value = wolBroadcastInput,
                                    onValueChange = { wolBroadcastInput = it },
                                    label = { Text("IP de Broadcast LAN (padrão 255.255.255.255)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = { viewModel.sendWakeOnLan(wolMacInput, wolBroadcastInput) },
                                    modifier = Modifier.fillMaxWidth().testTag("wol_send_button"),
                                ) {
                                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ligar Computador (Wake-on-LAN)")
                                }
                            }
                        }

                        // 2. Embedded PC Web Browser Layout Editor
                        RemoteSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            highlighted = webEditorUrl != null,
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Editor de Layouts pelo Navegador do PC", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    "Inicia um servidor web local no celular para desenhar layouts e macros grandes direto no navegador do computador.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (webEditorUrl != null) {
                                    Text(
                                        text = "Acesse no PC: $webEditorUrl",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = StatusConnected,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    if (!webEditorPin.isNullOrBlank()) {
                                        Text(
                                            text = "PIN da Sessão (obrigatório): $webEditorPin",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.testTag("web_editor_pin_label"),
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.toggleWebLayoutEditorServer() },
                                        modifier = Modifier.weight(1f).testTag("web_editor_toggle"),
                                    ) {
                                        Text(if (webEditorUrl != null) "Parar Servidor" else "Iniciar Editor Web")
                                    }
                                    if (webEditorUrl != null) {
                                        OutlinedButton(onClick = {
                                            clipboard.setText(AnnotatedString(webEditorUrl.orEmpty()))
                                            viewModel.showBanner("URL copiada!")
                                        }) {
                                            Text("URL")
                                        }
                                        if (!webEditorPin.isNullOrBlank()) {
                                            OutlinedButton(onClick = {
                                                clipboard.setText(AnnotatedString(webEditorPin.orEmpty()))
                                                viewModel.showBanner("PIN $webEditorPin copiado!")
                                            }) {
                                                Text("PIN")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Wi-Fi / USB PC Companion Agent Config + Live Telemetry
                        RemoteSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            highlighted = companionConnected,
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = if (companionConnected) StatusConnected else MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Agente Opcional Wi-Fi / USB", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (companionConnected) "Conectado • CPU ${pcStatus.cpuPercent}% • RAM ${pcStatus.ramPercent}%" +
                                                if (pcStatus.batteryPercent >= 0) " • Bat ${pcStatus.batteryPercent}%" else ""
                                            else "Use IP da rede Wi-Fi ou 127.0.0.1 via cabo USB (adb reverse)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (companionConnected) StatusConnected else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = hostInput,
                                        onValueChange = { hostInput = it },
                                        label = { Text("IP do PC (Wi-Fi / USB)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(2f),
                                    )
                                    OutlinedTextField(
                                        value = portInput,
                                        onValueChange = { portInput = it },
                                        label = { Text("Porta") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Troca automática de layout por app", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Alterna para Multimídia, Apresentador ou Numpad conforme o programa aberto no PC",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = settings.companionAutoLayoutSwitch,
                                        onCheckedChange = { v -> viewModel.updateSettings { it.copy(companionAutoLayoutSwitch = v) } },
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("PIN de Segurança (LAN/USB)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = settings.companionAuthToken.ifBlank { "Gerando..." },
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.testTag("companion_auth_pin_text"),
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = {
                                            clipboard.setText(AnnotatedString(settings.companionAuthToken))
                                            viewModel.showBanner("PIN do Agente PC copiado!")
                                        }) {
                                            Text("Copiar")
                                        }
                                        OutlinedButton(onClick = { viewModel.regenerateCompanionToken() }) {
                                            Text("Novo PIN")
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.updateSettings {
                                                it.copy(
                                                    companionHost = hostInput.trim(),
                                                    companionPort = portInput.toIntOrNull() ?: 8766,
                                                )
                                            }
                                            viewModel.toggleCompanionConnection()
                                        },
                                        modifier = Modifier.weight(1f).testTag("companion_connect_button"),
                                    ) {
                                        Text(if (companionPolling) "Desconectar" else "Conectar Agente")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            val script = PcCompanionClient.buildPythonCompanionScript(settings.companionAuthToken)
                                            clipboard.setText(AnnotatedString(script))
                                            viewModel.showBanner("Script Python seguro (com PIN) copiado! Salve como controle_bt_agent.py")
                                        },
                                    ) {
                                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Script PC")
                                    }
                                }

                                // Now-Playing Feedback & Scrubbing Bar (when active)
                                if (pcStatus.mediaTitle.isNotBlank() && pcStatus.mediaDurationSec > 0) {
                                    HorizontalDivider()
                                    Text("REPRODUZINDO NO PC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text("${pcStatus.mediaTitle} — ${pcStatus.mediaArtist}", style = MaterialTheme.typography.titleSmall)
                                    Slider(
                                        value = pcStatus.mediaPositionSec.toFloat(),
                                        onValueChange = { pos -> viewModel.seekCompanionMedia(pos.toInt()) },
                                        valueRange = 0f..pcStatus.mediaDurationSec.toFloat().coerceAtLeast(1f),
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Bidirectional Clipboard Card
                        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Área de Transferência Bidirecional (PC ↔ Celular)", style = MaterialTheme.typography.titleMedium)
                                if (pcStatus.clipboardText.isNotBlank()) {
                                    Text(
                                        "No PC: ${pcStatus.clipboardText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 3,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val localClip = clipboard.getText()?.text.orEmpty()
                                            if (localClip.isBlank()) viewModel.showBanner("Clipboard do celular vazio")
                                            else viewModel.pushClipboardToPc(localClip)
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Celular → PC")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.pullClipboardFromPc { text ->
                                                clipboard.setText(AnnotatedString(text))
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("PC → Celular")
                                    }
                                }
                            }
                        }

                        // Screen Magnifier ("Lupa" around cursor)
                        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Espelhamento Parcial / Lupa do Cursor", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    "Visualize um recorte ao redor do cursor do PC para clicar com precisão à distância. Toque na imagem para clicar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .clickable { viewModel.onMouseClick(HidConstants.MOUSE_LEFT) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val bmp = magnifierBitmap
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Lupa da tela do computador",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Text(
                                            "Conecte o Agente PC e toque em Capturar Lupa",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.refreshScreenMagnifier(fullScreen = false) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Capturar Lupa")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.refreshScreenMagnifier(fullScreen = true) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Tela Inteira")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Remote File Explorer & File Upload
                        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("Transferência e Explorador Remoto", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                        modifier = Modifier.weight(1f).testTag("companion_upload_file"),
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Enviar Arquivo ao PC")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.loadRemoteFiles("") },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Listar PC")
                                    }
                                }

                                if (currentPath.isNotBlank()) {
                                    Text(
                                        "Pasta: $currentPath",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                if (remoteFiles.isEmpty()) {
                                    Text(
                                        "Toque em 'Listar PC' (com o Agente PC rodando) para navegar em pastas e abrir arquivos/programas do computador remotamente.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    remoteFiles.forEach { entry ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (entry.isDirectory) viewModel.loadRemoteFiles(entry.path)
                                                    else viewModel.openRemoteFile(entry.path)
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                                contentDescription = null,
                                                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(entry.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
