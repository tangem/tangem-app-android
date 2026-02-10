package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMarketsScreen
import com.tangem.screens.onMarketsTokenDetailsScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openMarketTokenDetailsScreen(blockchainName: String, tokenName: String) {
    step("Open 'Markets' screen") {
        onMainScreen { searchThroughMarketPlaceholder.performClick() }
        waitForIdle()
    }
    step("Click on 'Search' placeholder") {
        onMarketsScreen { searchThroughMarketPlaceholder.performClick() }
    }
    step("Click on $blockchainName blockchain") {
        waitForIdle()
        onMarketsScreen { tokenWithTitle(blockchainName).clickWithAssertion() }
    }
    step("Click on $tokenName token") {
        waitForIdle()
        onMarketsTokenDetailsScreen { tokenWithTitle(tokenName).clickWithAssertion() }
    }
}