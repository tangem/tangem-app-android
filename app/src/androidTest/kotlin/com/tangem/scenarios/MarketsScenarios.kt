package com.tangem.scenarios

import androidx.compose.ui.test.ExperimentalTestApi
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMarketsExchangesScreen
import com.tangem.screens.onMarketsScreen
import com.tangem.screens.onMarketsTokenDetailsScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openTokenDetailsFromMarketsScreen(blockchainName: String, tokenName: String) {
    step("Open 'Markets' screen") {
        onMainScreen { marketsSheetDragHandle.clickWithAssertion() }
        waitForIdle()
    }
    step("Search for $blockchainName blockchain") {
        searchTokenInMarkets(blockchainName)
    }
    step("Click on $blockchainName blockchain") {
        waitForIdle()
        onMarketsScreen { tokenWithTitle(blockchainName).clickWithAssertion() }
    }
    step("Click on 'In your portfolio' block") {
        waitForIdle()
        onMarketsTokenDetailsScreen { inYourPortfolioBlock.clickWithAssertion() }
    }
    step("Click on $tokenName token in 'Your portfolio' bottom sheet") {
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
        onMainScreen { marketsSheetDragHandle.clickWithAssertion() }
        waitForIdle()
    }
}

/**
 * Finds [tokenName] through the markets sheet's search. The sheet must already be open.
 *
 * Search rather than the feed list: the feed shows only the top of the market, so a token further
 * down is not reachable by scrolling its lazy list.
 */
fun BaseTestCase.searchTokenInMarkets(tokenName: String) {
    onMarketsScreen {
        searchField.performClick()
        searchField.performTextInput(tokenName)
    }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
fun BaseTestCase.openMarketsTokenDetails(tokenName: String) {
    openMarketsScreen()
    step("Search for '$tokenName' token") {
        searchTokenInMarkets(tokenName)
    }
    step("Click on '$tokenName' token") {
        onMarketsScreen { tokenWithTitle(tokenName).clickWithAssertion() }
        waitForIdle()
    }
    step("Scroll to 'Listed on exchanges' block") {
        onMarketsScreen { scrollToListedOnBlock() }
        waitForIdle()
    }
}

fun BaseTestCase.openMarketsExchangesScreen(tokenName: String) {
    openMarketsTokenDetails(tokenName)
    // The block settles into place while the token details load, so a tap can land on the 'Add token'
    // button below it — retry until the exchanges screen is up.
    step("Click on 'Listed on exchanges' block") {
        awaitAssert {
            runCatching { onMarketsScreen { listedOnBlockContainer.clickWithAssertion() } }
            onMarketsExchangesScreen { exchangesTitle.assertIsDisplayed() }
        }
    }
}