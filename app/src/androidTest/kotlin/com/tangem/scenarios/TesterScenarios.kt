package com.tangem.scenarios

import android.view.KeyEvent
import com.tangem.common.BaseTestCase
import com.tangem.screens.onTesterMenuScreen
import com.tangem.utils.logging.TangemLogger

private const val TESTER_MENU_MAX_ATTEMPTS = 3

/**
 * Opens the tester menu by sending two 'Volume Down' key events in a single shell command.
 * VolumeButtonDoublePressObserver needs two ACTION_DOWN within 300ms; two separate pressKeyCode
 * calls are too slow, but both events in one `input keyevent` invocation arrive back-to-back within
 * the window. Retries up to [TESTER_MENU_MAX_ATTEMPTS] times if it doesn't appear.
 */
fun BaseTestCase.openTesterMenu() {
    repeat(TESTER_MENU_MAX_ATTEMPTS) { attempt ->
        waitForIdle()
        val volumeDown = KeyEvent.KEYCODE_VOLUME_DOWN
        device.uiDevice.executeShellCommand("input keyevent $volumeDown $volumeDown")

        val opened = runCatching {
            onTesterMenuScreen { addressesInfoButton.assertIsDisplayed() }
        }.isSuccess

        if (opened) {
            TangemLogger.i("Tester menu opened on attempt ${attempt + 1}")
            return
        }
        TangemLogger.w("Tester menu not opened on attempt ${attempt + 1}, retrying...")
    }
    error("Failed to open tester menu after $TESTER_MENU_MAX_ATTEMPTS attempts")
}