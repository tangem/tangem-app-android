package com.tangem.data.balancehiding

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.tangem.domain.balancehiding.DeviceFlipDetector
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class DefaultDeviceFlipDetector(context: Context) : DeviceFlipDetector {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    override fun getDeviceFlipFlow(): Flow<Unit> = callbackFlow {
        val listener = FlipListener { trySend(Unit) }

        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}