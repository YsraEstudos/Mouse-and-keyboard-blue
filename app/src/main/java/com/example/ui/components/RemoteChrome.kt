package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.BluetoothConnectionState
import com.example.data.ControlMode
import com.example.data.EasySwitchSlot
import com.example.data.HidProfileMode
import com.example.data.TargetOsProfile
import com.example.ui.theme.LocalTronStyle
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun RemoteSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val tron = LocalTronStyle.current
    val borderColor = when {
        tron.enabled && highlighted -> tron.glowColor
        tron.enabled -> tron.glowColor.copy(alpha = 0.45f)
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (tron.enabled || highlighted) 1.3.dp else 1.dp

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (tron.enabled) 0.dp else 2.dp,
        content = content,
    )
}

@Composable
fun TactileKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    minHeight: Dp = 48.dp,
    testTagName: String? = null,
) {
    val tron = LocalTronStyle.current
    val bg = when {
        active -> MaterialTheme.colorScheme.primaryContainer
        tron.enabled -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        active -> MaterialTheme.colorScheme.primary
        tron.enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        active -> MaterialTheme.colorScheme.primary
        tron.enabled -> tron.glowColor.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val baseMod = modifier
        .heightIn(min = minHeight)
        .clip(RoundedCornerShape(12.dp))
        .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)

    Surface(
        modifier = baseMod.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(if (active || tron.enabled) 1.2.dp else 0.8.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label.ifBlank { null },
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                if (label.isNotEmpty()) Spacer(Modifier.width(6.dp))
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Tactile button supporting continuous Key Repeat while held down (e.g. Volume +/-, Channel +/-, Backspace, Arrows).
 * Fires immediately on press, waits 380 ms, then repeats every 80 ms until released.
 */
@Composable
fun RepeatableTactileKeyButton(
    label: String,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    minHeight: Dp = 48.dp,
    testTagName: String? = null,
) {
    val tron = LocalTronStyle.current
    val scope = rememberCoroutineScope()
    val latestTrigger by rememberUpdatedState(onTrigger)
    var isHeld by remember { mutableStateOf(false) }

    val highlighted = active || isHeld
    val bg = when {
        highlighted -> MaterialTheme.colorScheme.primaryContainer
        tron.enabled -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        highlighted -> MaterialTheme.colorScheme.primary
        tron.enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = when {
        highlighted -> MaterialTheme.colorScheme.primary
        tron.enabled -> tron.glowColor.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val baseMod = modifier
        .heightIn(min = minHeight)
        .clip(RoundedCornerShape(12.dp))
        .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier)
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isHeld = true
                latestTrigger()
                val repeatJob = scope.launch {
                    delay(380L)
                    while (isActive) {
                        latestTrigger()
                        delay(80L)
                    }
                }
                waitForUpOrCancellation()
                repeatJob.cancel()
                isHeld = false
            }
        }

    Surface(
        modifier = baseMod,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(if (highlighted || tron.enabled) 1.2.dp else 0.8.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label.ifBlank { null },
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                if (label.isNotEmpty()) Spacer(Modifier.width(6.dp))
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Compact inline banner displayed on control screens when no Bluetooth HID device is connected,
 * giving instant visibility and a 1-tap path to connect.
 */
@Composable
fun DisconnectedStatusBanner(
    connectionState: BluetoothConnectionState,
    onOpenDevices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (connectionState is BluetoothConnectionState.Connected ||
        connectionState is BluetoothConnectionState.Connecting
    ) return

    val (text, accentColor) = when (connectionState) {
        is BluetoothConnectionState.BluetoothOff ->
            stringResource(R.string.banner_bt_off_title) to MaterialTheme.colorScheme.error
        is BluetoothConnectionState.PermissionRequired ->
            stringResource(R.string.banner_permission_title) to StatusWarning
        else ->
            stringResource(R.string.banner_disconnected_title) to MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenDevices)
            .testTag("disconnected_inline_banner"),
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = if (connectionState is BluetoothConnectionState.BluetoothOff) {
                        Icons.Default.BluetoothDisabled
                    } else {
                        Icons.Default.Bluetooth
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${stringResource(R.string.banner_disconnected_cta)} →",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Adaptive NavigationRail for tablets and unfolded foldables (width >= 600dp, Android 16 compliance).
 */
@Composable
fun RemoteNavigationRail(
    currentMode: ControlMode,
    onSelectMode: (ControlMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ControlMode.entries.forEach { mode ->
                NavigationRailItem(
                    selected = currentMode == mode,
                    onClick = { onSelectMode(mode) },
                    icon = { Icon(iconForMode(mode), contentDescription = mode.title) },
                    label = {
                        Text(
                            text = mode.title.substringBefore(" "),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    modifier = Modifier.testTag("rail_item_${mode.route}"),
                )
            }
        }
    }
}

@Composable
fun RemoteTopBar(
    currentMode: ControlMode,
    connectionState: BluetoothConnectionState,
    airMouseActive: Boolean,
    jigglerActive: Boolean,
    autoClickerActive: Boolean = false,
    recordingMacro: Boolean = false,
    osProfile: TargetOsProfile = TargetOsProfile.WINDOWS,
    hidProfile: HidProfileMode = HidProfileMode.UNIVERSAL,
    showDrawerMenuButton: Boolean = true,
    easySwitchSlots: List<EasySwitchSlot> = emptyList(),
    onActivateEasySwitch: (Int) -> Unit = {},
    onEnterPip: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tron = LocalTronStyle.current
    val connectedAddr = (connectionState as? BluetoothConnectionState.Connected)?.address
    val hasConfiguredEasySwitch = easySwitchSlots.any { it.address.isNotBlank() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("top_bar_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else if (showDrawerMenuButton) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("top_bar_menu_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Abrir menu de modos",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    Spacer(Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) {
                    Text(
                        text = currentMode.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val tags = buildList {
                        add(osProfile.shortTag)
                        add(hidProfile.shortLabel)
                        if (airMouseActive) add("AIR")
                        if (jigglerActive) add("JIG")
                        if (autoClickerActive) add("CLICK")
                        if (recordingMacro) add("REC ●")
                    }.joinToString(" • ")
                    Text(
                        text = tags,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (recordingMacro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }

                if (hasConfiguredEasySwitch) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        easySwitchSlots.forEach { slot ->
                            val isActive = connectedAddr != null && slot.address.equals(connectedAddr, ignoreCase = true)
                            val isSet = slot.address.isNotBlank()
                            Surface(
                                modifier = Modifier
                                    .size(width = 26.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onActivateEasySwitch(slot.slotNumber) }
                                    .testTag("easy_switch_top_${slot.slotNumber}"),
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isActive -> StatusConnected.copy(alpha = 0.24f)
                                    isSet -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isActive -> StatusConnected
                                        isSet -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${slot.slotNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) StatusConnected else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                val (pillColor, dotColor, pillLabel, btIcon) = when (connectionState) {
                    is BluetoothConnectionState.Connected -> Quad(
                        StatusConnected.copy(alpha = 0.16f),
                        StatusConnected,
                        connectionState.deviceName.take(10),
                        Icons.Default.BluetoothConnected,
                    )
                    is BluetoothConnectionState.Connecting -> Quad(
                        StatusWarning.copy(alpha = 0.16f),
                        StatusWarning,
                        "Conect...",
                        Icons.Default.Bluetooth,
                    )
                    is BluetoothConnectionState.BluetoothOff -> Quad(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.error,
                        "BT Off",
                        Icons.Default.BluetoothDisabled,
                    )
                    else -> Quad(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        "BT",
                        Icons.Default.Bluetooth,
                    )
                }

                Surface(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenDevices)
                        .testTag("bluetooth_status_pill"),
                    shape = CircleShape,
                    color = pillColor,
                    border = BorderStroke(
                        1.dp,
                        if (tron.enabled) tron.glowColor.copy(alpha = 0.6f) else dotColor.copy(alpha = 0.45f)
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = btIcon,
                            contentDescription = "Status Bluetooth",
                            tint = dotColor,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = pillLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }

                if (onEnterPip != null) {
                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier.testTag("top_bar_pip_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Modo janela flutuante (PiP)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (currentMode != ControlMode.SETTINGS) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("top_bar_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
            HorizontalDivider(
                color = if (tron.enabled) tron.glowColor.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun iconForMode(mode: ControlMode): ImageVector = when (mode) {
    ControlMode.MOUSE_KEYBOARD -> Icons.Default.Mouse
    ControlMode.MULTIMEDIA -> Icons.Default.Tv
    ControlMode.PC_KEYBOARD -> Icons.Default.Keyboard
    ControlMode.GAMEPAD -> Icons.Default.SportsEsports
    ControlMode.DIGITIZER -> Icons.Default.Draw
    ControlMode.NUMPAD -> Icons.Default.Dialpad
    ControlMode.PRESENTER -> Icons.Default.PresentToAll
    ControlMode.SCANNER -> Icons.Default.QrCodeScanner
    ControlMode.CUSTOM_LAYOUTS -> Icons.Default.DashboardCustomize
    ControlMode.VAULT -> Icons.Default.Fingerprint
    ControlMode.COMPANION -> Icons.Default.Wifi
    ControlMode.DEVICES -> Icons.Default.Bluetooth
    ControlMode.SETTINGS -> Icons.Default.Settings
}

@Composable
fun RemoteNavigationDrawerContent(
    currentMode: ControlMode,
    connectionState: BluetoothConnectionState,
    onSelectMode: (ControlMode) -> Unit,
) {
    val tron = LocalTronStyle.current
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(308.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                val statusText = when (connectionState) {
                    is BluetoothConnectionState.Connected -> "Conectado: ${connectionState.deviceName}"
                    is BluetoothConnectionState.Connecting -> "Conectando a ${connectionState.deviceName}..."
                    else -> "Bluetooth HID Direto • PC, Mac, iPhone, iPad e TV"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = if (tron.enabled) tron.glowColor.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outlineVariant,
            )

            DrawerSectionHeader("CONTROLES PRINCIPAIS (100% HID)")

            listOf(
                ControlMode.MOUSE_KEYBOARD,
                ControlMode.MULTIMEDIA,
                ControlMode.PC_KEYBOARD,
                ControlMode.NUMPAD,
                ControlMode.PRESENTER,
            ).forEach { mode ->
                DrawerModeItem(mode, currentMode == mode) { onSelectMode(mode) }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = if (tron.enabled) tron.glowColor.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outlineVariant,
            )

            DrawerSectionHeader("MODOS PRO & AUTOMAÇÃO (100% HID)")

            listOf(
                ControlMode.GAMEPAD,
                ControlMode.DIGITIZER,
                ControlMode.SCANNER,
                ControlMode.CUSTOM_LAYOUTS,
                ControlMode.VAULT,
            ).forEach { mode ->
                DrawerModeItem(mode, currentMode == mode) { onSelectMode(mode) }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = if (tron.enabled) tron.glowColor.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outlineVariant,
            )

            DrawerSectionHeader("CONEXÃO, WI-FI COMPANION & SISTEMA")

            listOf(
                ControlMode.COMPANION,
                ControlMode.DEVICES,
                ControlMode.SETTINGS,
            ).forEach { mode ->
                DrawerModeItem(mode, currentMode == mode) { onSelectMode(mode) }
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
    )
}

@Composable
private fun DrawerModeItem(
    mode: ControlMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = {
            Column {
                Text(mode.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    mode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        icon = { Icon(iconForMode(mode), contentDescription = mode.title) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag("drawer_item_${mode.route}"),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/**
 * 3-step First-Run Onboarding Wizard guiding native Bluetooth HID, permissions, iOS AssistiveTouch,
 * Target OS selection, and HID Profile compatibility.
 */
@Composable
fun OnboardingWizardDialog(
    selectedOs: TargetOsProfile,
    selectedHidProfile: HidProfileMode,
    onSelectOs: (TargetOsProfile) -> Unit,
    onSelectHidProfile: (HidProfileMode) -> Unit,
    onRequestBluetoothPermissions: () -> Unit,
    onOpenDevicesAndFinish: () -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onFinish,
        icon = { Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(
                text = when (step) {
                    0 -> stringResource(R.string.onboarding_title_1)
                    1 -> stringResource(R.string.onboarding_title_2)
                    else -> stringResource(R.string.onboarding_title_3)
                },
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Passo ${step + 1} de 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                when (step) {
                    0 -> {
                        Text(
                            text = stringResource(R.string.onboarding_desc_1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Recursos extras via Wi-Fi (Lupa da tela, Clipboard bidirecional e Arquivos) são 100% opcionais e protegidos por PIN automático.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    1 -> {
                        Text(
                            text = stringResource(R.string.onboarding_desc_2),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = onRequestBluetoothPermissions,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_request_bt_perm"),
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Autorizar Permissões Bluetooth")
                        }
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.onboarding_desc_3),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("Sistema alvo principal:", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TargetOsProfile.entries.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { os ->
                                        FilterChip(
                                            selected = selectedOs == os,
                                            onClick = { onSelectOs(os) },
                                            label = { Text(os.label) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        Text("Perfil Bluetooth HID:", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(HidProfileMode.UNIVERSAL, HidProfileMode.PRO_COMPLETE).forEach { prof ->
                                FilterChip(
                                    selected = selectedHidProfile == prof,
                                    onClick = { onSelectHidProfile(prof) },
                                    label = { Text(prof.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step < 2) {
                Button(
                    onClick = { step++ },
                    modifier = Modifier.testTag("onboarding_next_button"),
                ) {
                    Text("Próximo")
                }
            } else {
                Button(
                    onClick = onOpenDevicesAndFinish,
                    modifier = Modifier.testTag("onboarding_finish_connect"),
                ) {
                    Text("Conectar Agora")
                }
            }
        },
        dismissButton = {
            if (step > 0) {
                TextButton(onClick = { step-- }) { Text("Voltar") }
            } else {
                TextButton(onClick = onFinish, modifier = Modifier.testTag("onboarding_skip_button")) {
                    Text("Pular")
                }
            }
        },
    )
}

@Composable
fun GestureGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Comandos Gestuais Avançados", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                GestureRow("1 dedo • Deslizar / Toque", "Move o cursor (125 Hz suave) / Clique esquerdo")
                GestureRow("2 dedos • Toque rápido", "Clique direito (menu de contexto)")
                GestureRow("2 dedos • Deslizar ↑↓ ou ←→", "Rolagem vertical e horizontal (planilhas/timeline)")
                GestureRow("2 dedos • Pinça (Pinch-to-Zoom)", "Zoom + e Zoom - instantâneo (Ctrl/Cmd + Scroll)")
                GestureRow("3 dedos • Toque rápido", "Clique central (abrir/fechar aba)")
                GestureRow("3 dedos • Deslizar ← / →", "Voltar / Avançar página ou aba")
                GestureRow("3 dedos • Deslizar ↑ / ↓", "Visão de tarefas (Win+Tab / Cmd+Tab) / Início")
                GestureRow("4 dedos • Deslizar ← / →", "Trocar Área de Trabalho Virtual")
                GestureRow("Segurar botão Esquerdo", "Drag-Lock (arrastar janelas e seleções)")
                GestureRow("Botão Sniper / Precisão", "Segure para reduzir DPI em 60% para cliques cirúrgicos")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendi")
            }
        },
    )
}

@Composable
private fun GestureRow(gesture: String, action: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(gesture, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(action, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
