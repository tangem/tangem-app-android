package com.tangem.feature.tester.presentation.navigation

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.feature.tester.presentation.TesterActivity
import com.tangem.features.tester.api.TesterMenuLauncher

/**
 * Default implementation of [TesterMenuLauncher] that listens for shake events using the device's accelerometer.
 * When a shake is detected, it opens the tester menu.
 *
 * @param context the application context used to access system services
 *
[REDACTED_AUTHOR]
 */
internal class DefaultTesterMenuLauncher(private val context: Context) : TesterMenuLauncher {

    override val launchOnShakeObserver: DefaultLifecycleObserver by lazy(LazyThreadSafetyMode.NONE) {
        createObserver(context)
    }

    private fun createObserver(context: Context): DefaultLifecycleObserver {
        return object : DefaultLifecycleObserver {
            private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            private val shakeEventListener = ShakeEventListener(action = ::openTesterMenu)

            override fun onResume(owner: LifecycleOwner) {
                accelerometer?.let {
                    sensorManager.registerListener(
                        /* listener = */ shakeEventListener,
                        /* sensor = */ it,
                        /* samplingPeriodUs = */ SensorManager.SENSOR_DELAY_NORMAL,
                    )
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                sensorManager.unregisterListener(shakeEventListener)
            }
        }
    }

    private fun openTesterMenu() {
        val intent = Intent(context, TesterActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }
}