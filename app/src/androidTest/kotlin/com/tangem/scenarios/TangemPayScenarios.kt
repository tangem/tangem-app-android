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
import com.tangem.common.extensions.assertVisibility
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.HotWalletAccessCodeTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step

/** A fiat balance rendered with a currency symbol and two decimals, e.g. `$1,234.00`. */
private val FIAT_BALANCE_REGEX = """^\D\d[\d,]*\.\d{2}$""".toRegex()

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

/** Imports the hot wallet and stays on Main with the Tangem Pay tile rendered. */
fun BaseTestCase.openMainScreenWithTangemPayTile() {
    step("Import hot wallet from Tangem Pay seed phrase (with access code)") {
        openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
    }
    step("Assert Tangem Pay tile is displayed") {
        awaitSuccess { onTangemPayMainScreen { mainScreenTile.assertIsDisplayed() } }
    }
}

/**
 * Waits for the KYC screen the card-issuing flow hands over to and leaves it back to Main. The Sumsub SDK
 * never launches under WireMock — its access-token endpoint is unmocked — so the screen stays on its loader.
 */
fun BaseTestCase.exitTangemPayKycScreen(timeoutMillis: Long = WAIT_UNTIL_TIMEOUT_LONG) {
    step("Assert KYC screen is displayed") {
        awaitSuccess(timeoutMillis = timeoutMillis) {
            onTangemPayKycScreen { screenContainer.assertIsDisplayed() }
        }
    }
    step("Click on 'Back' button") {
        onTangemPayKycScreen { backButton.clickWithAssertion() }
    }
    step("Assert 'Main' screen is displayed") {
        awaitSuccess { onMainScreen { screenContainer.assertIsDisplayed() } }
    }
}

/** Scrolls the payment-account list down to the [name] transaction row and asserts it is displayed. */
fun BaseTestCase.assertTangemPayTransactionRowDisplayed(name: String) {
    step("Assert '$name' transaction row is displayed") {
        // When the row comes from the cache, it lands only once the failing history request gives up.
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen {
                scrollToTransactionWithText(name)
                transactionRowWithText(name).assertIsDisplayed()
            }
        }
    }
}

/** Asserts the Main tile renders the persisted account data flagged as stale. */
fun BaseTestCase.assertTangemPayTileShowsCachedData(balance: String, cardsSubtitle: String) {
    step("Assert Main tile shows the cached balance '$balance'") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { tileBalance.assertTextContainsSafe(balance, substring = true) }
        }
    }
    step("Assert Main tile shows the cached card data '$cardsSubtitle'") {
        onTangemPayMainScreen { tileWithSubtitle(cardsSubtitle).assertIsDisplayed() }
    }
    step("Assert cached-data icon is displayed on the Tangem Pay tile") {
        // The icon is composed only while the tile serves cached data, and it only turns up once the
        // failing refresh has downgraded the status to ONLY_CACHE.
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { tileCacheIcon.assertIsDisplayed() }
        }
    }
}

/** Asserts the session-expired banner and its 'Renew session' action are displayed. */
fun BaseTestCase.assertTangemPaySessionExpiredBanner() {
    step("Assert 'Payment account session expired' banner is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { sessionExpiredBannerTitle.assertIsDisplayed() }
        }
        onTangemPayMainScreen { sessionExpiredBannerMessage.assertIsDisplayed() }
    }
    step("Assert 'Renew session' button is displayed on the banner") {
        onTangemPayMainScreen { renewSessionButton.assertIsDisplayed() }
    }
}

/** Asserts the connection-issues banner is displayed. */
fun BaseTestCase.assertTangemPayConnectionIssuesBanner() {
    step("Assert 'Connection issues' banner is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { connectionIssuesBannerTitle.assertIsDisplayed() }
        }
        onTangemPayMainScreen { connectionIssuesBannerMessage.assertIsDisplayed() }
    }
}

/** Asserts the payment account keeps serving its cached data with every money action out of reach. */
fun BaseTestCase.assertTangemPayStaleState(cachedBalance: String, cachedTransaction: String) {
    step("Assert payment account balance shows the cached '$cachedBalance'") {
        onTangemPayMainScreen { balance.assertTextContainsSafe(cachedBalance, substring = true) }
    }
    step("Assert 'Add funds' and 'Withdraw' buttons are disabled") {
        onTangemPayMainScreen {
            addFundsButtonControl.assertIsNotEnabled()
            withdrawButtonControl.assertIsNotEnabled()
        }
    }
    step("Assert card is disabled") {
        onTangemPayMainScreen { cardButton.assertIsNotEnabled() }
    }
    assertTangemPayTransactionRowDisplayed(cachedTransaction)
}

/** From the open payment account screen, opens the details bottom sheet for transaction [name]. */
fun BaseTestCase.openTangemPayTransactionDetails(name: String) {
    assertTangemPayTransactionRowDisplayed(name)
    step("Click on '$name' transaction row") {
        onTangemPayMainScreen { transactionRowWithText(name).performClick() }
    }
    step("Assert transaction details bottom sheet is displayed") {
        awaitSuccess { onTangemPayTransactionDetailsSheet { amount.assertIsDisplayed() } }
    }
}

/** On the open transaction details sheet, asserts the Card field shows [cardNumber] with [cardName] under it. */
fun BaseTestCase.assertTangemPayTransactionCardField(cardNumber: String, cardName: String) {
    step("Assert 'Card' field shows '$cardNumber' and '$cardName'") {
        // The Card row arrives with the separate single-transaction request, after the sheet opens.
        awaitSuccess {
            onTangemPayTransactionDetailsSheet {
                cardFieldTitle.assertIsDisplayed()
                cardFieldValue(cardNumber).assertIsDisplayed()
                cardFieldValue(cardName).assertIsDisplayed()
            }
        }
    }
}

/** Asserts the payment-account elements that are on screen while the transaction history is empty. */
fun BaseTestCase.assertTangemPayEmptyPaymentAccountElements() {
    step("Assert 'Payment account' title and 'Multinetwork' subtitle are displayed") {
        onTangemPayMainScreen {
            paymentAccountTitle.assertIsDisplayed()
            multinetworkSubtitle.assertIsDisplayed()
        }
    }
    step("Assert the balance is a fiat amount with two decimals") {
        var balanceText = ""
        // The screen first paints a dash for the not-yet-loaded balance, so the format is awaited.
        awaitSuccess {
            onTangemPayMainScreen { balanceText = balance.extractText() }
            if (!FIAT_BALANCE_REGEX.matches(balanceText)) {
                throw AssertionError("Balance should be formatted as \$X.XX, but was '$balanceText'")
            }
        }
    }
    step("Assert card and 'Add card' buttons are displayed") {
        onTangemPayMainScreen {
            cardButton.assertIsDisplayed()
            addCardButton.assertIsDisplayed()
        }
    }
    step("Assert 'Add funds' and 'Withdraw' buttons are displayed") {
        onTangemPayMainScreen {
            topUpButton.assertIsDisplayed()
            withdrawButton.assertIsDisplayed()
        }
    }
    step("Assert empty transaction history state is displayed") {
        awaitSuccess {
            onTangemPayMainScreen {
                scrollToHistoryStateBlock()
                emptyHistoryIcon.assertIsDisplayed()
                emptyHistoryText.assertIsDisplayed()
            }
        }
    }
}

/** On the open transaction details sheet, asserts the fields every spend transaction shows. */
fun BaseTestCase.assertTangemPaySpendDetails(
    expectedDate: String,
    expectedMerchant: String,
    expectedCategory: String,
    expectedAmount: String,
) {
    step("Assert 'Purchase' title, '$expectedDate' date and category icon are displayed") {
        onTangemPayTransactionDetailsSheet {
            purchaseTitle.assertIsDisplayed()
            date(expectedDate).assertIsDisplayed()
            transactionIcon.assertIsDisplayed()
        }
    }
    step("Assert '$expectedMerchant', '$expectedCategory' and '$expectedAmount' are displayed") {
        onTangemPayTransactionDetailsSheet {
            merchantName(expectedMerchant).assertIsDisplayed()
            category(expectedCategory).assertIsDisplayed()
            amount.assertTextContainsSafe(expectedAmount, substring = true)
        }
    }
    step("Assert 'Get help' button is displayed") {
        onTangemPayTransactionDetailsSheet { getHelpButton.assertIsDisplayed() }
    }
}

/** On the open transaction details sheet, asserts the Card field failed to load. */
fun BaseTestCase.assertTangemPayTransactionCardFieldError() {
    step("Assert 'Card' field shows the 'Error loading' state") {
        // The error lands with the failing single-transaction request, after the sheet opens.
        awaitSuccess {
            onTangemPayTransactionDetailsSheet {
                cardFieldTitle.assertIsDisplayed()
                cardFieldErrorText.assertIsDisplayed()
            }
        }
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

/** Imports the hot wallet, opens Withdraw, dismisses the stories, and lands on the Withdraw swap screen. */
fun BaseTestCase.openTangemPayWithdrawSwapScreen() {
    openTangemPay()
    step("Assert initial balance contains '10'") {
        onTangemPayMainScreen { balance.assertTextContainsSafe("10", substring = true) }
    }
    step("Click on 'Withdraw' action chip") {
        onTangemPayMainScreen { withdrawButton.clickWithAssertion() }
    }
    // Stories auto-advance; a tap can miss mid-animation — retry the close until the Withdraw screen shows.
    step("Close 'Swap stories' and land on the 'Withdraw' screen (USDC pre-filled as source)") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { onSwapStoriesScreen { closeButton.performClick() } }
            runCatching { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }.isSuccess
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

/**
 * From the open payment account screen, taps '+' and walks the 'Order new card' type step to land on
 * the 'Issue an additional card?' bottom sheet.
 */
fun BaseTestCase.openTangemPayIssueCardSheet() {
    step("Click on 'Add card' button") {
        awaitSuccess { onTangemPayMainScreen { addCardButton.assertIsDisplayed() } }
        onTangemPayMainScreen { addCardButton.performClick() }
    }
    step("Assert 'Order new card' screen is displayed with 'Virtual' selected") {
        awaitSuccess { onTangemPayOrderCardTypeScreen { title.assertIsDisplayed() } }
        onTangemPayOrderCardTypeScreen { virtualTab.assertIsDisplayed() }
    }
    step("Click on 'Select' button") {
        // The button stays disabled until the offers request resolves the virtual issue fee.
        awaitSuccess { onTangemPayOrderCardTypeScreen { selectButton.assertIsEnabled() } }
        onTangemPayOrderCardTypeScreen { selectButton.performClick() }
    }
    step("Assert 'Issue an additional card?' sheet is displayed") {
        awaitSuccess { onTangemPayIssueCardSheet { title.assertIsDisplayed() } }
    }
}

/** On the open issue sheet, asserts the header block and the issue fee value. */
fun BaseTestCase.assertTangemPayIssueCardSheetContent(expectedFee: String) {
    step("Assert issue sheet title and description are displayed") {
        onTangemPayIssueCardSheet {
            title.assertIsDisplayed()
            description.assertIsDisplayed()
        }
    }
    step("Assert issue fee equals '$expectedFee'") {
        awaitSuccess { onTangemPayIssueCardSheet { feeValue.assertTextEquals(expectedFee) } }
    }
}

/** On the open issue sheet, asserts the insufficient-funds block with the issue action out of reach. */
fun BaseTestCase.assertTangemPayIssueCardUnableToCoverFee() {
    step("Assert 'Unable to cover fee' notification is displayed") {
        awaitSuccess { onTangemPayIssueCardSheet { insufficientFundsTitle.assertIsDisplayed() } }
        onTangemPayIssueCardSheet { insufficientFundsSubtitle.assertIsDisplayed() }
    }
    step("Assert 'Issue card' button is disabled and 'Add funds' button is displayed") {
        onTangemPayIssueCardSheet {
            issueCardButton.assertIsNotEnabled()
            addFundsButton.assertIsDisplayed()
        }
    }
}

/** Confirms the issue on the open sheet and waits for the payment account 'Issuing' banner. */
fun BaseTestCase.confirmTangemPayCardIssue() {
    step("Click on 'Issue card' button") {
        onTangemPayIssueCardSheet { issueCardButton.clickWithAssertion() }
    }
    step("Assert 'Issuing a new digital card' banner is displayed on the payment account") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { issuingCardBanner.assertIsDisplayed() }
        }
    }
}

/**
 * From the Main screen, issues one additional card end to end and comes back to Main. [cardsStateAfterIssue]
 * is the `tangem_pay_card_issue` state that surfaces the freshly issued card in `customer/me`.
 */
fun BaseTestCase.issueAdditionalCardFromMainScreen(cardsStateAfterIssue: String) {
    val cardIssueScenario = "tangem_pay_card_issue"
    val issueOrderScenario = "tangem_pay_card_issue_order"
    val issueOrderStartedState = "Started"
    val issueOrderCompletedState = "Completed"
    val issueOrdersScenario = "tangem_pay_card_issue_orders"
    val issueOrdersEmptyState = "Empty"

    step("Click on Tangem Pay tile") {
        onTangemPayMainScreen { mainScreenTile.performClick() }
    }
    step("Assert payment account balance is displayed") {
        awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
    }
    openTangemPayIssueCardSheet()
    confirmTangemPayCardIssue()
    step("Switch WireMock scenario '$cardIssueScenario' to '$cardsStateAfterIssue'") {
        setWireMockScenarioState(cardIssueScenario, cardsStateAfterIssue)
    }
    step("Switch WireMock scenario '$issueOrderScenario' to '$issueOrderCompletedState'") {
        setWireMockScenarioState(issueOrderScenario, issueOrderCompletedState)
    }
    step("Refresh until the issuing banner is gone") {
        // Pull-to-refresh is idempotent, so it is repeated per iteration until the completed order lands.
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            pullToRefreshTangemPay()
            runCatching { onTangemPayMainScreen { issuingCardBanner.assertDoesNotExist() } }.isSuccess
        }
    }
    step("Re-arm the issue scenarios for the next card") {
        setWireMockScenarioState(issueOrderScenario, issueOrderStartedState)
        setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
    }
    step("Return to the Main screen") {
        Espresso.pressBack()
        awaitSuccess { onTangemPayMainScreen { mainScreenTile.assertIsDisplayed() } }
    }
}

/** From the open payment account screen, opens the 'Add funds' bottom sheet. */
fun BaseTestCase.openTangemPayAddFunds() {
    step("Click on 'Add funds' button") {
        awaitSuccess { onTangemPayMainScreen { topUpButton.assertIsDisplayed() } }
        onTangemPayMainScreen { topUpButton.performClick() }
    }
    step("Assert 'Add funds' sheet is displayed") {
        awaitSuccess { onTangemPayAddFundsSheet { title.assertIsDisplayed() } }
    }
}

/** From the open payment account screen, opens the bank-transfer (virtual account) deposit sheet. */
fun BaseTestCase.openTangemPayBankTransferSheet() {
    openTangemPayAddFunds()
    step("Assert 'From a bank account' option is displayed") {
        onTangemPayAddFundsSheet { bankTransferOption.assertIsDisplayed() }
    }
    step("Click on 'From a bank account' option") {
        onTangemPayAddFundsSheet { bankTransferOption.performClick() }
    }
    step("Assert bank transfer deposit sheet is displayed") {
        awaitSuccess { onTangemPayVirtualAccountDepositSheet { showDetailsButton.assertIsDisplayed() } }
    }
}

/** On the open bank-transfer sheet, asserts the [feeType] row shows [expectedTitle] and [expectedValue]. */
fun BaseTestCase.assertVirtualAccountFee(feeType: String, expectedTitle: String, expectedValue: String) {
    step("Assert '$expectedTitle' fee equals '$expectedValue'") {
        awaitSuccess {
            onTangemPayVirtualAccountDepositSheet {
                feeTitle(feeType).assertTextEquals(expectedTitle)
                feeValue(feeType).assertTextEquals(expectedValue)
            }
        }
    }
}

/**
 * Asserts the [feeType] row is absent. Both rows are rendered as shimmer placeholders while the fees load,
 * so call this only after the sheet has settled — an asserted fee row or the error banner.
 */
fun BaseTestCase.assertVirtualAccountFeeNotDisplayed(feeType: String, expectedTitle: String) {
    step("Assert '$expectedTitle' fee row is not displayed") {
        onTangemPayVirtualAccountDepositSheet { feeTitle(feeType).assertVisibility(shouldBeDisplayed = false) }
    }
}