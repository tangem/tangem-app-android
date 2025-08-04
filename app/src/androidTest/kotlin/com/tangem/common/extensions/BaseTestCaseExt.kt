package com.tangem.common.extensions

import com.tangem.common.BaseTestCase

fun BaseTestCase.swipeToCloseApp() {

    device.uiDevice.swipe(
        device.uiDevice.displayWidth / 2,
        device.uiDevice.displayHeight / 2,
        device.uiDevice.displayWidth / 2,
        device.uiDevice.displayHeight / 30,
        15
    )

}