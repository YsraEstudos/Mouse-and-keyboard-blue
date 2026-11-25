package com.example.hid

import android.bluetooth.BluetoothHidDevice
import com.example.data.HidProfileMode

/**
 * Spec-compliant USB/Bluetooth HID Report Descriptors (USB HID 1.11) with selectable profiles:
 * - Universal (Keyboard + 5-Button Mouse + 24-bit Consumer): maximum compatibility with iPhone, iPad, macOS, Smart TVs & strict OEMs.
 * - Pro Complete (6-in-1): adds Native Gamepad, Pro Digitizer Pen, and System Power Control.
 * - Dedicated Gamepad: clean HID Gamepad profile for PC/Steam Deck/macOS/Android gaming.
 * - Dedicated Digitizer: clean HID Pen profile with pressure, tilt, and hover.
 */
object HidConstants {
    const val REPORT_ID_KEYBOARD: Int = 1
    const val REPORT_ID_MOUSE: Int = 2
    const val REPORT_ID_CONSUMER: Int = 3
    const val REPORT_ID_GAMEPAD: Int = 4
    const val REPORT_ID_DIGITIZER: Int = 5
    const val REPORT_ID_SYSTEM: Int = 6

    // Mouse 5-button bitmasks
    const val MOUSE_NONE: Int = 0x00
    const val MOUSE_LEFT: Int = 0x01
    const val MOUSE_RIGHT: Int = 0x02
    const val MOUSE_MIDDLE: Int = 0x04
    const val MOUSE_BACK: Int = 0x08    // Mouse 4 (Back)
    const val MOUSE_FORWARD: Int = 0x10 // Mouse 5 (Forward)

    // Keyboard Modifier bitmasks
    const val MOD_NONE: Int = 0x00
    const val MOD_LCTRL: Int = 0x01
    const val MOD_LSHIFT: Int = 0x02
    const val MOD_LALT: Int = 0x04
    const val MOD_LGUI: Int = 0x08 // Windows / Command
    const val MOD_RCTRL: Int = 0x10
    const val MOD_RSHIFT: Int = 0x20
    const val MOD_RALT: Int = 0x40 // AltGr
    const val MOD_RGUI: Int = 0x80

    // USB HID Keyboard/Keypad Page (0x07) Usage IDs
    const val KEY_NONE = 0x00
    const val KEY_A = 0x04
    const val KEY_B = 0x05
    const val KEY_C = 0x06
    const val KEY_D = 0x07
    const val KEY_E = 0x08
    const val KEY_F = 0x09
    const val KEY_G = 0x0A
    const val KEY_H = 0x0B
    const val KEY_I = 0x0C
    const val KEY_J = 0x0D
    const val KEY_K = 0x0E
    const val KEY_L = 0x0F
    const val KEY_M = 0x10
    const val KEY_N = 0x11
    const val KEY_O = 0x12
    const val KEY_P = 0x13
    const val KEY_Q = 0x14
    const val KEY_R = 0x15
    const val KEY_S = 0x16
    const val KEY_T = 0x17
    const val KEY_U = 0x18
    const val KEY_V = 0x19
    const val KEY_W = 0x1A
    const val KEY_X = 0x1B
    const val KEY_Y = 0x1C
    const val KEY_Z = 0x1D

    const val KEY_1 = 0x1E
    const val KEY_2 = 0x1F
    const val KEY_3 = 0x20
    const val KEY_4 = 0x21
    const val KEY_5 = 0x22
    const val KEY_6 = 0x23
    const val KEY_7 = 0x24
    const val KEY_8 = 0x25
    const val KEY_9 = 0x26
    const val KEY_0 = 0x27

    const val KEY_ENTER = 0x28
    const val KEY_ESC = 0x29
    const val KEY_BACKSPACE = 0x2A
    const val KEY_TAB = 0x2B
    const val KEY_SPACE = 0x2C
    const val KEY_MINUS = 0x2D
    const val KEY_EQUAL = 0x2E
    const val KEY_LEFT_BRACE = 0x2F
    const val KEY_RIGHT_BRACE = 0x30
    const val KEY_BACKSLASH = 0x31
    const val KEY_NON_US_HASH = 0x32
    const val KEY_SEMICOLON = 0x33
    const val KEY_APOSTROPHE = 0x34
    const val KEY_GRAVE = 0x35
    const val KEY_COMMA = 0x36
    const val KEY_DOT = 0x37
    const val KEY_SLASH = 0x38
    const val KEY_CAPS_LOCK = 0x39

    const val KEY_F1 = 0x3A
    const val KEY_F2 = 0x3B
    const val KEY_F3 = 0x3C
    const val KEY_F4 = 0x3D
    const val KEY_F5 = 0x3E
    const val KEY_F6 = 0x3F
    const val KEY_F7 = 0x40
    const val KEY_F8 = 0x41
    const val KEY_F9 = 0x42
    const val KEY_F10 = 0x43
    const val KEY_F11 = 0x44
    const val KEY_F12 = 0x45

    const val KEY_PRINT_SCREEN = 0x46
    const val KEY_SCROLL_LOCK = 0x47
    const val KEY_PAUSE = 0x48
    const val KEY_INSERT = 0x49
    const val KEY_HOME = 0x4A
    const val KEY_PAGE_UP = 0x4B
    const val KEY_DELETE = 0x4C
    const val KEY_END = 0x4D
    const val KEY_PAGE_DOWN = 0x4E
    const val KEY_RIGHT = 0x4F
    const val KEY_LEFT = 0x50
    const val KEY_DOWN = 0x51
    const val KEY_UP = 0x52

    // Keypad (Numpad) Usage IDs
    const val KEY_NUM_LOCK = 0x53
    const val KEY_KP_SLASH = 0x54
    const val KEY_KP_ASTERISK = 0x55
    const val KEY_KP_MINUS = 0x56
    const val KEY_KP_PLUS = 0x57
    const val KEY_KP_ENTER = 0x58
    const val KEY_KP_1 = 0x59
    const val KEY_KP_2 = 0x5A
    const val KEY_KP_3 = 0x5B
    const val KEY_KP_4 = 0x5C
    const val KEY_KP_5 = 0x5D
    const val KEY_KP_6 = 0x5E
    const val KEY_KP_7 = 0x5F
    const val KEY_KP_8 = 0x60
    const val KEY_KP_9 = 0x61
    const val KEY_KP_0 = 0x62
    const val KEY_KP_DOT = 0x63
    const val KEY_NON_US_BACKSLASH = 0x64
    const val KEY_APP_MENU = 0x65
    const val KEY_F15 = 0x6A // Neutral key for stealth human jiggler
    const val KEY_INTL_RO = 0x87

    // Consumer Control Report ID 3 Bitmasks (3 bytes = 24 bits)
    const val CONSUMER_PLAY_PAUSE = 1 shl 0
    const val CONSUMER_NEXT_TRACK = 1 shl 1
    const val CONSUMER_PREV_TRACK = 1 shl 2
    const val CONSUMER_STOP = 1 shl 3
    const val CONSUMER_VOL_UP = 1 shl 4
    const val CONSUMER_VOL_DOWN = 1 shl 5
    const val CONSUMER_MUTE = 1 shl 6
    const val CONSUMER_HOME = 1 shl 7
    const val CONSUMER_BACK = 1 shl 8
    const val CONSUMER_MENU = 1 shl 9
    const val CONSUMER_FAST_FORWARD = 1 shl 10
    const val CONSUMER_REWIND = 1 shl 11
    const val CONSUMER_POWER = 1 shl 12
    const val CONSUMER_CH_UP = 1 shl 13
    const val CONSUMER_CH_DOWN = 1 shl 14
    const val CONSUMER_SEARCH = 1 shl 15
    const val CONSUMER_BRIGHTNESS_UP = 1 shl 16
    const val CONSUMER_BRIGHTNESS_DOWN = 1 shl 17
    const val CONSUMER_SLEEP = 1 shl 18
    const val CONSUMER_LOCK = 1 shl 19
    const val CONSUMER_TV_INPUT = 1 shl 20
    const val CONSUMER_TV_GUIDE = 1 shl 21
    const val CONSUMER_TV_RED = 1 shl 22
    const val CONSUMER_TV_GREEN = 1 shl 23

    // Gamepad Button Bitmasks (16 bits)
    const val GP_BTN_A = 1 shl 0       // Cross
    const val GP_BTN_B = 1 shl 1       // Circle
    const val GP_BTN_X = 1 shl 2       // Square
    const val GP_BTN_Y = 1 shl 3       // Triangle
    const val GP_BTN_L1 = 1 shl 4
    const val GP_BTN_R1 = 1 shl 5
    const val GP_BTN_L2 = 1 shl 6
    const val GP_BTN_R2 = 1 shl 7
    const val GP_BTN_SELECT = 1 shl 8
    const val GP_BTN_START = 1 shl 9
    const val GP_BTN_L3 = 1 shl 10
    const val GP_BTN_R3 = 1 shl 11
    const val GP_BTN_HOME = 1 shl 12

    // Gamepad Hat Switch values (0=Center, 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W, 8=NW)
    const val GP_HAT_CENTER = 0
    const val GP_HAT_UP = 1
    const val GP_HAT_RIGHT = 3
    const val GP_HAT_DOWN = 5
    const val GP_HAT_LEFT = 7

    // Digitizer Stylus Flag bits
    const val PEN_TIP_SWITCH = 1 shl 0
    const val PEN_BARREL_SWITCH = 1 shl 1
    const val PEN_ERASER = 1 shl 2
    const val PEN_INVERT = 1 shl 3
    const val PEN_IN_RANGE = 1 shl 4

    // System Power Control Report ID 6 Bitmasks
    const val SYS_POWER_DOWN = 1 shl 0
    const val SYS_SLEEP = 1 shl 1
    const val SYS_WAKE_UP = 1 shl 2

    // ==================== MODULAR SUB-DESCRIPTORS (USB HID 1.11 STRICT) ====================

    private val KEYBOARD_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(),
        // Modifier byte (8 bits)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(), //   Usage Minimum (Left Control)
        0x29.toByte(), 0xE7.toByte(), //   Usage Maximum (Right GUI)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x08.toByte(), //   Report Count (8)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Var, Abs)
        // Reserved byte
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x81.toByte(), 0x01.toByte(), //   Input (Const)
        // LED Output Report (Num/Caps/Scroll/Compose/Kana + 3-bit pad) for strict Apple/Windows compliance
        0x95.toByte(), 0x05.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x08.toByte(), //   Usage Page (LEDs)
        0x19.toByte(), 0x01.toByte(), //   Usage Minimum (Num Lock)
        0x29.toByte(), 0x05.toByte(), //   Usage Maximum (Kana)
        0x91.toByte(), 0x02.toByte(), //   Output (Data, Var, Abs)
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x03.toByte(),
        0x91.toByte(), 0x01.toByte(), //   Output (Const)
        // 6-key rollover array (0x00..0x94 encoded with 2-byte positive Logical/Usage Maximum!)
        0x95.toByte(), 0x06.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x26.toByte(), 0x94.toByte(), 0x00.toByte(), // Logical Maximum (+148, 2-byte positive!)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x2A.toByte(), 0x94.toByte(), 0x00.toByte(), // Usage Maximum (+148, 2-byte)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array)
        0xC0.toByte(),                // End Collection
    )

    private val MOUSE_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_MOUSE.toByte(),
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
        // 5 Buttons (Left, Right, Middle, Back/M4, Forward/M5)
        0x05.toByte(), 0x09.toByte(),
        0x19.toByte(), 0x01.toByte(),
        0x29.toByte(), 0x05.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x05.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x02.toByte(),
        // 3-bit padding
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x03.toByte(),
        0x81.toByte(), 0x01.toByte(),
        // X, Y, Vertical Wheel (-127..127)
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x30.toByte(), // X
        0x09.toByte(), 0x31.toByte(), // Y
        0x09.toByte(), 0x38.toByte(), // Wheel
        0x15.toByte(), 0x81.toByte(), // Logical Min (-127)
        0x25.toByte(), 0x7F.toByte(), // Logical Max (+127)
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x03.toByte(),
        0x81.toByte(), 0x06.toByte(),
        // Horizontal Pan Wheel (AC Pan, Consumer Page 0x0238)
        0x05.toByte(), 0x0C.toByte(),
        0x0A.toByte(), 0x38.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x06.toByte(),
        0xC0.toByte(),
        0xC0.toByte(),
    )

    private val CONSUMER_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x0C.toByte(), // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(), // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),
        0x85.toByte(), REPORT_ID_CONSUMER.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x18.toByte(), // 24 bits (3 bytes)
        0x09.toByte(), 0xCD.toByte(), // 0: Play/Pause
        0x09.toByte(), 0xB5.toByte(), // 1: Next
        0x09.toByte(), 0xB6.toByte(), // 2: Prev
        0x09.toByte(), 0xB7.toByte(), // 3: Stop
        0x09.toByte(), 0xE9.toByte(), // 4: Vol+
        0x09.toByte(), 0xEA.toByte(), // 5: Vol-
        0x09.toByte(), 0xE2.toByte(), // 6: Mute
        0x0A.toByte(), 0x23.toByte(), 0x02.toByte(), // 7: AC Home
        0x0A.toByte(), 0x24.toByte(), 0x02.toByte(), // 8: AC Back
        0x09.toByte(), 0x40.toByte(), // 9: Menu
        0x09.toByte(), 0xB3.toByte(), // 10: FF
        0x09.toByte(), 0xB4.toByte(), // 11: Rew
        0x09.toByte(), 0x30.toByte(), // 12: Power
        0x09.toByte(), 0x9C.toByte(), // 13: Ch+
        0x09.toByte(), 0x9D.toByte(), // 14: Ch-
        0x0A.toByte(), 0x21.toByte(), 0x02.toByte(), // 15: AC Search
        0x09.toByte(), 0x6F.toByte(), // 16: Display Brightness Increment
        0x09.toByte(), 0x70.toByte(), // 17: Display Brightness Decrement
        0x09.toByte(), 0x32.toByte(), // 18: Sleep
        0x0A.toByte(), 0x9E.toByte(), 0x01.toByte(), // 19: AL Terminal Lock / Screensaver
        0x09.toByte(), 0x89.toByte(), // 20: Media Select TV / Source
        0x09.toByte(), 0x8D.toByte(), // 21: Media Select Program Guide
        0x09.toByte(), 0x69.toByte(), // 22: Red Menu Button
        0x09.toByte(), 0x6A.toByte(), // 23: Green Menu Button
        0x81.toByte(), 0x02.toByte(),
        0xC0.toByte(),
    )

    private val GAMEPAD_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(), // Usage (Game Pad)
        0xA1.toByte(), 0x01.toByte(),
        0x85.toByte(), REPORT_ID_GAMEPAD.toByte(),
        // 16 Buttons (2 bytes)
        0x05.toByte(), 0x09.toByte(),
        0x19.toByte(), 0x01.toByte(),
        0x29.toByte(), 0x10.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x10.toByte(),
        0x81.toByte(), 0x02.toByte(),
        // Hat Switch (D-Pad, 4 bits + 4 bits padding = 1 byte)
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x39.toByte(), // Hat switch
        0x15.toByte(), 0x01.toByte(),
        0x25.toByte(), 0x08.toByte(),
        0x75.toByte(), 0x04.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x42.toByte(), // Null state
        0x75.toByte(), 0x04.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x01.toByte(), // Padding
        // Dual Analog Sticks: X, Y, Z(RX), Rz(RY) (-127..127, 4 bytes)
        0x09.toByte(), 0x30.toByte(), // LX
        0x09.toByte(), 0x31.toByte(), // LY
        0x09.toByte(), 0x32.toByte(), // RX
        0x09.toByte(), 0x35.toByte(), // RY
        0x15.toByte(), 0x81.toByte(),
        0x25.toByte(), 0x7F.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x04.toByte(),
        0x81.toByte(), 0x02.toByte(),
        // Analog Triggers: Brake (L2) & Accelerator (R2) (0..255, 2 bytes)
        0x05.toByte(), 0x02.toByte(), // Simulation Controls
        0x09.toByte(), 0xC5.toByte(), // Brake (L2)
        0x09.toByte(), 0xC4.toByte(), // Accelerator (R2)
        0x15.toByte(), 0x00.toByte(),
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x02.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0xC0.toByte(),
    )

    private val DIGITIZER_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x0D.toByte(), // Usage Page (Digitizers)
        0x09.toByte(), 0x02.toByte(), // Usage (Pen)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_DIGITIZER.toByte(),
        0x09.toByte(), 0x20.toByte(), //   Usage (Stylus)
        0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
        // Flags: Tip, Barrel, Eraser, Invert, In-Range (5 bits + 3 pad = 1 byte)
        0x09.toByte(), 0x42.toByte(), // Tip Switch
        0x09.toByte(), 0x44.toByte(), // Barrel Switch
        0x09.toByte(), 0x45.toByte(), // Eraser
        0x09.toByte(), 0x3C.toByte(), // Invert
        0x09.toByte(), 0x32.toByte(), // In Range (Hover)
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x05.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x03.toByte(),
        0x81.toByte(), 0x03.toByte(), // Padding
        // Absolute X, Y (0..32767, 4 bytes)
        0x05.toByte(), 0x01.toByte(), // Generic Desktop
        0x09.toByte(), 0x30.toByte(), // X
        0x09.toByte(), 0x31.toByte(), // Y
        0x15.toByte(), 0x00.toByte(),
        0x26.toByte(), 0xFF.toByte(), 0x7F.toByte(), // 32767
        0x75.toByte(), 0x10.toByte(),
        0x95.toByte(), 0x02.toByte(),
        0x81.toByte(), 0x02.toByte(),
        // Tip Pressure (0..1023, 2 bytes)
        0x05.toByte(), 0x0D.toByte(), // Digitizers
        0x09.toByte(), 0x30.toByte(), // Tip Pressure
        0x15.toByte(), 0x00.toByte(),
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // 1023
        0x75.toByte(), 0x10.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x02.toByte(),
        // Tilt X & Tilt Y (-90..90, 2 bytes)
        0x09.toByte(), 0x3D.toByte(), // X Tilt
        0x09.toByte(), 0x3E.toByte(), // Y Tilt
        0x15.toByte(), 0xA6.toByte(), // -90
        0x25.toByte(), 0x5A.toByte(), // +90
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x02.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0xC0.toByte(),
        0xC0.toByte(),
    )

    private val SYSTEM_POWER_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x80.toByte(), // Usage (System Control)
        0xA1.toByte(), 0x01.toByte(),
        0x85.toByte(), REPORT_ID_SYSTEM.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x03.toByte(),
        0x09.toByte(), 0x81.toByte(), // System Power Down
        0x09.toByte(), 0x82.toByte(), // System Sleep
        0x09.toByte(), 0x83.toByte(), // System Wake Up
        0x81.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x05.toByte(),
        0x81.toByte(), 0x01.toByte(), // 5-bit padding
        0xC0.toByte(),
    )

    val UNIVERSAL_HID_DESCRIPTOR: ByteArray =
        KEYBOARD_DESCRIPTOR + MOUSE_DESCRIPTOR + CONSUMER_DESCRIPTOR

    val HID_REPORT_DESCRIPTOR: ByteArray =
        KEYBOARD_DESCRIPTOR + MOUSE_DESCRIPTOR + CONSUMER_DESCRIPTOR +
            GAMEPAD_DESCRIPTOR + DIGITIZER_DESCRIPTOR + SYSTEM_POWER_DESCRIPTOR

    fun descriptorForProfile(profile: HidProfileMode): ByteArray = when (profile) {
        HidProfileMode.UNIVERSAL -> UNIVERSAL_HID_DESCRIPTOR
        HidProfileMode.PRO_COMPLETE -> HID_REPORT_DESCRIPTOR
        HidProfileMode.GAMEPAD_ONLY -> GAMEPAD_DESCRIPTOR
        HidProfileMode.DIGITIZER_ONLY -> DIGITIZER_DESCRIPTOR
    }

    fun subclassForProfile(profile: HidProfileMode): Byte = when (profile) {
        HidProfileMode.UNIVERSAL, HidProfileMode.PRO_COMPLETE -> BluetoothHidDevice.SUBCLASS1_COMBO
        HidProfileMode.GAMEPAD_ONLY -> BluetoothHidDevice.SUBCLASS2_GAMEPAD
        HidProfileMode.DIGITIZER_ONLY -> BluetoothHidDevice.SUBCLASS2_DIGITIZER_TABLET
    }

    /**
     * Validates a USB HID report descriptor byte array:
     * 1. Every short item has valid length within array bounds.
     * 2. Every Collection (0xA1) has a balanced End Collection (0xC0).
     * 3. Every Logical Maximum (0x25/0x26) is >= current Logical Minimum (0x15/0x16) as signed values.
     */
    fun validateDescriptor(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        var i = 0
        var depth = 0
        var logicalMin = 0
        while (i < bytes.size) {
            val prefix = bytes[i].toInt() and 0xFF
            if (prefix == 0xFE) return false // long items not used
            val sizeCode = prefix and 0x03
            val dataLen = if (sizeCode == 3) 4 else sizeCode
            val tag = prefix and 0xFC
            if (i + 1 + dataLen > bytes.size) return false

            when (tag) {
                0xA0 -> depth++ // Collection
                0xC0 -> {
                    depth--
                    if (depth < 0) return false
                }
                0x14 -> { // Logical Minimum (signed)
                    logicalMin = readSignedInt(bytes, i + 1, dataLen)
                }
                0x24 -> { // Logical Maximum (signed)
                    val logicalMax = readSignedInt(bytes, i + 1, dataLen)
                    if (logicalMax < logicalMin) return false
                }
            }
            i += 1 + dataLen
        }
        return depth == 0
    }

    private fun readSignedInt(bytes: ByteArray, offset: Int, len: Int): Int = when (len) {
        0 -> 0
        1 -> bytes[offset].toInt() // sign-extended Byte (-128..127)
        2 -> (bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)
        4 -> (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            (bytes[offset + 3].toInt() shl 24)
        else -> 0
    }
}
