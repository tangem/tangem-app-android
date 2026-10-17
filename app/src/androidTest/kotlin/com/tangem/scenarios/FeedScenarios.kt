package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.screens.onMarketsScreen

fun BaseTestCase.expandFeed() {
    onMarketsScreen { searchField.performClick() }
    waitForIdle()
}