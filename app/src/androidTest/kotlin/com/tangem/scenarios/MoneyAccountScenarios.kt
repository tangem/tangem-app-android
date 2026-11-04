package com.tangem.scenarios

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.screens.VirtualAccountRequisitesBottomSheetPageObject
import com.tangem.screens.moneyaccount.onMoneyAccountAddFundsSheet
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.onTxHistoryScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import com.tangem.screens.onVirtualAccountRequisitesBottomSheet
import io.qameta.allure.kotlin.Allure.step
import org.junit.Assert.assertFalse

fun BaseTestCase.openMainWithMoneyAccountWallet(dismissMarketsTooltip: Boolean = true) {
    step("Import hot wallet from Tangem Pay seed phrase") {
        openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, dismissMarketsTooltip = dismissMarketsTooltip)
    }
    step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
}

fun BaseTestCase.openMoneyAccount() {
    openMainWithMoneyAccountWallet()
    step("Click on 'Money account' row") {
        awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) { onMoneyAccountScreen { mainRow.assertIsDisplayed() } }
        clickMoneyAccountRowUntilDetailsOpen()
    }
}

// The row renders before the account status settles, and a tap in that window is swallowed.
@OptIn(ExperimentalTestApi::class)
fun BaseTestCase.clickMoneyAccountRowUntilDetailsOpen() {
    var detailsBalance: SemanticsMatcher? = null
    onMoneyAccountScreen { detailsBalance = balanceMatcher }
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        runCatching { onMoneyAccountScreen { clickMainRow() } }
        runCatching {
            composeTestRule.waitUntilAtLeastOneExists(checkNotNull(detailsBalance), DETAILS_OPEN_TIMEOUT_MILLIS)
        }.isSuccess
    }
}

fun BaseTestCase.openMoneyAccountAddFunds() {
    step("Click on 'Add funds' button") {
        awaitSuccess { onMoneyAccountScreen { addFundsButton.assertIsDisplayed() } }
        onMoneyAccountScreen { addFundsButton.performClick() }
    }
    step("Assert 'Add funds' sheet is displayed") {
        awaitSuccess { onMoneyAccountAddFundsSheet { bankTransferOption.assertIsDisplayed() } }
    }
}

fun BaseTestCase.openMoneyAccountSendHub() {
    step("Click on 'Send' button") {
        awaitSuccess { onMoneyAccountScreen { sendButton.assertIsDisplayed() } }
        onMoneyAccountScreen { sendButton.performClick() }
    }
    step("Assert 'Send' hub screen is displayed") {
        awaitSuccess { onTangemPayWithdrawHubScreen { withinPortfolioCard.assertIsDisplayed() } }
    }
}

fun BaseTestCase.openMoneyAccountBankDetails() {
    openMoneyAccountAddFunds()
    step("Click on 'From a bank account' option") {
        onMoneyAccountAddFundsSheet { bankTransferOption.performClick() }
    }
    step("Assert 'Account details' screen is displayed") {
        awaitSuccess {
            onVirtualAccountRequisitesBottomSheet {
                requisiteValue(VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER).assertIsDisplayed()
            }
        }
        onVirtualAccountRequisitesBottomSheet { accountDetailsTitle.assertIsDisplayed() }
    }
}

fun BaseTestCase.returnToMainFromMoneyAccount() {
    step("Click on system 'Back' button") { device.uiDevice.pressBack() }
    step("Assert 'Money account' row is displayed") {
        awaitSuccess { onMoneyAccountScreen { mainRow.assertIsDisplayed() } }
    }
}

@OptIn(ExperimentalTestApi::class)
fun BaseTestCase.refreshMainUntilMoneyAccountBalance(balance: String) {
    var rowWithBalance: SemanticsMatcher? = null
    onMoneyAccountScreen { rowWithBalance = mainRowWithTextMatcher(balance) }
    step("Refresh 'Main' until the 'Money account' row shows '$balance'") {
        refreshMainUntil {
            composeTestRule.waitUntilAtLeastOneExists(checkNotNull(rowWithBalance), BALANCE_RELOAD_TIMEOUT_MILLIS)
        }
    }
}

fun BaseTestCase.refreshMainUntilMoneyAccountCacheIcon() {
    step("Refresh 'Main' until the 'Money account' row shows the cached-data icon") {
        refreshMainUntil {
            composeTestRule.waitUntil(timeoutMillis = BALANCE_RELOAD_TIMEOUT_MILLIS) {
                var isShown = false
                onMoneyAccountScreen { isShown = isMainRowCacheIconShown() }
                isShown
            }
        }
    }
}

fun BaseTestCase.refreshMainUntilCustomerSummaryRequested() {
    val requestsBefore = getWireMockRequestCount("GET", CUSTOMER_SUMMARY_PATH)
    step("Refresh 'Main' until '$CUSTOMER_SUMMARY_PATH' is requested") {
        refreshMainUntil {
            composeTestRule.waitUntil(timeoutMillis = BALANCE_RELOAD_TIMEOUT_MILLIS) {
                getWireMockRequestCount("GET", CUSTOMER_SUMMARY_PATH) > requestsBefore
            }
        }
    }
}

// A pull can land before the switched mock reaches the app, so pull until the expected change shows up.
private fun BaseTestCase.refreshMainUntil(awaitChange: () -> Unit) {
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        closeFinishWalletActivationSheetIfShown()
        pullToRefreshMainScreen()
        runCatching(awaitChange).isSuccess
    }
}

fun BaseTestCase.assertMoneyAccountRowShowsCachedBalance(balance: String) {
    step("Assert 'Money account' row shows the cached '$balance'") {
        waitForMoneyAccountRowWithText(balance)
    }
    step("Assert cached-data icon is displayed on the 'Money account' row") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onMoneyAccountScreen { mainRowCacheIcon.assertIsDisplayed() }
        }
    }
}

fun BaseTestCase.assertMoneyAccountActionsDisabled() {
    step("Assert 'Add funds' and 'Send' buttons are disabled") {
        onMoneyAccountScreen {
            addFundsButton.assertIsNotEnabled()
            sendButton.assertIsNotEnabled()
        }
    }
}

fun BaseTestCase.refreshMoneyAccountUntilTransaction(title: String) {
    step("Refresh 'Money account' screen until the '$title' transaction is displayed") {
        // Reopening the account shows the cached history; only a pull re-reads it, and a pull can land too early.
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onMoneyAccountScreen { swipeDownToRefresh() }
            runCatching {
                composeTestRule.waitUntil(timeoutMillis = BALANCE_RELOAD_TIMEOUT_MILLIS) {
                    var isShown = false
                    onTxHistoryScreen { isShown = isTransactionItemShown(title) }
                    isShown
                }
            }.isSuccess
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun BaseTestCase.waitForMoneyAccountRowWithText(text: String, timeoutMillis: Long = WAIT_UNTIL_TIMEOUT_LONG) {
    var rowWithText: SemanticsMatcher? = null
    onMoneyAccountScreen { rowWithText = mainRowWithTextMatcher(text) }
    composeTestRule.waitUntilAtLeastOneExists(checkNotNull(rowWithText), timeoutMillis)
}

fun BaseTestCase.waitForMoneyAccountRowWithTextNeverShowing(
    text: String,
    forbiddenText: String,
    timeoutMillis: Long = WAIT_UNTIL_TIMEOUT_LONG,
) {
    composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
        var isForbiddenShown = false
        var isExpectedShown = false
        onMoneyAccountScreen {
            isForbiddenShown = isMainRowWithTextShown(forbiddenText)
            isExpectedShown = isMainRowWithTextShown(text)
        }
        assertFalse("'Money account' row must not show '$forbiddenText'", isForbiddenShown)
        isExpectedShown
    }
}

fun BaseTestCase.setMoneyAccountWithTwoAccountsScenarios() {
    val moneyAccountScenario = "tangem_pay_money_account"
    val moneyAccountActiveState = "AccountActive"

    setTangemPayWithTwoAccountsScenarios()
    step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
        setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
    }
}

fun BaseTestCase.openMainWithMoneyAccountAndTwoAccounts() {
    val activeAccountBalance = "$25.00"

    importPayWalletWithTwoSyncedAccounts()
    step("Assert 'Money account' row shows '$activeAccountBalance'") {
        waitForMoneyAccountRowWithText(activeAccountBalance)
    }
    step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
}

private const val DETAILS_OPEN_TIMEOUT_MILLIS = 3_000L
private const val BALANCE_RELOAD_TIMEOUT_MILLIS = 5_000L
private const val CUSTOMER_SUMMARY_PATH = "/bff-v2/v1/customer/summary"