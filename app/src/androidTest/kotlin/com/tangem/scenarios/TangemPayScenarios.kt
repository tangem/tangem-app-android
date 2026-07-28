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
    // Existing customer: callers set the `tangem_pay_eligibility` scenario to PaeraCustomer (in
    // additionalBeforeSection, before this runs), which drives the checkCustomerWalletId mock -> Payment account.
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

/** Opens Tangem Pay and taps the card to reach the card management page. */
fun BaseTestCase.openTangemPayCardPage() {
    openTangemPay()
    step("Click on 'Card' button") {
        onTangemPayMainScreen { cardButton.clickWithAssertion() }
    }
    step("Assert card page 'More' button is displayed") {
        awaitSuccess { onTangemPayCardPageScreen { moreButton.assertIsDisplayed() } }
    }
}

/** Opens the card page and taps 'Change' on the daily limit block to reach the limit setup screen. */
fun BaseTestCase.openTangemPayDailyLimitSetup() {
    openTangemPayCardPage()
    step("Click on 'Change' daily limit button") {
        awaitSuccess { onTangemPayCardPageScreen { dailyLimitChangeButton.assertIsDisplayed() } }
        onTangemPayCardPageScreen { dailyLimitChangeButton.performClick() }
    }
    step("Assert daily limit setup screen is displayed") {
        awaitSuccess { onTangemPayDailyLimitScreen { amountField.assertIsDisplayed() } }
        onTangemPayDailyLimitScreen { setLimitsButton.assertIsDisplayed() }
    }
}

/** From the card page, opens the 'Replace card' reissue bottom sheet via the 'More' menu. */
fun BaseTestCase.openReissueSheet() {
    step("Click on 'More' button") {
        onTangemPayCardPageScreen { moreButton.clickWithAssertion() }
    }
    step("Click on 'Replace card' menu item") {
        awaitSuccess { onTangemPayCardPageScreen { replaceCardMenuItem.assertIsDisplayed() } }
        onTangemPayCardPageScreen { replaceCardMenuItem.performClick() }
    }
    step("Assert reissue bottom sheet is displayed") {
        awaitSuccess { onTangemPayReissueSheet { confirmButton.assertIsDisplayed() } }
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