package com.tangem.data.balancehiding

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.domain.balancehiding.DeviceFlipDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDeviceFlipDetector @Inject constructor(
    @ApplicationContext context: Context,
) : DeviceFlipDetector, DefaultLifecycleObserver {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private var isResumedState = AtomicBoolean(false)

    override fun onPause(owner: LifecycleOwner) {
        isResumedState.set(false)
    }

    override fun onResume(owner: LifecycleOwner) {
        isResumedState.set(true)
    }

    override fun getDeviceFlipFlow(): Flow<Unit> = callbackFlow {
        val listener = FlipListener {
            if (isResumedState.get()) {
                trySend(Unit)
            }
        }

        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}