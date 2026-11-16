package com.tangem.scenarios

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankRecipient
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.findWireMockRequestBodiesByPathPattern
import com.tangem.common.utils.findWireMockRequestBodiesNewestFirst
import com.tangem.common.utils.findWireMockRequestHeaderValuesNewestFirst
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.screens.VirtualAccountRequisitesBottomSheetPageObject
import com.tangem.screens.moneyaccount.onMoneyAccountAddFundsSheet
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.onSwapSelectTokenScreen
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTxHistoryScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawErrorScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawPreviewScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawSuccessScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import com.tangem.screens.onVirtualAccountRequisitesBottomSheet
import io.qameta.allure.kotlin.Allure.step
import io.github.kakaocup.kakao.common.utilities.getResourceString
import java.math.BigDecimal
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

fun BaseTestCase.openMainWithMoneyAccountWallet(dismissMarketsTooltip: Boolean = true, accessCode: String = "") {
    step("Import hot wallet from Tangem Pay seed phrase") {
        openMainScreenWithExistingHotWallet(
            seedPhrase = SVS_SEED_PHRASE_12,
            accessCode = accessCode,
            dismissMarketsTooltip = dismissMarketsTooltip,
        )
    }
    if (accessCode.isNotEmpty()) enterHotWalletUnlockAccessCode(accessCode)
    step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
}

fun enableMoneyAccountKeyDerivationWithAccessCode() {
    System.setProperty(DERIVE_WITH_ACCESS_CODE_KEY, "1")
}

fun disableMoneyAccountKeyDerivationWithAccessCode() {
    System.clearProperty(DERIVE_WITH_ACCESS_CODE_KEY)
}

fun BaseTestCase.openMoneyAccount(accessCode: String = "") {
    openMainWithMoneyAccountWallet(accessCode = accessCode)
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

fun BaseTestCase.topUpMoneyAccountFromWallet(amount: String) {
    openMoneyAccountAddFunds()
    step("Click on 'From your Tangem Wallet' option") {
        onMoneyAccountAddFundsSheet { swapOption.performClick() }
    }
    step("Assert 'Add funds' Swap screen is displayed") {
        awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { addFundsTitle.assertIsDisplayed() } }
    }
    step("Enter amount '$amount'") { inputAmount(amount) }
    step("Assert Transfer mode is ready without a provider") { assertTransferReady() }
    step("Assert network fee is displayed") { waitForFeeDisplayed() }
    step("Hold to confirm the transfer") { holdToConfirmTransferUntilInProgress() }
    step("Assert 'Transfer in progress' screen is displayed") {
        awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) { onSwapSuccessScreen { transferInProgressTitle.assertIsDisplayed() } }
    }
    step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
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

fun BaseTestCase.setMoneyAccountBankWithdrawScenarios() {
    val eligibilityState = "PaeraCustomer"
    val moneyAccountScenario = "tangem_pay_money_account"
    val moneyAccountActiveState = "AccountActive"
    val channelsBankOfframpState = "BankOfframp"

    step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
        setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
    }
    step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
        setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
    }
    step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBankOfframpState'") {
        setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBankOfframpState)
    }
}

fun BaseTestCase.openMoneyAccountBankWithdrawPreview(recipient: TangemPayBankRecipient, amount: String) {
    openMoneyAccountSendHub()
    openTangemPayRecentRecipient(recipient)
    enterTangemPayBankWithdrawAmount(amount)
    step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
}

data class MoneyAccountSignedTransfer(val tokenContract: String, val callData: String)

fun polygonUsdcTransfer(recipient: String, amount: String): MoneyAccountSignedTransfer {
    val amountUnits = BigDecimal(amount).movePointRight(USDC_DECIMALS).toBigIntegerExact()
    return MoneyAccountSignedTransfer(
        tokenContract = POLYGON_USDC_CONTRACT,
        callData = ERC20_TRANSFER_SELECTOR +
            recipient.removePrefix("0x").lowercase().padStart(ABI_WORD_HEX_LENGTH, '0') +
            amountUnits.toString(HEX_RADIX).padStart(ABI_WORD_HEX_LENGTH, '0'),
    )
}

fun findMoneyAccountTransfersSignedSince(signsBefore: Int): List<MoneyAccountSignedTransfer> {
    val bodies = findWireMockRequestBodiesNewestFirst("POST", GASLESS_SIGN_ENDPOINT_PATTERN)
    return bodies.take((bodies.size - signsBefore).coerceAtLeast(0)).map { body ->
        val transaction = JSONObject(body).getJSONObject("gaslessTransaction").getJSONObject("transaction")
        MoneyAccountSignedTransfer(
            tokenContract = transaction.getString("to").lowercase(),
            callData = transaction.getString("data").lowercase(),
        )
    }
}

fun findMoneyAccountRouteIdempotencyKeysSince(routesBefore: Int): List<String> {
    val keys = findWireMockRequestHeaderValuesNewestFirst("POST", BANK_OFFRAMP_ROUTE_ENDPOINT, IDEMPOTENCY_KEY_HEADER)
    return keys.take((keys.size - routesBefore).coerceAtLeast(0))
}

fun countMoneyAccountBankWithdrawRoutes(): Int = getWireMockRequestCount("POST", BANK_OFFRAMP_ROUTE_ENDPOINT)

fun countMoneyAccountTransferSignRequests(): Int = getWireMockRequestCount("POST", GASLESS_SIGN_ENDPOINT_PATTERN)

fun BaseTestCase.clickMoneyAccountBankWithdrawPay(total: String) {
    step("Click on 'Pay $total' button") {
        awaitSuccess { onTangemPayBankWithdrawPreviewScreen { payButton(total).assertIsEnabled() } }
        onTangemPayBankWithdrawPreviewScreen { payButton(total).performClick() }
    }
}

fun BaseTestCase.closeMoneyAccountBankWithdrawPreview() {
    step("Click on 'Close' button") {
        onTangemPayBankWithdrawPreviewScreen { closeButton.performClick() }
    }
    step("Assert 'Money account' screen is displayed") {
        awaitSuccess { onMoneyAccountScreen { sendButton.assertIsDisplayed() } }
    }
}

fun BaseTestCase.closeMoneyAccountBankWithdrawSuccess() {
    step("Click on 'Close' button") {
        onTangemPayBankWithdrawSuccessScreen { closeButton.performClick() }
    }
    step("Assert 'Money account' screen is displayed") {
        awaitSuccess { onMoneyAccountScreen { sendButton.assertIsDisplayed() } }
    }
}

fun BaseTestCase.closeMoneyAccountBankWithdrawError() {
    step("Click on 'Close' button") {
        onTangemPayBankWithdrawErrorScreen { closeButton.performClick() }
    }
    step("Assert 'Money account' screen is displayed") {
        awaitSuccess { onMoneyAccountScreen { sendButton.assertIsDisplayed() } }
    }
}

fun BaseTestCase.chooseMoneyAccountWithdrawReceiveToken(accountName: String, tokenName: String, networkName: String) {
    step("Click on 'Choose token' button in the 'To' block") {
        onSwapTokenScreen { receiveChooseTokenButton.performClick() }
    }
    step("Expand account '$accountName' in receive selector") {
        awaitSuccess { onSwapSelectTokenScreen { tokenWithName(accountName).performClick() } }
    }
    step("Click on '$tokenName' on '$networkName'") {
        awaitSuccess { onSwapSelectTokenScreen { tokenWithNameAndNetwork(tokenName, networkName).performClick() } }
    }
}

fun BaseTestCase.openMoneyAccountWithdrawSwapWithAmount(
    sourceBalance: String,
    accountName: String,
    tokenName: String,
    networkName: String,
    amount: String,
) {
    openMoneyAccountSendHub()
    step("Click on 'Within your portfolio' card") {
        onTangemPayWithdrawHubScreen { withinPortfolioCard.performClick() }
    }
    step("Assert 'Withdraw' Swap screen is displayed") {
        awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }
    }
    step("Assert source balance contains '$sourceBalance'") {
        awaitSuccess { onSwapTokenScreen { swapCardBalance.assertTextContains(sourceBalance, substring = true) } }
    }
    chooseMoneyAccountWithdrawReceiveToken(accountName, tokenName, networkName)
    enterWithdrawAmount(amount)
    step("Assert receive amount is displayed") {
        awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { receiveAmount.assertIsDisplayed() } }
    }
}

fun BaseTestCase.returnToMoneyAccountFromSendHub() {
    step("Click on system 'Back' button") { device.uiDevice.pressBack() }
    step("Assert 'Money account' screen is displayed") {
        awaitSuccess { onMoneyAccountScreen { sendButton.assertIsDisplayed() } }
    }
}

fun countMoneyAccountNetworkFeeEstimates(): Int =
    findWireMockRequestBodiesByPathPattern("POST", POLYGON_RPC_PATH_PATTERN).count { body ->
        body.contains(ESTIMATE_GAS_METHOD) && body.contains(MONEY_ACCOUNT_ADDRESS_HEX, ignoreCase = true)
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

fun BaseTestCase.closeMoneyAccountBankDetails() {
    step("Click on system 'Back' button") { device.uiDevice.pressBack() }
    step("Assert 'Money account' screen is displayed") {
        awaitSuccess { onMoneyAccountScreen { sendButton.assertIsDisplayed() } }
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

fun BaseTestCase.refreshMainUntilCustomerSummaryRequested() {
    val requestCountBefore = getWireMockRequestCount(method = "GET", urlPathPattern = CUSTOMER_SUMMARY_PATH)
    step("Refresh 'Main' until 'customer/summary' is requested") {
        // A pull during a refresh that is still running is dropped, so pull until the journal shows a new request.
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            closeFinishWalletActivationSheetIfShown()
            pullToRefreshMainScreen()
            runCatching {
                composeTestRule.waitUntil(timeoutMillis = BALANCE_RELOAD_TIMEOUT_MILLIS) {
                    getWireMockRequestCount(method = "GET", urlPathPattern = CUSTOMER_SUMMARY_PATH) > requestCountBefore
                }
            }.isSuccess
        }
    }
}

fun BaseTestCase.refreshMainUntilMoneyAccountRowHidden() {
    step("Refresh 'Main' until the 'Money account' row is hidden") {
        // A pull can land before the switched mock reaches the app, so pull until the row is gone.
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            closeFinishWalletActivationSheetIfShown()
            pullToRefreshMainScreen()
            runCatching {
                composeTestRule.waitUntil(timeoutMillis = BALANCE_RELOAD_TIMEOUT_MILLIS) {
                    var isShown = true
                    onMoneyAccountScreen { isShown = isMainRowShown() }
                    !isShown
                }
            }.isSuccess
        }
    }
}

// Polls the whole window: a cached status or a late fetch can bring the row back after Main has settled.
fun BaseTestCase.assertMoneyAccountRowNeverShown(durationMillis: Long = BALANCE_RELOAD_TIMEOUT_MILLIS) {
    assertForWholeWindow(durationMillis) {
        var isShown = false
        onMoneyAccountScreen { isShown = isMainRowShown() }
        assertFalse("'Money account' row must not be displayed", isShown)
    }
}

fun BaseTestCase.assertMoneyAccountRowKeepsShowing(text: String, durationMillis: Long = BALANCE_RELOAD_TIMEOUT_MILLIS) {
    assertForWholeWindow(durationMillis) {
        var isShown = false
        onMoneyAccountScreen { isShown = isMainRowWithTextShown(text) }
        assertTrue("'Money account' row must keep showing '$text'", isShown)
    }
}

private fun BaseTestCase.assertForWholeWindow(durationMillis: Long, assertion: () -> Unit) {
    try {
        composeTestRule.waitUntil(timeoutMillis = durationMillis) {
            assertion()
            false
        }
    } catch (_: ComposeTimeoutException) {
        // The assertion held for the whole window.
    }
}

fun BaseTestCase.refreshMoneyAccountUntilTransaction(title: String) {
    step("Refresh 'Money account' screen until the '$title' transaction is displayed") {
        refreshMoneyAccountUntil {
            var isShown = false
            onTxHistoryScreen { isShown = isTransactionItemShown(title) }
            isShown
        }
    }
}

fun BaseTestCase.refreshMoneyAccountUntilBalance(expectedBalance: String) {
    step("Refresh 'Money account' screen until the balance is '$expectedBalance'") {
        refreshMoneyAccountUntil {
            var isShown = false
            onMoneyAccountScreen { isShown = isBalanceShown(expectedBalance) }
            isShown
        }
    }
}

// A pull can land before the throttled reload or the switched mock reaches the app, so pull until it shows.
private fun BaseTestCase.refreshMoneyAccountUntil(isChanged: () -> Boolean) {
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        onMoneyAccountScreen { swipeDownToRefresh() }
        runCatching {
            composeTestRule.waitUntil(timeoutMillis = BALANCE_RELOAD_TIMEOUT_MILLIS) { isChanged() }
        }.isSuccess
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

fun BaseTestCase.assertMoneyAccountMenuHasTermsAndSupportOnly(termsTitle: String) {
    val supportTitle = getResourceString(CoreResR.string.tangempay_pay_support)
    val currentPlanTitle = getResourceString(CoreResR.string.tangempay_current_plan_title)
    val closeAccountTitle = getResourceString(CoreResR.string.money_account_close_account)
    onMoneyAccountScreen {
        step("Assert '$termsTitle' menu item is displayed") {
            awaitSuccess { menuItem(termsTitle).assertIsDisplayed() }
        }
        step("Assert '$supportTitle' menu item is displayed") { menuItem(supportTitle).assertIsDisplayed() }
        step("Assert '$termsTitle' menu item is above '$supportTitle'") {
            val termsTop = menuItemTop(termsTitle)
            val supportTop = menuItemTop(supportTitle)
            assertTrue("'$termsTitle' top $termsTop is not above '$supportTitle' top $supportTop", termsTop < supportTop)
        }
        step("Assert '$currentPlanTitle' menu item is not displayed") {
            menuItem(currentPlanTitle).assertDoesNotExist()
        }
        step("Assert '$closeAccountTitle' menu item is not displayed") {
            menuItem(closeAccountTitle).assertDoesNotExist()
        }
    }
}

private const val DETAILS_OPEN_TIMEOUT_MILLIS = 3_000L
private const val BALANCE_RELOAD_TIMEOUT_MILLIS = 5_000L
private const val CUSTOMER_SUMMARY_PATH = "/bff-v2/v1/customer/summary"

private const val BANK_OFFRAMP_ROUTE_ENDPOINT = "/bff-v2/v1/account/bank-offramp/route"
private const val POLYGON_RPC_PATH_PATTERN = "/polygon/?"
private const val ESTIMATE_GAS_METHOD = "eth_estimateGas"
private const val MONEY_ACCOUNT_ADDRESS_HEX = "5aab90356b4cd7fb64f7f70dc6fe063a7b52f011"
private const val GASLESS_SIGN_ENDPOINT_PATTERN = "/gasless/api/v[12]/transaction/sign"
private const val IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key"
private const val DERIVE_WITH_ACCESS_CODE_KEY = "uitest.moneyaccount.derive_with_access_code"
private const val POLYGON_USDC_CONTRACT = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
private const val ERC20_TRANSFER_SELECTOR = "0xa9059cbb"
private const val USDC_DECIMALS = 6
private const val ABI_WORD_HEX_LENGTH = 64
private const val HEX_RADIX = 16