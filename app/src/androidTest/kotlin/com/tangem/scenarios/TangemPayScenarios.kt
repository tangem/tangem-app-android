package com.tangem.scenarios

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.screens.tangempay.*
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openTangemPay() {
    step("Import hot wallet from Tangem Pay seed phrase (with access code)") {
        openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
    }
    step("Click on Tangem Pay tile") {
        onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
    }
    step("Assert payment account balance is displayed") {
        onTangemPayMainScreen { balance.assertIsDisplayed() }
    }
}

// Compose Test gesture — UiAutomator swipe doesn't reach Material3 PullToRefreshBox's NestedScrollConnection.
fun BaseTestCase.pullToRefreshTangemPay() {
    val balance = composeTestRule.onNode(hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_BALANCE))
    balance.performTouchInput {
        swipeDown(startY = 0f, endY = visibleSize.height.toFloat() * 6f, durationMillis = 800)
    }
    composeTestRule.mainClock.advanceTimeBy(2_000L)
    waitForIdle()
}