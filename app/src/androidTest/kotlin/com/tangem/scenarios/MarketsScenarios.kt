package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeMarketsBlock
import com.tangem.common.extensions.swipeVertical
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMarketsExchangesScreen
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

fun BaseTestCase.assertMarketsExchangesScreen() {
    step("Assert 'Title' is displayed") {
        onMarketsExchangesScreen { exchangesTitle.assertIsDisplayed() }
    }
    step("Assert 'Exchange name' is displayed") {
        onMarketsExchangesScreen { exchangeName.assertIsDisplayed() }
    }
    step("Assert 'Logo' is displayed") {
        onMarketsExchangesScreen { exchangeLogo.assertIsDisplayed() }
    }
    step("Assert 'Exchange type' is displayed") {
        onMarketsExchangesScreen { exchangeType.assertIsDisplayed() }
    }
    step("Assert 'Trust score' is displayed") {
        onMarketsExchangesScreen { trustScore.assertIsDisplayed() }
    }
}

fun BaseTestCase.openMarketsScreen() {
    step("Open 'Main Screen'") {
        openMainScreen()
    }
    step("Synchronize addresses") {
        synchronizeAddresses()
    }
    step("Open 'Markets' screen") {
        swipeMarketsBlock(SwipeDirection.UP)
        waitForIdle()
    }
}

fun BaseTestCase.openMarketsExchangesScreen(tokenName: String, shouldClickSeeAllButton: Boolean = false) {
    openMarketsScreen()
    if (shouldClickSeeAllButton)
        step("Click on 'See all' button") {
            onMarketsScreen { seeAllButton.clickWithAssertion() }
        }
    step("Click on '$tokenName' token") {
        onMarketsScreen { tokenWithTitle(tokenName).clickWithAssertion() }
        waitForIdle()
    }
    step("Scroll down") {
        swipeVertical(SwipeDirection.UP)
        swipeVertical(SwipeDirection.UP)
    }
    step("Click on 'Listed on exchanges' block") {
        onMarketsScreen { listedOnBlockContainer.performClick() }
        waitForIdle()
    }
}