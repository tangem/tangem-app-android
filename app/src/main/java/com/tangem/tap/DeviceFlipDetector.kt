package com.tangem.tap

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@ExperimentalCoroutinesApi
class DeviceFlipDetector(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val zAxisThreshold = -6
    private val throttleTimeMs = 3000
    private var lastTriggerTime = 0L
    private var isScreenDown = false

    fun deviceFlipEvents(): Flow<Unit> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                /* no-op */
            }

            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val currentTime = SystemClock.elapsedRealtime()
                    val zAxisValue = it.values[2]

                    if (zAxisValue < zAxisThreshold && !isScreenDown) {
                        isScreenDown = true
                        lastTriggerTime = currentTime
                    } else if (zAxisValue >= zAxisThreshold) {
                        if (isScreenDown && currentTime - lastTriggerTime <= throttleTimeMs) {
                            lastTriggerTime = currentTime
                            trySend(Unit)
                        }
                        isScreenDown = false
                    }
                }
            }
        }

        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}