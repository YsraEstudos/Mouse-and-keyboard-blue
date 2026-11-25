package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppSettings
import com.example.data.ControlMode
import com.example.data.ScreenOrientationMode
import com.example.hid.HidConstants
import com.example.system.SamsungCompatibilityInspector
import com.example.system.SamsungDeviceProfile
import com.example.ui.components.DisconnectedStatusBanner
import com.example.ui.components.OnboardingWizardDialog
import com.example.ui.components.RemoteNavigationDrawerContent
import com.example.ui.components.RemoteNavigationRail
import com.example.ui.components.RemoteTopBar
import com.example.ui.components.TactileKeyButton
import com.example.ui.screens.BarcodeScannerScreen
import com.example.ui.screens.CustomLayoutsScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.DigitizerScreen
import com.example.ui.screens.GamepadScreen
import com.example.ui.screens.MouseKeyboardScreen
import com.example.ui.screens.MultimediaScreen
import com.example.ui.screens.NumpadScreen
import com.example.ui.screens.PcCompanionScreen
import com.example.ui.screens.PcKeyboardScreen
import com.example.ui.screens.PresenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.ControleBtTheme
import com.example.viewmodel.RemoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: RemoteViewModel by viewModels()
    private var isInPipModeState by mutableStateOf(false)
    private var lastTouchTimestamp by mutableLongStateOf(System.currentTimeMillis())
    private var isScreenDimmed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()

            LaunchedEffect(
                settings.keepScreenOn,
                settings.showOverLockScreen,
                settings.startFullscreen,
                settings.screenOrientation,
                deviceProfile.isDexActive,
            ) {
                applyWindowPreferences(settings, deviceProfile)
            }

            // OLED Saver Auto-Dimming on Inactivity (suppressed automatically when running on Samsung DeX)
            LaunchedEffect(
                settings.oledSaverEnabled,
                settings.oledDimAfterSeconds,
                lastTouchTimestamp,
                deviceProfile.isDexActive,
            ) {
                if (!settings.oledSaverEnabled || SamsungCompatibilityInspector.shouldSuppressOledDimForDeX(deviceProfile.isDexActive)) {
                    restoreScreenBrightness()
                } else {
                    restoreScreenBrightness()
                    delay(settings.oledDimAfterSeconds.coerceIn(10, 120) * 1000L)
                    dimScreenForOledSaver()
                }
            }

            ControleBtTheme(
                themeMode = settings.themeMode,
                accentPalette = settings.accentPalette,
                tronStyleEnabled = settings.tronStyleEnabled,
                oledSaverEnabled = settings.oledSaverEnabled,
            ) {
                if (isInPipModeState) {
                    PipMiniRemoteOverlay(viewModel = viewModel)
                } else {
                    ControleBtAppRoot(
                        viewModel = viewModel,
                        settings = settings,
                        onEnterPip = { enterFloatingPipMode() },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setAppInForeground(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setAppInForeground(false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.refreshDeviceProfile(newConfig)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastTouchTimestamp >= 1500L) {
            lastTouchTimestamp = now
        }
        if (isScreenDimmed) {
            restoreScreenBrightness()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
    }

    private fun enterFloatingPipMode() {
        runCatching {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 10))
                .build()
            enterPictureInPictureMode(params)
        }.onFailure {
            viewModel.showBanner("Modo PiP indisponível neste aparelho")
        }
    }

    private fun dimScreenForOledSaver() {
        runCatching {
            val lp = window.attributes
            if (lp.screenBrightness != 0.01f) {
                lp.screenBrightness = 0.01f
                window.attributes = lp
            }
            isScreenDimmed = true
        }
    }

    private fun restoreScreenBrightness() {
        runCatching {
            val lp = window.attributes
            if (lp.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
            isScreenDimmed = false
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            // Android Share Sheet ("Compartilhar -> Controle BT")
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                    if (sharedText.isNotBlank()) {
                        viewModel.handleIncomingSharedText(sharedText)
                    }
                }
            }
            // Tasker / MacroDroid explicit Automation Broadcast/Activity Intent
            "com.example.action.AUTOMATION" -> {
                viewModel.handleAutomationTrigger(
                    deviceMac = intent.getStringExtra("mac"),
                    modeRoute = intent.getStringExtra("mode"),
                    macroName = intent.getStringExtra("macro"),
                    textToType = intent.getStringExtra("text"),
                )
            }
            // NFC Tag NDEF or Deep Link: controlebt://connect?mac=...&mode=...&macro=...&text=...
            Intent.ACTION_VIEW, "android.nfc.action.NDEF_DISCOVERED" -> {
                val uri = intent.data
                if (uri != null && uri.scheme.equals("controlebt", ignoreCase = true)) {
                    viewModel.handleAutomationTrigger(
                        deviceMac = uri.getQueryParameter("mac"),
                        modeRoute = uri.getQueryParameter("mode"),
                        macroName = uri.getQueryParameter("macro"),
                        textToType = uri.getQueryParameter("text"),
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (viewModel.handleHardwareVolumeKey(isVolumeUp = true)) true
                else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (viewModel.handleHardwareVolumeKey(isVolumeUp = false)) true
                else super.onKeyDown(keyCode, event)
            }
            else -> {
                if (viewModel.handleSPenRemoteKeyEvent(keyCode)) true
                else super.onKeyDown(keyCode, event)
            }
        }
    }

    private fun applyWindowPreferences(settings: AppSettings, deviceProfile: SamsungDeviceProfile) {
        if (settings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setShowWhenLocked(settings.showOverLockScreen)

        // Android 16 Large-Screen & Samsung DeX Policy: never lock orientation on large screens or desktop mode
        val isLargeScreen = resources.configuration.smallestScreenWidthDp >= 600 || deviceProfile.isDexActive
        val desiredOrientation = if (isLargeScreen) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            when (settings.screenOrientation) {
                ScreenOrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
        if (requestedOrientation != desiredOrientation) {
            requestedOrientation = desiredOrientation
        }

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (settings.startFullscreen && !deviceProfile.isDexActive) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun PipMiniRemoteOverlay(viewModel: RemoteViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Controle BT • Flutuante", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TactileKeyButton("Vol-", { viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_DOWN, "Vol-") }, Modifier.weight(1f), Icons.AutoMirrored.Filled.VolumeDown, minHeight = 40.dp)
                TactileKeyButton("Play", { viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_PLAY_PAUSE, "Play/Pause") }, Modifier.weight(1.2f), Icons.Default.PlayArrow, active = true, minHeight = 40.dp)
                TactileKeyButton("Vol+", { viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_VOL_UP, "Vol+") }, Modifier.weight(1f), Icons.AutoMirrored.Filled.VolumeUp, minHeight = 40.dp)
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TactileKeyButton("Voltar", { viewModel.hidManager.sendConsumerControl(HidConstants.CONSUMER_BACK, "Voltar") }, Modifier.weight(1f), Icons.AutoMirrored.Filled.ArrowBack, minHeight = 40.dp)
                TactileKeyButton("Clique", { viewModel.onMouseClick(HidConstants.MOUSE_LEFT) }, Modifier.weight(1f), minHeight = 40.dp)
                TactileKeyButton("Próx.", { viewModel.hidManager.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT, "Próx.") }, Modifier.weight(1f), minHeight = 40.dp)
            }
        }
    }
}

@Composable
private fun ControleBtAppRoot(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    onEnterPip: () -> Unit,
) {
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isRecordingMacro by viewModel.isRecordingMacro.collectAsStateWithLifecycle()
    val pendingSharedText by viewModel.pendingSharedText.collectAsStateWithLifecycle()
    val pendingAutomation by viewModel.pendingAutomation.collectAsStateWithLifecycle()
    val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val requiredBtPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val onboardingPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.hidManager.initialize(settings.hidProfileMode)
        viewModel.runBluetoothDiagnostics()
    }

    BackHandler(enabled = drawerState.isOpen || currentMode != ControlMode.MOUSE_KEYBOARD) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.selectMode(ControlMode.MOUSE_KEYBOARD)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp || deviceProfile.isDexActive

        if (isExpandedScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                RemoteNavigationRail(
                    currentMode = currentMode,
                    onSelectMode = { mode -> viewModel.selectMode(mode) },
                )
                Box(modifier = Modifier.weight(1f)) {
                    MainAppScaffold(
                        viewModel = viewModel,
                        settings = settings,
                        deviceProfile = deviceProfile,
                        currentMode = currentMode,
                        connectionState = connectionState,
                        statusBanner = statusBanner,
                        isRecordingMacro = isRecordingMacro,
                        showDrawerMenuButton = false,
                        onOpenDrawer = {},
                        onEnterPip = onEnterPip,
                    )
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = currentMode != ControlMode.MOUSE_KEYBOARD &&
                    currentMode != ControlMode.PRESENTER &&
                    currentMode != ControlMode.CUSTOM_LAYOUTS &&
                    currentMode != ControlMode.GAMEPAD &&
                    currentMode != ControlMode.DIGITIZER,
                drawerContent = {
                    RemoteNavigationDrawerContent(
                        currentMode = currentMode,
                        connectionState = connectionState,
                        onSelectMode = { mode ->
                            viewModel.selectMode(mode)
                            scope.launch { drawerState.close() }
                        },
                    )
                },
            ) {
                MainAppScaffold(
                    viewModel = viewModel,
                    settings = settings,
                    deviceProfile = deviceProfile,
                    currentMode = currentMode,
                    connectionState = connectionState,
                    statusBanner = statusBanner,
                    isRecordingMacro = isRecordingMacro,
                    showDrawerMenuButton = true,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onEnterPip = onEnterPip,
                )
            }
        }
    }

    // First-Run 3-Step Onboarding Wizard
    if (!settings.onboardingCompleted) {
        OnboardingWizardDialog(
            selectedOs = settings.targetOsProfile,
            selectedHidProfile = settings.hidProfileMode,
            onSelectOs = { os -> viewModel.updateSettings { it.copy(targetOsProfile = os) } },
            onSelectHidProfile = { profile -> viewModel.setHidProfileMode(profile) },
            onRequestBluetoothPermissions = {
                if (requiredBtPermissions.isNotEmpty()) {
                    onboardingPermLauncher.launch(requiredBtPermissions)
                } else {
                    viewModel.hidManager.initialize(settings.hidProfileMode)
                }
            },
            onOpenDevicesAndFinish = {
                viewModel.completeOnboarding()
                viewModel.selectMode(ControlMode.DEVICES)
            },
            onFinish = { viewModel.completeOnboarding() },
        )
    }

    // Incoming Android Share Sheet Confirmation Dialog
    if (pendingSharedText != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSharedText() },
            title = { Text("Enviar Texto Compartilhado ao PC?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "O texto abaixo será digitado diretamente no campo ativo do computador via Bluetooth HID:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = pendingSharedText.orEmpty().take(280),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { viewModel.confirmSendSharedText(appendEnter = false) }) {
                        Text("Digitar")
                    }
                    Button(onClick = { viewModel.confirmSendSharedText(appendEnter = true) }) {
                        Text("Digitar + Enter")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSharedText() }) { Text("Cancelar") }
            },
        )
    }

    // External Deep Link / NFC / Tasker Automation Confirmation Gate
    pendingAutomation?.let { req ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingAutomation() },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.confirm_automation_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.confirm_automation_msg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (!req.deviceMac.isNullOrBlank()) {
                                Text("Dispositivo MAC: ${req.deviceMac}", style = MaterialTheme.typography.labelMedium)
                            }
                            if (!req.modeRoute.isNullOrBlank()) {
                                Text("Abrir modo: ${req.modeRoute}", style = MaterialTheme.typography.labelMedium)
                            }
                            if (!req.macroName.isNullOrBlank()) {
                                Text("Executar macro: ${req.macroName}", style = MaterialTheme.typography.labelMedium)
                            }
                            if (!req.textToType.isNullOrBlank()) {
                                Text("Digitar texto: \"${req.textToType.take(180)}\"", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmPendingAutomation() }) {
                    Text("Permitir")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingAutomation() }) {
                    Text("Bloquear")
                }
            },
        )
    }
}

@Composable
private fun MainAppScaffold(
    viewModel: RemoteViewModel,
    settings: AppSettings,
    deviceProfile: SamsungDeviceProfile,
    currentMode: ControlMode,
    connectionState: com.example.bluetooth.BluetoothConnectionState,
    statusBanner: String?,
    isRecordingMacro: Boolean,
    showDrawerMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onEnterPip: () -> Unit,
) {
    val showDisconnectedBanner = currentMode != ControlMode.DEVICES &&
        currentMode != ControlMode.SETTINGS &&
        currentMode != ControlMode.COMPANION

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            RemoteTopBar(
                currentMode = currentMode,
                connectionState = connectionState,
                airMouseActive = settings.airMouseEnabled && !deviceProfile.isDexActive,
                jigglerActive = settings.mouseJigglerEnabled,
                autoClickerActive = settings.autoClickerEnabled,
                recordingMacro = isRecordingMacro,
                osProfile = settings.targetOsProfile,
                hidProfile = settings.hidProfileMode,
                showDrawerMenuButton = showDrawerMenuButton,
                easySwitchSlots = settings.easySwitchSlots(),
                onActivateEasySwitch = { slot -> viewModel.activateEasySwitchSlot(slot) },
                onEnterPip = onEnterPip,
                onOpenDrawer = onOpenDrawer,
                onNavigateBack = if (currentMode == ControlMode.SETTINGS || currentMode == ControlMode.DEVICES) {
                    { viewModel.selectMode(ControlMode.MOUSE_KEYBOARD) }
                } else null,
                onOpenDevices = { viewModel.selectMode(ControlMode.DEVICES) },
                onOpenSettings = { viewModel.selectMode(ControlMode.SETTINGS) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (deviceProfile.isDexActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${deviceProfile.dexSourceLabel}: barra lateral ativa e Air Mouse pausado para estabilidade.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                if (showDisconnectedBanner) {
                    DisconnectedStatusBanner(
                        connectionState = connectionState,
                        onOpenDevices = { viewModel.selectMode(ControlMode.DEVICES) },
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentMode) {
                        ControlMode.MOUSE_KEYBOARD -> MouseKeyboardScreen(viewModel = viewModel, settings = settings)
                        ControlMode.MULTIMEDIA -> MultimediaScreen(viewModel = viewModel)
                        ControlMode.PC_KEYBOARD -> PcKeyboardScreen(viewModel = viewModel)
                        ControlMode.GAMEPAD -> GamepadScreen(viewModel = viewModel, settings = settings)
                        ControlMode.DIGITIZER -> DigitizerScreen(viewModel = viewModel, settings = settings)
                        ControlMode.NUMPAD -> NumpadScreen(viewModel = viewModel, settings = settings)
                        ControlMode.PRESENTER -> PresenterScreen(viewModel = viewModel, settings = settings)
                        ControlMode.SCANNER -> BarcodeScannerScreen(viewModel = viewModel, settings = settings)
                        ControlMode.CUSTOM_LAYOUTS -> CustomLayoutsScreen(viewModel = viewModel)
                        ControlMode.VAULT -> VaultScreen(viewModel = viewModel)
                        ControlMode.COMPANION -> PcCompanionScreen(viewModel = viewModel, settings = settings)
                        ControlMode.DEVICES -> DevicesScreen(viewModel = viewModel)
                        ControlMode.SETTINGS -> SettingsScreen(viewModel = viewModel, settings = settings)
                    }
                }
            }

            // Floating ephemeral status toast/banner
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp, start = 16.dp, end = 16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        text = statusBanner.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
