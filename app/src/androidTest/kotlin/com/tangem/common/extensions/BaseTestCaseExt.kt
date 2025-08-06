package com.tangem.common.extensions

import com.tangem.common.BaseTestCase

fun BaseTestCase.swipeUp(
    startHeightRatio: Float = 0.8f,
    endHeightRatio: Float = 0.03f,
    steps: Int = 15
) {
    device.uiDevice.swipe(
        device.uiDevice.displayWidth / 2,
        (device.uiDevice.displayHeight * startHeightRatio).toInt(),
        device.uiDevice.displayWidth / 2,
        (device.uiDevice.displayHeight * endHeightRatio).toInt(),
        steps
    )
}