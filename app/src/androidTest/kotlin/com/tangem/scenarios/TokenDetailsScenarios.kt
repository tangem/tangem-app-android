package com.tangem.scenarios

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTokenDetailsScreen

/**
 * Pulls the 'Token details' screen to refresh.
 *
 * Compose Test gesture on purpose: the shared UiAutomator-based `pullToRefresh()` never reaches the
 * Material3 `PullToRefreshBox`, so `onRefresh` silently does not fire.
 *
 * Swipes on the balance block rather than the screen container: a swipe on the container is consumed by
 * the inner list and never becomes the overscroll `PullToRefreshBox` reacts to.
 *
 * Even as a Compose gesture it may not land, so callers must retry it until they can observe
 * the refresh — `TokenDetailsModel.onRefreshSwipe` re-reads the balance and the transaction history in
 * one go, so either one disappearing is proof that the other was re-read too.
 */
fun BaseTestCase.pullToRefreshTokenDetails() {
    composeTestRule.onNode(hasTestTag(TokenDetailsScreenTestTags.BALANCE_FIAT), useUnmergedTree = true)
        .performTouchInput {
            swipeDown(startY = 0f, endY = visibleSize.height.toFloat() * 6f, durationMillis = 800)
        }
    waitForIdle()
}

/**
 * Opens [tokenTitle]'s details from the token list on Main.
 *
 * A single tap is not enough: the row keeps re-laying out while its APY promo banner resolves, and a
 * tap that lands mid-relayout is dropped, leaving the test on Main. Retry the tap — once the details
 * screen is up the row is gone, so the click throws and only the assertion decides the outcome.
 */
fun BaseTestCase.openTokenDetailsFromTokenList(tokenTitle: String) {
    awaitAssert(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        runCatching { onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() } }
        onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
    }
}