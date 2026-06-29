package com.tangem.scenarios

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.test.DetailsScreenTestTags
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.MockContent
import com.tangem.tap.domain.sdk.mocks.MockProvider
import io.qameta.allure.kotlin.Allure.step

// Custom/nested clickables (Add-Wallet row, wallet tab) reject touch injection — drive OnClick directly.
private fun BaseTestCase.clickViaSemantics(matcher: SemanticsMatcher, useUnmergedTree: Boolean = false) {
    composeTestRule.onNode(matcher, useUnmergedTree).performSemanticsAction(SemanticsActions.OnClick)
}

// Both pager pages stay mounted; the off-screen one ignores swipes, so always swipe the on-screen wallet card.
private fun BaseTestCase.swipeDisplayedWallet(toPrevious: Boolean) {
    val nodes = composeTestRule.onAllNodes(hasTestTag(MainScreenTestTags.WALLET_LIST_ITEM))
    for (i in 0 until nodes.fetchSemanticsNodes().size) {
        val swiped = runCatching {
            nodes[i].assertIsDisplayed()
            nodes[i].performTouchInput { if (toPrevious) swipeRight() else swipeLeft() }
        }.isSuccess
        if (swiped) break
    }
    waitForIdle()
}

/** 'Add Wallet' scans a card immediately (no type chooser), so [mockContent] must be set before the click. */
fun BaseTestCase.addNewCardWallet(mockContent: MockContent) {
    step("Click 'More' button on TopBar") {
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    MockProvider.setMocks(mockContent)
    step("Click on 'Add Wallet' button (scans a new hardware wallet)") {
        clickViaSemantics(hasTestTag(DetailsScreenTestTags.ADD_WALLET_BUTTON))
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
            if (!shown) swipeDisplayedWallet(toPrevious = false)
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

/** Both pager pages mount the same token; clicks the [tokenName] copy currently on-screen. */
fun BaseTestCase.clickDisplayedTokenOnMain(tokenName: String) {
    val matcher = hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM) and hasAnyDescendant(hasText(tokenName))
    step("Click on token '$tokenName' on the visible wallet") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            val nodes = composeTestRule.onAllNodes(matcher, useUnmergedTree = true)
            (0 until nodes.fetchSemanticsNodes().size).any { i ->
                runCatching {
                    nodes[i].assertIsDisplayed()
                    nodes[i].performClick()
                }.isSuccess
            }
        }
    }
}

fun BaseTestCase.switchToPreviousWallet() {
    step("Swipe wallet card to the previous wallet") {
        swipeDisplayedWallet(toPrevious = true)
    }
}

/** Picks a [token] the recipient [walletName] already holds, via the wallet tab. */
fun BaseTestCase.selectReceiveTokenOnWallet(token: String, walletName: String) {
    step("Click on 'Choose token' button") {
        onSwapTokenScreen { chooseTokenButton.performClick() }
    }
    // The wallet tab is a custom clickable Row that Kakao's performClick can't hit (autoscroll fails); invoke OnClick directly.
    step("Select wallet tab '$walletName'") {
        clickViaSemantics(
            hasTestTag(BuyTokenScreenTestTags.WALLET_TAB) and hasAnyDescendant(hasText(walletName)),
            useUnmergedTree = true,
        )
    }
    step("Click on token with name '$token'") {
        clickViaSemantics(
            hasTestTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM) and hasAnyDescendant(hasText(token)),
            useUnmergedTree = true,
        )
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