package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.HidProfileMode
import com.example.data.MacroStep
import com.example.data.TargetOsProfile
import com.example.hid.HidConstants
import com.example.hid.KeyboardLayouts
import com.example.system.SamsungCompatibilityInspector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class DeviceCategory {
    COMPUTER,
    TV_MEDIA,
    PHONE_TABLET,
    OTHER,
}

data class PairedDeviceUi(
    val name: String,
    val address: String,
    val category: DeviceCategory,
    val isConnected: Boolean,
)

data class HidDiagnosticItem(
    val label: String,
    val status: String,
    val isHealthy: Boolean,
    val recommendation: String = "",
)

sealed class BluetoothConnectionState {
    data object Unsupported : BluetoothConnectionState()
    data object PermissionRequired : BluetoothConnectionState()
    data object BluetoothOff : BluetoothConnectionState()
    data object Disconnected : BluetoothConnectionState()
    data class Connecting(val deviceName: String, val address: String) : BluetoothConnectionState()
    data class Connected(val deviceName: String, val address: String) : BluetoothConnectionState()
}

/**
 * Pure thread-safe 125 Hz (8 ms) mouse motion coalescer.
 * Accumulates dx/dy/vWheel/hWheel deltas within an 8 ms window to prevent L2CAP buffer bloat
 * on 90/120/144 Hz touchscreens and gyroscopes, while letting clicks flush immediately.
 */
class MouseDeltaCoalescer(
    private val onFlush: (dx: Int, dy: Int, vWheel: Int, hWheel: Int) -> Unit,
) {
    private val pendingDx = AtomicInteger(0)
    private val pendingDy = AtomicInteger(0)
    private val pendingVWheel = AtomicInteger(0)
    private val pendingHWheel = AtomicInteger(0)

    fun enqueue(dx: Int, dy: Int, vWheel: Int = 0, hWheel: Int = 0) {
        if (dx != 0) pendingDx.addAndGet(dx)
        if (dy != 0) pendingDy.addAndGet(dy)
        if (vWheel != 0) pendingVWheel.addAndGet(vWheel)
        if (hWheel != 0) pendingHWheel.addAndGet(hWheel)
    }

    fun flushNow(): Boolean {
        val dx = pendingDx.getAndSet(0)
        val dy = pendingDy.getAndSet(0)
        val vw = pendingVWheel.getAndSet(0)
        val hw = pendingHWheel.getAndSet(0)
        if (dx == 0 && dy == 0 && vw == 0 && hw == 0) return false

        // Chunk large accumulated deltas cleanly into [-127, 127] HID packets
        var remX = dx
        var remY = dy
        var remV = vw
        var remH = hw
        while (remX != 0 || remY != 0 || remV != 0 || remH != 0) {
            val stepX = remX.coerceIn(-127, 127)
            val stepY = remY.coerceIn(-127, 127)
            val stepV = remV.coerceIn(-127, 127)
            val stepH = remH.coerceIn(-127, 127)
            onFlush(stepX, stepY, stepV, stepH)
            remX -= stepX
            remY -= stepY
            remV -= stepV
            remH -= stepH
        }
        return true
    }

    fun clear() {
        pendingDx.set(0)
        pendingDy.set(0)
        pendingVWheel.set(0)
        pendingHWheel.set(0)
    }
}

/**
 * Native Android Bluetooth HID Device Profile (HIDP) controller (Android 9+ / API 28+):
 * - Selectable HID Profiles (Universal Apple/TV, Pro 6-in-1, Dedicated Gamepad, Dedicated Digitizer)
 * - 125 Hz (8 ms) off-main-thread Mouse/AirMouse delta coalescing
 * - Serialized keyboard/macro Channel to prevent out-of-order characters or stuck modifiers
 * - Live BroadcastReceiver for Bluetooth radio state & bonding changes
 */
class BluetoothHidManager(private val context: Context) {

    private val tag = "BluetoothHidManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hidExecutor = Executors.newSingleThreadExecutor()

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isHidAppRegistered: Boolean = false
    private var activeMouseButtons: Int = HidConstants.MOUSE_NONE
    private var activeHidProfile: HidProfileMode = HidProfileMode.UNIVERSAL
    private var macroJob: Job? = null
    private val receiverRegistered = AtomicBoolean(false)

    // Serialized queue for all keyboard, text, consumer, and system HID operations
    private val keyCommandChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    // 125 Hz (8 ms) mouse motion coalescer
    private val mouseCoalescer = MouseDeltaCoalescer { dx, dy, vWheel, hWheel ->
        sendMouseReportDirect(dx, dy, vWheel, hWheel)
    }

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PairedDeviceUi>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDeviceUi>> = _pairedDevices.asStateFlow()

    private val _lastActionLabel = MutableStateFlow("Pronto para parear via Bluetooth HID")
    val lastActionLabel: StateFlow<String> = _lastActionLabel.asStateFlow()

    private val _isHidProfileReady = MutableStateFlow(false)
    val isHidProfileReady: StateFlow<Boolean> = _isHidProfileReady.asStateFlow()

    private val _isMacroLoopRunning = MutableStateFlow(false)
    val isMacroLoopRunning: StateFlow<Boolean> = _isMacroLoopRunning.asStateFlow()

    private val _currentHidProfile = MutableStateFlow(HidProfileMode.UNIVERSAL)
    val currentHidProfile: StateFlow<HidProfileMode> = _currentHidProfile.asStateFlow()

    private val _isLowLatencyQosActive = MutableStateFlow(false)
    val isLowLatencyQosActive: StateFlow<Boolean> = _isLowLatencyQosActive.asStateFlow()

    @Volatile
    private var pendingQosRegistration: Boolean = false
    @Volatile
    private var forceStandardNoQos: Boolean = false

    @Volatile
    var onMacroActionRecorded: ((action: String, value: String) -> Unit)? = null

    init {
        // Ordered keyboard/control consumer loop
        scope.launch {
            for (cmd in keyCommandChannel) {
                runCatching { cmd() }
            }
        }
        // 125 Hz (8 ms) coalescing dispatcher loop for smooth mouse & air mouse
        scope.launch {
            while (isActive) {
                mouseCoalescer.flushNow()
                delay(8L)
            }
        }
        registerBluetoothStateReceiver()
    }

    companion object {
        @Volatile
        private var activeInstance: BluetoothHidManager? = null

        fun getInstance(context: Context): BluetoothHidManager =
            activeInstance ?: synchronized(this) {
                activeInstance ?: BluetoothHidManager(context.applicationContext).also {
                    activeInstance = it
                }
            }

        fun disconnectActive() {
            activeInstance?.disconnect()
        }

        /**
         * Pure shortcut adaptation helper for Windows, macOS, iPhone/iPad (iOS), Linux, and Android.
         */
        fun adaptShortcutForOs(shortcut: String, os: TargetOsProfile): String {
            val upper = shortcut.uppercase().trim()
            return when (os) {
                TargetOsProfile.MACOS -> when (upper) {
                    "CTRL+C" -> "CMD+C"
                    "CTRL+V" -> "CMD+V"
                    "CTRL+X" -> "CMD+X"
                    "CTRL+Z" -> "CMD+Z"
                    "CTRL+Y" -> "CMD+SHIFT+Z"
                    "CTRL+S" -> "CMD+S"
                    "CTRL+A" -> "CMD+A"
                    "CTRL+T" -> "CMD+T"
                    "CTRL+W" -> "CMD+W"
                    "CTRL+R" -> "CMD+R"
                    "CTRL+MINUS" -> "CMD+MINUS"
                    "CTRL+0" -> "CMD+0"
                    "CTRL+EQUAL" -> "CMD+EQUAL"
                    "CTRL+SHIFT+T" -> "CMD+SHIFT+T"
                    "CTRL+L" -> "CMD+L"
                    "ALT+TAB" -> "CMD+TAB"
                    "ALT+F4" -> "CMD+Q"
                    "WIN+D" -> "CMD+F3"
                    "WIN+TAB" -> "CTRL+UP"
                    "WIN+L" -> "CTRL+CMD+Q"
                    "ALT+LEFT" -> "CMD+LEFT"
                    "ALT+RIGHT" -> "CMD+RIGHT"
                    "CTRL+WIN+LEFT" -> "CTRL+LEFT"
                    "CTRL+WIN+RIGHT" -> "CTRL+RIGHT"
                    else -> upper
                }
                TargetOsProfile.IOS -> when (upper) {
                    "CTRL+C" -> "CMD+C"
                    "CTRL+V" -> "CMD+V"
                    "CTRL+X" -> "CMD+X"
                    "CTRL+Z" -> "CMD+Z"
                    "CTRL+Y" -> "CMD+SHIFT+Z"
                    "CTRL+S" -> "CMD+S"
                    "CTRL+A" -> "CMD+A"
                    "CTRL+T" -> "CMD+T"
                    "CTRL+W" -> "CMD+W"
                    "CTRL+R" -> "CMD+R"
                    "CTRL+L" -> "CMD+L"
                    "CTRL+SHIFT+T" -> "CMD+SHIFT+T"
                    "ALT+TAB" -> "CMD+TAB"
                    "WIN+D" -> "CMD+H"       // iOS/iPadOS Home Screen
                    "WIN+TAB" -> "CMD+TAB"   // App Switcher
                    "WIN+S", "CTRL+F" -> "CMD+SPACE" // Spotlight Search
                    "ALT+F4" -> "CMD+H"
                    "ALT+LEFT" -> "CMD+LEFT"
                    "ALT+RIGHT" -> "CMD+RIGHT"
                    "WIN+L" -> "CTRL+CMD+Q"
                    else -> upper
                }
                else -> upper
            }
        }
    }

    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                            connectedDevice = null
                            isHidAppRegistered = false
                            _isHidProfileReady.value = false
                            _connectionState.value = BluetoothConnectionState.BluetoothOff
                            _pairedDevices.value = emptyList()
                            BluetoothHidForegroundService.stop(context)
                        }
                        BluetoothAdapter.STATE_ON -> {
                            _connectionState.value = BluetoothConnectionState.Disconnected
                            initialize()
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPairedDevices()
                }
            }
        }
    }

    private fun registerBluetoothStateReceiver() {
        if (!receiverRegistered.compareAndSet(false, true)) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        runCatching {
            ContextCompat.registerReceiver(
                context,
                bluetoothBroadcastReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun setHidProfile(profile: HidProfileMode) {
        if (activeHidProfile == profile && isHidAppRegistered) return
        activeHidProfile = profile
        _currentHidProfile.value = profile
        val device = hidDevice
        if (device != null && hasBluetoothPermissions()) {
            scope.launch {
                runCatching { device.unregisterApp() }
                isHidAppRegistered = false
                _isHidProfileReady.value = false
                delay(180)
                registerHidApp()
            }
        }
    }

    fun initialize(preferredProfile: HidProfileMode? = null) {
        if (preferredProfile != null) {
            activeHidProfile = preferredProfile
            _currentHidProfile.value = preferredProfile
        }
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionState.value = BluetoothConnectionState.Unsupported
            return
        }
        if (!hasBluetoothPermissions()) {
            _connectionState.value = BluetoothConnectionState.PermissionRequired
            return
        }
        if (!adapter.isEnabled) {
            _connectionState.value = BluetoothConnectionState.BluetoothOff
            return
        }
        refreshPairedDevices()
        if (hidDevice == null) {
            setupHidProfileProxy()
        } else if (!isHidAppRegistered) {
            registerHidApp()
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null || !hasBluetoothPermissions() || !adapter.isEnabled) {
            _pairedDevices.value = emptyList()
            return
        }
        val connectedAddr = connectedDevice?.address
        val bonded = runCatching { adapter.bondedDevices.orEmpty() }.getOrDefault(emptySet())
        _pairedDevices.value = bonded.map { device ->
            val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: "Dispositivo ${device.address.takeLast(5)}"
            PairedDeviceUi(
                name = name,
                address = device.address,
                category = inferCategory(device, name),
                isConnected = device.address == connectedAddr &&
                    _connectionState.value is BluetoothConnectionState.Connected,
            )
        }.sortedByDescending { it.isConnected }
    }

    @SuppressLint("MissingPermission")
    private fun inferCategory(device: BluetoothDevice, name: String): DeviceCategory {
        val major = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
        val lower = name.lowercase()
        return when {
            lower.contains("tv") || lower.contains("chromecast") || lower.contains("fire") ||
                lower.contains("shield") || lower.contains("mi box") || major == BluetoothClass.Device.Major.AUDIO_VIDEO ->
                DeviceCategory.TV_MEDIA
            major == BluetoothClass.Device.Major.COMPUTER || lower.contains("pc") ||
                lower.contains("mac") || lower.contains("desktop") || lower.contains("laptop") ||
                lower.contains("windows") || lower.contains("linux") ->
                DeviceCategory.COMPUTER
            major == BluetoothClass.Device.Major.PHONE || lower.contains("iphone") || lower.contains("ipad") ->
                DeviceCategory.PHONE_TABLET
            else -> DeviceCategory.OTHER
        }
    }

    fun runDiagnostics(): List<HidDiagnosticItem> {
        val items = mutableListOf<HidDiagnosticItem>()
        items.add(
            HidDiagnosticItem(
                label = "Android HID Nativo (API 28+)",
                status = "Compatível (Android API ${Build.VERSION.SDK_INT})",
                isHealthy = true,
            )
        )
        val adapter = bluetoothAdapter
        items.add(
            HidDiagnosticItem(
                label = "Hardware e Rádio Bluetooth",
                status = when {
                    adapter == null -> "Ausente"
                    adapter.isEnabled -> "Ativado"
                    else -> "Desligado"
                },
                isHealthy = adapter != null && adapter.isEnabled,
                recommendation = if (adapter == null || !adapter.isEnabled) "Ative o Bluetooth no aparelho." else "",
            )
        )
        val permsOk = hasBluetoothPermissions()
        items.add(
            HidDiagnosticItem(
                label = "Permissões Bluetooth",
                status = if (permsOk) "Concedidas" else "Pendentes",
                isHealthy = permsOk,
                recommendation = if (permsOk) "" else "Autorize 'Dispositivos por perto' nas permissões do app.",
            )
        )
        val descriptorValid = HidConstants.validateDescriptor(HidConstants.descriptorForProfile(activeHidProfile))
        items.add(
            HidDiagnosticItem(
                label = "Perfil HID (${activeHidProfile.shortLabel})",
                status = if (isHidAppRegistered) "Registrado • Descritor válido" else "Aguardando registro",
                isHealthy = isHidAppRegistered && descriptorValid,
                recommendation = if (isHidAppRegistered) "" else "Tente o perfil 'Padrão Universal' ou reinicie o Bluetooth (algumas ROMs MIUI/HyperOS exigem isso).",
            )
        )
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeCaps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val vpnActive = activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        items.add(
            HidDiagnosticItem(
                label = "Conflito com VPN / Rede Local",
                status = if (vpnActive) "VPN ativa detectada" else "Sem VPN bloqueando",
                isHealthy = !vpnActive,
                recommendation = if (vpnActive) "VPNs podem bloquear o Companion Wi-Fi e Wake-on-LAN (LAN)." else "",
            )
        )
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBattery = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        val samsungProfile = SamsungCompatibilityInspector.inspect(context)
        items.add(
            HidDiagnosticItem(
                label = if (samsungProfile.isSamsung) "One UI Device Care / Bateria" else "Otimização de Bateria",
                status = if (ignoringBattery) "Sem restrições (Irrestrito)" else "Otimizado pelo sistema",
                isHealthy = ignoringBattery,
                recommendation = if (ignoringBattery) "" else SamsungCompatibilityInspector.buildOneUiDeviceCareGuide(samsungProfile.semPlatformInt),
            )
        )
        if (samsungProfile.isS23Ultra || samsungProfile.hasFastConnect7800) {
            items.add(
                HidDiagnosticItem(
                    label = "Rádio S23 Ultra (FastConnect 7800 / QoS 125Hz)",
                    status = if (_isLowLatencyQosActive.value) "QoS 8ms ativo (${samsungProfile.oneUiLabel})" else "Modo HID padrão ativo",
                    isHealthy = true,
                    recommendation = if (_isLowLatencyQosActive.value) "" else "Reative o Bluetooth caso queira reaplicar QoS de baixa latência.",
                )
            )
        }
        return items
    }

    fun buildDiagnosticsTextReport(): String {
        val list = runDiagnostics()
        val samsungProfile = SamsungCompatibilityInspector.inspect(context)
        return buildString {
            appendLine("=== Controle BT — Relatório de Diagnóstico ===")
            appendLine("Android SDK: API ${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.MODEL})")
            appendLine("Sistema / Chip: ${samsungProfile.oneUiLabel} | ${samsungProfile.chipSummary}")
            appendLine("Tela: ${samsungProfile.activeRefreshRateHz.toInt()}Hz (máx ${samsungProfile.maxRefreshRateHz.toInt()}Hz) | DeX: ${if (samsungProfile.isDexActive) samsungProfile.dexSourceLabel else "Não"}")
            appendLine("Perfil HID ativo: ${activeHidProfile.label} | QoS 8ms: ${_isLowLatencyQosActive.value}")
            appendLine("Status conexão: ${connectionState.value::class.simpleName}")
            appendLine("---")
            list.forEach { item ->
                val mark = if (item.isHealthy) "[OK]" else "[ATENÇÃO]"
                appendLine("$mark ${item.label}: ${item.status}")
                if (item.recommendation.isNotBlank()) {
                    appendLine("    Dica: ${item.recommendation}")
                }
            }
        }
    }

    private fun setupHidProfileProxy() {
        val adapter = bluetoothAdapter ?: return
        runCatching {
            adapter.getProfileProxy(context, profileServiceListener, BluetoothProfile.HID_DEVICE)
        }.onFailure { e ->
            Log.e(tag, "Failed to bind HID_DEVICE profile proxy", e)
        }
    }

    private val profileServiceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE && proxy is BluetoothHidDevice) {
                hidDevice = proxy
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                isHidAppRegistered = false
                _isHidProfileReady.value = false
                connectedDevice = null
                _connectionState.value = BluetoothConnectionState.Disconnected
                BluetoothHidForegroundService.stop(context)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val device = hidDevice ?: return
        if (!hasBluetoothPermissions()) return
        val profile = activeHidProfile
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Controle BT",
            profile.label,
            "Controle BT",
            HidConstants.subclassForProfile(profile),
            HidConstants.descriptorForProfile(profile),
        )
        val deviceProfile = SamsungCompatibilityInspector.inspect(context)
        val shouldTryQos = !forceStandardNoQos && (deviceProfile.hasFastConnect7800 || deviceProfile.isSamsung)
        val outQos = if (shouldTryQos) {
            BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                12800,
                640,
                25600,
                8000, // 8 ms window matching 125Hz MouseDeltaCoalescer & 120Hz LTPO frame budget
                4000,
            )
        } else {
            null
        }
        val inQos = if (shouldTryQos) {
            BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800,
                32,
                1600,
                10000,
                10000,
            )
        } else {
            null
        }

        pendingQosRegistration = shouldTryQos
        val accepted = runCatching {
            device.registerApp(sdpSettings, inQos, outQos, hidExecutor, hidCallback)
        }.onFailure { e ->
            Log.e(tag, "registerApp failed", e)
        }.getOrDefault(false)

        if (!accepted && shouldTryQos) {
            // Immediate safe fallback without QoS if controller rejects L2CAP QoS tuning
            pendingQosRegistration = false
            forceStandardNoQos = true
            _isLowLatencyQosActive.value = false
            runCatching {
                device.registerApp(sdpSettings, null, null, hidExecutor, hidCallback)
            }.onFailure { e ->
                Log.e(tag, "registerApp standard fallback failed", e)
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isHidAppRegistered = registered
            _isHidProfileReady.value = registered
            if (registered) {
                _isLowLatencyQosActive.value = pendingQosRegistration
                val qosTag = if (pendingQosRegistration) " • QoS 8ms" else ""
                _lastActionLabel.value = "Perfil HID ativo (${activeHidProfile.shortLabel}$qosTag)"
            } else if (pendingQosRegistration && !forceStandardNoQos) {
                pendingQosRegistration = false
                forceStandardNoQos = true
                _isLowLatencyQosActive.value = false
                registerHidApp()
            } else {
                _isLowLatencyQosActive.value = false
                _lastActionLabel.value = "Perfil HID recusado pelo sistema — tente Padrão Universal"
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val deviceName = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: device.address
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _connectionState.value = BluetoothConnectionState.Connected(deviceName, device.address)
                    _lastActionLabel.value = "Conectado a $deviceName"
                    BluetoothHidForegroundService.start(context, deviceName)
                    refreshPairedDevices()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = BluetoothConnectionState.Connecting(deviceName, device.address)
                    _lastActionLabel.value = "Conectando a $deviceName..."
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device.address) {
                        connectedDevice = null
                    }
                    mouseCoalescer.clear()
                    activeMouseButtons = HidConstants.MOUSE_NONE
                    _connectionState.value = BluetoothConnectionState.Disconnected
                    _lastActionLabel.value = "Desconectado de $deviceName"
                    BluetoothHidForegroundService.stop(context)
                    refreshPairedDevices()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String): Boolean {
        val normalized = address.trim().uppercase()
        if (!BluetoothAdapter.checkBluetoothAddress(normalized)) {
            _lastActionLabel.value = "Endereço MAC inválido: $address"
            return false
        }
        if (!hasBluetoothPermissions()) {
            initialize()
            return false
        }
        val adapter = bluetoothAdapter ?: return false
        val device = runCatching { adapter.getRemoteDevice(normalized) }.getOrNull() ?: return false
        val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: normalized
        val hid = hidDevice
        if (hid == null || !isHidAppRegistered) {
            initialize()
            _lastActionLabel.value = "Ativando perfil HID... tente novamente em 1s"
            return false
        }
        val previous = connectedDevice
        if (previous != null && previous.address != normalized) {
            runCatching { hid.disconnect(previous) }
        }
        _connectionState.value = BluetoothConnectionState.Connecting(name, normalized)
        return runCatching {
            hid.connect(device)
        }.onFailure { e ->
            Log.e(tag, "HID connect error", e)
            _connectionState.value = BluetoothConnectionState.Disconnected
            _lastActionLabel.value = "Falha ao conectar a $name"
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val device = connectedDevice
        if (hasBluetoothPermissions() && device != null) {
            runCatching { hidDevice?.disconnect(device) }
        }
        connectedDevice = null
        activeMouseButtons = HidConstants.MOUSE_NONE
        mouseCoalescer.clear()
        _connectionState.value = BluetoothConnectionState.Disconnected
        _lastActionLabel.value = "Conexão encerrada"
        BluetoothHidForegroundService.stop(context)
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun sendReport(reportId: Int, data: ByteArray): Boolean {
        val device = connectedDevice ?: return false
        val hid = hidDevice ?: return false
        if (!hasBluetoothPermissions()) return false
        return runCatching { hid.sendReport(device, reportId, data) }.getOrDefault(false)
    }

    // ==================== MOUSE (REPORT ID 2: 125 Hz COALESCED) ====================

    fun setMouseButtonState(buttonMask: Int, pressed: Boolean) {
        activeMouseButtons = if (pressed) {
            activeMouseButtons or buttonMask
        } else {
            activeMouseButtons and buttonMask.inv()
        }
        // Flush any accumulated cursor delta immediately and send button transition off-main-thread
        scope.launch {
            mouseCoalescer.flushNow()
            sendMouseReportDirect(0, 0, 0, 0)
        }
        _lastActionLabel.value = when (buttonMask) {
            HidConstants.MOUSE_LEFT -> if (pressed) "Clique Esq. (segurar)" else "Soltou Esq."
            HidConstants.MOUSE_RIGHT -> if (pressed) "Clique Dir." else "Soltou Dir."
            HidConstants.MOUSE_MIDDLE -> if (pressed) "Clique Central" else "Soltou Central"
            HidConstants.MOUSE_BACK -> "Mouse 4 (Voltar)"
            HidConstants.MOUSE_FORWARD -> "Mouse 5 (Avançar)"
            else -> "Mouse"
        }
    }

    fun sendMouseClick(buttonMask: Int) {
        scope.launch {
            mouseCoalescer.flushNow()
            val prev = activeMouseButtons
            activeMouseButtons = prev or buttonMask
            sendMouseReportDirect(0, 0, 0, 0)
            delay(26)
            activeMouseButtons = prev and buttonMask.inv()
            sendMouseReportDirect(0, 0, 0, 0)
        }
        val label = when (buttonMask) {
            HidConstants.MOUSE_LEFT -> "Clique Esquerdo"
            HidConstants.MOUSE_RIGHT -> "Clique Direito"
            HidConstants.MOUSE_MIDDLE -> "Clique Central"
            HidConstants.MOUSE_BACK -> "Mouse 4 (Voltar)"
            HidConstants.MOUSE_FORWARD -> "Mouse 5 (Avançar)"
            else -> "Clique"
        }
        _lastActionLabel.value = label
        onMacroActionRecorded?.invoke(
            "MOUSE",
            when (buttonMask) {
                HidConstants.MOUSE_RIGHT -> "RIGHT_CLICK"
                HidConstants.MOUSE_MIDDLE -> "MIDDLE_CLICK"
                else -> "LEFT_CLICK"
            }
        )
    }

    fun sendMouseMove(dx: Int, dy: Int, wheel: Int = 0, hWheel: Int = 0) {
        if (dx == 0 && dy == 0 && wheel == 0 && hWheel == 0) return
        mouseCoalescer.enqueue(dx, dy, wheel, hWheel)
    }

    private fun sendMouseReportDirect(dx: Int, dy: Int, vWheel: Int, hWheel: Int) {
        val report = byteArrayOf(
            activeMouseButtons.toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            vWheel.coerceIn(-127, 127).toByte(),
            hWheel.coerceIn(-127, 127).toByte(),
        )
        sendReport(HidConstants.REPORT_ID_MOUSE, report)
    }

    fun sendPinchZoom(zoomIn: Boolean, os: TargetOsProfile = TargetOsProfile.WINDOWS) {
        val mod = if (os == TargetOsProfile.MACOS || os == TargetOsProfile.IOS) {
            HidConstants.MOD_LGUI
        } else {
            HidConstants.MOD_LCTRL
        }
        keyCommandChannel.trySend {
            val down = byteArrayOf(mod.toByte(), 0, 0, 0, 0, 0, 0, 0)
            sendReport(HidConstants.REPORT_ID_KEYBOARD, down)
            delay(12)
            sendMouseReportDirect(0, 0, if (zoomIn) 2 else -2, 0)
            delay(16)
            sendReport(HidConstants.REPORT_ID_KEYBOARD, ByteArray(8))
        }
        _lastActionLabel.value = if (zoomIn) "Pinça • Zoom +" else "Pinça • Zoom -"
    }

    // ==================== KEYBOARD (REPORT ID 1: SERIALIZED QUEUE) ====================

    fun sendKeyPress(modifier: Int, keyCode: Int, label: String? = null) {
        keyCommandChannel.trySend {
            sendKeyPressSuspend(modifier, keyCode)
        }
        if (label != null) {
            _lastActionLabel.value = "Tecla: $label"
            onMacroActionRecorded?.invoke("KEY", label.uppercase())
        }
    }

    suspend fun sendKeyPressSuspend(modifier: Int, keyCode: Int) {
        val down = byteArrayOf(
            modifier.toByte(),
            0x00,
            keyCode.toByte(),
            0x00, 0x00, 0x00, 0x00, 0x00,
        )
        val up = ByteArray(8)
        sendReport(HidConstants.REPORT_ID_KEYBOARD, down)
        delay(16)
        sendReport(HidConstants.REPORT_ID_KEYBOARD, up)
        delay(12)
    }

    fun sendKeyRepeatBackspace(count: Int) {
        if (count <= 0) return
        keyCommandChannel.trySend {
            repeat(count) {
                sendKeyPressSuspend(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)
            }
        }
    }

    fun sendText(text: String, layoutId: String, appendEnter: Boolean = false) {
        if (text.isEmpty() && !appendEnter) return
        _lastActionLabel.value = if (text.length > 22) {
            "Enviando texto (${text.length} caracteres)"
        } else {
            "Digitado: \"$text\""
        }
        if (text.isNotEmpty()) {
            onMacroActionRecorded?.invoke("TEXT", text)
        }
        keyCommandChannel.trySend {
            sendTextSuspend(text, layoutId, appendEnter)
        }
    }

    fun enqueueOrderedKeyboardAction(block: suspend () -> Unit) {
        keyCommandChannel.trySend(block)
    }

    suspend fun sendTextSuspend(text: String, layoutId: String, appendEnter: Boolean = false) {
        for (ch in text) {
            val strokes = KeyboardLayouts.charToStrokes(ch, layoutId)
            for (stroke in strokes) {
                sendKeyPressSuspend(stroke.modifier, stroke.keyCode)
            }
        }
        if (appendEnter) {
            sendKeyPressSuspend(HidConstants.MOD_NONE, HidConstants.KEY_ENTER)
        }
    }

    fun adaptShortcutForOs(shortcut: String, os: TargetOsProfile): String =
        Companion.adaptShortcutForOs(shortcut, os)

    fun sendAdaptiveShortcut(shortcut: String, os: TargetOsProfile, label: String? = null) {
        val adapted = adaptShortcutForOs(shortcut, os)
        sendShortcutString(adapted, label ?: "$adapted (${os.shortTag})")
    }

    fun sendShortcutString(shortcut: String, label: String? = null) {
        val parts = shortcut.uppercase().split("+").map { it.trim() }.filter { it.isNotEmpty() }
        var modifier = HidConstants.MOD_NONE
        var keyCode = HidConstants.KEY_NONE
        for (part in parts) {
            when (part) {
                "CTRL", "CONTROL" -> modifier = modifier or HidConstants.MOD_LCTRL
                "SHIFT" -> modifier = modifier or HidConstants.MOD_LSHIFT
                "ALT" -> modifier = modifier or HidConstants.MOD_LALT
                "ALTGR" -> modifier = modifier or HidConstants.MOD_RALT
                "WIN", "CMD", "GUI", "SUPER" -> modifier = modifier or HidConstants.MOD_LGUI
                else -> keyCode = KeyboardLayouts.keyNameToUsageId(part)
            }
        }
        if (keyCode != HidConstants.KEY_NONE || modifier != HidConstants.MOD_NONE) {
            keyCommandChannel.trySend { sendKeyPressSuspend(modifier, keyCode) }
            _lastActionLabel.value = label ?: shortcut
            onMacroActionRecorded?.invoke("SHORTCUT", shortcut.uppercase())
        }
    }

    // ==================== CONSUMER & SYSTEM POWER (REPORTS 3 & 6) ====================

    fun sendConsumerControl(usageBitMask: Int, label: String) {
        _lastActionLabel.value = label
        keyCommandChannel.trySend {
            val b0 = (usageBitMask and 0xFF).toByte()
            val b1 = ((usageBitMask shr 8) and 0xFF).toByte()
            val b2 = ((usageBitMask shr 16) and 0xFF).toByte()
            sendReport(HidConstants.REPORT_ID_CONSUMER, byteArrayOf(b0, b1, b2))
            delay(36)
            sendReport(HidConstants.REPORT_ID_CONSUMER, byteArrayOf(0x00, 0x00, 0x00))
        }
    }

    fun sendSystemPowerControl(systemBitMask: Int, label: String) {
        _lastActionLabel.value = label
        keyCommandChannel.trySend {
            sendReport(HidConstants.REPORT_ID_SYSTEM, byteArrayOf(systemBitMask.toByte()))
            delay(40)
            sendReport(HidConstants.REPORT_ID_SYSTEM, byteArrayOf(0x00))
        }
    }

    // ==================== NATIVE HID GAMEPAD (REPORT ID 4) ====================

    fun sendGamepadReport(
        buttonsMask: Int,
        hatSwitch: Int,
        leftX: Int,
        leftY: Int,
        rightX: Int,
        rightY: Int,
        l2Trigger: Int,
        r2Trigger: Int,
    ) {
        val report = byteArrayOf(
            (buttonsMask and 0xFF).toByte(),
            ((buttonsMask shr 8) and 0xFF).toByte(),
            (hatSwitch and 0x0F).toByte(),
            leftX.coerceIn(-127, 127).toByte(),
            leftY.coerceIn(-127, 127).toByte(),
            rightX.coerceIn(-127, 127).toByte(),
            rightY.coerceIn(-127, 127).toByte(),
            l2Trigger.coerceIn(0, 255).toByte(),
            r2Trigger.coerceIn(0, 255).toByte(),
        )
        scope.launch {
            sendReport(HidConstants.REPORT_ID_GAMEPAD, report)
        }
    }

    // ==================== PRO HID DIGITIZER PEN (REPORT ID 5) ====================

    fun sendDigitizerReport(
        tipDown: Boolean,
        barrelPressed: Boolean,
        eraserActive: Boolean,
        inRangeHover: Boolean,
        normX: Float,
        normY: Float,
        pressure: Float,
        tiltXDeg: Int = 0,
        tiltYDeg: Int = 0,
    ) {
        var flags = 0
        if (tipDown && !eraserActive) flags = flags or HidConstants.PEN_TIP_SWITCH
        if (barrelPressed) flags = flags or HidConstants.PEN_BARREL_SWITCH
        if (eraserActive && tipDown) flags = flags or HidConstants.PEN_ERASER
        if (eraserActive) flags = flags or HidConstants.PEN_INVERT
        if (inRangeHover) flags = flags or HidConstants.PEN_IN_RANGE

        val absX = (normX.coerceIn(0f, 1f) * 32767f).toInt()
        val absY = (normY.coerceIn(0f, 1f) * 32767f).toInt()
        val pres = (pressure.coerceIn(0f, 1f) * 1023f).toInt()
        val tx = tiltXDeg.coerceIn(-90, 90)
        val ty = tiltYDeg.coerceIn(-90, 90)

        val report = byteArrayOf(
            flags.toByte(),
            (absX and 0xFF).toByte(),
            ((absX shr 8) and 0xFF).toByte(),
            (absY and 0xFF).toByte(),
            ((absY shr 8) and 0xFF).toByte(),
            (pres and 0xFF).toByte(),
            ((pres shr 8) and 0xFF).toByte(),
            tx.toByte(),
            ty.toByte(),
        )
        scope.launch {
            sendReport(HidConstants.REPORT_ID_DIGITIZER, report)
        }
    }

    // ==================== MACROS & LOOPS ====================

    fun stopMacroLoop() {
        macroJob?.cancel()
        macroJob = null
        _isMacroLoopRunning.value = false
        _lastActionLabel.value = "Loop de macro parado"
    }

    fun executeMacro(
        steps: List<MacroStep>,
        layoutId: String,
        macroName: String,
        repeatCount: Int = 1,
    ) {
        macroJob?.cancel()
        _lastActionLabel.value = if (repeatCount == 0) "Macro em loop: $macroName" else "Macro: $macroName"
        _isMacroLoopRunning.value = repeatCount == 0 || repeatCount > 1

        macroJob = scope.launch {
            var iteration = 0
            try {
                while (isActive && (repeatCount == 0 || iteration < repeatCount)) {
                    for (step in steps) {
                        if (!isActive) break
                        when (step.action.uppercase()) {
                            "DELAY" -> {
                                val ms = step.value.toLongOrNull()?.coerceIn(20L, 10000L) ?: 150L
                                delay(ms)
                            }
                            "TEXT" -> sendTextSuspend(step.value, layoutId, appendEnter = false)
                            "KEY" -> {
                                val code = KeyboardLayouts.keyNameToUsageId(step.value)
                                if (code != HidConstants.KEY_NONE) {
                                    sendKeyPressSuspend(HidConstants.MOD_NONE, code)
                                }
                            }
                            "MOUSE" -> {
                                val mask = when (step.value.uppercase()) {
                                    "RIGHT_CLICK" -> HidConstants.MOUSE_RIGHT
                                    "MIDDLE_CLICK" -> HidConstants.MOUSE_MIDDLE
                                    else -> HidConstants.MOUSE_LEFT
                                }
                                activeMouseButtons = activeMouseButtons or mask
                                sendMouseReportDirect(0, 0, 0, 0)
                                delay(26)
                                activeMouseButtons = activeMouseButtons and mask.inv()
                                sendMouseReportDirect(0, 0, 0, 0)
                            }
                            "SHORTCUT" -> {
                                val parts = step.value.uppercase().split("+").map { it.trim() }
                                var mod = HidConstants.MOD_NONE
                                var key = HidConstants.KEY_NONE
                                for (p in parts) {
                                    when (p) {
                                        "CTRL", "CONTROL" -> mod = mod or HidConstants.MOD_LCTRL
                                        "SHIFT" -> mod = mod or HidConstants.MOD_LSHIFT
                                        "ALT" -> mod = mod or HidConstants.MOD_LALT
                                        "WIN", "CMD", "GUI" -> mod = mod or HidConstants.MOD_LGUI
                                        else -> key = KeyboardLayouts.keyNameToUsageId(p)
                                    }
                                }
                                if (key != HidConstants.KEY_NONE || mod != HidConstants.MOD_NONE) {
                                    sendKeyPressSuspend(mod, key)
                                }
                            }
                        }
                    }
                    iteration++
                    if (repeatCount == 0 || iteration < repeatCount) {
                        delay(180)
                    }
                }
            } finally {
                _isMacroLoopRunning.value = false
            }
        }
    }

    fun sendLayoutAutoTestProbe(layoutId: String) {
        _lastActionLabel.value = "Enviando teste automático de layout ($layoutId)"
        keyCommandChannel.trySend {
            sendTextSuspend("azAZ 09 @ç~´/?", layoutId, appendEnter = true)
        }
    }
}
