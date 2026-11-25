package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AccentPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.SecureRandom

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "controle_bt_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val REQUIRE_AUTOMATION_CONFIRM = booleanPreferencesKey("require_automation_confirm")
        val HID_PROFILE_MODE = stringPreferencesKey("hid_profile_mode")

        val SHOW_MEDIA_CONTROLS = booleanPreferencesKey("show_media_controls")
        val SHOW_SHORTCUTS = booleanPreferencesKey("show_shortcuts")
        val SHOW_ANDROID_NAV = booleanPreferencesKey("show_android_nav")
        val ENABLE_GESTURES = booleanPreferencesKey("enable_gestures")
        val SHOW_SIDE_MOUSE_BTNS = booleanPreferencesKey("show_side_mouse_btns")
        val LEFT_HANDED = booleanPreferencesKey("left_handed")
        val ONE_HANDED = booleanPreferencesKey("one_handed")
        val TARGET_OS = stringPreferencesKey("target_os")

        val AIR_MOUSE_ENABLED = booleanPreferencesKey("air_mouse_enabled")
        val AIR_MOUSE_SENS = floatPreferencesKey("air_mouse_sens")
        val AIR_MOUSE_TRIGGER = booleanPreferencesKey("air_mouse_trigger")
        val TAP_TO_CLICK = booleanPreferencesKey("tap_to_click")
        val MOUSE_BTN_POS = stringPreferencesKey("mouse_btn_pos")
        val SHOW_LEFT_BTN = booleanPreferencesKey("show_left_btn")
        val SHOW_MID_BTN = booleanPreferencesKey("show_mid_btn")
        val SHOW_RIGHT_BTN = booleanPreferencesKey("show_right_btn")
        val POINTER_SPEED = floatPreferencesKey("pointer_speed")
        val POINTER_ACCEL = stringPreferencesKey("pointer_accel")
        val SCROLLBAR_POS = stringPreferencesKey("scrollbar_pos")
        val SCROLL_SPEED = floatPreferencesKey("scroll_speed")
        val INVERT_SCROLL = booleanPreferencesKey("invert_scroll")
        val INERTIA_SCROLL = booleanPreferencesKey("inertia_scroll")
        val MOUSE_JIGGLER = booleanPreferencesKey("mouse_jiggler")
        val MOUSE_JIGGLER_INTERVAL = intPreferencesKey("mouse_jiggler_interval")
        val MOUSE_JIGGLER_HUMAN = booleanPreferencesKey("mouse_jiggler_human")
        val AUTOCLICKER_ENABLED = booleanPreferencesKey("autoclicker_enabled")
        val AUTOCLICKER_CPS = intPreferencesKey("autoclicker_cps")
        val AUTOCLICKER_RIGHT = booleanPreferencesKey("autoclicker_right")
        val PEN_DRAWING = booleanPreferencesKey("pen_drawing")

        val GAMEPAD_DEADZONE = floatPreferencesKey("gamepad_deadzone")
        val GAMEPAD_PS_LABELS = booleanPreferencesKey("gamepad_ps_labels")
        val DIGITIZER_MONITOR = stringPreferencesKey("digitizer_monitor")

        val PRESENTER_NOTES = stringPreferencesKey("presenter_notes")
        val PRESENTER_TARGET_MIN = intPreferencesKey("presenter_target_min")
        val PRESENTER_VIB_ALERTS = booleanPreferencesKey("presenter_vib_alerts")

        val KEYBOARD_LAYOUT = stringPreferencesKey("keyboard_layout")
        val DIRECT_TYPING = booleanPreferencesKey("direct_typing")
        val PASSWORD_MODE = booleanPreferencesKey("password_mode")
        val AUTOCORRECT = booleanPreferencesKey("autocorrect")
        val AUTOCORRECT_ON_SPACE = booleanPreferencesKey("autocorrect_on_space")
        val TEXT_BAR_MODE = stringPreferencesKey("text_bar_mode")
        val VOLUME_ACTION = stringPreferencesKey("volume_action")
        val SCANNER_AUTO_ENTER = booleanPreferencesKey("scanner_auto_enter")

        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TRON_STYLE = booleanPreferencesKey("tron_style")
        val OLED_SAVER = booleanPreferencesKey("oled_saver")
        val OLED_DIM_SEC = intPreferencesKey("oled_dim_sec")
        val START_FULLSCREEN = booleanPreferencesKey("start_fullscreen")
        val ORIENTATION = stringPreferencesKey("orientation")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_OVER_LOCK = booleanPreferencesKey("show_over_lock")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")

        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val LAST_DEVICE_ADDR = stringPreferencesKey("last_device_addr")
        val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
        val ES1_NAME = stringPreferencesKey("es1_name")
        val ES1_ADDR = stringPreferencesKey("es1_addr")
        val ES2_NAME = stringPreferencesKey("es2_name")
        val ES2_ADDR = stringPreferencesKey("es2_addr")
        val ES3_NAME = stringPreferencesKey("es3_name")
        val ES3_ADDR = stringPreferencesKey("es3_addr")

        val COMP_HOST = stringPreferencesKey("comp_host")
        val COMP_PORT = intPreferencesKey("comp_port")
        val COMP_TOKEN = stringPreferencesKey("comp_token")
        val COMP_AUTO_LAYOUT = booleanPreferencesKey("comp_auto_layout")
        val WOL_MAC = stringPreferencesKey("wol_mac")
        val WOL_BCAST = stringPreferencesKey("wol_bcast")
    }

    private fun readFromPreferences(p: Preferences): AppSettings = AppSettings(
        onboardingCompleted = p[Keys.ONBOARDING_DONE] ?: false,
        requireAutomationConfirmation = p[Keys.REQUIRE_AUTOMATION_CONFIRM] ?: true,
        hidProfileMode = HidProfileMode.fromId(p[Keys.HID_PROFILE_MODE] ?: HidProfileMode.UNIVERSAL.id),

        showMediaControlsOnMain = p[Keys.SHOW_MEDIA_CONTROLS] ?: false,
        showQuickShortcuts = p[Keys.SHOW_SHORTCUTS] ?: true,
        showAndroidNavButtons = p[Keys.SHOW_ANDROID_NAV] ?: true,
        enableGestureCommands = p[Keys.ENABLE_GESTURES] ?: true,
        showSideMouseButtons = p[Keys.SHOW_SIDE_MOUSE_BTNS] ?: true,
        leftHandedMode = p[Keys.LEFT_HANDED] ?: false,
        oneHandedMode = p[Keys.ONE_HANDED] ?: false,
        targetOsProfile = TargetOsProfile.fromId(p[Keys.TARGET_OS] ?: "windows"),

        airMouseEnabled = p[Keys.AIR_MOUSE_ENABLED] ?: false,
        airMouseSensitivity = p[Keys.AIR_MOUSE_SENS] ?: 1.2f,
        airMouseTriggerMode = p[Keys.AIR_MOUSE_TRIGGER] ?: false,
        touchpadTapToClick = p[Keys.TAP_TO_CLICK] ?: true,
        mouseButtonsPosition = MouseButtonsPosition.fromId(p[Keys.MOUSE_BTN_POS] ?: "bottom"),
        showLeftMouseButton = p[Keys.SHOW_LEFT_BTN] ?: true,
        showMiddleMouseButton = p[Keys.SHOW_MID_BTN] ?: true,
        showRightMouseButton = p[Keys.SHOW_RIGHT_BTN] ?: true,
        pointerSpeed = p[Keys.POINTER_SPEED] ?: 1.4f,
        pointerAccelCurve = PointerAccelCurve.fromId(p[Keys.POINTER_ACCEL] ?: "accelerated"),
        scrollbarPosition = ScrollbarPosition.fromId(p[Keys.SCROLLBAR_POS] ?: "right"),
        scrollSpeed = p[Keys.SCROLL_SPEED] ?: 1.2f,
        invertScrollDirection = p[Keys.INVERT_SCROLL] ?: false,
        inertiaScrolling = p[Keys.INERTIA_SCROLL] ?: true,
        mouseJigglerEnabled = p[Keys.MOUSE_JIGGLER] ?: false,
        mouseJigglerIntervalSec = p[Keys.MOUSE_JIGGLER_INTERVAL] ?: 30,
        mouseJigglerHumanMode = p[Keys.MOUSE_JIGGLER_HUMAN] ?: true,
        autoClickerEnabled = p[Keys.AUTOCLICKER_ENABLED] ?: false,
        autoClickerCps = p[Keys.AUTOCLICKER_CPS] ?: 5,
        autoClickerRightButton = p[Keys.AUTOCLICKER_RIGHT] ?: false,
        penDrawingMode = p[Keys.PEN_DRAWING] ?: false,

        gamepadDeadzone = p[Keys.GAMEPAD_DEADZONE] ?: 0.12f,
        gamepadPlayStationLabels = p[Keys.GAMEPAD_PS_LABELS] ?: false,
        digitizerMonitorMapping = DigitizerMonitorMapping.fromId(p[Keys.DIGITIZER_MONITOR] ?: "single"),

        presenterNotes = p[Keys.PRESENTER_NOTES]
            ?: "Tópico 1: Abertura e objetivo\nTópico 2: Demonstração ao vivo\nTópico 3: Próximos passos e perguntas",
        presenterTargetMinutes = p[Keys.PRESENTER_TARGET_MIN] ?: 15,
        presenterVibrationAlerts = p[Keys.PRESENTER_VIB_ALERTS] ?: true,

        keyboardLayoutId = p[Keys.KEYBOARD_LAYOUT] ?: "pt_BR_ABNT2",
        directTypingMode = p[Keys.DIRECT_TYPING] ?: true,
        passwordMode = p[Keys.PASSWORD_MODE] ?: false,
        autoCorrectEnabled = p[Keys.AUTOCORRECT] ?: true,
        autoCorrectOnSpace = p[Keys.AUTOCORRECT_ON_SPACE] ?: true,
        textInputBarMode = TextInputBarMode.fromId(p[Keys.TEXT_BAR_MODE] ?: "always"),
        volumeButtonAction = VolumeButtonAction.fromId(p[Keys.VOLUME_ACTION] ?: "remote_volume"),
        scannerAutoEnter = p[Keys.SCANNER_AUTO_ENTER] ?: true,

        accentPalette = AccentPalette.fromId(p[Keys.ACCENT_PALETTE] ?: "cyan"),
        themeMode = ThemeMode.fromId(p[Keys.THEME_MODE] ?: "dark"),
        tronStyleEnabled = p[Keys.TRON_STYLE] ?: false,
        oledSaverEnabled = p[Keys.OLED_SAVER] ?: false,
        oledDimAfterSeconds = p[Keys.OLED_DIM_SEC] ?: 25,
        startFullscreen = p[Keys.START_FULLSCREEN] ?: false,
        screenOrientation = ScreenOrientationMode.fromId(p[Keys.ORIENTATION] ?: "auto"),
        keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: true,
        showOverLockScreen = p[Keys.SHOW_OVER_LOCK] ?: false,
        hapticFeedback = p[Keys.HAPTIC_FEEDBACK] ?: true,

        autoReconnectEnabled = p[Keys.AUTO_RECONNECT] ?: true,
        lastDeviceAddress = p[Keys.LAST_DEVICE_ADDR] ?: "",
        lastDeviceName = p[Keys.LAST_DEVICE_NAME] ?: "",
        easySwitch1Name = p[Keys.ES1_NAME] ?: "",
        easySwitch1Addr = p[Keys.ES1_ADDR] ?: "",
        easySwitch2Name = p[Keys.ES2_NAME] ?: "",
        easySwitch2Addr = p[Keys.ES2_ADDR] ?: "",
        easySwitch3Name = p[Keys.ES3_NAME] ?: "",
        easySwitch3Addr = p[Keys.ES3_ADDR] ?: "",

        companionHost = p[Keys.COMP_HOST] ?: "192.168.1.100",
        companionPort = p[Keys.COMP_PORT] ?: 8766,
        companionAuthToken = p[Keys.COMP_TOKEN] ?: "",
        companionAutoLayoutSwitch = p[Keys.COMP_AUTO_LAYOUT] ?: true,
        wolMacAddress = p[Keys.WOL_MAC] ?: "",
        wolBroadcastIp = p[Keys.WOL_BCAST] ?: "255.255.255.255",
    )

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map(::readFromPreferences)

    suspend fun ensureCompanionToken(): String {
        var token = ""
        context.settingsDataStore.edit { p ->
            val existing = p[Keys.COMP_TOKEN].orEmpty()
            token = existing.ifBlank { generateSixDigitPin() }
            p[Keys.COMP_TOKEN] = token
        }
        return token
    }

    suspend fun resetToDefaults() {
        val freshPin = generateSixDigitPin()
        update { current ->
            AppSettings(
                onboardingCompleted = true,
                lastDeviceAddress = current.lastDeviceAddress,
                lastDeviceName = current.lastDeviceName,
                companionAuthToken = freshPin,
            )
        }
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val next = transform(readFromPreferences(p))
            p[Keys.ONBOARDING_DONE] = next.onboardingCompleted
            p[Keys.REQUIRE_AUTOMATION_CONFIRM] = next.requireAutomationConfirmation
            p[Keys.HID_PROFILE_MODE] = next.hidProfileMode.id

            p[Keys.SHOW_MEDIA_CONTROLS] = next.showMediaControlsOnMain
            p[Keys.SHOW_SHORTCUTS] = next.showQuickShortcuts
            p[Keys.SHOW_ANDROID_NAV] = next.showAndroidNavButtons
            p[Keys.ENABLE_GESTURES] = next.enableGestureCommands
            p[Keys.SHOW_SIDE_MOUSE_BTNS] = next.showSideMouseButtons
            p[Keys.LEFT_HANDED] = next.leftHandedMode
            p[Keys.ONE_HANDED] = next.oneHandedMode
            p[Keys.TARGET_OS] = next.targetOsProfile.id

            p[Keys.AIR_MOUSE_ENABLED] = next.airMouseEnabled
            p[Keys.AIR_MOUSE_SENS] = next.airMouseSensitivity
            p[Keys.AIR_MOUSE_TRIGGER] = next.airMouseTriggerMode
            p[Keys.TAP_TO_CLICK] = next.touchpadTapToClick
            p[Keys.MOUSE_BTN_POS] = next.mouseButtonsPosition.id
            p[Keys.SHOW_LEFT_BTN] = next.showLeftMouseButton
            p[Keys.SHOW_MID_BTN] = next.showMiddleMouseButton
            p[Keys.SHOW_RIGHT_BTN] = next.showRightMouseButton
            p[Keys.POINTER_SPEED] = next.pointerSpeed
            p[Keys.POINTER_ACCEL] = next.pointerAccelCurve.id
            p[Keys.SCROLLBAR_POS] = next.scrollbarPosition.id
            p[Keys.SCROLL_SPEED] = next.scrollSpeed
            p[Keys.INVERT_SCROLL] = next.invertScrollDirection
            p[Keys.INERTIA_SCROLL] = next.inertiaScrolling
            p[Keys.MOUSE_JIGGLER] = next.mouseJigglerEnabled
            p[Keys.MOUSE_JIGGLER_INTERVAL] = next.mouseJigglerIntervalSec
            p[Keys.MOUSE_JIGGLER_HUMAN] = next.mouseJigglerHumanMode
            p[Keys.AUTOCLICKER_ENABLED] = next.autoClickerEnabled
            p[Keys.AUTOCLICKER_CPS] = next.autoClickerCps
            p[Keys.AUTOCLICKER_RIGHT] = next.autoClickerRightButton
            p[Keys.PEN_DRAWING] = next.penDrawingMode

            p[Keys.GAMEPAD_DEADZONE] = next.gamepadDeadzone
            p[Keys.GAMEPAD_PS_LABELS] = next.gamepadPlayStationLabels
            p[Keys.DIGITIZER_MONITOR] = next.digitizerMonitorMapping.id

            p[Keys.PRESENTER_NOTES] = next.presenterNotes
            p[Keys.PRESENTER_TARGET_MIN] = next.presenterTargetMinutes
            p[Keys.PRESENTER_VIB_ALERTS] = next.presenterVibrationAlerts

            p[Keys.KEYBOARD_LAYOUT] = next.keyboardLayoutId
            p[Keys.DIRECT_TYPING] = next.directTypingMode
            p[Keys.PASSWORD_MODE] = next.passwordMode
            p[Keys.AUTOCORRECT] = next.autoCorrectEnabled
            p[Keys.AUTOCORRECT_ON_SPACE] = next.autoCorrectOnSpace
            p[Keys.TEXT_BAR_MODE] = next.textInputBarMode.id
            p[Keys.VOLUME_ACTION] = next.volumeButtonAction.id
            p[Keys.SCANNER_AUTO_ENTER] = next.scannerAutoEnter

            p[Keys.ACCENT_PALETTE] = next.accentPalette.id
            p[Keys.THEME_MODE] = next.themeMode.id
            p[Keys.TRON_STYLE] = next.tronStyleEnabled
            p[Keys.OLED_SAVER] = next.oledSaverEnabled
            p[Keys.OLED_DIM_SEC] = next.oledDimAfterSeconds
            p[Keys.START_FULLSCREEN] = next.startFullscreen
            p[Keys.ORIENTATION] = next.screenOrientation.id
            p[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            p[Keys.SHOW_OVER_LOCK] = next.showOverLockScreen
            p[Keys.HAPTIC_FEEDBACK] = next.hapticFeedback

            p[Keys.AUTO_RECONNECT] = next.autoReconnectEnabled
            p[Keys.LAST_DEVICE_ADDR] = next.lastDeviceAddress
            p[Keys.LAST_DEVICE_NAME] = next.lastDeviceName
            p[Keys.ES1_NAME] = next.easySwitch1Name
            p[Keys.ES1_ADDR] = next.easySwitch1Addr
            p[Keys.ES2_NAME] = next.easySwitch2Name
            p[Keys.ES2_ADDR] = next.easySwitch2Addr
            p[Keys.ES3_NAME] = next.easySwitch3Name
            p[Keys.ES3_ADDR] = next.easySwitch3Addr

            p[Keys.COMP_HOST] = next.companionHost
            p[Keys.COMP_PORT] = next.companionPort
            p[Keys.COMP_TOKEN] = next.companionAuthToken
            p[Keys.COMP_AUTO_LAYOUT] = next.companionAutoLayoutSwitch
            p[Keys.WOL_MAC] = next.wolMacAddress
            p[Keys.WOL_BCAST] = next.wolBroadcastIp
        }
    }

    companion object {
        fun generateSixDigitPin(): String {
            val num = SecureRandom().nextInt(900_000) + 100_000
            return num.toString()
        }
    }
}
