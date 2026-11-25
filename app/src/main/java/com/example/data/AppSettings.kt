package com.example.data

import com.example.ui.theme.AccentPalette

enum class ControlMode(val route: String, val title: String, val subtitle: String) {
    MOUSE_KEYBOARD("mouse_keyboard", "Mouse e Teclado", "Touchpad gestual, Mouse 4/5, Sniper e corretor"),
    MULTIMEDIA("multimedia", "Mídia, TV e Web", "Controle TV completo, navegador e energia HID"),
    PC_KEYBOARD("pc_keyboard", "Teclado de PC", "Teclado físico completo, Sticky Keys e perfis SO"),
    GAMEPAD("gamepad", "Gamepad Nativo", "Controle Bluetooth HID (Xbox / PlayStation)"),
    DIGITIZER("digitizer", "Mesa Digitalizadora", "Caneta HID absoluta com pressão, tilt e hover"),
    NUMPAD("numpad", "Teclado Numérico", "Bloco numérico com calculadora integrada"),
    PRESENTER("presenter", "Apresentador Pro", "Slides, notas teleprompter e alertas vibrantes"),
    SCANNER("scanner", "Scanner QR e OCR", "Leitor óptico de QR, barras e texto de papel"),
    CUSTOM_LAYOUTS("custom_layouts", "Layouts e Macros", "Stream Deck, gravador de macros e backup"),
    VAULT("vault", "Cofre Biométrico", "Login automático criptografado com digital/rosto"),
    COMPANION("companion", "PC Wi-Fi / WoL", "Wake-on-LAN, Lupa, Clipboard, Arquivos e Web Editor"),
    DEVICES("devices", "Dispositivos Bluetooth", "Easy-Switch, conexão MAC e diagnóstico HID"),
    SETTINGS("settings", "Configurações", "Controles, mouse, teclado, exibição e OLED"),
}

enum class TargetOsProfile(val id: String, val label: String, val shortTag: String) {
    WINDOWS("windows", "Windows", "WIN"),
    MACOS("macos", "macOS", "MAC"),
    IOS("ios", "iPhone / iPad", "iOS"),
    LINUX("linux", "Linux", "LNX"),
    ANDROID("android", "Android / TV", "AND");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: WINDOWS
    }
}

enum class HidProfileMode(val id: String, val label: String, val shortLabel: String, val description: String) {
    UNIVERSAL(
        id = "universal",
        label = "Padrão Universal (Apple / TV / PC)",
        shortLabel = "Universal",
        description = "Teclado + Mouse 5 botões + Mídia. Máxima compatibilidade com iPhone, iPad, Mac, Smart TVs e ROMs rigorosas.",
    ),
    PRO_COMPLETE(
        id = "pro_complete",
        label = "Completo Pro (6-em-1)",
        shortLabel = "Pro 6-em-1",
        description = "Todos os perfis juntos: Teclado, Mouse, Mídia, Gamepad, Mesa Digitalizadora e Energia HID (Windows/Linux).",
    ),
    GAMEPAD_ONLY(
        id = "gamepad_only",
        label = "Gamepad Dedicado",
        shortLabel = "Gamepad",
        description = "Controle de videogame Bluetooth HID limpo para jogos no PC, Steam Deck, macOS e Android.",
    ),
    DIGITIZER_ONLY(
        id = "digitizer_only",
        label = "Mesa Digitalizadora Dedicada",
        shortLabel = "Caneta",
        description = "Caneta absoluta HID com pressão, tilt e hover para desenho no Windows, Linux e macOS.",
    );

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: UNIVERSAL
    }
}

enum class PointerAccelCurve(val id: String, val label: String) {
    LINEAR("linear", "Linear"),
    ACCELERATED("accelerated", "Acelerada (macOS)");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: ACCELERATED
    }
}

enum class DigitizerMonitorMapping(val id: String, val label: String, val xStart: Float, val xEnd: Float) {
    SINGLE("single", "Monitor Único", 0f, 1f),
    DUAL_1("dual_1", "Monitor 1 de 2", 0f, 0.5f),
    DUAL_2("dual_2", "Monitor 2 de 2", 0.5f, 1f),
    TRIPLE_1("triple_1", "Monitor 1 de 3", 0f, 0.333f),
    TRIPLE_2("triple_2", "Monitor 2 de 3", 0.333f, 0.666f),
    TRIPLE_3("triple_3", "Monitor 3 de 3", 0.666f, 1f);

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: SINGLE
    }
}

enum class MouseButtonsPosition(val id: String, val label: String) {
    BOTTOM("bottom", "Inferior"),
    TOP("top", "Superior");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: BOTTOM
    }
}

enum class ScrollbarPosition(val id: String, val label: String) {
    RIGHT("right", "Direita"),
    LEFT("left", "Esquerda"),
    BOTTOM("bottom", "Inferior");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: RIGHT
    }
}

enum class TextInputBarMode(val id: String, val label: String) {
    ALWAYS("always", "Sempre visível"),
    KEYBOARD_ONLY("keyboard_only", "Com teclado aberto"),
    HIDDEN("hidden", "Ocultar barra");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: ALWAYS
    }
}

enum class VolumeButtonAction(val id: String, val label: String) {
    REMOTE_VOLUME("remote_volume", "Volume do PC / TV"),
    SCROLL_PAGE("scroll_page", "Rolar página"),
    SLIDE_NAV("slide_nav", "Avançar / Voltar slide"),
    PHONE_DEFAULT("phone_default", "Padrão do celular");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: REMOTE_VOLUME
    }
}

enum class ThemeMode(val id: String, val label: String) {
    DARK("dark", "Escuro"),
    LIGHT("light", "Claro"),
    SYSTEM("system", "Sistema");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: DARK
    }
}

enum class ScreenOrientationMode(val id: String, val label: String) {
    AUTO("auto", "Automática"),
    PORTRAIT("portrait", "Vertical"),
    LANDSCAPE("landscape", "Horizontal");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: AUTO
    }
}

data class EasySwitchSlot(
    val slotNumber: Int,
    val name: String = "",
    val address: String = "",
)

data class AppSettings(
    // Onboarding & Segurança
    val onboardingCompleted: Boolean = false,
    val requireAutomationConfirmation: Boolean = true,
    val hidProfileMode: HidProfileMode = HidProfileMode.UNIVERSAL,

    // Aba Controles
    val showMediaControlsOnMain: Boolean = false,
    val showQuickShortcuts: Boolean = true,
    val showAndroidNavButtons: Boolean = true,
    val enableGestureCommands: Boolean = true,
    val showSideMouseButtons: Boolean = true,
    val leftHandedMode: Boolean = false,
    val oneHandedMode: Boolean = false,
    val targetOsProfile: TargetOsProfile = TargetOsProfile.WINDOWS,

    // Aba Mouse
    val airMouseEnabled: Boolean = false,
    val airMouseSensitivity: Float = 1.2f,
    val airMouseTriggerMode: Boolean = false,
    val touchpadTapToClick: Boolean = true,
    val mouseButtonsPosition: MouseButtonsPosition = MouseButtonsPosition.BOTTOM,
    val showLeftMouseButton: Boolean = true,
    val showMiddleMouseButton: Boolean = true,
    val showRightMouseButton: Boolean = true,
    val pointerSpeed: Float = 1.4f,
    val pointerAccelCurve: PointerAccelCurve = PointerAccelCurve.ACCELERATED,
    val scrollbarPosition: ScrollbarPosition = ScrollbarPosition.RIGHT,
    val scrollSpeed: Float = 1.2f,
    val invertScrollDirection: Boolean = false,
    val inertiaScrolling: Boolean = true,
    val mouseJigglerEnabled: Boolean = false,
    val mouseJigglerIntervalSec: Int = 30,
    val mouseJigglerHumanMode: Boolean = true,
    val autoClickerEnabled: Boolean = false,
    val autoClickerCps: Int = 5,
    val autoClickerRightButton: Boolean = false,
    val penDrawingMode: Boolean = false,

    // Gamepad & Digitizer
    val gamepadDeadzone: Float = 0.12f,
    val gamepadPlayStationLabels: Boolean = false,
    val digitizerMonitorMapping: DigitizerMonitorMapping = DigitizerMonitorMapping.SINGLE,

    // Apresentador Pro
    val presenterNotes: String = "Tópico 1: Abertura e objetivo\nTópico 2: Demonstração ao vivo\nTópico 3: Próximos passos e perguntas",
    val presenterTargetMinutes: Int = 15,
    val presenterVibrationAlerts: Boolean = true,

    // Aba Teclado
    val keyboardLayoutId: String = "pt_BR_ABNT2",
    val directTypingMode: Boolean = true,
    val passwordMode: Boolean = false,
    val autoCorrectEnabled: Boolean = true,
    val autoCorrectOnSpace: Boolean = true,
    val textInputBarMode: TextInputBarMode = TextInputBarMode.ALWAYS,
    val volumeButtonAction: VolumeButtonAction = VolumeButtonAction.REMOTE_VOLUME,
    val scannerAutoEnter: Boolean = true,

    // Aba Exibição
    val accentPalette: AccentPalette = AccentPalette.CYAN,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val tronStyleEnabled: Boolean = false,
    val oledSaverEnabled: Boolean = false,
    val oledDimAfterSeconds: Int = 25,
    val startFullscreen: Boolean = false,
    val screenOrientation: ScreenOrientationMode = ScreenOrientationMode.AUTO,
    val keepScreenOn: Boolean = true,
    val showOverLockScreen: Boolean = false,
    val hapticFeedback: Boolean = true,

    // Conectividade, Easy-Switch (1|2|3) & Reconexão
    val autoReconnectEnabled: Boolean = true,
    val lastDeviceAddress: String = "",
    val lastDeviceName: String = "",
    val easySwitch1Name: String = "",
    val easySwitch1Addr: String = "",
    val easySwitch2Name: String = "",
    val easySwitch2Addr: String = "",
    val easySwitch3Name: String = "",
    val easySwitch3Addr: String = "",

    // Parte 2: Companheiro Wi-Fi / USB & Wake-on-LAN
    val companionHost: String = "192.168.1.100",
    val companionPort: Int = 8766,
    val companionAuthToken: String = "",
    val companionAutoLayoutSwitch: Boolean = true,
    val wolMacAddress: String = "",
    val wolBroadcastIp: String = "255.255.255.255",
) {
    fun easySwitchSlots(): List<EasySwitchSlot> = listOf(
        EasySwitchSlot(1, easySwitch1Name, easySwitch1Addr),
        EasySwitchSlot(2, easySwitch2Name, easySwitch2Addr),
        EasySwitchSlot(3, easySwitch3Name, easySwitch3Addr),
    )
}
