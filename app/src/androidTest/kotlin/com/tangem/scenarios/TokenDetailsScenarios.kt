package com.tangem.scenarios

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TokenDetailsScreenTestTags

/**
 * Pulls the 'Token details' screen to refresh.
 *
 * Compose Test gesture on purpose: the shared UiAutomator-based `pullToRefresh()` never reaches the
 * Material3 `PullToRefreshBox` NestedScrollConnection, so `onRefresh` silently never fires and the screen
 * keeps serving stale data.
 */
fun BaseTestCase.pullToRefreshTokenDetails() {
    composeTestRule.onNode(hasTestTag(TokenDetailsScreenTestTags.SCREEN_CONTAINER))
        .performTouchInput {
            swipeDown(startY = 0f, endY = visibleSize.height.toFloat() * 6f, durationMillis = 800)
        }
    waitForIdle()
}