package com.example

import com.example.bluetooth.BluetoothHidManager
import com.example.bluetooth.MouseDeltaCoalescer
import com.example.data.CustomLayoutSerializer
import com.example.data.DigitizerMonitorMapping
import com.example.data.HidProfileMode
import com.example.data.TargetOsProfile
import com.example.hid.HidConstants
import com.example.hid.KeyboardLayouts
import com.example.hid.SmartAutocorrectEngine
import com.example.network.LocalWebLayoutServer
import com.example.network.PcCompanionClient
import com.example.system.SamsungCompatibilityInspector
import com.example.ui.screens.evaluateSimpleExpression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControleBtCoreTest {

    @Test
    fun smartAutocorrectRestoresAccentsAndFixesTypos() {
        assertEquals("você", SmartAutocorrectEngine.autoCorrectWord("voce"))
        assertEquals("não", SmartAutocorrectEngine.autoCorrectWord("nao"))
        assertEquals("também", SmartAutocorrectEngine.autoCorrectWord("tb"))
        assertEquals("configuração", SmartAutocorrectEngine.autoCorrectWord("configuracao"))
        assertEquals("Você", SmartAutocorrectEngine.autoCorrectWord("Voce"))
        assertEquals("computador", SmartAutocorrectEngine.autoCorrectWord("computdor"))
    }

    @Test
    fun abnt2LayoutMapsSpecialCharactersAndAccents() {
        val cedilla = KeyboardLayouts.charToStrokes('ç', "pt_BR_ABNT2")
        assertEquals(1, cedilla.size)
        assertEquals(HidConstants.KEY_SEMICOLON, cedilla.first().keyCode)

        val aTilde = KeyboardLayouts.charToStrokes('ã', "pt_BR_ABNT2")
        assertEquals(2, aTilde.size)
        assertEquals(HidConstants.KEY_APOSTROPHE, aTilde[0].keyCode)
        assertEquals(HidConstants.KEY_A, aTilde[1].keyCode)

        val detected = KeyboardLayouts.detectLayoutFromQr("https://appground.io/keyboard?layout=pt_BR_ABNT2")
        assertNotNull(detected)
        assertEquals("pt_BR_ABNT2", detected?.id)
    }

    @Test
    fun numpadCalculatorEvaluatesPrecedenceAccurately() {
        assertEquals("14", evaluateSimpleExpression("2+3*4"))
        assertEquals("187.5", evaluateSimpleExpression("1250*0,15"))
        assertEquals("25", evaluateSimpleExpression("100/4"))
    }

    @Test
    fun customLayoutJsonAndFullBundleBackupRoundTripSucceeds() {
        val starter = CustomLayoutSerializer.starterLayouts().first()
        val json = CustomLayoutSerializer.exportLayoutJson(starter)
        val restored = CustomLayoutSerializer.importLayoutJson(json)
        assertNotNull(restored)
        assertEquals(starter.name, restored?.name)
        assertTrue(restored!!.widgets.isNotEmpty())

        // Stream Deck gallery + Full Multi-Layout Backup Bundle round-trip
        val gallery = CustomLayoutSerializer.streamDeckGallery()
        assertTrue(gallery.size >= 7)
        val bundleJson = CustomLayoutSerializer.exportAllBundleJson(gallery)
        val restoredBundle = CustomLayoutSerializer.importAllBundleJson(bundleJson)
        assertNotNull(restoredBundle)
        assertEquals(gallery.size, restoredBundle!!.size)
        assertEquals(gallery.first().name, restoredBundle.first().name)
    }

    @Test
    fun allHidProfileDescriptorsPassUsbHid111Validation() {
        // Validate all 4 selectable HID profiles (Universal Apple/TV, Pro 6-in-1, Gamepad, Digitizer)
        HidProfileMode.entries.forEach { profile ->
            val desc = HidConstants.descriptorForProfile(profile)
            assertTrue("Descriptor for $profile should be non-empty", desc.isNotEmpty())
            assertTrue("Descriptor for $profile failed USB HID 1.11 validation", HidConstants.validateDescriptor(desc))
        }

        // Verify the old buggy 1-byte signed Logical Maximum (0x25, 0x94 = -108) is rejected
        val malformedDescriptor = byteArrayOf(
            0x05, 0x01, // Usage Page (Generic Desktop)
            0x09, 0x06, // Usage (Keyboard)
            0xA1.toByte(), 0x01, // Collection (Application)
            0x15, 0x00, // Logical Minimum (0)
            0x25, 0x94.toByte(), // BUGGY: 1-byte signed Logical Maximum = -108 < 0!
            0xC0.toByte(), // End Collection
        )
        assertFalse("Malformed descriptor with negative Logical Max must fail validation", HidConstants.validateDescriptor(malformedDescriptor))

        assertEquals(0f, DigitizerMonitorMapping.DUAL_1.xStart, 0.001f)
        assertEquals(0.5f, DigitizerMonitorMapping.DUAL_1.xEnd, 0.001f)
        assertEquals(0.5f, DigitizerMonitorMapping.DUAL_2.xStart, 0.001f)
        assertEquals(1.0f, DigitizerMonitorMapping.DUAL_2.xEnd, 0.001f)
    }

    @Test
    fun mouseDeltaCoalescerMergesAndChunksLargeDeltas() {
        val flushed = mutableListOf<List<Int>>()
        val coalescer = MouseDeltaCoalescer { dx, dy, vw, hw ->
            flushed.add(listOf(dx, dy, vw, hw))
        }

        // Empty flush returns false
        assertFalse(coalescer.flushNow())
        assertTrue(flushed.isEmpty())

        // Coalesce rapid micro-deltas into a single HID packet
        coalescer.enqueue(10, -5)
        coalescer.enqueue(15, -10, vWheel = 2)
        coalescer.enqueue(-5, 3, hWheel = -1)
        assertTrue(coalescer.flushNow())
        assertEquals(1, flushed.size)
        assertEquals(listOf(20, -12, 2, -1), flushed.first())

        // Large delta (> 127) splits cleanly into valid signed 8-bit HID chunks
        flushed.clear()
        coalescer.enqueue(300, -260)
        assertTrue(coalescer.flushNow())
        assertEquals(3, flushed.size)
        assertEquals(300, flushed.sumOf { it[0] })
        assertEquals(-260, flushed.sumOf { it[1] })
        assertTrue(flushed.all { it[0] in -127..127 && it[1] in -127..127 })
    }

    @Test
    fun iosAndMacOsShortcutsAdaptProperly() {
        assertEquals("CMD+H", BluetoothHidManager.adaptShortcutForOs("WIN+D", TargetOsProfile.IOS))
        assertEquals("CMD+SPACE", BluetoothHidManager.adaptShortcutForOs("WIN+S", TargetOsProfile.IOS))
        assertEquals("CMD+TAB", BluetoothHidManager.adaptShortcutForOs("ALT+TAB", TargetOsProfile.IOS))
        assertEquals("CMD+C", BluetoothHidManager.adaptShortcutForOs("CTRL+C", TargetOsProfile.IOS))
        assertEquals("CMD+V", BluetoothHidManager.adaptShortcutForOs("CTRL+V", TargetOsProfile.IOS))
        assertEquals("CMD+Q", BluetoothHidManager.adaptShortcutForOs("ALT+F4", TargetOsProfile.MACOS))
        assertEquals("CTRL+C", BluetoothHidManager.adaptShortcutForOs("CTRL+C", TargetOsProfile.WINDOWS))
    }

    @Test
    fun companionNetworkValidationAndPinSecurityChecksHold() {
        // Local / Private LAN hosts allowed
        assertTrue(PcCompanionClient.isLocalOrPrivateHost("127.0.0.1"))
        assertTrue(PcCompanionClient.isLocalOrPrivateHost("localhost"))
        assertTrue(PcCompanionClient.isLocalOrPrivateHost("192.168.1.42"))
        assertTrue(PcCompanionClient.isLocalOrPrivateHost("10.0.0.15"))
        assertTrue(PcCompanionClient.isLocalOrPrivateHost("172.16.4.8"))
        assertTrue(PcCompanionClient.isLocalOrPrivateHost("172.31.255.1"))
        assertTrue(PcCompanionClient.isLocalOrPrivateHost("meu-macbook.local"))

        // Public internet / invalid hosts blocked
        assertFalse(PcCompanionClient.isLocalOrPrivateHost("8.8.8.8"))
        assertFalse(PcCompanionClient.isLocalOrPrivateHost("1.1.1.1"))
        assertFalse(PcCompanionClient.isLocalOrPrivateHost("example.com"))
        assertFalse(PcCompanionClient.isLocalOrPrivateHost("172.32.0.1"))
        assertFalse(PcCompanionClient.isLocalOrPrivateHost(""))

        // LocalWebLayoutServer PIN authorization
        assertTrue(LocalWebLayoutServer.isPinAuthorized("482913", "482913"))
        assertFalse(LocalWebLayoutServer.isPinAuthorized("482914", "482913"))
        assertFalse(LocalWebLayoutServer.isPinAuthorized("", "482913"))

        // Hardened Python companion script embeds token, constant-time compare, and avoids os.system
        val script = PcCompanionClient.buildPythonCompanionScript("654321")
        assertTrue(script.contains("654321"))
        assertTrue(script.contains("hmac.compare_digest"))
        assertTrue(script.contains("subprocess.Popen"))
        assertFalse(script.contains("os.system"))
    }

    @Test
    fun samsungS23UltraAndOneUiCompatibilityDetectionWorksAccurately() {
        // S23 Ultra regional variants & codename
        assertTrue(SamsungCompatibilityInspector.isGalaxyS23UltraModel("SM-S918B"))
        assertTrue(SamsungCompatibilityInspector.isGalaxyS23UltraModel("SM-S918U1"))
        assertTrue(SamsungCompatibilityInspector.isGalaxyS23UltraModel("SM-S918E"))
        assertTrue(SamsungCompatibilityInspector.isGalaxyS23UltraModel("unknown", "dm3q"))
        assertFalse(SamsungCompatibilityInspector.isGalaxyS23UltraModel("Pixel 8 Pro", "husky"))

        // Snapdragon 8 Gen 2 / SM8550
        assertTrue(SamsungCompatibilityInspector.isSnapdragon8Gen2("SM8550"))
        assertTrue(SamsungCompatibilityInspector.isSnapdragon8Gen2("", "kalama"))
        assertFalse(SamsungCompatibilityInspector.isSnapdragon8Gen2("Tensor G3", "zuma"))

        // One UI 5.1 -> 6.1 -> 7.0 -> 8.0 parsing
        assertTrue(SamsungCompatibilityInspector.parseOneUiVersionName(140100, 33).contains("One UI 5.1"))
        assertTrue(SamsungCompatibilityInspector.parseOneUiVersionName(150100, 34).contains("One UI 6.1"))
        assertTrue(SamsungCompatibilityInspector.parseOneUiVersionName(160000, 35).contains("One UI 7"))
        assertTrue(SamsungCompatibilityInspector.parseOneUiVersionName(170000, 36).contains("One UI 8"))
        assertTrue(SamsungCompatibilityInspector.parseOneUiVersionName(0, 36).contains("Android 16"))

        // DeX stability guards & One UI Device Care guidance
        assertTrue(SamsungCompatibilityInspector.shouldPauseAirMouseForDeX(true))
        assertFalse(SamsungCompatibilityInspector.shouldPauseAirMouseForDeX(false))
        assertTrue(SamsungCompatibilityInspector.shouldSuppressOledDimForDeX(true))
        assertTrue(SamsungCompatibilityInspector.buildOneUiDeviceCareGuide(160000).contains("Irrestrito"))
        assertTrue(SamsungCompatibilityInspector.buildOneUiDeviceCareGuide(150100).contains("Não restrito"))
    }
}
