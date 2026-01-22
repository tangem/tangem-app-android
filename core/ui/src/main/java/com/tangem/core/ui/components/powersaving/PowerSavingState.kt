package com.tangem.core.ui.components.powersaving

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PowerSavingState(
    appContext: Context,
    lifecycle: androidx.lifecycle.Lifecycle,
) {

    private val powerManager =
        appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: error("PowerManager not available")

    val isPowerSavingModeEnabled: StateFlow<Boolean>
        field = MutableStateFlow(powerManager.isPowerSaveMode)

    init {
        val intentFilter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }

        val receiver = PowerSavingModeReceiver()

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                appContext.unregisterReceiver(receiver)
            }
        })
    }

    inner class PowerSavingModeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                isPowerSavingModeEnabled.value = powerManager.isPowerSaveMode
            }
        }
    }
}

@Composable
internal fun rememberPowerSavingState(): PowerSavingState {
    val appContext = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return remember(appContext, lifecycle) {
        PowerSavingState(appContext = appContext, lifecycle = lifecycle)
    }
}