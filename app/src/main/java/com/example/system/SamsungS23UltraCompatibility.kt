package com.example.system

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import android.view.WindowManager

enum class ThermalHealthLevel(val label: String) {
    NORMAL("Térmica normal"),
    MODERATE("Aquecimento leve"),
    ELEVATED("Aquecimento moderado"),
    THROTTLED("Restrição térmica ativa"),
}

data class SamsungDeviceProfile(
    val manufacturer: String = Build.MANUFACTURER.orEmpty(),
    val model: String = Build.MODEL.orEmpty(),
    val deviceCodename: String = Build.DEVICE.orEmpty(),
    val socModel: String = "",
    val isSamsung: Boolean = false,
    val isS23Ultra: Boolean = false,
    val hasSnapdragon8Gen2: Boolean = false,
    val hasFastConnect7800: Boolean = false,
    val hasBleSPen: Boolean = false,
    val semPlatformInt: Int = 0,
    val oneUiLabel: String = "Android Padrão",
    val activeRefreshRateHz: Float = 60f,
    val maxRefreshRateHz: Float = 60f,
    val isLtpo120HzCapable: Boolean = false,
    val isDexActive: Boolean = false,
    val dexSourceLabel: String = "",
    val isIgnoringBatteryOptimizations: Boolean = false,
    val thermalLevel: ThermalHealthLevel = ThermalHealthLevel.NORMAL,
    val thermalHeadroomPercent: Int? = null,
) {
    val displayTitle: String
        get() = when {
            isS23Ultra -> "Samsung Galaxy S23 Ultra ($model)"
            isSamsung -> "Samsung $model"
            else -> "$manufacturer $model".trim()
        }

    val chipSummary: String
        get() = when {
            hasSnapdragon8Gen2 -> "Snapdragon 8 Gen 2 • FastConnect 7800 (BT 5.3)"
            socModel.isNotBlank() -> "SoC: $socModel"
            else -> "Hardware: ${Build.HARDWARE}"
        }
}

/**
 * Hardware, One UI, Snapdragon 8 Gen 2 (SM8550), FastConnect 7800, 120Hz LTPO,
 * BLE S Pen, Device Care and hybrid Samsung DeX compatibility inspector.
 * All non-Samsung Android 9–16 devices fall back cleanly without errors or clutter.
 */
object SamsungCompatibilityInspector {

    fun isGalaxyS23UltraModel(model: String, deviceCodename: String = ""): Boolean {
        val normModel = model.trim().uppercase()
        val normDevice = deviceCodename.trim().lowercase()
        return normModel.startsWith("SM-S918") || normDevice == "dm3q" || normDevice == "dm3"
    }

    fun isSnapdragon8Gen2(socModel: String, hardware: String = ""): Boolean {
        val normSoc = socModel.trim().uppercase()
        val normHw = hardware.trim().uppercase()
        return normSoc.contains("SM8550") || normSoc.contains("8 GEN 2") ||
            normHw.contains("KALAMA") || normHw.contains("SM8550")
    }

    fun isBleSPenCapableModel(model: String, isS23Ultra: Boolean): Boolean {
        if (isS23Ultra) return true
        val norm = model.trim().uppercase()
        // Note10/20 Ultra, S22/S23/S24 Ultra & Tab S series BLE S Pen models
        return norm.startsWith("SM-S928") || norm.startsWith("SM-S908") ||
            norm.startsWith("SM-N985") || norm.startsWith("SM-N986") ||
            norm.startsWith("SM-X")
    }

    fun parseOneUiVersionName(semPlatformInt: Int, sdkInt: Int): String = when {
        semPlatformInt >= 170000 -> "One UI 8.x (Android ${sdkIntToAndroidLabel(sdkInt)})"
        semPlatformInt >= 160000 -> "One UI 7.x (Android ${sdkIntToAndroidLabel(sdkInt)})"
        semPlatformInt >= 150100 -> "One UI 6.1+"
        semPlatformInt >= 150000 -> "One UI 6.0"
        semPlatformInt >= 140100 -> "One UI 5.1+"
        semPlatformInt >= 140000 -> "One UI 5.0"
        semPlatformInt >= 130100 -> "One UI 4.1+"
        semPlatformInt > 0 -> "One UI (SEM $semPlatformInt)"
        else -> "Android ${sdkIntToAndroidLabel(sdkInt)} (API $sdkInt)"
    }

    private fun sdkIntToAndroidLabel(sdkInt: Int): String = when {
        sdkInt >= 36 -> "16"
        sdkInt == 35 -> "15"
        sdkInt == 34 -> "14"
        sdkInt == 33 -> "13"
        sdkInt == 32 -> "12L"
        sdkInt == 31 -> "12"
        sdkInt == 30 -> "11"
        sdkInt == 29 -> "10"
        else -> "9"
    }

    fun readSemPlatformInt(): Int = runCatching {
        val field = Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
        field.isAccessible = true
        field.getInt(null)
    }.getOrDefault(0)

    fun readSocModel(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Build.SOC_MODEL.orEmpty()
    } else {
        Build.HARDWARE.orEmpty()
    }

    fun shouldPauseAirMouseForDeX(isDexActive: Boolean): Boolean = isDexActive

    fun shouldSuppressOledDimForDeX(isDexActive: Boolean): Boolean = isDexActive

    fun buildOneUiDeviceCareGuide(semPlatformInt: Int): String = when {
        semPlatformInt >= 160000 -> {
            "One UI 7/8: Toque em \"Bateria do App\" → Bateria → selecione \"Irrestrito\". Depois abra Assistência do Aparelho → Bateria → Limites de uso em segundo plano → Apps nunca suspensos em repouso → adicione Controle BT."
        }
        semPlatformInt > 0 -> {
            "One UI 5/6: Toque em \"Bateria do App\" → Bateria → selecione \"Não restrito\". Em Assistência do aparelho → Bateria → Limites de uso em segundo plano → Aplicativos nunca suspensos → adicione Controle BT."
        }
        else -> {
            "Desative a restrição de bateria em Configurações → Aplicativos → Controle BT → Bateria para evitar quedas com a tela bloqueada."
        }
    }

    fun detectDexState(context: Context, configuration: Configuration): Pair<Boolean, String> {
        // 1. Legacy One UI DeX configuration reflection (One UI 5.1 / 6.x / 7.0)
        val semDesktopEnabled = runCatching {
            val enabledVal = Configuration::class.java.getField("SEM_DESKTOP_MODE_ENABLED").getInt(null)
            val currentVal = Configuration::class.java.getField("semDesktopModeEnabled").getInt(configuration)
            currentVal == enabledVal
        }.getOrDefault(false)
        if (semDesktopEnabled) {
            return true to "Samsung DeX Clássico"
        }

        // 2. Modern Android 15/16 / One UI 8 Desktop Windowing UiMode
        val uiModeMgr = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeMgr?.currentModeType == Configuration.UI_MODE_TYPE_DESK) {
            return true to "Modo Desktop / DeX"
        }

        // 3. Presentation / Secondary Display running on external monitor
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val activeDisplay = resolveActiveDisplay(context)
        if (activeDisplay != null && activeDisplay.displayId != Display.DEFAULT_DISPLAY) {
            return true to "Monitor Externo (${activeDisplay.name})"
        }
        val presentationDisplays = dm?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        if (!presentationDisplays.isNullOrEmpty() && configuration.smallestScreenWidthDp >= 600) {
            return true to "Tela Externa / DeX Wireless"
        }

        return false to ""
    }

    @Suppress("DEPRECATION")
    private fun resolveActiveDisplay(context: Context): Display? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = runCatching { context.display }.getOrNull()
            if (display != null) return display
        }
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        return runCatching { wm?.defaultDisplay }.getOrNull()
    }

    fun inspect(context: Context, overrideConfig: Configuration? = null): SamsungDeviceProfile {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val socModel = readSocModel()
        val isSamsung = manufacturer.equals("Samsung", ignoreCase = true)
        val isS23Ultra = isGalaxyS23UltraModel(model, device)
        val hasSnapdragon8Gen2 = isSnapdragon8Gen2(socModel, Build.HARDWARE.orEmpty())
        val semInt = if (isSamsung) readSemPlatformInt() else 0
        val oneUiLabel = parseOneUiVersionName(semInt, Build.VERSION.SDK_INT)

        val display = resolveActiveDisplay(context)
        val activeHz = display?.refreshRate ?: 60f
        val maxHz = runCatching {
            display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: activeHz
        }.getOrDefault(activeHz)

        val config = overrideConfig ?: context.resources.configuration
        val (dexActive, dexLabel) = detectDexState(context, config)

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBattery = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        val thermalLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm != null) {
            when (runCatching { pm.currentThermalStatus }.getOrDefault(PowerManager.THERMAL_STATUS_NONE)) {
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalHealthLevel.MODERATE
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalHealthLevel.ELEVATED
                PowerManager.THERMAL_STATUS_SEVERE,
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalHealthLevel.THROTTLED
                else -> ThermalHealthLevel.NORMAL
            }
        } else {
            ThermalHealthLevel.NORMAL
        }

        val headroomPct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm != null) {
            val h = runCatching { pm.getThermalHeadroom(10) }.getOrDefault(Float.NaN)
            if (!h.isNaN() && h >= 0f) (h * 100f).toInt().coerceIn(0, 150) else null
        } else {
            null
        }

        return SamsungDeviceProfile(
            manufacturer = manufacturer,
            model = model,
            deviceCodename = device,
            socModel = socModel,
            isSamsung = isSamsung,
            isS23Ultra = isS23Ultra,
            hasSnapdragon8Gen2 = hasSnapdragon8Gen2,
            hasFastConnect7800 = isS23Ultra || hasSnapdragon8Gen2,
            hasBleSPen = isBleSPenCapableModel(model, isS23Ultra),
            semPlatformInt = semInt,
            oneUiLabel = oneUiLabel,
            activeRefreshRateHz = activeHz,
            maxRefreshRateHz = maxHz,
            isLtpo120HzCapable = maxHz >= 119f,
            isDexActive = dexActive,
            dexSourceLabel = dexLabel,
            isIgnoringBatteryOptimizations = ignoringBattery,
            thermalLevel = thermalLevel,
            thermalHeadroomPercent = headroomPct,
        )
    }

    /** Play-Policy-safe direct shortcut to App Info → Battery (Unrestricted / Never sleep). */
    fun createAppDetailsBatteryIntent(context: Context): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun createBatteryOptimizationSettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun createSamsungSPenSettingsIntent(context: Context): Intent {
        val pm = context.packageManager
        val samsungIntent = Intent("com.samsung.settings.SPEN_SETTING").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = pm.resolveActivity(samsungIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved != null) return samsungIntent

        val advIntent = Intent("com.samsung.settings.ADVANCED_FEATURES").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (pm.resolveActivity(advIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            return advIntent
        }

        return Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
