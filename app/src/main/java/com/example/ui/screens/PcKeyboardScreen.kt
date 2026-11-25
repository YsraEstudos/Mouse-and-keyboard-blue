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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FilterChip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.TargetOsProfile
import com.example.hid.HidConstants
import com.example.ui.components.RemoteSurface
import com.example.ui.components.TactileKeyButton
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalTronStyle
import com.example.viewmodel.RemoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class PcKeyDef(
    val label: String,
    val usageId: Int,
    val weight: Float = 1f,
    val repeatable: Boolean = false,
)

@Composable
fun PcKeyboardScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val osProfile = settings.targetOsProfile
    var activeModifiers by remember { mutableIntStateOf(HidConstants.MOD_NONE) }
    var stickyLockModifiers by remember { mutableStateOf(false) }

    fun toggleModifier(mask: Int) {
        viewModel.performHaptic(false)
        activeModifiers = activeModifiers xor mask
    }

    fun sendPcKey(usageId: Int, label: String) {
        viewModel.performHaptic(false)
        viewModel.hidManager.sendKeyPress(activeModifiers, usageId, label)
        if (!stickyLockModifiers && activeModifiers != HidConstants.MOD_NONE) {
            activeModifiers = HidConstants.MOD_NONE
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Automatic OS Profile Selector (Windows / macOS / Linux / Android)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Perfil SO:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TargetOsProfile.entries.forEach { profile ->
                FilterChip(
                    selected = osProfile == profile,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.updateSettings { it.copy(targetOsProfile = profile) }
                    },
                    label = { Text(profile.label) },
                )
            }
        }

        // Quick PC System Shortcuts Strip (OS-Adaptive!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val isApple = osProfile == TargetOsProfile.MACOS || osProfile == TargetOsProfile.IOS
            val isIos = osProfile == TargetOsProfile.IOS
            val combos = listOf(
                (if (isApple) "⌘+Tab" else "Alt+Tab") to "ALT+TAB",
                (when { isIos -> "⌘+H Início"; isApple -> "⌘+M"; else -> "Win+D" }) to "WIN+D",
                (if (isIos) "⌘+Espaço" else if (isApple) "⌘+Q" else "Alt+F4") to (if (isIos) "WIN+S" else "ALT+F4"),
                (if (isApple) "⌘+C" else "Ctrl+C") to "CTRL+C",
                (if (isApple) "⌘+V" else "Ctrl+V") to "CTRL+V",
                (if (isApple) "⌘+S" else "Ctrl+S") to "CTRL+S",
                (if (isApple) "⌘+A" else "Ctrl+A") to "CTRL+A",
                "Ctrl+Alt+Del" to "CTRL+ALT+DEL",
                "Ctrl+Shift+Esc" to "CTRL+SHIFT+ESC",
                "Win+L" to "WIN+L",
            )
            combos.forEach { (title, shortcut) ->
                TactileKeyButton(
                    label = title,
                    onClick = {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendAdaptiveShortcut(shortcut, osProfile, title)
                    },
                    minHeight = 40.dp,
                )
            }
        }

        // Function Keys F1-F12 + Esc + Nav Strip
        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val fnKeys = listOf(
                        PcKeyDef("ESC", HidConstants.KEY_ESC),
                        PcKeyDef("F1", HidConstants.KEY_F1),
                        PcKeyDef("F2", HidConstants.KEY_F2),
                        PcKeyDef("F3", HidConstants.KEY_F3),
                        PcKeyDef("F4", HidConstants.KEY_F4),
                        PcKeyDef("F5", HidConstants.KEY_F5),
                        PcKeyDef("F6", HidConstants.KEY_F6),
                        PcKeyDef("F7", HidConstants.KEY_F7),
                        PcKeyDef("F8", HidConstants.KEY_F8),
                        PcKeyDef("F9", HidConstants.KEY_F9),
                        PcKeyDef("F10", HidConstants.KEY_F10),
                        PcKeyDef("F11", HidConstants.KEY_F11),
                        PcKeyDef("F12", HidConstants.KEY_F12),
                    )
                    fnKeys.forEach { k ->
                        KeyCap(
                            label = k.label,
                            onClick = { sendPcKey(k.usageId, k.label) },
                            modifier = Modifier.height(42.dp),
                            minWidthDp = 46,
                        )
                    }
                }

                // System Navigation Keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val navKeys = listOf(
                        PcKeyDef("PrtSc", HidConstants.KEY_PRINT_SCREEN),
                        PcKeyDef("Ins", HidConstants.KEY_INSERT),
                        PcKeyDef("Del", HidConstants.KEY_DELETE, repeatable = true),
                        PcKeyDef("Home", HidConstants.KEY_HOME),
                        PcKeyDef("End", HidConstants.KEY_END),
                        PcKeyDef("PgUp", HidConstants.KEY_PAGE_UP),
                        PcKeyDef("PgDn", HidConstants.KEY_PAGE_DOWN),
                    )
                    navKeys.forEach { k ->
                        KeyCap(
                            label = k.label,
                            onClick = { sendPcKey(k.usageId, k.label) },
                            repeatable = k.repeatable,
                            modifier = Modifier.weight(1f).height(40.dp),
                        )
                    }
                }
            }
        }

        // Main Physical Keyboard Matrix
        RemoteSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                // Row 1: Numbers
                KeyRow(
                    keys = listOf(
                        PcKeyDef("1", HidConstants.KEY_1),
                        PcKeyDef("2", HidConstants.KEY_2),
                        PcKeyDef("3", HidConstants.KEY_3),
                        PcKeyDef("4", HidConstants.KEY_4),
                        PcKeyDef("5", HidConstants.KEY_5),
                        PcKeyDef("6", HidConstants.KEY_6),
                        PcKeyDef("7", HidConstants.KEY_7),
                        PcKeyDef("8", HidConstants.KEY_8),
                        PcKeyDef("9", HidConstants.KEY_9),
                        PcKeyDef("0", HidConstants.KEY_0),
                        PcKeyDef("-", HidConstants.KEY_MINUS),
                        PcKeyDef("⌫", HidConstants.KEY_BACKSPACE, weight = 1.4f, repeatable = true),
                    ),
                    onKey = ::sendPcKey,
                )

                // Row 2: Tab + QWERTY
                KeyRow(
                    keys = listOf(
                        PcKeyDef("Tab", HidConstants.KEY_TAB, weight = 1.3f),
                        PcKeyDef("Q", HidConstants.KEY_Q),
                        PcKeyDef("W", HidConstants.KEY_W),
                        PcKeyDef("E", HidConstants.KEY_E),
                        PcKeyDef("R", HidConstants.KEY_R),
                        PcKeyDef("T", HidConstants.KEY_T),
                        PcKeyDef("Y", HidConstants.KEY_Y),
                        PcKeyDef("U", HidConstants.KEY_U),
                        PcKeyDef("I", HidConstants.KEY_I),
                        PcKeyDef("O", HidConstants.KEY_O),
                        PcKeyDef("P", HidConstants.KEY_P),
                    ),
                    onKey = ::sendPcKey,
                )

                // Row 3: Caps + ASDF + Ç
                KeyRow(
                    keys = listOf(
                        PcKeyDef("Caps", HidConstants.KEY_CAPS_LOCK, weight = 1.3f),
                        PcKeyDef("A", HidConstants.KEY_A),
                        PcKeyDef("S", HidConstants.KEY_S),
                        PcKeyDef("D", HidConstants.KEY_D),
                        PcKeyDef("F", HidConstants.KEY_F),
                        PcKeyDef("G", HidConstants.KEY_G),
                        PcKeyDef("H", HidConstants.KEY_H),
                        PcKeyDef("J", HidConstants.KEY_J),
                        PcKeyDef("K", HidConstants.KEY_K),
                        PcKeyDef("L", HidConstants.KEY_L),
                        PcKeyDef("Ç", HidConstants.KEY_SEMICOLON),
                    ),
                    onKey = ::sendPcKey,
                )

                // Row 4: Shift + ZXCV + Enter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    KeyCap(
                        label = "Shift",
                        active = (activeModifiers and HidConstants.MOD_LSHIFT) != 0,
                        onClick = { toggleModifier(HidConstants.MOD_LSHIFT) },
                        modifier = Modifier.weight(1.5f).height(46.dp).testTag("pc_key_shift"),
                    )
                    val row4 = listOf(
                        PcKeyDef("Z", HidConstants.KEY_Z),
                        PcKeyDef("X", HidConstants.KEY_X),
                        PcKeyDef("C", HidConstants.KEY_C),
                        PcKeyDef("V", HidConstants.KEY_V),
                        PcKeyDef("B", HidConstants.KEY_B),
                        PcKeyDef("N", HidConstants.KEY_N),
                        PcKeyDef("M", HidConstants.KEY_M),
                        PcKeyDef(",", HidConstants.KEY_COMMA),
                        PcKeyDef(".", HidConstants.KEY_DOT),
                    )
                    row4.forEach { k ->
                        KeyCap(
                            label = k.label,
                            onClick = { sendPcKey(k.usageId, k.label) },
                            modifier = Modifier.weight(1f).height(46.dp),
                        )
                    }
                    KeyCap(
                        label = "↵",
                        highlighted = true,
                        onClick = { sendPcKey(HidConstants.KEY_ENTER, "Enter") },
                        modifier = Modifier.weight(1.4f).height(46.dp).testTag("pc_key_enter"),
                    )
                }

                // Row 5: Modifiers + Spacebar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    KeyCap(
                        label = "Ctrl",
                        active = (activeModifiers and HidConstants.MOD_LCTRL) != 0,
                        onClick = { toggleModifier(HidConstants.MOD_LCTRL) },
                        modifier = Modifier.weight(1.2f).height(48.dp).testTag("pc_key_ctrl"),
                    )
                    val isAppleMods = osProfile == TargetOsProfile.MACOS || osProfile == TargetOsProfile.IOS
                    KeyCap(
                        label = if (isAppleMods) "⌘Cmd" else "Win",
                        active = (activeModifiers and HidConstants.MOD_LGUI) != 0,
                        onClick = { toggleModifier(HidConstants.MOD_LGUI) },
                        modifier = Modifier.weight(1.1f).height(48.dp).testTag("pc_key_win"),
                    )
                    KeyCap(
                        label = if (isAppleMods) "Opt" else "Alt",
                        active = (activeModifiers and HidConstants.MOD_LALT) != 0,
                        onClick = { toggleModifier(HidConstants.MOD_LALT) },
                        modifier = Modifier.weight(1.1f).height(48.dp).testTag("pc_key_alt"),
                    )
                    KeyCap(
                        label = "ESPAÇO",
                        onClick = { sendPcKey(HidConstants.KEY_SPACE, "Espaço") },
                        modifier = Modifier.weight(3.2f).height(48.dp).testTag("pc_key_space"),
                    )
                    KeyCap(
                        label = "AltGr",
                        active = (activeModifiers and HidConstants.MOD_RALT) != 0,
                        onClick = { toggleModifier(HidConstants.MOD_RALT) },
                        modifier = Modifier.weight(1.2f).height(48.dp),
                    )
                    KeyCap(
                        label = if (stickyLockModifiers) "Trava ●" else "Trava",
                        active = stickyLockModifiers,
                        onClick = {
                            viewModel.performHaptic(false)
                            stickyLockModifiers = !stickyLockModifiers
                        },
                        modifier = Modifier.weight(1.3f).height(48.dp),
                    )
                }
            }
        }

        // Dedicated Arrow Keys Cluster + Extra Symbols
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            RemoteSurface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        KeyCap("/", onClick = { sendPcKey(HidConstants.KEY_INTL_RO, "/") }, modifier = Modifier.weight(1f).height(44.dp))
                        KeyCap("=", onClick = { sendPcKey(HidConstants.KEY_EQUAL, "=") }, modifier = Modifier.weight(1f).height(44.dp))
                        KeyCap("[", onClick = { sendPcKey(HidConstants.KEY_RIGHT_BRACE, "[") }, modifier = Modifier.weight(1f).height(44.dp))
                        KeyCap("]", onClick = { sendPcKey(HidConstants.KEY_NON_US_HASH, "]") }, modifier = Modifier.weight(1f).height(44.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        KeyCap("´", onClick = { sendPcKey(HidConstants.KEY_LEFT_BRACE, "´") }, modifier = Modifier.weight(1f).height(44.dp))
                        KeyCap("~", onClick = { sendPcKey(HidConstants.KEY_APOSTROPHE, "~") }, modifier = Modifier.weight(1f).height(44.dp))
                        KeyCap(";", onClick = { sendPcKey(HidConstants.KEY_SLASH, ";") }, modifier = Modifier.weight(1f).height(44.dp))
                        KeyCap("Menu", onClick = { sendPcKey(HidConstants.KEY_APP_MENU, "Menu") }, modifier = Modifier.weight(1.2f).height(44.dp))
                    }
                }
            }

            // Inverted-T Arrow Cluster
            RemoteSurface(modifier = Modifier.weight(0.95f), shape = RoundedCornerShape(14.dp)) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Spacer(Modifier.weight(1f))
                        KeyCap(
                            label = "↑",
                            onClick = { sendPcKey(HidConstants.KEY_UP, "↑") },
                            repeatable = true,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("pc_arrow_up"),
                        )
                        Spacer(Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        KeyCap(
                            label = "←",
                            onClick = { sendPcKey(HidConstants.KEY_LEFT, "←") },
                            repeatable = true,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("pc_arrow_left"),
                        )
                        KeyCap(
                            label = "↓",
                            onClick = { sendPcKey(HidConstants.KEY_DOWN, "↓") },
                            repeatable = true,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("pc_arrow_down"),
                        )
                        KeyCap(
                            label = "→",
                            onClick = { sendPcKey(HidConstants.KEY_RIGHT, "→") },
                            repeatable = true,
                            modifier = Modifier.weight(1f).height(44.dp).testTag("pc_arrow_right"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<PcKeyDef>,
    onKey: (Int, String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        keys.forEach { k ->
            KeyCap(
                label = k.label,
                onClick = { onKey(k.usageId, k.label) },
                repeatable = k.repeatable,
                modifier = Modifier.weight(k.weight).height(46.dp),
            )
        }
    }
}

@Composable
private fun KeyCap(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    highlighted: Boolean = false,
    repeatable: Boolean = false,
    minWidthDp: Int = 0,
) {
    val tron = LocalTronStyle.current
    val scope = rememberCoroutineScope()
    val latestClick by rememberUpdatedState(onClick)
    var isHeld by remember { mutableStateOf(false) }

    val isPressedHighlight = active || isHeld
    val bg = when {
        isPressedHighlight -> MaterialTheme.colorScheme.primary
        highlighted -> MaterialTheme.colorScheme.primaryContainer
        tron.enabled -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        isPressedHighlight -> MaterialTheme.colorScheme.onPrimary
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = when {
        isPressedHighlight || highlighted -> MaterialTheme.colorScheme.primary
        tron.enabled -> tron.glowColor.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val finalMod = if (minWidthDp > 0) {
        modifier.padding(horizontal = 1.dp)
    } else {
        modifier
    }

    val interactionMod = if (repeatable) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isHeld = true
                latestClick()
                val repeatJob = scope.launch {
                    delay(380L)
                    while (isActive) {
                        latestClick()
                        delay(75L)
                    }
                }
                waitForUpOrCancellation()
                repeatJob.cancel()
                isHeld = false
            }
        }
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Surface(
        modifier = finalMod
            .clip(RoundedCornerShape(8.dp))
            .then(interactionMod),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(if (isPressedHighlight || tron.enabled) 1.2.dp else 0.7.dp, border),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = if (minWidthDp > 0) 10.dp else 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = if (label.length > 3) 10.sp else 12.sp,
                color = fg,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
