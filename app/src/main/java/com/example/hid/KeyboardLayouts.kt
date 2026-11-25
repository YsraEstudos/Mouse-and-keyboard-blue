package com.example.hid

import com.example.hid.HidConstants.KEY_0
import com.example.hid.HidConstants.KEY_1
import com.example.hid.HidConstants.KEY_2
import com.example.hid.HidConstants.KEY_3
import com.example.hid.HidConstants.KEY_4
import com.example.hid.HidConstants.KEY_5
import com.example.hid.HidConstants.KEY_6
import com.example.hid.HidConstants.KEY_7
import com.example.hid.HidConstants.KEY_8
import com.example.hid.HidConstants.KEY_9
import com.example.hid.HidConstants.KEY_A
import com.example.hid.HidConstants.KEY_APOSTROPHE
import com.example.hid.HidConstants.KEY_B
import com.example.hid.HidConstants.KEY_BACKSLASH
import com.example.hid.HidConstants.KEY_C
import com.example.hid.HidConstants.KEY_COMMA
import com.example.hid.HidConstants.KEY_D
import com.example.hid.HidConstants.KEY_DOT
import com.example.hid.HidConstants.KEY_E
import com.example.hid.HidConstants.KEY_ENTER
import com.example.hid.HidConstants.KEY_EQUAL
import com.example.hid.HidConstants.KEY_F
import com.example.hid.HidConstants.KEY_G
import com.example.hid.HidConstants.KEY_GRAVE
import com.example.hid.HidConstants.KEY_H
import com.example.hid.HidConstants.KEY_I
import com.example.hid.HidConstants.KEY_INTL_RO
import com.example.hid.HidConstants.KEY_J
import com.example.hid.HidConstants.KEY_K
import com.example.hid.HidConstants.KEY_L
import com.example.hid.HidConstants.KEY_LEFT_BRACE
import com.example.hid.HidConstants.KEY_M
import com.example.hid.HidConstants.KEY_MINUS
import com.example.hid.HidConstants.KEY_N
import com.example.hid.HidConstants.KEY_NON_US_BACKSLASH
import com.example.hid.HidConstants.KEY_NON_US_HASH
import com.example.hid.HidConstants.KEY_O
import com.example.hid.HidConstants.KEY_P
import com.example.hid.HidConstants.KEY_Q
import com.example.hid.HidConstants.KEY_R
import com.example.hid.HidConstants.KEY_RIGHT_BRACE
import com.example.hid.HidConstants.KEY_S
import com.example.hid.HidConstants.KEY_SEMICOLON
import com.example.hid.HidConstants.KEY_SLASH
import com.example.hid.HidConstants.KEY_SPACE
import com.example.hid.HidConstants.KEY_T
import com.example.hid.HidConstants.KEY_TAB
import com.example.hid.HidConstants.KEY_U
import com.example.hid.HidConstants.KEY_V
import com.example.hid.HidConstants.KEY_W
import com.example.hid.HidConstants.KEY_X
import com.example.hid.HidConstants.KEY_Y
import com.example.hid.HidConstants.KEY_Z
import com.example.hid.HidConstants.MOD_LSHIFT
import com.example.hid.HidConstants.MOD_NONE
import com.example.hid.HidConstants.MOD_RALT

data class HidKeyStroke(
    val modifier: Int,
    val keyCode: Int,
)

data class KeyboardLanguageLayout(
    val id: String,
    val displayName: String,
    val regionTag: String,
    val description: String,
)

/**
 * Maps Unicode characters (including Portuguese ABNT2 accents, ç, @, symbols, and international layouts)
 * to sequences of USB HID keystrokes (supporting dead keys for accented vowels).
 */
object KeyboardLayouts {

    val allLayouts: List<KeyboardLanguageLayout> = listOf(
        KeyboardLanguageLayout("pt_BR_ABNT2", "Português (Brasil ABNT2)", "PT-BR", "PC Windows/Linux Brasil (ç, ~, ´, @)"),
        KeyboardLanguageLayout("en_US", "English (US)", "EN-US", "Padrão internacional / Mac / Linux"),
        KeyboardLanguageLayout("en_US_TV", "English (US - Android TV)", "TV-US", "Recomendado para Android TV e Google TV"),
        KeyboardLanguageLayout("pt_PT", "Português (Portugal)", "PT-PT", "Teclado europeu ISO PT"),
        KeyboardLanguageLayout("es_ES", "Español (España)", "ES-ES", "Teclado ISO com ñ e ¡¿"),
        KeyboardLanguageLayout("es_LATAM", "Español (Latinoamérica)", "ES-LA", "Layout latino-americano"),
        KeyboardLanguageLayout("fr_FR", "Français (AZERTY)", "FR-FR", "Teclado francês AZERTY"),
        KeyboardLanguageLayout("de_DE", "Deutsch (QWERTZ)", "DE-DE", "Teclado alemão QWERTZ (ä, ö, ü)"),
        KeyboardLanguageLayout("it_IT", "Italiano", "IT-IT", "Teclado italiano ISO"),
        KeyboardLanguageLayout("en_GB", "English (UK)", "EN-GB", "Teclado britânico ISO (£, @)"),
        KeyboardLanguageLayout("nl_NL", "Nederlands", "NL-NL", "Teclado holandês"),
        KeyboardLanguageLayout("ja_JP", "日本語 (JIS)", "JA-JP", "Teclado japonês JIS"),
        KeyboardLanguageLayout("ko_KR", "한국어", "KO-KR", "Teclado coreano"),
        KeyboardLanguageLayout("ru_RU", "Русский", "RU-RU", "Teclado cirílico russo"),
    )

    fun getById(id: String): KeyboardLanguageLayout =
        allLayouts.find { it.id == id } ?: allLayouts.first()

    /**
     * Parses an auto-config QR code from appground.io/keyboard (or query params) to detect layout ID.
     */
    fun detectLayoutFromQr(rawQr: String): KeyboardLanguageLayout? {
        val lower = rawQr.lowercase()
        if (!lower.contains("keyboard") && !lower.contains("layout") && !lower.contains("appground")) {
            return null
        }
        return when {
            lower.contains("pt_br") || lower.contains("abnt") || lower.contains("br") -> getById("pt_BR_ABNT2")
            lower.contains("pt_pt") -> getById("pt_PT")
            lower.contains("tv") -> getById("en_US_TV")
            lower.contains("es_la") || lower.contains("latam") -> getById("es_LATAM")
            lower.contains("es") -> getById("es_ES")
            lower.contains("fr") || lower.contains("azerty") -> getById("fr_FR")
            lower.contains("de") || lower.contains("qwertz") -> getById("de_DE")
            lower.contains("it") -> getById("it_IT")
            lower.contains("gb") || lower.contains("uk") -> getById("en_GB")
            else -> getById("en_US")
        }
    }

    /**
     * Converts a character into 1 or 2 HID keystrokes (dead key + base letter for accented chars).
     */
    fun charToStrokes(char: Char, layoutId: String): List<HidKeyStroke> {
        if (char == '\n' || char == '\r') return listOf(HidKeyStroke(MOD_NONE, KEY_ENTER))
        if (char == '\t') return listOf(HidKeyStroke(MOD_NONE, KEY_TAB))
        if (char == ' ') return listOf(HidKeyStroke(MOD_NONE, KEY_SPACE))

        // Layout-specific overrides first
        when (layoutId) {
            "pt_BR_ABNT2" -> mapPtBrAbnt2(char)?.let { return it }
            "fr_FR" -> mapAzerty(char)?.let { return it }
            "de_DE" -> mapQwertz(char)?.let { return it }
            "en_GB" -> mapEnGb(char)?.let { return it }
            "es_ES", "es_LATAM" -> mapSpanish(char)?.let { return it }
            "pt_PT" -> mapPtPt(char)?.let { return it }
        }

        // Fallback to standard US QWERTY (also used by Android TV)
        return mapUsQwerty(char)
    }

    private fun letterKey(lower: Char): Int? = when (lower) {
        'a' -> KEY_A; 'b' -> KEY_B; 'c' -> KEY_C; 'd' -> KEY_D; 'e' -> KEY_E
        'f' -> KEY_F; 'g' -> KEY_G; 'h' -> KEY_H; 'i' -> KEY_I; 'j' -> KEY_J
        'k' -> KEY_K; 'l' -> KEY_L; 'm' -> KEY_M; 'n' -> KEY_N; 'o' -> KEY_O
        'p' -> KEY_P; 'q' -> KEY_Q; 'r' -> KEY_R; 's' -> KEY_S; 't' -> KEY_T
        'u' -> KEY_U; 'v' -> KEY_V; 'w' -> KEY_W; 'x' -> KEY_X; 'y' -> KEY_Y
        'z' -> KEY_Z
        else -> null
    }

    private fun mapPtBrAbnt2(c: Char): List<HidKeyStroke>? {
        // ABNT2 dead keys:
        // Acute (´) = KEY_LEFT_BRACE (no shift), Grave (`) = Shift + KEY_LEFT_BRACE
        // Tilde (~) = KEY_APOSTROPHE (no shift), Circumflex (^) = Shift + KEY_APOSTROPHE
        // Ç = KEY_SEMICOLON
        val acute = HidKeyStroke(MOD_NONE, KEY_LEFT_BRACE)
        val grave = HidKeyStroke(MOD_LSHIFT, KEY_LEFT_BRACE)
        val tilde = HidKeyStroke(MOD_NONE, KEY_APOSTROPHE)
        val circumflex = HidKeyStroke(MOD_LSHIFT, KEY_APOSTROPHE)

        return when (c) {
            'ç' -> listOf(HidKeyStroke(MOD_NONE, KEY_SEMICOLON))
            'Ç' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_SEMICOLON))
            ';' -> listOf(HidKeyStroke(MOD_NONE, KEY_SLASH))
            ':' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_SLASH))
            '/' -> listOf(HidKeyStroke(MOD_NONE, KEY_INTL_RO))
            '?' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_INTL_RO))
            '\\' -> listOf(HidKeyStroke(MOD_NONE, KEY_NON_US_BACKSLASH))
            '|' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_NON_US_BACKSLASH))
            '\'' -> listOf(HidKeyStroke(MOD_NONE, KEY_GRAVE))
            '"' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_GRAVE))
            '[' -> listOf(HidKeyStroke(MOD_NONE, KEY_RIGHT_BRACE))
            '{' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_RIGHT_BRACE))
            ']' -> listOf(HidKeyStroke(MOD_NONE, KEY_NON_US_HASH))
            '}' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_NON_US_HASH))
            '~' -> listOf(tilde, HidKeyStroke(MOD_NONE, KEY_SPACE))
            '^' -> listOf(circumflex, HidKeyStroke(MOD_NONE, KEY_SPACE))
            '´' -> listOf(acute, HidKeyStroke(MOD_NONE, KEY_SPACE))
            '`' -> listOf(grave, HidKeyStroke(MOD_NONE, KEY_SPACE))
            '°' -> listOf(HidKeyStroke(MOD_RALT, KEY_E))
            'ª' -> listOf(HidKeyStroke(MOD_RALT, KEY_RIGHT_BRACE))
            'º' -> listOf(HidKeyStroke(MOD_RALT, KEY_NON_US_HASH))
            'á' -> listOf(acute, HidKeyStroke(MOD_NONE, KEY_A))
            'Á' -> listOf(acute, HidKeyStroke(MOD_LSHIFT, KEY_A))
            'à' -> listOf(grave, HidKeyStroke(MOD_NONE, KEY_A))
            'À' -> listOf(grave, HidKeyStroke(MOD_LSHIFT, KEY_A))
            'ã' -> listOf(tilde, HidKeyStroke(MOD_NONE, KEY_A))
            'Ã' -> listOf(tilde, HidKeyStroke(MOD_LSHIFT, KEY_A))
            'â' -> listOf(circumflex, HidKeyStroke(MOD_NONE, KEY_A))
            'Â' -> listOf(circumflex, HidKeyStroke(MOD_LSHIFT, KEY_A))
            'é' -> listOf(acute, HidKeyStroke(MOD_NONE, KEY_E))
            'É' -> listOf(acute, HidKeyStroke(MOD_LSHIFT, KEY_E))
            'ê' -> listOf(circumflex, HidKeyStroke(MOD_NONE, KEY_E))
            'Ê' -> listOf(circumflex, HidKeyStroke(MOD_LSHIFT, KEY_E))
            'í' -> listOf(acute, HidKeyStroke(MOD_NONE, KEY_I))
            'Í' -> listOf(acute, HidKeyStroke(MOD_LSHIFT, KEY_I))
            'ó' -> listOf(acute, HidKeyStroke(MOD_NONE, KEY_O))
            'Ó' -> listOf(acute, HidKeyStroke(MOD_LSHIFT, KEY_O))
            'õ' -> listOf(tilde, HidKeyStroke(MOD_NONE, KEY_O))
            'Õ' -> listOf(tilde, HidKeyStroke(MOD_LSHIFT, KEY_O))
            'ô' -> listOf(circumflex, HidKeyStroke(MOD_NONE, KEY_O))
            'Ô' -> listOf(circumflex, HidKeyStroke(MOD_LSHIFT, KEY_O))
            'ú' -> listOf(acute, HidKeyStroke(MOD_NONE, KEY_U))
            'Ú' -> listOf(acute, HidKeyStroke(MOD_LSHIFT, KEY_U))
            else -> null
        }
    }

    private fun mapPtPt(c: Char): List<HidKeyStroke>? = when (c) {
        'ç' -> listOf(HidKeyStroke(MOD_NONE, KEY_SEMICOLON))
        'Ç' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_SEMICOLON))
        '@' -> listOf(HidKeyStroke(MOD_RALT, KEY_2))
        else -> mapPtBrAbnt2(c)
    }

    private fun mapSpanish(c: Char): List<HidKeyStroke>? = when (c) {
        'ñ' -> listOf(HidKeyStroke(MOD_NONE, KEY_SEMICOLON))
        'Ñ' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_SEMICOLON))
        '@' -> listOf(HidKeyStroke(MOD_RALT, KEY_2))
        '¡' -> listOf(HidKeyStroke(MOD_NONE, KEY_EQUAL))
        '¿' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_EQUAL))
        else -> null
    }

    private fun mapAzerty(c: Char): List<HidKeyStroke>? = when (c) {
        'a' -> listOf(HidKeyStroke(MOD_NONE, KEY_Q))
        'A' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_Q))
        'q' -> listOf(HidKeyStroke(MOD_NONE, KEY_A))
        'Q' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_A))
        'z' -> listOf(HidKeyStroke(MOD_NONE, KEY_W))
        'Z' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_W))
        'w' -> listOf(HidKeyStroke(MOD_NONE, KEY_Z))
        'W' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_Z))
        'm' -> listOf(HidKeyStroke(MOD_NONE, KEY_SEMICOLON))
        'M' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_SEMICOLON))
        '@' -> listOf(HidKeyStroke(MOD_RALT, KEY_0))
        else -> null
    }

    private fun mapQwertz(c: Char): List<HidKeyStroke>? = when (c) {
        'y' -> listOf(HidKeyStroke(MOD_NONE, KEY_Z))
        'Y' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_Z))
        'z' -> listOf(HidKeyStroke(MOD_NONE, KEY_Y))
        'Z' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_Y))
        'ü' -> listOf(HidKeyStroke(MOD_NONE, KEY_LEFT_BRACE))
        'ö' -> listOf(HidKeyStroke(MOD_NONE, KEY_SEMICOLON))
        'ä' -> listOf(HidKeyStroke(MOD_NONE, KEY_APOSTROPHE))
        '@' -> listOf(HidKeyStroke(MOD_RALT, KEY_Q))
        else -> null
    }

    private fun mapEnGb(c: Char): List<HidKeyStroke>? = when (c) {
        '@' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_APOSTROPHE))
        '"' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_2))
        '£' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_3))
        '#' -> listOf(HidKeyStroke(MOD_NONE, KEY_NON_US_HASH))
        else -> null
    }

    private fun mapUsQwerty(c: Char): List<HidKeyStroke> {
        val lower = c.lowercaseChar()
        letterKey(lower)?.let { code ->
            val mod = if (c.isUpperCase()) MOD_LSHIFT else MOD_NONE
            return listOf(HidKeyStroke(mod, code))
        }
        return when (c) {
            '1' -> listOf(HidKeyStroke(MOD_NONE, KEY_1))
            '2' -> listOf(HidKeyStroke(MOD_NONE, KEY_2))
            '3' -> listOf(HidKeyStroke(MOD_NONE, KEY_3))
            '4' -> listOf(HidKeyStroke(MOD_NONE, KEY_4))
            '5' -> listOf(HidKeyStroke(MOD_NONE, KEY_5))
            '6' -> listOf(HidKeyStroke(MOD_NONE, KEY_6))
            '7' -> listOf(HidKeyStroke(MOD_NONE, KEY_7))
            '8' -> listOf(HidKeyStroke(MOD_NONE, KEY_8))
            '9' -> listOf(HidKeyStroke(MOD_NONE, KEY_9))
            '0' -> listOf(HidKeyStroke(MOD_NONE, KEY_0))
            '!' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_1))
            '@' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_2))
            '#' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_3))
            '$' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_4))
            '%' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_5))
            '^' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_6))
            '&' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_7))
            '*' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_8))
            '(' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_9))
            ')' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_0))
            '-' -> listOf(HidKeyStroke(MOD_NONE, KEY_MINUS))
            '_' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_MINUS))
            '=' -> listOf(HidKeyStroke(MOD_NONE, KEY_EQUAL))
            '+' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_EQUAL))
            '[' -> listOf(HidKeyStroke(MOD_NONE, KEY_LEFT_BRACE))
            '{' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_LEFT_BRACE))
            ']' -> listOf(HidKeyStroke(MOD_NONE, KEY_RIGHT_BRACE))
            '}' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_RIGHT_BRACE))
            '\\' -> listOf(HidKeyStroke(MOD_NONE, KEY_BACKSLASH))
            '|' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_BACKSLASH))
            ';' -> listOf(HidKeyStroke(MOD_NONE, KEY_SEMICOLON))
            ':' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_SEMICOLON))
            '\'' -> listOf(HidKeyStroke(MOD_NONE, KEY_APOSTROPHE))
            '"' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_APOSTROPHE))
            '`' -> listOf(HidKeyStroke(MOD_NONE, KEY_GRAVE))
            '~' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_GRAVE))
            ',' -> listOf(HidKeyStroke(MOD_NONE, KEY_COMMA))
            '<' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_COMMA))
            '.' -> listOf(HidKeyStroke(MOD_NONE, KEY_DOT))
            '>' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_DOT))
            '/' -> listOf(HidKeyStroke(MOD_NONE, KEY_SLASH))
            '?' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_SLASH))
            // Graceful ASCII decomposition fallback for accented characters on non-deadkey layouts
            'á', 'à', 'ã', 'â' -> listOf(HidKeyStroke(MOD_NONE, KEY_A))
            'Á', 'À', 'Ã', 'Â' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_A))
            'é', 'ê' -> listOf(HidKeyStroke(MOD_NONE, KEY_E))
            'É', 'Ê' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_E))
            'í' -> listOf(HidKeyStroke(MOD_NONE, KEY_I))
            'Í' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_I))
            'ó', 'õ', 'ô' -> listOf(HidKeyStroke(MOD_NONE, KEY_O))
            'Ó', 'Õ', 'Ô' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_O))
            'ú' -> listOf(HidKeyStroke(MOD_NONE, KEY_U))
            'Ú' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_U))
            'ç' -> listOf(HidKeyStroke(MOD_NONE, KEY_C))
            'Ç' -> listOf(HidKeyStroke(MOD_LSHIFT, KEY_C))
            else -> emptyList()
        }
    }

    /**
     * Named key resolver used by PC Keyboard & Custom Layouts / Macros.
     */
    fun keyNameToUsageId(keyName: String): Int = when (keyName.uppercase()) {
        "ENTER" -> KEY_ENTER
        "ESC" -> HidConstants.KEY_ESC
        "BACKSPACE" -> HidConstants.KEY_BACKSPACE
        "TAB" -> KEY_TAB
        "SPACE" -> KEY_SPACE
        "DELETE", "DEL" -> HidConstants.KEY_DELETE
        "INSERT", "INS" -> HidConstants.KEY_INSERT
        "HOME" -> HidConstants.KEY_HOME
        "END" -> HidConstants.KEY_END
        "PGUP", "PAGEUP" -> HidConstants.KEY_PAGE_UP
        "PGDN", "PAGEDOWN" -> HidConstants.KEY_PAGE_DOWN
        "UP" -> HidConstants.KEY_UP
        "DOWN" -> HidConstants.KEY_DOWN
        "LEFT" -> HidConstants.KEY_LEFT
        "RIGHT" -> HidConstants.KEY_RIGHT
        "PRTSC" -> HidConstants.KEY_PRINT_SCREEN
        "CAPSLOCK" -> HidConstants.KEY_CAPS_LOCK
        "NUMLOCK" -> HidConstants.KEY_NUM_LOCK
        "F1" -> HidConstants.KEY_F1
        "F2" -> HidConstants.KEY_F2
        "F3" -> HidConstants.KEY_F3
        "F4" -> HidConstants.KEY_F4
        "F5" -> HidConstants.KEY_F5
        "F6" -> HidConstants.KEY_F6
        "F7" -> HidConstants.KEY_F7
        "F8" -> HidConstants.KEY_F8
        "F9" -> HidConstants.KEY_F9
        "F10" -> HidConstants.KEY_F10
        "F11" -> HidConstants.KEY_F11
        "F12" -> HidConstants.KEY_F12
        else -> {
            if (keyName.length == 1) {
                charToStrokes(keyName[0], "en_US").firstOrNull()?.keyCode ?: HidConstants.KEY_NONE
            } else {
                HidConstants.KEY_NONE
            }
        }
    }
}
