package com.tangem.feature.tester.presentation.navigation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Listener for device shake events.
 *
 * This class implements [SensorEventListener] and is used to detect device shaking based on accelerometer data.
 * When a shake is detected, the provided action is invoked.
 *
 * @property action lambda function to be called when a shake is detected
 *
[REDACTED_AUTHOR]
 */
internal class ShakeEventListener(private val action: () -> Unit) : SensorEventListener {

    private var lastShakeTime = 0L

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val acceleration = calculateAcceleration(event = event)

        val currentTime = System.currentTimeMillis()
        val currentShakeInterval = currentTime - lastShakeTime

        if (acceleration > SHAKE_THRESHOLD && currentShakeInterval > SHAKE_INTERVAL_MS) {
            lastShakeTime = currentTime
            action()
        }
    }

    private fun calculateAcceleration(event: SensorEvent): Float {
        val (x, y, z) = event.toXYZ()

        return sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
    }

    private fun SensorEvent.toXYZ() = Triple(values[0], values[1], values[2])

    private companion object {
        private const val SHAKE_THRESHOLD: Float = 12f
        private const val SHAKE_INTERVAL_MS: Long = 1000
    }
}