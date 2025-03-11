package com.tangem.data.balancehiding

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.SystemClock

internal class FlipListener(private val action: () -> Unit) : SensorEventListener {

    private val zAxisThreshold = -6
    private val throttleTimeMs = 3000
    private var lastTriggerTime = 0L
    private var isScreenDown = false

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
                // TODO add module logging
                // Timber.tag("onSensorChanged").d("screen down")
            } else if (zAxisValue >= zAxisThreshold) {
                if (isScreenDown && currentTime - lastTriggerTime <= throttleTimeMs) {
                    // Timber.tag("onSensorChanged").d("screen up!")
                    lastTriggerTime = currentTime
                    action.invoke()
                }
                isScreenDown = false
            }
        }
    }
}