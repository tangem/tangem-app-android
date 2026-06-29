package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.MockContent
import com.tangem.tap.domain.sdk.mocks.MockProvider
import io.qameta.allure.kotlin.Allure.step

/** 'Add Wallet' scans a card immediately (no type chooser), so [mockContent] must be set before the click. */
fun BaseTestCase.addNewCardWallet(mockContent: MockContent) {
    step("Click 'More' button on TopBar") {
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    MockProvider.setMocks(mockContent)
    step("Click on 'Add Wallet' button (scans a new hardware wallet)") {
        onDetailsScreen { addWalletButton.clickWithAssertion() }
    }
    // Gate on the top-bar More button, not the container — the bottom Markets sheet can leave the container un-"displayed".
    step("Assert 'Main' screen is displayed with the new wallet") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
            runCatching { onMainScreenTopBar { moreButton.assertIsDisplayed() } }.isSuccess
        }
    }
    // The added card is the newest pager page; its "Synchronize addresses" prompt is off-screen until swiped to.
    step("Synchronize the new card wallet's addresses") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            var shown = false
            onMainScreen { shown = synchronizeAddressesButton.isDisplayedSafely() }
            if (!shown) onMainScreen { swipeToAdjacentWallet(toPrevious = false) }
            shown
        }
        // Let the pager fling settle — a click mid-animation is eaten by the button's clickableSingle debounce.
        waitForIdle()
        onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
        // The prompt clears once the card's addresses are derived (re-scan + reload over many, some failing, RPCs).
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
            var generated = false
            onMainScreen { generated = !synchronizeAddressesButton.isDisplayedSafely() }
            generated
        }
    }
}

fun BaseTestCase.clickDisplayedTokenOnMain(tokenName: String) {
    step("Click on token '$tokenName' on the visible wallet") {
        onMainScreen { clickDisplayedToken(tokenName) }
    }
}

fun BaseTestCase.switchToPreviousWallet() {
    step("Swipe wallet card to the previous wallet") {
        onMainScreen { swipeToAdjacentWallet(toPrevious = true) }
    }
}

/** Picks a [token] the recipient [walletName] already holds, via the wallet tab. */
fun BaseTestCase.selectReceiveTokenOnWallet(token: String, walletName: String) {
    step("Click on 'Choose token' button") {
        onSwapTokenScreen { chooseTokenButton.performClick() }
    }
    step("Select wallet tab '$walletName'") {
        onBuyTokenScreen { walletTab(walletName).performClick() }
    }
    step("Click on token with name '$token'") {
        onBuyTokenScreen { tokenWithTitle(token).performClick() }
    }
}

/** Adds [token] to [recipientWalletName] which lacks it, via market search. */
fun BaseTestCase.addMissingReceiveTokenToWallet(token: String, recipientWalletName: String) {
    step("Click on 'Choose token' button") {
        onSwapTokenScreen { chooseTokenButton.performClick() }
    }
    step("Type '$token' in search field") {
        onSwapSelectTokenScreen {
            searchBarBlock.performClick()
            searchBarBlock.performTextInput(token)
        }
    }
    step("Click on market token '$token'") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { onSwapSelectTokenScreen { marketsTokenWithName(token).performClick() } }.isSuccess
        }
    }
    // The 'Add token' sheet pre-selects the recipient (the only wallet missing the token, since the source already holds it).
    step("Assert recipient wallet '$recipientWalletName' is selected") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { onAddToPortfolioScreen { walletName(recipientWalletName).assertIsDisplayed() } }.isSuccess
        }
    }
    step("Click on 'Add' button") {
        onAddToPortfolioScreen { addButton.performClick() }
    }
}