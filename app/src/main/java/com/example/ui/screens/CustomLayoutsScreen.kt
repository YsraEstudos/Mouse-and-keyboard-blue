package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomLayout
import com.example.data.CustomLayoutWidget
import com.example.data.CustomWidgetType
import com.example.data.MacroStep
import com.example.data.CustomLayoutSerializer
import com.example.hid.HidConstants
import com.example.ui.components.RemoteSurface
import com.example.ui.components.TactileKeyButton
import com.example.ui.components.TouchpadWithScrollbar
import com.example.viewmodel.RemoteViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CustomLayoutsScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val layouts by viewModel.customLayouts.collectAsStateWithLifecycle()
    val isMacroLoopRunning by viewModel.isMacroLoopRunning.collectAsStateWithLifecycle()
    val isRecordingMacro by viewModel.isRecordingMacro.collectAsStateWithLifecycle()
    val recordedSteps by viewModel.recordedMacroSteps.collectAsStateWithLifecycle()

    var selectedLayoutId by remember { mutableStateOf<Long?>(null) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Usar, 1 = Galeria Stream Deck, 2 = Gerenciar & Backup
    var editingLayout by remember { mutableStateOf<CustomLayout?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingDeleteLayout by remember { mutableStateOf<CustomLayout?>(null) }

    val activeLayout = layouts.find { it.id == selectedLayoutId } ?: layouts.firstOrNull()

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }.onSuccess {
                viewModel.showBanner("Arquivo JSON salvo (compatível com Google Drive)!")
            }
        }
        pendingExportJson = null
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { content ->
                viewModel.importLayoutJson(content)
            }
        }
    }

    if (editingLayout != null) {
        LayoutEditorView(
            initialLayout = editingLayout!!,
            viewModel = viewModel,
            onSave = { updated ->
                viewModel.saveCustomLayout(updated)
                editingLayout = null
                activeTab = 0
            },
            onCancel = { editingLayout = null },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Active Macro Loop Stop Banner OR Live Macro Recording Banner
        if (isMacroLoopRunning) {
            RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Macro em loop contínuo ativa...", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Button(
                        onClick = { viewModel.hidManager.stopMacroLoop() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Parar Loop")
                    }
                }
            }
        }

        // Top Mode Tabs + Quick Add
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Usar") },
                modifier = Modifier.testTag("custom_tab_use"),
            )
            FilterChip(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Galeria Stream Deck") },
                modifier = Modifier.testTag("custom_tab_gallery"),
            )
            FilterChip(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Gerenciar (${layouts.size})") },
                modifier = Modifier.testTag("custom_tab_manage"),
            )
            IconButton(
                onClick = {
                    editingLayout = CustomLayout(
                        id = 0L,
                        name = "Novo Controle",
                        description = "Layout personalizado",
                        category = "Personalizado",
                        widgets = listOf(
                            CustomLayoutWidget(UUID.randomUUID().toString(), "Touchpad", CustomWidgetType.TOUCHPAD.name, colSpan = 4, heightUnits = 2),
                            CustomLayoutWidget(UUID.randomUUID().toString(), "Enter", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "ENTER"),
                            CustomLayoutWidget(UUID.randomUUID().toString(), "Copiar", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+C"),
                        ),
                    )
                },
                modifier = Modifier.testTag("custom_create_button"),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Criar layout", tint = MaterialTheme.colorScheme.primary)
            }
        }

        when (activeTab) {
            1 -> {
                // Stream Deck Ready-Made Gallery (OBS, Photoshop, Premiere, Excel, VS Code, Spotify, YouTube)
                val galleryPresets = remember { com.example.data.CustomLayoutSerializer.streamDeckGallery() }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(galleryPresets) { preset ->
                        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${preset.category} • ${preset.widgets.size} atalhos/macros — ${preset.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Button(onClick = {
                                    viewModel.installGalleryPreset(preset)
                                    activeTab = 0
                                }) {
                                    Text("Instalar")
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Cloud Backup / Restore All + Manage Installed Layouts
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("BACKUP EM NUVEM / GOOGLE DRIVE (SAF)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val bundleJson = viewModel.exportAllLayoutsBackupJson()
                                        pendingExportJson = bundleJson
                                        clipboard.setText(AnnotatedString(bundleJson))
                                        exportFileLauncher.launch("controle_bt_backup_completo.json")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("custom_backup_all"),
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Backup Tudo")
                            }
                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f).testTag("custom_import_button"),
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Restaurar")
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(layouts, key = { it.id }) { layout ->
                        RemoteSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            highlighted = activeLayout?.id == layout.id,
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column {
                                    Text(layout.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${layout.category} • ${layout.widgets.size} controles • ${layout.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    OutlinedButton(onClick = { selectedLayoutId = layout.id; activeTab = 0 }) { Text("Abrir") }
                                    OutlinedButton(onClick = { editingLayout = layout }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Editar")
                                    }
                                    OutlinedButton(onClick = { viewModel.duplicateCustomLayout(layout) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Duplicar")
                                    }
                                    OutlinedButton(onClick = {
                                        val json = viewModel.exportLayoutJson(layout)
                                        clipboard.setText(AnnotatedString(json))
                                        pendingExportJson = json
                                        exportFileLauncher.launch("${layout.name.lowercase().replace(" ", "_")}.json")
                                    }) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Exportar")
                                    }
                                    if (layouts.size > 1) {
                                        IconButton(onClick = { pendingDeleteLayout = layout }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir layout", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Tab 0: Active Layout Selector Pills + Live Macro Recorder Bar
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    layouts.forEach { layout ->
                        FilterChip(
                            selected = activeLayout?.id == layout.id,
                            onClick = { selectedLayoutId = layout.id },
                            label = { Text(layout.name) },
                        )
                    }
                }

                // Live Macro Recorder Bar
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), highlighted = isRecordingMacro) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (isRecordingMacro) "Gravando Macro (${recordedSteps.size} passos)..."
                            else "Gravador de Macros em Tempo Real",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isRecordingMacro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (isRecordingMacro) {
                            Button(
                                onClick = {
                                    val steps = viewModel.stopMacroRecording()
                                    val target = activeLayout
                                    if (steps.isNotEmpty() && target != null) {
                                        val newMacroWidget = CustomLayoutWidget(
                                            id = UUID.randomUUID().toString(),
                                            label = "Macro Gravada (${steps.size})",
                                            type = CustomWidgetType.MACRO.name,
                                            colSpan = 4,
                                            macroSteps = steps,
                                            repeatCount = 1,
                                        )
                                        viewModel.saveCustomLayout(target.copy(widgets = target.widgets + newMacroWidget))
                                        viewModel.showBanner("Macro gravada salva neste layout!")
                                    } else {
                                        viewModel.showBanner("Gravação encerrada sem passos")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("macro_record_stop"),
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Salvar")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.startMacroRecording() },
                                modifier = Modifier.testTag("macro_record_start"),
                            ) {
                                Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Gravar")
                            }
                        }
                    }
                }

                // Render Active Custom Layout Grid (4-column flow rows)
                if (activeLayout != null) {
                    val rows = remember(activeLayout.widgets) { packWidgetsIntoRows(activeLayout.widgets) }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rows.forEach { rowWidgets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowWidgets.forEach { widget ->
                                    val type = runCatching { CustomWidgetType.valueOf(widget.type) }
                                        .getOrDefault(CustomWidgetType.KEY)
                                    val weight = widget.colSpan.coerceIn(1, 4).toFloat()
                                    val cellHeight = (widget.heightUnits.coerceIn(1, 3) * 62 + (widget.heightUnits - 1) * 16).dp

                                    if (type == CustomWidgetType.TOUCHPAD) {
                                        TouchpadWithScrollbar(
                                            modifier = Modifier.weight(weight).height(cellHeight.coerceAtLeast(156.dp)),
                                            showScrollbar = false,
                                            tapToClick = true,
                                            onMove = { dx, dy -> viewModel.onTouchpadMove(dx, dy) },
                                            onTapLeftClick = { viewModel.onMouseClick(HidConstants.MOUSE_LEFT) },
                                            onPenHoldChange = {},
                                            onScrollDelta = { viewModel.onScrollDelta(it) },
                                            onScrollFling = { viewModel.onScrollFling(it) },
                                            onGesture = { viewModel.onGestureCommand(it) },
                                        )
                                    } else {
                                        val isMacro = type == CustomWidgetType.MACRO
                                        val loopSuffix = if (isMacro && widget.repeatCount == 0) " ⟳"
                                        else if (isMacro && widget.repeatCount > 1) " (${widget.repeatCount}x)"
                                        else ""
                                        TactileKeyButton(
                                            label = widget.label + loopSuffix,
                                            active = isMacro,
                                            minHeight = cellHeight,
                                            modifier = Modifier.weight(weight),
                                            onClick = { viewModel.triggerCustomWidget(widget) },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar / Restaurar Backup JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            showImportDialog = false
                            importFileLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir do Celular / Google Drive")
                    }
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        label = { Text("Ou cole o JSON aqui") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importJsonInput.isNotBlank()) {
                        viewModel.importLayoutJson(importJsonInput)
                        importJsonInput = ""
                        showImportDialog = false
                    }
                }) { Text("Importar") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") } },
        )
    }

    pendingDeleteLayout?.let { layout ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLayout = null },
            title = { Text("Excluir layout?") },
            text = { Text("Deseja remover permanentemente o layout \"${layout.name}\"?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteCustomLayout(layout)
                    pendingDeleteLayout = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLayout = null }) { Text("Cancelar") }
            },
        )
    }
}

private fun packWidgetsIntoRows(widgets: List<CustomLayoutWidget>): List<List<CustomLayoutWidget>> {
    val rows = mutableListOf<List<CustomLayoutWidget>>()
    var currentRow = mutableListOf<CustomLayoutWidget>()
    var currentSpan = 0
    for (w in widgets) {
        val span = w.colSpan.coerceIn(1, 4)
        if (currentSpan + span > 4 && currentRow.isNotEmpty()) {
            rows.add(currentRow)
            currentRow = mutableListOf()
            currentSpan = 0
        }
        currentRow.add(w)
        currentSpan += span
    }
    if (currentRow.isNotEmpty()) rows.add(currentRow)
    return rows
}

@Composable
private fun LayoutEditorView(
    initialLayout: CustomLayout,
    viewModel: RemoteViewModel,
    onSave: (CustomLayout) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(initialLayout.name) }
    var description by remember { mutableStateOf(initialLayout.description) }
    val widgets = remember { mutableStateListOf<CustomLayoutWidget>().apply { addAll(initialLayout.widgets) } }
    var showAddWidgetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Editor de Layout", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) { Text("Cancelar") }
                Button(
                    onClick = {
                        onSave(initialLayout.copy(name = name, description = description, widgets = widgets.toList()))
                    },
                    modifier = Modifier.testTag("layout_editor_save"),
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salvar")
                }
            }
        }

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do layout") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("CONTROLES (${widgets.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { showAddWidgetDialog = true },
                modifier = Modifier.testTag("layout_editor_add_widget"),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Adicionar Controle / Macro")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(widgets, key = { it.id }) { widget ->
                RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(widget.label, style = MaterialTheme.typography.titleSmall)
                            val detail = if (widget.type == CustomWidgetType.MACRO.name) {
                                val loopTxt = if (widget.repeatCount == 0) "Loop Contínuo" else "${widget.repeatCount}x"
                                "MACRO (${widget.macroSteps.size} passos • $loopTxt) • ${widget.colSpan}/4"
                            } else {
                                "${widget.type}: ${widget.keyValue.ifEmpty { "Área tátil" }} • ${widget.colSpan}/4"
                            }
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { widgets.remove(widget) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover controle", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddWidgetDialog) {
        AddCustomWidgetDialog(
            recordedSteps = viewModel.recordedMacroSteps.value,
            onAdd = { newWidget ->
                widgets.add(newWidget)
                showAddWidgetDialog = false
            },
            onDismiss = { showAddWidgetDialog = false },
        )
    }
}

@Composable
private fun AddCustomWidgetDialog(
    recordedSteps: List<MacroStep>,
    onAdd: (CustomLayoutWidget) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by remember { mutableStateOf(CustomWidgetType.SHORTCUT) }
    var label by remember { mutableStateOf("Atalho") }
    var keyValue by remember { mutableStateOf("CTRL+C") }
    var colSpan by remember { mutableIntStateOf(2) }
    var repeatCount by remember { mutableIntStateOf(1) }
    var macroStepsText by remember {
        mutableStateOf(
            if (recordedSteps.isNotEmpty()) {
                recordedSteps.joinToString("\n") { "${it.action}:${it.value}" }
            } else {
                "SHORTCUT:CTRL+S\nDELAY:250\nKEY:F5"
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Controle / Macro") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        CustomWidgetType.KEY to "Tecla",
                        CustomWidgetType.SHORTCUT to "Atalho",
                        CustomWidgetType.MACRO to "Macro",
                        CustomWidgetType.TOUCHPAD to "Touchpad",
                        CustomWidgetType.MEDIA to "Mídia",
                        CustomWidgetType.MOUSE_BUTTON to "Mouse",
                    ).forEach { (type, title) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                when (type) {
                                    CustomWidgetType.KEY -> { label = "Espaço"; keyValue = "SPACE"; colSpan = 2 }
                                    CustomWidgetType.SHORTCUT -> { label = "Copiar"; keyValue = "CTRL+C"; colSpan = 2 }
                                    CustomWidgetType.MACRO -> { label = "Minha Macro"; colSpan = 4 }
                                    CustomWidgetType.TOUCHPAD -> { label = "Touchpad"; keyValue = ""; colSpan = 4 }
                                    CustomWidgetType.MEDIA -> { label = "Play/Pause"; keyValue = "PLAY_PAUSE"; colSpan = 2 }
                                    CustomWidgetType.MOUSE_BUTTON -> { label = "Clique Esq."; keyValue = "LEFT_CLICK"; colSpan = 2 }
                                }
                            },
                            label = { Text(title) },
                        )
                    }
                }

                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Rótulo do botão") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (selectedType == CustomWidgetType.MACRO) {
                    OutlinedTextField(
                        value = macroStepsText,
                        onValueChange = { macroStepsText = it },
                        label = { Text("Passos (KEY:, SHORTCUT:, TEXT:, DELAY:ms)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    )
                    Text("Repetição (Loop):", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(1 to "1x", 3 to "3x", 5 to "5x", 10 to "10x", 0 to "Contínuo ⟳").forEach { (cnt, txt) ->
                            FilterChip(
                                selected = repeatCount == cnt,
                                onClick = { repeatCount = cnt },
                                label = { Text(txt) },
                            )
                        }
                    }
                } else if (selectedType != CustomWidgetType.TOUCHPAD) {
                    OutlinedTextField(
                        value = keyValue,
                        onValueChange = { keyValue = it },
                        label = {
                            Text(
                                when (selectedType) {
                                    CustomWidgetType.KEY -> "Tecla (ex: W, ESC, F5, ENTER, SPACE)"
                                    CustomWidgetType.SHORTCUT -> "Combinação (ex: CTRL+SHIFT+T, ALT+TAB)"
                                    CustomWidgetType.MEDIA -> "Ação (PLAY_PAUSE, VOL_UP, VOL_DOWN, MUTE, NEXT, PREV)"
                                    else -> "Botão (LEFT_CLICK, RIGHT_CLICK, MIDDLE_CLICK)"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Largura:", style = MaterialTheme.typography.labelLarge)
                    (1..4).forEach { span ->
                        FilterChip(selected = colSpan == span, onClick = { colSpan = span }, label = { Text("$span/4") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedMacro = if (selectedType == CustomWidgetType.MACRO) {
                    macroStepsText.lines().mapNotNull { line ->
                        val idx = line.indexOf(':')
                        if (idx > 0) {
                            MacroStep(
                                action = line.substring(0, idx).trim().uppercase(),
                                value = line.substring(idx + 1).trim(),
                            )
                        } else null
                    }
                } else emptyList()

                onAdd(
                    CustomLayoutWidget(
                        id = UUID.randomUUID().toString(),
                        label = label.ifBlank { selectedType.name },
                        type = selectedType.name,
                        colSpan = colSpan,
                        heightUnits = if (selectedType == CustomWidgetType.TOUCHPAD) 2 else 1,
                        keyValue = keyValue.trim(),
                        macroSteps = parsedMacro,
                        repeatCount = if (selectedType == CustomWidgetType.MACRO) repeatCount else 1,
                    )
                )
            }) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
