package com.example.viewmodel

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.AirMouseController
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.BluetoothHidManager
import com.example.bluetooth.HidDiagnosticItem
import com.example.bluetooth.PairedDeviceUi
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.ControlMode
import com.example.data.CustomLayout
import com.example.data.CustomLayoutExportDto
import com.example.data.CustomLayoutRepository
import com.example.data.CustomLayoutWidget
import com.example.data.CustomWidgetType
import com.example.data.HidProfileMode
import com.example.data.MacroStep
import com.example.data.PointerAccelCurve
import com.example.data.SettingsRepository
import com.example.data.VolumeButtonAction
import com.example.hid.AutocorrectSuggestion
import com.example.hid.HidConstants
import com.example.hid.KeyboardLayouts
import com.example.hid.SmartAutocorrectEngine
import com.example.network.LocalWebLayoutServer
import com.example.network.PcCompanionClient
import com.example.network.PcStatusDto
import com.example.network.RemoteFileEntry
import com.example.network.WakeOnLanSender
import com.example.security.CredentialVaultManager
import com.example.security.VaultCredential
import com.example.system.SamsungCompatibilityInspector
import com.example.system.SamsungDeviceProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class ScannedBarcodeItem(
    val value: String,
    val formatLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class PendingAutomationRequest(
    val deviceMac: String?,
    val modeRoute: String?,
    val macroName: String?,
    val textToType: String?,
)

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val database = AppDatabase.getInstance(application)
    private val layoutRepo = CustomLayoutRepository(database.customLayoutDao())
    private val vaultManager = CredentialVaultManager(application)
    private val companionClient = PcCompanionClient()
    val hidManager = BluetoothHidManager.getInstance(application)

    private val webLayoutServer = LocalWebLayoutServer(layoutRepo) { msg ->
        showBanner(msg)
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(Application.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Application.VIBRATOR_SERVICE) as? Vibrator
    }

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings(),
    )

    val customLayouts: StateFlow<List<CustomLayout>> = layoutRepo.allLayouts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val connectionState: StateFlow<BluetoothConnectionState> = hidManager.connectionState
    val pairedDevices: StateFlow<List<PairedDeviceUi>> = hidManager.pairedDevices
    val lastActionLabel: StateFlow<String> = hidManager.lastActionLabel
    val isHidProfileReady: StateFlow<Boolean> = hidManager.isHidProfileReady
    val isMacroLoopRunning: StateFlow<Boolean> = hidManager.isMacroLoopRunning
    val webEditorUrl: StateFlow<String?> = webLayoutServer.serverUrl
    val webEditorPin: StateFlow<String?> = webLayoutServer.sessionPin

    private val _currentMode = MutableStateFlow(ControlMode.MOUSE_KEYBOARD)
    val currentMode: StateFlow<ControlMode> = _currentMode.asStateFlow()

    private val _gestureHudText = MutableStateFlow<String?>(null)
    val gestureHudText: StateFlow<String?> = _gestureHudText.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _sniperPrecisionActive = MutableStateFlow(false)
    val sniperPrecisionActive: StateFlow<Boolean> = _sniperPrecisionActive.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _suggestions = MutableStateFlow<List<AutocorrectSuggestion>>(emptyList())
    val suggestions: StateFlow<List<AutocorrectSuggestion>> = _suggestions.asStateFlow()

    private val _lastAutoCorrectionNotice = MutableStateFlow<String?>(null)
    val lastAutoCorrectionNotice: StateFlow<String?> = _lastAutoCorrectionNotice.asStateFlow()

    // Incoming Android Share Sheet & External Automation confirmation gates
    private val _pendingSharedText = MutableStateFlow<String?>(null)
    val pendingSharedText: StateFlow<String?> = _pendingSharedText.asStateFlow()

    private val _pendingAutomation = MutableStateFlow<PendingAutomationRequest?>(null)
    val pendingAutomation: StateFlow<PendingAutomationRequest?> = _pendingAutomation.asStateFlow()

    private val _scanHistory = MutableStateFlow<List<ScannedBarcodeItem>>(emptyList())
    val scanHistory: StateFlow<List<ScannedBarcodeItem>> = _scanHistory.asStateFlow()

    private val _presenterAirMouseHeld = MutableStateFlow(false)
    val presenterAirMouseHeld: StateFlow<Boolean> = _presenterAirMouseHeld.asStateFlow()

    private val _isRecordingMacro = MutableStateFlow(false)
    val isRecordingMacro: StateFlow<Boolean> = _isRecordingMacro.asStateFlow()

    private val _recordedMacroSteps = MutableStateFlow<List<MacroStep>>(emptyList())
    val recordedMacroSteps: StateFlow<List<MacroStep>> = _recordedMacroSteps.asStateFlow()
    private var lastMacroRecordTimestamp: Long = 0L

    // On-demand Biometric Credential Vault (stays locked & empty in RAM until authenticated!)
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _vaultCredentials = MutableStateFlow<List<VaultCredential>>(emptyList())
    val vaultCredentials: StateFlow<List<VaultCredential>> = _vaultCredentials.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<HidDiagnosticItem>>(emptyList())
    val diagnostics: StateFlow<List<HidDiagnosticItem>> = _diagnostics.asStateFlow()

    val isLowLatencyQosActive: StateFlow<Boolean> = hidManager.isLowLatencyQosActive

    private val _deviceProfile = MutableStateFlow(SamsungCompatibilityInspector.inspect(application))
    val deviceProfile: StateFlow<SamsungDeviceProfile> = _deviceProfile.asStateFlow()

    private val _lastSPenActionNotice = MutableStateFlow<String?>(null)
    val lastSPenActionNotice: StateFlow<String?> = _lastSPenActionNotice.asStateFlow()

    // Parte 2: Wi-Fi / USB PC Companion state (lifecycle-aware polling)
    private val _companionConnected = MutableStateFlow(false)
    val companionConnected: StateFlow<Boolean> = _companionConnected.asStateFlow()

    private val _pcStatus = MutableStateFlow(PcStatusDto())
    val pcStatus: StateFlow<PcStatusDto> = _pcStatus.asStateFlow()

    private val _magnifierBitmap = MutableStateFlow<Bitmap?>(null)
    val magnifierBitmap: StateFlow<Bitmap?> = _magnifierBitmap.asStateFlow()

    private val _remoteFiles = MutableStateFlow<List<RemoteFileEntry>>(emptyList())
    val remoteFiles: StateFlow<List<RemoteFileEntry>> = _remoteFiles.asStateFlow()

    private val _currentRemotePath = MutableStateFlow("")
    val currentRemotePath: StateFlow<String> = _currentRemotePath.asStateFlow()

    private val _companionPolling = MutableStateFlow(false)
    val companionPolling: StateFlow<Boolean> = _companionPolling.asStateFlow()

    private var isAppInForeground: Boolean = true

    private val airMouseController = AirMouseController(application) { dx, dy ->
        val factor = if (_sniperPrecisionActive.value) 0.45f else 1f
        hidManager.sendMouseMove((dx * factor).roundToInt(), (dy * factor).roundToInt())
    }

    val isAirMouseSupported: Boolean get() = airMouseController.isSupported

    private var jigglerJob: Job? = null
    private var autoClickerJob: Job? = null
    private var inertiaScrollJob: Job? = null
    private var gestureHudJob: Job? = null
    private var bannerJob: Job? = null
    private var companionPollJob: Job? = null
    private var lastScanCode: String = ""
    private var lastScanTime: Long = 0L
    private var lastAutoSwitchedApp: String = ""
    private var autoReconnectAttempted: Boolean = false

    init {
        viewModelScope.launch {
            settingsRepo.ensureCompanionToken()
            val initial = settingsRepo.settingsFlow.first()
            hidManager.initialize(initial.hidProfileMode)
            layoutRepo.ensureStarterLayouts()
        }
        observeSettingsForHardware()
        observeConnectionForPersistence()
        attemptSmartAutoReconnect()
    }

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (!inForeground) {
            // Auto-lock vault & pause Wi-Fi polling when app moves to background
            lockVault()
            companionPollJob?.cancel()
            companionPollJob = null
        } else {
            refreshDeviceProfile()
            if (_companionPolling.value) {
                startCompanionPollLoop()
            }
            hidManager.refreshPairedDevices()
        }
    }

    fun refreshDeviceProfile(overrideConfig: Configuration? = null) {
        val next = SamsungCompatibilityInspector.inspect(getApplication(), overrideConfig)
        _deviceProfile.value = next
        if (SamsungCompatibilityInspector.shouldPauseAirMouseForDeX(next.isDexActive)) {
            airMouseController.stop()
        } else {
            val s = settings.value
            updateAirMouseState(s.airMouseEnabled || _presenterAirMouseHeld.value)
        }
    }

    fun setHidProfileMode(profile: HidProfileMode) {
        updateSettings { it.copy(hidProfileMode = profile) }
        hidManager.setHidProfile(profile)
        showBanner("Perfil HID alterado para: ${profile.shortLabel}")
    }

    fun completeOnboarding() {
        updateSettings { it.copy(onboardingCompleted = true) }
    }

    fun reopenOnboarding() {
        updateSettings { it.copy(onboardingCompleted = false) }
    }

    fun resetSettingsToDefaults() {
        viewModelScope.launch {
            settingsRepo.resetToDefaults()
            hidManager.setHidProfile(HidProfileMode.UNIVERSAL)
            showBanner("Configurações restauradas ao padrão original")
        }
    }

    fun regenerateCompanionToken() {
        val newPin = SettingsRepository.generateSixDigitPin()
        updateSettings { it.copy(companionAuthToken = newPin) }
        showBanner("Novo PIN do Agente PC gerado: $newPin")
    }

    private fun attemptSmartAutoReconnect() {
        viewModelScope.launch {
            isHidProfileReady.collectLatest { ready ->
                if (ready && !autoReconnectAttempted) {
                    val s = settings.first()
                    if (s.autoReconnectEnabled && s.lastDeviceAddress.isNotBlank() &&
                        connectionState.value is BluetoothConnectionState.Disconnected
                    ) {
                        autoReconnectAttempted = true
                        delay(450)
                        hidManager.connectToDevice(s.lastDeviceAddress)
                    }
                }
            }
        }
    }

    private fun observeSettingsForHardware() {
        viewModelScope.launch {
            settings.collectLatest { current ->
                airMouseController.sensitivity = current.airMouseSensitivity
                airMouseController.requireTriggerHold = current.airMouseTriggerMode && !_presenterAirMouseHeld.value
                updateAirMouseState(current.airMouseEnabled || _presenterAirMouseHeld.value)
                updateMouseJiggler(current.mouseJigglerEnabled, current.mouseJigglerIntervalSec, current.mouseJigglerHumanMode)
                updateAutoClicker(current.autoClickerEnabled, current.autoClickerCps, current.autoClickerRightButton)
            }
        }
    }

    private fun observeConnectionForPersistence() {
        viewModelScope.launch {
            connectionState.collectLatest { state ->
                if (state is BluetoothConnectionState.Connected) {
                    updateSettings {
                        it.copy(lastDeviceAddress = state.address, lastDeviceName = state.deviceName)
                    }
                }
            }
        }
    }

    private fun updateAirMouseState(shouldRun: Boolean) {
        if (SamsungCompatibilityInspector.shouldPauseAirMouseForDeX(_deviceProfile.value.isDexActive)) {
            airMouseController.stop()
            return
        }
        if (shouldRun && airMouseController.isSupported) {
            airMouseController.start()
        } else {
            airMouseController.stop()
        }
    }

    fun setAirMouseTriggerHeld(held: Boolean) {
        airMouseController.isTriggerHeld = held
        if (held) performHaptic(false)
    }

    fun recalibrateAirMouse() {
        airMouseController.recalibrate()
        performHaptic(true)
        showBanner("Air Mouse recalibrado e centralizado")
    }

    fun setPresenterAirMouseHold(held: Boolean) {
        _presenterAirMouseHeld.value = held
        airMouseController.requireTriggerHold = settings.value.airMouseTriggerMode && !held
        updateAirMouseState(held || settings.value.airMouseEnabled)
        if (held) performHaptic(false)
    }

    fun setSniperPrecision(active: Boolean) {
        _sniperPrecisionActive.value = active
        if (active) performHaptic(false)
    }

    private fun updateMouseJiggler(enabled: Boolean, intervalSec: Int, humanMode: Boolean) {
        jigglerJob?.cancel()
        if (!enabled) return
        val baseSec = intervalSec.coerceIn(5, 300)
        jigglerJob = viewModelScope.launch {
            var toggle = true
            while (isActive) {
                if (humanMode) {
                    val jitterDelayMs = ((baseSec * (0.72 + Random.nextDouble() * 0.56)) * 1000L).toLong()
                    delay(jitterDelayMs)
                    val angleStart = Random.nextDouble(0.0, Math.PI * 2)
                    val radius = Random.nextDouble(2.0, 5.5)
                    for (step in 1..6) {
                        val a = angleStart + (step * 0.42)
                        val dx = (cos(a) * radius).roundToInt()
                        val dy = (sin(a) * radius).roundToInt()
                        hidManager.sendMouseMove(dx, dy)
                        delay(35L + Random.nextLong(25L))
                    }
                    if (Random.nextInt(3) == 0) {
                        hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F15, "Jiggler Humano (F15)")
                    }
                } else {
                    delay(baseSec * 1000L)
                    val delta = if (toggle) 2 else -2
                    toggle = !toggle
                    hidManager.sendMouseMove(delta, 0)
                }
            }
        }
    }

    private fun updateAutoClicker(enabled: Boolean, cps: Int, rightButton: Boolean) {
        autoClickerJob?.cancel()
        if (!enabled) return
        val intervalMs = (1000L / cps.coerceIn(1, 20)).coerceAtLeast(50L)
        val mask = if (rightButton) HidConstants.MOUSE_RIGHT else HidConstants.MOUSE_LEFT
        autoClickerJob = viewModelScope.launch {
            while (isActive) {
                hidManager.sendMouseClick(mask)
                delay(intervalMs)
            }
        }
    }

    fun selectMode(mode: ControlMode) {
        if (_currentMode.value == ControlMode.VAULT && mode != ControlMode.VAULT) {
            lockVault()
        }
        _currentMode.value = mode
        performHaptic(false)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
        }
    }

    fun performHaptic(strong: Boolean = false) {
        if (!settings.value.hapticFeedback) return
        val vib = vibrator ?: return
        runCatching {
            val ms = if (strong) 34L else 12L
            val amp = if (strong) 185 else 80
            vib.vibrate(VibrationEffect.createOneShot(ms, amp))
        }
    }

    fun performPresenterAlertVibration(urgent: Boolean) {
        val vib = vibrator ?: return
        runCatching {
            val pattern = if (urgent) longArrayOf(0, 180, 120, 180, 120, 260) else longArrayOf(0, 140, 100, 140)
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    fun showBanner(message: String) {
        bannerJob?.cancel()
        _statusBanner.value = message
        bannerJob = viewModelScope.launch {
            delay(3200)
            _statusBanner.value = null
        }
    }

    private fun showGestureHud(label: String) {
        gestureHudJob?.cancel()
        _gestureHudText.value = label
        gestureHudJob = viewModelScope.launch {
            delay(1150)
            _gestureHudText.value = null
        }
    }

    // ==================== EASY-SWITCH (1 | 2 | 3) & DIAGNOSTICS ====================

    fun assignEasySwitchSlot(slotNumber: Int, name: String, address: String) {
        updateSettings { s ->
            when (slotNumber) {
                1 -> s.copy(easySwitch1Name = name, easySwitch1Addr = address)
                2 -> s.copy(easySwitch2Name = name, easySwitch2Addr = address)
                else -> s.copy(easySwitch3Name = name, easySwitch3Addr = address)
            }
        }
        showBanner("Slot $slotNumber definido para $name")
    }

    fun activateEasySwitchSlot(slotNumber: Int) {
        val slot = settings.value.easySwitchSlots().find { it.slotNumber == slotNumber } ?: return
        if (slot.address.isBlank()) {
            selectMode(ControlMode.DEVICES)
            showBanner("Associe um dispositivo ao Slot $slotNumber")
            return
        }
        performHaptic(true)
        hidManager.connectToDevice(slot.address)
        showBanner("Easy-Switch $slotNumber → ${slot.name.ifBlank { slot.address }}")
    }

    fun runBluetoothDiagnostics() {
        _diagnostics.value = hidManager.runDiagnostics()
    }

    fun buildDiagnosticsReport(): String = hidManager.buildDiagnosticsTextReport()

    // ==================== SHARE SHEET & HARDENED AUTOMATION INTENTS ====================

    fun handleIncomingSharedText(text: String) {
        if (text.isBlank()) return
        _pendingSharedText.value = text
    }

    fun confirmSendSharedText(appendEnter: Boolean = false) {
        val text = _pendingSharedText.value ?: return
        hidManager.sendText(text, settings.value.keyboardLayoutId, appendEnter = appendEnter)
        _pendingSharedText.value = null
        showBanner("Texto compartilhado digitado no PC!")
    }

    fun dismissSharedText() {
        _pendingSharedText.value = null
    }

    fun handleAutomationTrigger(deviceMac: String?, modeRoute: String?, macroName: String?, textToType: String?) {
        val req = PendingAutomationRequest(deviceMac, modeRoute, macroName, textToType)
        val hasInputInjection = !textToType.isNullOrBlank() || !macroName.isNullOrBlank()
        if (settings.value.requireAutomationConfirmation && hasInputInjection) {
            _pendingAutomation.value = req
            return
        }
        executeAutomationRequest(req)
    }

    fun confirmPendingAutomation() {
        val req = _pendingAutomation.value ?: return
        _pendingAutomation.value = null
        executeAutomationRequest(req)
    }

    fun dismissPendingAutomation() {
        _pendingAutomation.value = null
        showBanner("Automação externa bloqueada")
    }

    private fun executeAutomationRequest(req: PendingAutomationRequest) {
        if (!req.deviceMac.isNullOrBlank()) {
            hidManager.connectToDevice(req.deviceMac)
        }
        if (!req.modeRoute.isNullOrBlank()) {
            ControlMode.entries.find { it.route.equals(req.modeRoute, ignoreCase = true) }?.let { selectMode(it) }
        }
        if (!req.textToType.isNullOrBlank()) {
            hidManager.sendText(req.textToType, settings.value.keyboardLayoutId)
        }
        if (!req.macroName.isNullOrBlank()) {
            viewModelScope.launch {
                val all = layoutRepo.allLayouts.first()
                val match = all.flatMap { it.widgets }.firstOrNull {
                    it.label.equals(req.macroName, ignoreCase = true) && it.type == CustomWidgetType.MACRO.name
                }
                if (match != null) {
                    hidManager.executeMacro(match.macroSteps, settings.value.keyboardLayoutId, match.label, match.repeatCount)
                }
            }
        }
        showBanner("Automação executada")
    }

    // ==================== MOUSE, ACCEL CURVE, SNIPER & ADVANCED GESTURES ====================

    fun onTouchpadMove(dxPx: Float, dyPx: Float) {
        val s = settings.value
        val sniperFactor = if (_sniperPrecisionActive.value) 0.42f else 1f
        val baseScale = 0.65f * s.pointerSpeed * sniperFactor
        val mag = hypot(dxPx.toDouble(), dyPx.toDouble()).toFloat()

        val accelFactor = if (s.pointerAccelCurve == PointerAccelCurve.ACCELERATED && !_sniperPrecisionActive.value) {
            (0.72f + (mag / 18f).pow(1.22f).coerceAtMost(2.4f))
        } else {
            1f
        }

        val dx = (dxPx * baseScale * accelFactor).roundToInt()
        val dy = (dyPx * baseScale * accelFactor).roundToInt()
        if (dx != 0 || dy != 0) {
            hidManager.sendMouseMove(dx, dy)
        }
    }

    fun onScrollDelta(deltaPx: Float, horizontal: Boolean = false) {
        inertiaScrollJob?.cancel()
        val s = settings.value
        val sign = if (s.invertScrollDirection) 1 else -1
        val scaled = (deltaPx * 0.08f * s.scrollSpeed).roundToInt() * sign
        if (scaled != 0) {
            if (horizontal) {
                hidManager.sendMouseMove(0, 0, wheel = 0, hWheel = scaled.coerceIn(-12, 12))
            } else {
                hidManager.sendMouseMove(0, 0, wheel = scaled.coerceIn(-12, 12), hWheel = 0)
            }
        }
    }

    fun onScrollFling(velocityPxPerSec: Float) {
        val s = settings.value
        if (!s.inertiaScrolling) return
        inertiaScrollJob?.cancel()
        val sign = if (s.invertScrollDirection) 1 else -1
        inertiaScrollJob = viewModelScope.launch {
            var velocity = velocityPxPerSec * 0.0045f * s.scrollSpeed
            while (abs(velocity) >= 0.45f) {
                val tick = velocity.roundToInt().coerceIn(-8, 8) * sign
                if (tick != 0) {
                    hidManager.sendMouseMove(0, 0, wheel = tick)
                }
                velocity *= 0.84f
                delay(28)
            }
        }
    }

    fun onMouseClick(buttonMask: Int) {
        performHaptic(false)
        hidManager.sendMouseClick(buttonMask)
    }

    fun onMouseButtonHold(buttonMask: Int, pressed: Boolean) {
        performHaptic(pressed)
        hidManager.setMouseButtonState(buttonMask, pressed)
    }

    fun onPinchZoom(zoomIn: Boolean) {
        if (!settings.value.enableGestureCommands) return
        showGestureHud(if (zoomIn) "Pinça • Zoom +" else "Pinça • Zoom -")
        hidManager.sendPinchZoom(zoomIn, settings.value.targetOsProfile)
    }

    fun onGestureCommand(gestureName: String) {
        if (!settings.value.enableGestureCommands) return
        performHaptic(true)
        val os = settings.value.targetOsProfile
        when (gestureName) {
            "TWO_FINGER_TAP" -> {
                showGestureHud("2 dedos • Clique Direito")
                hidManager.sendMouseClick(HidConstants.MOUSE_RIGHT)
            }
            "THREE_FINGER_TAP" -> {
                showGestureHud("3 dedos • Clique Central")
                hidManager.sendMouseClick(HidConstants.MOUSE_MIDDLE)
            }
            "THREE_FINGER_LEFT" -> {
                showGestureHud("3 dedos ← • Voltar")
                hidManager.sendAdaptiveShortcut("ALT+LEFT", os, "Voltar")
            }
            "THREE_FINGER_RIGHT" -> {
                showGestureHud("3 dedos → • Avançar")
                hidManager.sendAdaptiveShortcut("ALT+RIGHT", os, "Avançar")
            }
            "THREE_FINGER_UP" -> {
                showGestureHud("3 dedos ↑ • Visão de Tarefas / Apps")
                hidManager.sendAdaptiveShortcut("WIN+TAB", os, "Visão de Tarefas")
            }
            "THREE_FINGER_DOWN" -> {
                showGestureHud("3 dedos ↓ • Início / Desktop")
                hidManager.sendAdaptiveShortcut("WIN+D", os, "Área de Trabalho")
            }
            "FOUR_FINGER_LEFT" -> {
                showGestureHud("4 dedos ← • Desktop Anterior")
                hidManager.sendAdaptiveShortcut("CTRL+WIN+LEFT", os, "Desktop Anterior")
            }
            "FOUR_FINGER_RIGHT" -> {
                showGestureHud("4 dedos → • Próximo Desktop")
                hidManager.sendAdaptiveShortcut("CTRL+WIN+RIGHT", os, "Próximo Desktop")
            }
        }
    }

    // ==================== SMART AUTOCORRECT & TEXT ENTRY ====================

    fun onInputTextChanged(newText: String) {
        val oldText = _inputText.value
        val s = settings.value

        if (s.directTypingMode) {
            handleDirectModeDiff(oldText, newText, s)
        } else {
            if (s.autoCorrectEnabled && !s.passwordMode && s.autoCorrectOnSpace &&
                newText.length == oldText.length + 1 && newText.last() == ' '
            ) {
                val wordBeforeSpace = extractTrailingWord(oldText)
                val corrected = SmartAutocorrectEngine.autoCorrectWord(wordBeforeSpace)
                if (corrected != null && corrected != wordBeforeSpace) {
                    val replaced = oldText.dropLast(wordBeforeSpace.length) + corrected + " "
                    _inputText.value = replaced
                    _suggestions.value = emptyList()
                    _lastAutoCorrectionNotice.value = "Corrigido: $wordBeforeSpace → $corrected"
                    return
                }
            }
            _inputText.value = newText
            updateSuggestionsForText(newText, s)
        }
    }

    private fun handleDirectModeDiff(oldText: String, newText: String, s: AppSettings) {
        if (newText.length < oldText.length) {
            val deletedCount = oldText.length - newText.length
            hidManager.sendKeyRepeatBackspace(deletedCount)
            _inputText.value = newText
            updateSuggestionsForText(newText, s)
            return
        }

        if (newText.startsWith(oldText) && newText.length > oldText.length) {
            val added = newText.substring(oldText.length)
            if (added == " " && s.autoCorrectEnabled && !s.passwordMode && s.autoCorrectOnSpace) {
                val trailingWord = extractTrailingWord(oldText)
                val corrected = SmartAutocorrectEngine.autoCorrectWord(trailingWord)
                if (corrected != null && corrected != trailingWord) {
                    val updatedBuffer = oldText.dropLast(trailingWord.length) + corrected + " "
                    _inputText.value = updatedBuffer
                    _suggestions.value = emptyList()
                    _lastAutoCorrectionNotice.value = "Auto-corrigido: $trailingWord → $corrected"
                    hidManager.enqueueOrderedKeyboardAction {
                        repeat(trailingWord.length) {
                            hidManager.sendKeyPressSuspend(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)
                        }
                        hidManager.sendTextSuspend("$corrected ", s.keyboardLayoutId)
                    }
                    return
                }
            }
            _inputText.value = newText
            hidManager.sendText(added, s.keyboardLayoutId)
            updateSuggestionsForText(newText, s)
        } else {
            val commonPrefixLen = oldText.commonPrefixWith(newText).length
            val backspaces = oldText.length - commonPrefixLen
            val appended = newText.substring(commonPrefixLen)
            _inputText.value = newText
            hidManager.enqueueOrderedKeyboardAction {
                repeat(backspaces) {
                    hidManager.sendKeyPressSuspend(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)
                }
                if (appended.isNotEmpty()) {
                    hidManager.sendTextSuspend(appended, s.keyboardLayoutId)
                }
            }
            updateSuggestionsForText(newText, s)
        }
    }

    private fun updateSuggestionsForText(text: String, s: AppSettings) {
        if (!s.autoCorrectEnabled || s.passwordMode) {
            _suggestions.value = emptyList()
            return
        }
        _suggestions.value = SmartAutocorrectEngine.getSuggestions(extractTrailingWord(text))
    }

    private fun extractTrailingWord(text: String): String {
        if (text.isEmpty() || text.last().isWhitespace()) return ""
        val idx = text.lastIndexOfAny(charArrayOf(' ', '\n', '\t'))
        return if (idx == -1) text else text.substring(idx + 1)
    }

    fun applySuggestion(suggestion: AutocorrectSuggestion) {
        performHaptic(false)
        val current = _inputText.value
        val trailingWord = extractTrailingWord(current)
        if (trailingWord.isEmpty()) return

        val updated = current.dropLast(trailingWord.length) + suggestion.replacement + " "
        _inputText.value = updated
        _suggestions.value = emptyList()
        _lastAutoCorrectionNotice.value = "Corrigido: ${suggestion.originalWord} → ${suggestion.replacement}"

        val s = settings.value
        if (s.directTypingMode) {
            hidManager.enqueueOrderedKeyboardAction {
                repeat(trailingWord.length) {
                    hidManager.sendKeyPressSuspend(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)
                }
                hidManager.sendTextSuspend("${suggestion.replacement} ", s.keyboardLayoutId)
            }
        }
    }

    fun sendBatchInputText(appendEnter: Boolean = false) {
        val text = _inputText.value
        if (text.isEmpty() && !appendEnter) return
        performHaptic(false)
        hidManager.sendText(text, settings.value.keyboardLayoutId, appendEnter = appendEnter)
        _inputText.value = ""
        _suggestions.value = emptyList()
    }

    fun clearInputText() {
        _inputText.value = ""
        _suggestions.value = emptyList()
        _lastAutoCorrectionNotice.value = null
    }

    fun sendPastedOrVoiceText(text: String) {
        if (text.isBlank()) return
        performHaptic(false)
        val s = settings.value
        if (s.directTypingMode) {
            hidManager.sendText(text, s.keyboardLayoutId)
            showBanner("Texto enviado (${text.length} caracteres)")
        } else {
            val combined = if (_inputText.value.isEmpty()) text else "${_inputText.value} $text"
            _inputText.value = combined
            updateSuggestionsForText(combined, s)
        }
    }

    // ==================== HARDWARE VOLUME BUTTONS ====================

    fun handleHardwareVolumeKey(isVolumeUp: Boolean): Boolean {
        return when (settings.value.volumeButtonAction) {
            VolumeButtonAction.PHONE_DEFAULT -> false
            VolumeButtonAction.REMOTE_VOLUME -> {
                performHaptic(false)
                if (isVolumeUp) hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_UP, "Volume +")
                else hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_DOWN, "Volume -")
                true
            }
            VolumeButtonAction.SCROLL_PAGE -> {
                performHaptic(false)
                hidManager.sendMouseMove(0, 0, wheel = if (isVolumeUp) 5 else -5)
                true
            }
            VolumeButtonAction.SLIDE_NAV -> {
                performHaptic(false)
                if (isVolumeUp) hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT, "Próximo Slide")
                else hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_LEFT, "Slide Anterior")
                true
            }
        }
    }

    // ==================== SAMSUNG S PEN BLE AIR ACTIONS ====================

    fun handleSPenRemoteKeyEvent(keyCode: Int): Boolean {
        val mode = _currentMode.value
        val label = when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                performHaptic(true)
                if (mode == ControlMode.MULTIMEDIA) {
                    hidManager.sendConsumerControl(HidConstants.CONSUMER_NEXT_TRACK, "S Pen • Próxima Faixa")
                    "S Pen: Próxima Faixa"
                } else {
                    hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT, "S Pen • Próximo Slide")
                    "S Pen: Próximo Slide"
                }
            }
            KeyEvent.KEYCODE_PAGE_UP -> {
                performHaptic(true)
                if (mode == ControlMode.MULTIMEDIA) {
                    hidManager.sendConsumerControl(HidConstants.CONSUMER_PREV_TRACK, "S Pen • Faixa Anterior")
                    "S Pen: Faixa Anterior"
                } else {
                    hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_LEFT, "S Pen • Slide Anterior")
                    "S Pen: Slide Anterior"
                }
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                performHaptic(true)
                hidManager.sendConsumerControl(HidConstants.CONSUMER_PLAY_PAUSE, "S Pen • Play/Pause")
                "S Pen: Play / Pause"
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                performHaptic(false)
                hidManager.sendConsumerControl(HidConstants.CONSUMER_NEXT_TRACK, "S Pen • Próxima Faixa")
                "S Pen: Próxima Faixa"
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                performHaptic(false)
                hidManager.sendConsumerControl(HidConstants.CONSUMER_PREV_TRACK, "S Pen • Faixa Anterior")
                "S Pen: Faixa Anterior"
            }
            KeyEvent.KEYCODE_F5 -> {
                performHaptic(true)
                if (mode == ControlMode.PRESENTER) {
                    hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F5, "S Pen • Iniciar Slides (F5)")
                    "S Pen: Iniciar Apresentação"
                } else {
                    hidManager.sendMouseClick(HidConstants.MOUSE_LEFT)
                    "S Pen: Clique Esquerdo"
                }
            }
            else -> return false
        }
        _lastSPenActionNotice.value = label
        showBanner(label)
        return true
    }

    // ==================== BARCODE / QR & OCR SCANNER ====================

    fun onBarcodeDetected(rawValue: String, formatLabel: String) {
        val now = System.currentTimeMillis()
        if (rawValue == lastScanCode && now - lastScanTime < 2200L) return
        lastScanCode = rawValue
        lastScanTime = now

        performHaptic(true)
        val detectedLayout = KeyboardLayouts.detectLayoutFromQr(rawValue)
        if (detectedLayout != null) {
            updateSettings { it.copy(keyboardLayoutId = detectedLayout.id) }
            hidManager.sendLayoutAutoTestProbe(detectedLayout.id)
            showBanner("Layout detectado automaticamente: ${detectedLayout.displayName}")
            return
        }

        hidManager.sendText(rawValue, settings.value.keyboardLayoutId, appendEnter = settings.value.scannerAutoEnter)
        _scanHistory.value = (listOf(ScannedBarcodeItem(rawValue, formatLabel)) + _scanHistory.value).take(20)
        showBanner("Código enviado: $rawValue")
    }

    fun sendOcrTextToPc(text: String) {
        if (text.isBlank()) return
        performHaptic(true)
        hidManager.sendText(text.trim(), settings.value.keyboardLayoutId, appendEnter = settings.value.scannerAutoEnter)
        _scanHistory.value = (listOf(ScannedBarcodeItem(text.trim(), "OCR Texto")) + _scanHistory.value).take(20)
        showBanner("Texto OCR digitado no PC!")
    }

    fun resendScannedCode(code: String) {
        performHaptic(false)
        hidManager.sendText(code, settings.value.keyboardLayoutId, appendEnter = settings.value.scannerAutoEnter)
        showBanner("Reenviado: $code")
    }

    // ==================== LIVE MACRO RECORDER, CUSTOM LAYOUTS & GALLERY ====================

    fun startMacroRecording() {
        _recordedMacroSteps.value = emptyList()
        lastMacroRecordTimestamp = System.currentTimeMillis()
        _isRecordingMacro.value = true
        hidManager.onMacroActionRecorded = { action, value ->
            val now = System.currentTimeMillis()
            val delta = (now - lastMacroRecordTimestamp).coerceIn(0L, 5000L)
            lastMacroRecordTimestamp = now
            val next = _recordedMacroSteps.value.toMutableList()
            if (next.isNotEmpty() && delta >= 100L) {
                next.add(MacroStep("DELAY", delta.toString()))
            }
            next.add(MacroStep(action, value))
            _recordedMacroSteps.value = next
        }
        showBanner("Gravando macro... execute teclas ou cliques")
    }

    fun stopMacroRecording(): List<MacroStep> {
        hidManager.onMacroActionRecorded = null
        _isRecordingMacro.value = false
        return _recordedMacroSteps.value
    }

    fun triggerCustomWidget(widget: CustomLayoutWidget) {
        performHaptic(false)
        val os = settings.value.targetOsProfile
        when (runCatching { CustomWidgetType.valueOf(widget.type) }.getOrDefault(CustomWidgetType.KEY)) {
            CustomWidgetType.KEY -> {
                val code = KeyboardLayouts.keyNameToUsageId(widget.keyValue)
                if (code != HidConstants.KEY_NONE) {
                    hidManager.sendKeyPress(HidConstants.MOD_NONE, code, widget.label)
                } else if (widget.keyValue.equals("SHIFT", ignoreCase = true)) {
                    hidManager.sendKeyPress(HidConstants.MOD_LSHIFT, HidConstants.KEY_NONE, "Shift")
                }
            }
            CustomWidgetType.SHORTCUT -> hidManager.sendAdaptiveShortcut(widget.keyValue, os, widget.label)
            CustomWidgetType.MACRO -> hidManager.executeMacro(
                steps = widget.macroSteps,
                layoutId = settings.value.keyboardLayoutId,
                macroName = widget.label,
                repeatCount = widget.repeatCount,
            )
            CustomWidgetType.MEDIA -> {
                when (widget.keyValue.uppercase()) {
                    "PLAY_PAUSE" -> hidManager.sendConsumerControl(HidConstants.CONSUMER_PLAY_PAUSE, "Play/Pause")
                    "NEXT" -> hidManager.sendConsumerControl(HidConstants.CONSUMER_NEXT_TRACK, "Próxima faixa")
                    "PREV" -> hidManager.sendConsumerControl(HidConstants.CONSUMER_PREV_TRACK, "Faixa anterior")
                    "VOL_UP" -> hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_UP, "Volume +")
                    "VOL_DOWN" -> hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_DOWN, "Volume -")
                    "MUTE" -> hidManager.sendConsumerControl(HidConstants.CONSUMER_MUTE, "Mudo")
                    "HOME" -> hidManager.sendConsumerControl(HidConstants.CONSUMER_HOME, "Início")
                    "BACK" -> hidManager.sendConsumerControl(HidConstants.CONSUMER_BACK, "Voltar")
                }
            }
            CustomWidgetType.MOUSE_BUTTON -> {
                when (widget.keyValue.uppercase()) {
                    "LEFT_CLICK" -> hidManager.sendMouseClick(HidConstants.MOUSE_LEFT)
                    "RIGHT_CLICK" -> hidManager.sendMouseClick(HidConstants.MOUSE_RIGHT)
                    "MIDDLE_CLICK" -> hidManager.sendMouseClick(HidConstants.MOUSE_MIDDLE)
                }
            }
            CustomWidgetType.TOUCHPAD -> Unit
        }
    }

    fun installGalleryPreset(preset: CustomLayoutExportDto) {
        viewModelScope.launch {
            layoutRepo.installPreset(preset)
            showBanner("Pacote \"${preset.name}\" instalado!")
        }
    }

    fun saveCustomLayout(layout: CustomLayout) {
        viewModelScope.launch {
            layoutRepo.saveLayout(layout)
            showBanner("Layout \"${layout.name}\" salvo")
        }
    }

    fun duplicateCustomLayout(layout: CustomLayout) {
        viewModelScope.launch {
            layoutRepo.duplicateLayout(layout)
            showBanner("Layout duplicado")
        }
    }

    fun deleteCustomLayout(layout: CustomLayout) {
        viewModelScope.launch {
            layoutRepo.deleteLayout(layout.id)
            showBanner("Layout removido")
        }
    }

    fun exportLayoutJson(layout: CustomLayout): String = layoutRepo.exportToJson(layout)

    suspend fun exportAllLayoutsBackupJson(): String = layoutRepo.exportAllBackupJson()

    fun importLayoutJson(json: String) {
        viewModelScope.launch {
            val result = layoutRepo.importFromJson(json)
            result.onSuccess { count ->
                showBanner("$count layout(s) importado(s) com sucesso!")
            }.onFailure {
                showBanner("Erro ao importar: JSON inválido")
            }
        }
    }

    // ==================== ON-DEMAND BIOMETRIC CREDENTIAL VAULT ====================

    fun unlockVault() {
        viewModelScope.launch {
            _vaultCredentials.value = vaultManager.loadCredentials()
            _isVaultUnlocked.value = true
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _vaultCredentials.value = emptyList()
    }

    fun saveVaultCredential(cred: VaultCredential) {
        viewModelScope.launch {
            val updated = vaultManager.addOrUpdate(cred)
            if (_isVaultUnlocked.value) {
                _vaultCredentials.value = updated
            }
            showBanner("Credencial criptografada salva no Cofre")
        }
    }

    fun deleteVaultCredential(id: String) {
        viewModelScope.launch {
            val updated = vaultManager.delete(id)
            if (_isVaultUnlocked.value) {
                _vaultCredentials.value = updated
            }
            showBanner("Credencial removida do Cofre")
        }
    }

    fun autoTypeVaultCredential(cred: VaultCredential) {
        performHaptic(true)
        val layoutId = settings.value.keyboardLayoutId
        hidManager.enqueueOrderedKeyboardAction {
            if (cred.username.isNotEmpty()) {
                hidManager.sendTextSuspend(cred.username, layoutId)
                delay(90)
                hidManager.sendKeyPressSuspend(HidConstants.MOD_NONE, HidConstants.KEY_TAB)
                delay(90)
            }
            if (cred.password.isNotEmpty()) {
                hidManager.sendTextSuspend(cred.password, layoutId, appendEnter = cred.pressEnterAfter)
            }
        }
        showBanner("Login \"${cred.title}\" digitado com segurança")
    }

    // ==================== PARTE 2: WI-FI COMPANION, WOL & WEB LAYOUT EDITOR ====================

    fun sendWakeOnLan(mac: String, broadcastIp: String) {
        performHaptic(true)
        viewModelScope.launch {
            val res = WakeOnLanSender.sendMagicPacket(mac, broadcastIp)
            res.onSuccess {
                updateSettings { it.copy(wolMacAddress = mac, wolBroadcastIp = broadcastIp) }
                showBanner("Magic Packet Wake-on-LAN enviado para $mac!")
            }.onFailure { e ->
                showBanner(e.message ?: "Erro ao enviar Wake-on-LAN")
            }
        }
    }

    fun toggleWebLayoutEditorServer() {
        if (webLayoutServer.serverUrl.value != null) {
            webLayoutServer.stop()
            showBanner("Servidor Web Editor encerrado")
        } else {
            webLayoutServer.start(8765)
            showBanner("Editor Web protegido por PIN ativo na porta 8765")
        }
    }

    fun toggleCompanionConnection() {
        if (_companionPolling.value) {
            companionPollJob?.cancel()
            companionPollJob = null
            _companionPolling.value = false
            _companionConnected.value = false
            showBanner("Companheiro Wi-Fi desconectado")
            return
        }
        val s = settings.value
        if (!PcCompanionClient.isLocalOrPrivateHost(s.companionHost)) {
            showBanner("Use apenas um IP local (ex: 192.168.x.x ou 127.0.0.1)")
            return
        }
        _companionPolling.value = true
        if (isAppInForeground) {
            startCompanionPollLoop()
        }
    }

    private fun startCompanionPollLoop() {
        companionPollJob?.cancel()
        companionPollJob = viewModelScope.launch {
            while (isActive && isAppInForeground) {
                val s = settings.value
                val res = companionClient.fetchStatus(s.companionHost, s.companionPort, s.companionAuthToken)
                res.onSuccess { status ->
                    _companionConnected.value = true
                    _pcStatus.value = status
                    if (s.companionAutoLayoutSwitch && status.activeApp.isNotBlank() &&
                        status.activeApp != lastAutoSwitchedApp
                    ) {
                        lastAutoSwitchedApp = status.activeApp
                        applyAutoLayoutForPcApp(status.activeApp)
                    }
                }.onFailure {
                    _companionConnected.value = false
                }
                delay(2500)
            }
        }
    }

    private fun applyAutoLayoutForPcApp(appName: String) {
        val lower = appName.lowercase()
        when {
            lower.contains("powerpoint") || lower.contains("keynote") || lower.contains("slides") ->
                selectMode(ControlMode.PRESENTER)
            lower.contains("youtube") || lower.contains("spotify") || lower.contains("vlc") || lower.contains("netflix") ->
                selectMode(ControlMode.MULTIMEDIA)
            lower.contains("excel") || lower.contains("calc") ->
                selectMode(ControlMode.NUMPAD)
        }
    }

    fun pushClipboardToPc(text: String) {
        val s = settings.value
        viewModelScope.launch {
            companionClient.pushClipboard(s.companionHost, s.companionPort, s.companionAuthToken, text)
                .onSuccess { showBanner("Área de transferência enviada ao PC") }
                .onFailure { e -> showBanner(e.message ?: "Falha ao enviar clipboard") }
        }
    }

    fun pullClipboardFromPc(onResult: (String) -> Unit) {
        val s = settings.value
        viewModelScope.launch {
            companionClient.pullClipboard(s.companionHost, s.companionPort, s.companionAuthToken)
                .onSuccess { clip ->
                    onResult(clip)
                    showBanner("Clipboard do PC copiado para o celular!")
                }
                .onFailure { e -> showBanner(e.message ?: "Agente PC inacessível") }
        }
    }

    fun refreshScreenMagnifier(fullScreen: Boolean = false) {
        val s = settings.value
        viewModelScope.launch {
            companionClient.fetchScreenMagnifier(s.companionHost, s.companionPort, s.companionAuthToken, fullScreen)
                .onSuccess { bmp ->
                    val old = _magnifierBitmap.value
                    _magnifierBitmap.value = bmp
                    if (old != null && old != bmp && !old.isRecycled) {
                        old.recycle()
                    }
                }
                .onFailure { e -> showBanner(e.message ?: "Não foi possível capturar a Lupa") }
        }
    }

    fun seekCompanionMedia(positionSec: Int) {
        val s = settings.value
        viewModelScope.launch {
            companionClient.seekMedia(s.companionHost, s.companionPort, s.companionAuthToken, positionSec)
        }
    }

    fun loadRemoteFiles(path: String = "") {
        val s = settings.value
        viewModelScope.launch {
            companionClient.listRemoteFiles(s.companionHost, s.companionPort, s.companionAuthToken, path)
                .onSuccess { files ->
                    _currentRemotePath.value = path
                    _remoteFiles.value = files
                }
                .onFailure { e -> showBanner(e.message ?: "Não foi possível listar arquivos do PC") }
        }
    }

    fun openRemoteFile(path: String) {
        val s = settings.value
        viewModelScope.launch {
            companionClient.openRemoteFile(s.companionHost, s.companionPort, s.companionAuthToken, path)
                .onSuccess { showBanner("Abrindo no PC...") }
                .onFailure { e -> showBanner(e.message ?: "Falha ao abrir arquivo remoto") }
        }
    }

    fun uploadFileToPc(fileName: String, bytes: ByteArray) {
        val s = settings.value
        viewModelScope.launch {
            companionClient.uploadFileToPc(s.companionHost, s.companionPort, s.companionAuthToken, fileName, bytes)
                .onSuccess { showBanner("Arquivo \"$fileName\" enviado para Downloads do PC!") }
                .onFailure { e -> showBanner(e.message ?: "Falha ao enviar arquivo ao PC") }
        }
    }

    override fun onCleared() {
        airMouseController.stop()
        webLayoutServer.stop()
        lockVault()
        _magnifierBitmap.value?.let { if (!it.isRecycled) it.recycle() }
        _magnifierBitmap.value = null
        jigglerJob?.cancel()
        autoClickerJob?.cancel()
        inertiaScrollJob?.cancel()
        companionPollJob?.cancel()
        hidManager.onMacroActionRecorded = null
        super.onCleared()
    }
}
