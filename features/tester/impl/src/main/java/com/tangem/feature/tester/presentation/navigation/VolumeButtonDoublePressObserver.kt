
package com.tangem.feature.tester.presentation.navigation

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import androidx.lifecycle.LifecycleOwner
import com.tangem.feature.tester.presentation.TesterActivity
import com.tangem.features.tester.api.KeyEventObserver

/**
 * A key event observer that listens for volume down button presses to open the tester menu.
 * It requires two consecutive volume down presses within a specified interval to trigger the menu.
 */
internal class VolumeButtonDoublePressObserver(private val context: Context) : KeyEventObserver {

    private var lastVolumeDownTime = 0L
    private var volumeDownCount = 0
    private var isReady = false

    override fun onResume(owner: LifecycleOwner) {
        isReady = true
    }

    override fun onPause(owner: LifecycleOwner) {
        isReady = false
    }

    /**
     * Returns true if the tester menu was opened.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isReady) return false

        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val now = SystemClock.elapsedRealtime()
            volumeDownCount = if (now - lastVolumeDownTime <= DOUBLE_PRESS_INTERVAL_MS) {
                volumeDownCount + 1
            } else {
                1
            }
            lastVolumeDownTime = now

            if (volumeDownCount == REQUIRED_PRESS_COUNT) {
                volumeDownCount = 0
                openTesterMenu(context)
                return true
            }
        }

        return false
    }

    private fun openTesterMenu(context: Context) {
        val intent = Intent(context, TesterActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    companion object {
        private const val DOUBLE_PRESS_INTERVAL_MS = 300L
        private const val REQUIRED_PRESS_COUNT = 2
    }
}