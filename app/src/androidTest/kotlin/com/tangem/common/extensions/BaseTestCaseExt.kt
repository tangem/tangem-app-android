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

fun BaseTestCase.pullToRefresh() {
    swipeVertical(
        direction = SwipeDirection.DOWN,
        startHeightRatio = 0.2f,
        endHeightRatio = 0.8f,
        steps = 2000
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

enum class SwipeDirection {
    UP, DOWN
}