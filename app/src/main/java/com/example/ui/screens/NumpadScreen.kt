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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.hid.HidConstants
import com.example.ui.components.RemoteSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalTronStyle
import com.example.viewmodel.RemoteViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun NumpadScreen(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    var calculatorMode by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf("") }
    var useCommaDecimal by remember { mutableStateOf(true) }

    val evaluatedResult = remember(expression) { evaluateSimpleExpression(expression) }

    fun onPadInput(symbol: String, hidCode: Int) {
        viewModel.performHaptic(false)
        if (calculatorMode) {
            when (symbol) {
                "BACKSPACE" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
                "CLEAR" -> expression = ""
                "ENTER" -> {
                    val toSend = evaluatedResult ?: expression
                    if (toSend.isNotBlank()) {
                        val formatted = if (useCommaDecimal) toSend.replace('.', ',') else toSend
                        viewModel.hidManager.sendText(formatted, settings.keyboardLayoutId, appendEnter = false)
                        viewModel.showBanner("Resultado enviado: $formatted")
                        expression = ""
                    }
                }
                else -> {
                    val normalized = if (symbol == ",") "." else symbol
                    expression += normalized
                }
            }
        } else {
            when (symbol) {
                "," -> viewModel.hidManager.sendText(",", settings.keyboardLayoutId)
                "." -> viewModel.hidManager.sendText(".", settings.keyboardLayoutId)
                else -> viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, hidCode, symbol)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Mode Switcher: Direct Numpad vs Calculator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !calculatorMode,
                    onClick = { calculatorMode = false },
                    leadingIcon = { Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Direto (Planilhas)") },
                    modifier = Modifier.testTag("numpad_mode_direct"),
                )
                FilterChip(
                    selected = calculatorMode,
                    onClick = { calculatorMode = true },
                    leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Calculadora") },
                    modifier = Modifier.testTag("numpad_mode_calc"),
                )
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = useCommaDecimal,
                    onClick = { useCommaDecimal = !useCommaDecimal },
                    label = { Text(if (useCommaDecimal) "Dec: ," else "Dec: .") },
                )
            }

            // Calculator Display Strip
            if (calculatorMode) {
                RemoteSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    highlighted = evaluatedResult != null,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expression.ifEmpty { "0" },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "= ${evaluatedResult?.let { if (useCommaDecimal) it.replace('.', ',') else it } ?: "—"}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val value = evaluatedResult ?: expression
                                    if (value.isNotBlank()) {
                                        viewModel.performHaptic(true)
                                        val formatted = if (useCommaDecimal) value.replace('.', ',') else value
                                        viewModel.hidManager.sendText(formatted, settings.keyboardLayoutId)
                                        viewModel.showBanner("Enviado ao PC: $formatted")
                                        expression = ""
                                    }
                                }
                                .testTag("numpad_send_calc_result"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar resultado",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "Enviar",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // Numpad Grid (5 rows x 4 columns)
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Row 1: NumLock/C, /, *, Backspace
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadKey(
                        label = if (calculatorMode) "C" else "Num",
                        accent = true,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    ) {
                        onPadInput(if (calculatorMode) "CLEAR" else "NumLock", HidConstants.KEY_NUM_LOCK)
                    }
                    NumpadKey("/", accent = true, modifier = Modifier.weight(1f).fillMaxSize()) {
                        onPadInput("/", HidConstants.KEY_KP_SLASH)
                    }
                    NumpadKey("*", accent = true, modifier = Modifier.weight(1f).fillMaxSize()) {
                        onPadInput("*", HidConstants.KEY_KP_ASTERISK)
                    }
                    NumpadIconKey(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        desc = "Apagar",
                        accent = true,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    ) {
                        onPadInput("BACKSPACE", HidConstants.KEY_BACKSPACE)
                    }
                }

                // Row 2: 7 8 9 -
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadKey("7", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("7", HidConstants.KEY_KP_7) }
                    NumpadKey("8", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("8", HidConstants.KEY_KP_8) }
                    NumpadKey("9", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("9", HidConstants.KEY_KP_9) }
                    NumpadKey("-", accent = true, modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("-", HidConstants.KEY_KP_MINUS) }
                }

                // Row 3: 4 5 6 +
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadKey("4", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("4", HidConstants.KEY_KP_4) }
                    NumpadKey("5", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("5", HidConstants.KEY_KP_5) }
                    NumpadKey("6", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("6", HidConstants.KEY_KP_6) }
                    NumpadKey("+", accent = true, modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("+", HidConstants.KEY_KP_PLUS) }
                }

                // Row 4: 1 2 3 Tab
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadKey("1", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("1", HidConstants.KEY_KP_1) }
                    NumpadKey("2", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("2", HidConstants.KEY_KP_2) }
                    NumpadKey("3", modifier = Modifier.weight(1f).fillMaxSize()) { onPadInput("3", HidConstants.KEY_KP_3) }
                    NumpadKey("Tab", accent = true, modifier = Modifier.weight(1f).fillMaxSize()) {
                        viewModel.performHaptic(false)
                        viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_TAB, "Tab")
                    }
                }

                // Row 5: 0 (wide), , / ., Enter
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadKey("0", modifier = Modifier.weight(1.5f).fillMaxSize()) { onPadInput("0", HidConstants.KEY_KP_0) }
                    NumpadKey(",", modifier = Modifier.weight(0.75f).fillMaxSize()) { onPadInput(",", HidConstants.KEY_COMMA) }
                    NumpadKey(".", modifier = Modifier.weight(0.75f).fillMaxSize()) { onPadInput(".", HidConstants.KEY_KP_DOT) }
                    NumpadKey(
                        label = if (calculatorMode) "=" else "Enter",
                        primaryFilled = true,
                        modifier = Modifier.weight(1f).fillMaxSize().testTag("numpad_enter"),
                    ) {
                        onPadInput("ENTER", HidConstants.KEY_KP_ENTER)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadKey(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    primaryFilled: Boolean = false,
    onClick: () -> Unit,
) {
    val tron = LocalTronStyle.current
    val bg = when {
        primaryFilled -> MaterialTheme.colorScheme.primary
        accent -> MaterialTheme.colorScheme.primaryContainer
        tron.enabled -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        primaryFilled -> MaterialTheme.colorScheme.onPrimary
        accent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        primaryFilled || accent -> MaterialTheme.colorScheme.primary
        tron.enabled -> tron.glowColor.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(if (tron.enabled || primaryFilled) 1.3.dp else 0.8.dp, borderColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = if (label.length > 2) 15.sp else 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NumpadIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    accent: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val tron = LocalTronStyle.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (tron.enabled) 1.3.dp else 0.8.dp,
            if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/**
 * Clean, deterministic math expression evaluator supporting +, -, *, / with standard precedence.
 */
internal fun evaluateSimpleExpression(raw: String): String? {
    val clean = raw.replace(" ", "").replace(",", ".")
    if (clean.isEmpty()) return null
    return runCatching {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < clean.length) {
            val ch = clean[i]
            if (ch.isDigit() || ch == '.' || (ch == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/")))) {
                val sb = StringBuilder()
                sb.append(ch)
                i++
                while (i < clean.length && (clean[i].isDigit() || clean[i] == '.')) {
                    sb.append(clean[i])
                    i++
                }
                tokens.add(sb.toString())
            } else if (ch in charArrayOf('+', '-', '*', '/')) {
                tokens.add(ch.toString())
                i++
            } else {
                return null
            }
        }
        if (tokens.isEmpty()) return null
        if (tokens.last() in listOf("+", "-", "*", "/")) tokens.removeAt(tokens.lastIndex)
        if (tokens.isEmpty()) return null

        // Pass 1: * and /
        val pass1 = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val t = tokens[idx]
            if ((t == "*" || t == "/") && pass1.isNotEmpty() && idx + 1 < tokens.size) {
                val left = pass1.removeAt(pass1.lastIndex).toDouble()
                val right = tokens[idx + 1].toDouble()
                val res = if (t == "*") left * right else {
                    if (right == 0.0) return null
                    left / right
                }
                pass1.add(res.toString())
                idx += 2
            } else {
                pass1.add(t)
                idx++
            }
        }

        // Pass 2: + and -
        var acc = pass1.first().toDouble()
        var j = 1
        while (j + 1 < pass1.size) {
            val op = pass1[j]
            val rhs = pass1[j + 1].toDouble()
            acc = if (op == "+") acc + rhs else acc - rhs
            j += 2
        }

        val bd = BigDecimal.valueOf(acc).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
        bd.toPlainString()
    }.getOrNull()
}
