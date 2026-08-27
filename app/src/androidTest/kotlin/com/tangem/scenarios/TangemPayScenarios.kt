package com.tangem.scenarios

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.HOLD_DURATION_MS
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.HotWalletAccessCodeTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openTangemPay() {
    // Existing customer: callers set the `tangem_pay_eligibility` scenario to PaeraCustomer (in
    // additionalBeforeSection, before this runs), which drives the checkCustomerWalletId mock -> Payment account.
    step("Import hot wallet from Tangem Pay seed phrase (with access code)") {
        openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
    }
    // The tile only renders once the customer check resolves, so it can lag behind the Main screen.
    step("Click on Tangem Pay tile") {
        awaitSuccess { onTangemPayMainScreen { mainScreenTile.assertIsDisplayed() } }
        onTangemPayMainScreen { mainScreenTile.performClick() }
    }
    step("Assert payment account balance is displayed") {
        onTangemPayMainScreen { balance.assertIsDisplayed() }
    }
}

/** From the open payment account screen, opens the details bottom sheet for transaction [name]. */
fun BaseTestCase.openTangemPayTransactionDetails(name: String) {
    step("Assert '$name' transaction row is displayed") {
        awaitSuccess {
            onTangemPayMainScreen {
                scrollToTransactionWithText(name)
                transactionRowWithText(name).assertIsDisplayed()
            }
        }
    }
    step("Click on '$name' transaction row") {
        onTangemPayMainScreen { transactionRowWithText(name).performClick() }
    }
    step("Assert transaction details bottom sheet is displayed") {
        awaitSuccess { onTangemPayTransactionDetailsSheet { amount.assertIsDisplayed() } }
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

/** From the already-open Payment account screen, taps the card to reach the card management page. */
fun BaseTestCase.openTangemPayCardPageFromPaymentAccount() {
    step("Assert payment account balance is displayed") {
        awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
    }
    step("Click on 'Card' button") {
        onTangemPayMainScreen { cardButton.clickWithAssertion() }
    }
    step("Assert card page 'More' button is displayed") {
        awaitSuccess { onTangemPayCardPageScreen { moreButton.assertIsDisplayed() } }
    }
}

/** From the card page, taps the 'Add to wallet' banner and waits for the Google Pay guide. */
fun BaseTestCase.openTangemPayAddToWalletGuide() {
    step("Click on 'Add to wallet' banner") {
        awaitSuccess { onTangemPayCardPageScreen { addToWalletBanner.assertIsDisplayed() } }
        onTangemPayCardPageScreen { addToWalletBanner.performClick() }
    }
    step("Assert Add to wallet guide is displayed") {
        awaitSuccess { onTangemPayAddToWalletGuideScreen { container.assertIsDisplayed() } }
    }
}

/** From the card page, taps the 'Show details' row and waits for the revealed card number. */
fun BaseTestCase.revealCardDetailsFromCardPage() {
    step("Click on 'Show details' row") {
        onTangemPayCardPageScreen { showDetailsButton.clickWithAssertion() }
    }
    step("Assert revealed card number is displayed") {
        awaitSuccess { onTangemPayCardPageScreen { numberValue.assertIsDisplayed() } }
    }
}

/** Opens the card page and taps the card name to reach the rename screen. */
fun BaseTestCase.openTangemPayCardRename() {
    openTangemPayCardPage()
    step("Click on card name to start renaming") {
        awaitSuccess { onTangemPayCardPageScreen { cardNameEditButton.assertIsDisplayed() } }
        onTangemPayCardPageScreen { cardNameEditButton.performClick() }
    }
    step("Assert card rename screen is displayed") {
        awaitSuccess {
            onTangemPayCardRenameScreen {
                nameField.assertIsDisplayed()
                doneButton.assertIsDisplayed()
            }
        }
    }
}

/** Opens the card page and taps the 'PIN code' row to reach the PIN entry screen (requires an unset PIN). */
fun BaseTestCase.openTangemPayChangePin() {
    openTangemPayCardPage()
    step("Click on 'PIN code' row") {
        onTangemPayCardPageScreen { changePinRow.clickWithAssertion() }
    }
    step("Assert PIN entry screen is displayed") {
        awaitSuccess {
            onTangemPayChangePinScreen {
                title.assertIsDisplayed()
                inputField.assertIsDisplayed()
            }
        }
    }
}

/** Opens the card page and taps the 'PIN code' row to reach the current-PIN sheet (requires a PIN already set). */
fun BaseTestCase.openTangemPayViewPin() {
    openTangemPayCardPage()
    step("Click on 'PIN code' row") {
        onTangemPayCardPageScreen { changePinRow.clickWithAssertion() }
    }
    step("Assert current PIN sheet is displayed") {
        awaitSuccess { onTangemPayViewPinSheet { title.assertIsDisplayed() } }
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

/** From the card page (card active), taps the Freeze row and confirms via the confirmation sheet. */
fun BaseTestCase.freezeCardFromCardPage() {
    step("Click on 'Freeze' card row") {
        onTangemPayCardPageScreen { freezeCardRowActive.clickWithAssertion() }
    }
    step("Assert freeze confirmation sheet is displayed") {
        awaitSuccess { onTangemPayFreezeConfirmation { freezeTitle.assertIsDisplayed() } }
    }
    step("Click on 'Submit' button (confirm freeze)") {
        onTangemPayFreezeConfirmation { submitButton.clickWithAssertion() }
    }
}

/** From the card page (card frozen), taps the Unfreeze row and confirms via the confirmation sheet. */
fun BaseTestCase.unfreezeCardFromCardPage() {
    step("Click on 'Unfreeze' card row") {
        onTangemPayCardPageScreen { unfreezeCardRow.clickWithAssertion() }
    }
    step("Assert unfreeze confirmation sheet is displayed") {
        awaitSuccess { onTangemPayFreezeConfirmation { unfreezeTitle.assertIsDisplayed() } }
    }
    step("Click on 'Submit' button (confirm unfreeze)") {
        onTangemPayFreezeConfirmation { submitButton.clickWithAssertion() }
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

/** Imports the hot wallet, opens Withdraw, dismisses the note + stories, and lands on the Swap screen. */
fun BaseTestCase.openTangemPayWithdrawSwapScreen() {
    openTangemPay()
    step("Assert initial balance contains '10'") {
        onTangemPayMainScreen { balance.assertTextContainsSafe("10", substring = true) }
    }
    step("Click on 'Withdraw' action chip") {
        onTangemPayMainScreen { withdrawButton.clickWithAssertion() }
    }
    step("Acknowledge withdrawal note sheet") {
        onTangemPayWithdrawNoteSheet {
            title.assertIsDisplayed()
            gotItButton.clickWithAssertion()
        }
    }
    // Stories auto-advance; a tap can miss mid-animation — retry the close until the Swap screen shows.
    step("Close 'Swap stories' and land on the 'Swap' screen (USDC pre-filled as source)") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { onSwapStoriesScreen { closeButton.performClick() } }
            runCatching { onSwapTokenScreen { title.assertIsDisplayed() } }.isSuccess
        }
    }
}

/** On the withdraw Swap screen, picks [tokenName] as the receive token from the 'Main account' section. */
fun BaseTestCase.chooseWithdrawReceiveToken(tokenName: String) {
    step("Click on 'Choose token' button (to)") {
        onSwapTokenScreen { chooseTokenButton.clickWithAssertion() }
    }
    step("Click on 'Main account'") {
        onSwapSelectTokenScreen { tokenWithName("Main account").clickWithAssertion() }
    }
    step("Click on token '$tokenName'") {
        waitForIdle()
        onSwapSelectTokenScreen { tokenWithName(tokenName).clickWithAssertion() }
    }
}

// The withdraw HoldToConfirm re-disables during the ~10s quote refresh; re-hold each poll until it takes.
fun BaseTestCase.confirmTangemPayWithdrawByHolding(accessCode: String) {
    val buttonMatcher = hasTestTag(BaseButtonTestTags.BUTTON)
    val accessCodeInput = hasTestTag(HotWalletAccessCodeTestTags.ACCESS_CODE_INPUT)
    val swapInProgress = hasText(getResourceString(CoreUiR.string.swap_in_progress))
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
        val buttons = composeTestRule.onAllNodes(buttonMatcher)
        val count = buttons.fetchSemanticsNodes().size
        if (count > 0) {
            // HoldToConfirm is always last — withdraw renders an extra BASE_BUTTON for notifications.
            runCatching {
                buttons[count - 1].performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
            }
        }
        composeTestRule.onAllNodes(accessCodeInput).fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(swapInProgress, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    if (composeTestRule.onAllNodes(accessCodeInput).fetchSemanticsNodes().isNotEmpty()) {
        composeTestRule.onNode(accessCodeInput).performTextInput(accessCode)
        waitForIdle()
    }
}

/** Enters [amount] into the withdraw Swap 'from' field and dismisses the keyboard. */
fun BaseTestCase.enterWithdrawAmount(amount: String) {
    step("Enter withdraw amount '$amount'") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { textInput.assertIsDisplayed() } }
        onSwapTokenScreen {
            textInput.clickWithAssertion()
            textInput.performTextReplacement(amount)
        }
    }
    step("Dismiss keyboard") {
        Espresso.closeSoftKeyboard()
        waitForIdle()
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