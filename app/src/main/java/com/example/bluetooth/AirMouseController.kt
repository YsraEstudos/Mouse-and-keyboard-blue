package com.example.bluetooth

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hardware Gyroscope Air Mouse controller with:
 * - Adjustable sensitivity
 * - Trigger-hold mode (move only while trigger is held so lowering arm doesn't drift pointer)
 * - Instant zero-bias recalibration
 */
class AirMouseController(
    context: Context,
    private val onMove: (dx: Int, dy: Int) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val isSupported: Boolean
        get() = gyroscope != null

    @Volatile
    var sensitivity: Float = 1.2f

    @Volatile
    var requireTriggerHold: Boolean = false

    @Volatile
    var isTriggerHeld: Boolean = false

    private var isActive: Boolean = false
    private var residualX: Float = 0f
    private var residualY: Float = 0f
    private var biasPitch: Float = 0f
    private var biasYaw: Float = 0f
    private var lastRawPitch: Float = 0f
    private var lastRawYaw: Float = 0f

    fun start() {
        if (isActive || gyroscope == null) return
        residualX = 0f
        residualY = 0f
        sensorManager?.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        isActive = true
    }

    fun stop() {
        if (!isActive) return
        sensorManager?.unregisterListener(this)
        isActive = false
        residualX = 0f
        residualY = 0f
    }

    /**
     * Captures current resting gyroscope bias to recenter and stop any subtle drift immediately.
     */
    fun recalibrate() {
        biasPitch = lastRawPitch
        biasYaw = lastRawYaw
        residualX = 0f
        residualY = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        lastRawPitch = event.values[0]
        lastRawYaw = event.values[2]

        if (requireTriggerHold && !isTriggerHeld) return

        val pitchRate = lastRawPitch - biasPitch
        val yawRate = lastRawYaw - biasYaw

        val deadzone = 0.045f
        val filteredYaw = if (abs(yawRate) > deadzone) yawRate else 0f
        val filteredPitch = if (abs(pitchRate) > deadzone) pitchRate else 0f

        val scale = 14.5f * sensitivity
        val rawDx = (-filteredYaw * scale) + residualX
        val rawDy = (-filteredPitch * scale) + residualY

        val intDx = rawDx.roundToInt()
        val intDy = rawDy.roundToInt()
        residualX = rawDx - intDx
        residualY = rawDy - intDy

        if (intDx != 0 || intDy != 0) {
            onMove(intDx.coerceIn(-45, 45), intDy.coerceIn(-45, 45))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
