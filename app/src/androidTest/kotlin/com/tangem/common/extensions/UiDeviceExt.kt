package com.tangem.common.extensions

import androidx.test.uiautomator.By
import com.tangem.common.BaseTestCase
import com.tangem.wallet.R
import io.github.kakaocup.kakao.common.utilities.getResourceString

fun BaseTestCase.swipeVertical(
    direction: SwipeDirection,
    startHeightRatio: Float = if (direction == SwipeDirection.UP) 0.8f else 0.03f,
    endHeightRatio: Float = if (direction == SwipeDirection.UP) 0.03f else 0.8f,
    steps: Int = 15,
) {
    device.uiDevice.swipe(
        device.uiDevice.displayWidth / 2,
        (device.uiDevice.displayHeight * startHeightRatio).toInt(),
        device.uiDevice.displayWidth / 2,
        (device.uiDevice.displayHeight * endHeightRatio).toInt(),
        steps
    )
}

fun BaseTestCase.pullToRefresh(steps: Int = 1000) {
    swipeVertical(
        direction = SwipeDirection.DOWN,
        startHeightRatio = 0.2f,
        endHeightRatio = 0.8f,
        steps = steps
    )
}

fun BaseTestCase.swipeMarketsBlock(direction: SwipeDirection) {
    val searchBarText = device.uiDevice
        .findObject(By.textContains(getResourceString(R.string.markets_search_header_title)))
    val bounds = searchBarText.visibleBounds

    val centerX = bounds.centerX()
    val startY = bounds.centerY()
    val endY = when (direction) {
        SwipeDirection.UP -> 50
        SwipeDirection.DOWN -> device.uiDevice.displayHeight - 100
    }

    device.uiDevice.swipe(centerX, startY, centerX, endY, 100)
}

fun BaseTestCase.openTheAppFromRecents() {
    device.uiDevice.pressRecentApps()

    val centerX = device.uiDevice.displayWidth / 2
    val centerY = device.uiDevice.displayHeight / 3
    device.uiDevice.click(centerX, centerY)
    device.uiDevice.click(centerX, centerY)
}

fun BaseTestCase.disableWiFi() {
    device.uiDevice.executeShellCommand("svc wifi disable")
}

fun BaseTestCase.disableMobileData() {
    device.uiDevice.executeShellCommand("svc data disable")
}

fun BaseTestCase.enableWiFi() {
    device.uiDevice.executeShellCommand("svc wifi enable")
}

fun BaseTestCase.enableMobileData() {
    device.uiDevice.executeShellCommand("svc data enable")
}

fun BaseTestCase.stopApp(packageName: String) {
    device.apps.kill(packageName)
}

fun BaseTestCase.launchApp(packageName: String) {
    device.apps.waitForAppLaunchAndReady(packageName = packageName)
    device.apps.launch(packageName)
}

enum class SwipeDirection {
    UP, DOWN
}