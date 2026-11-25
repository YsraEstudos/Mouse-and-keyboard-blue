package com.example.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.BluetoothHidManager
import com.example.data.SettingsRepository
import com.example.hid.HidConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android Quick Settings Tile (Notification Shade) to open Controle BT or toggle Mouse Jiggler quickly.
 */
class ControleBtTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTileVisuals()
    }

    override fun onClick() {
        super.onClick()
        val hid = BluetoothHidManager.getInstance(applicationContext)
        val connected = hid.connectionState.value is BluetoothConnectionState.Connected
        if (connected) {
            scope.launch {
                val repo = SettingsRepository(applicationContext)
                repo.update { it.copy(mouseJigglerEnabled = !it.mouseJigglerEnabled) }
                updateTileVisuals()
            }
        } else {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pi = PendingIntent.getActivity(
                    applicationContext,
                    101,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                startActivityAndCollapse(pi)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun updateTileVisuals() {
        val tile = qsTile ?: return
        val hid = BluetoothHidManager.getInstance(applicationContext)
        val state = hid.connectionState.value
        scope.launch {
            val settings = runCatching { SettingsRepository(applicationContext).settingsFlow.first() }.getOrNull()
            val jigglerOn = settings?.mouseJigglerEnabled == true
            tile.state = when {
                state is BluetoothConnectionState.Connected || jigglerOn -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            tile.label = when {
                jigglerOn -> "Jiggler ON"
                state is BluetoothConnectionState.Connected -> state.deviceName.take(14)
                else -> "Controle BT"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (state is BluetoothConnectionState.Connected) "HID Conectado" else "Abrir remoto"
            }
            tile.updateTile()
        }
    }
}

/**
 * Android Home Screen AppWidget providing instant Play/Pause, Volume +/-, Jiggler toggle, and app launch.
 */
class ControleBtWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_VOL_UP = "com.example.widget.ACTION_VOL_UP"
        const val ACTION_VOL_DOWN = "com.example.widget.ACTION_VOL_DOWN"
        const val ACTION_TOGGLE_JIGGLER = "com.example.widget.ACTION_TOGGLE_JIGGLER"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_controle_bt)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_open_app,
                PendingIntent.getActivity(context, 10, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play, broadcastPi(context, ACTION_PLAY_PAUSE, 11))
            views.setOnClickPendingIntent(R.id.widget_btn_vol_down, broadcastPi(context, ACTION_VOL_DOWN, 12))
            views.setOnClickPendingIntent(R.id.widget_btn_vol_up, broadcastPi(context, ACTION_VOL_UP, 13))
            views.setOnClickPendingIntent(R.id.widget_btn_jiggler, broadcastPi(context, ACTION_TOGGLE_JIGGLER, 14))

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val hid = BluetoothHidManager.getInstance(context)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> hid.sendConsumerControl(HidConstants.CONSUMER_PLAY_PAUSE, "Widget: Play/Pause")
            ACTION_VOL_UP -> hid.sendConsumerControl(HidConstants.CONSUMER_VOL_UP, "Widget: Vol+")
            ACTION_VOL_DOWN -> hid.sendConsumerControl(HidConstants.CONSUMER_VOL_DOWN, "Widget: Vol-")
            ACTION_TOGGLE_JIGGLER -> {
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    SettingsRepository(context).update { it.copy(mouseJigglerEnabled = !it.mouseJigglerEnabled) }
                }
            }
        }
    }

    private fun broadcastPi(context: Context, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, ControleBtWidgetProvider::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
