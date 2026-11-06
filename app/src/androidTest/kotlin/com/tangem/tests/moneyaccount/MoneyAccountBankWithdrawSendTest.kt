package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankAccounts.ACME
import com.tangem.common.constants.TangemPayBankAccounts.JOHN
import com.tangem.common.constants.TangemPayBankRoutes
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawAmountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawErrorScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawPreviewScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawSuccessScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Test

@HiltAndroidTest
class MoneyAccountBankWithdrawSendTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val bankAccountsScenario = "tangem_pay_bank_accounts"
    private val twoAccountsState = "TwoAccounts"
    private val routeScenario = "tangem_pay_bank_offramp_route"
    private val optionsScenario = "tangem_pay_bank_offramp_options"
    private val basicState = "Started"
    private val plusState = "Plus"
    private val amount = "1"
    private val achRoute = TangemPayBankRoutes.ACH.copy(fee = "$1.51")
    private val achPayTotal = "$2.51"
    private val plusAchRoute = TangemPayBankRoutes.ACH.copy(fee = "$0.01")
    private val plusAchPayTotal = "$1.01"

    @AllureId("13362")
    @DisplayName("Money account: Pay creates a bank-offramp route and transfers the account USDC to its address")
    @Test
    fun payCreatesRouteAndTransfersUsdcTest() {
        val balanceScenario = "money_account_usdc_balance"
        val balanceAfterWithdrawState = "AfterWithdraw"
        val balanceAfterWithdraw = "$22.49"
        val feesFooter = getResourceString(CoreResR.string.money_account_send_fees_footer, achRoute.title)
        val recipientLabel = getResourceString(CoreResR.string.send_recipient)
        val bankNameLabel = getResourceString(CoreResR.string.tangempay_withdraw_bank_field_bank_name)
        val accountNumberLabel = getResourceString(CoreResR.string.tangempay_withdraw_bank_field_account_number)

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val routesBefore = countMoneyAccountBankWithdrawRoutes()
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Assert '${achRoute.title}' route with fee '${achRoute.fee}' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { routeRow(achRoute).assertIsDisplayed() }
                }
            }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert '${achRoute.title}' route with fee '${achRoute.fee}' is displayed on 'Confirm transfer'") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { routeRow(achRoute).assertIsDisplayed() } }
            }
            step("Assert '$feesFooter' footer is displayed") {
                onTangemPayBankWithdrawPreviewScreen { detailValue(feesFooter).assertIsDisplayed() }
            }
            step("Assert no bank-offramp route is created before 'Pay'") {
                assertEquals(routesBefore, countMoneyAccountBankWithdrawRoutes())
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert today's date is displayed") { assertTangemPayBankWithdrawSendingDateToday() }
            step("Assert '$recipientLabel' row is displayed") {
                onTangemPayBankWithdrawSuccessScreen { detailText(recipientLabel).assertIsDisplayed() }
            }
            listOf(bankNameLabel to JOHN.bankName, accountNumberLabel to JOHN.accountNumber).forEach { (label, value) ->
                step("Assert '$label' '$value' is displayed") {
                    onTangemPayBankWithdrawSuccessScreen {
                        detailText(label).assertIsDisplayed()
                        detailText(value).assertIsDisplayed()
                    }
                }
            }
            step("Assert one bank-offramp route is created") {
                assertEquals(routesBefore + 1, countMoneyAccountBankWithdrawRoutes())
            }
            step("Assert one USDC transfer is signed") {
                assertEquals(signsBefore + 1, countMoneyAccountTransferSignRequests())
            }
            step("Set WireMock scenario '$balanceScenario' to '$balanceAfterWithdrawState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterWithdrawState)
            }
            step("Close 'Sending' screen") { closeMoneyAccountBankWithdrawSuccess() }
            step("Assert account balance is '$balanceAfterWithdraw'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMoneyAccountScreen { balance.assertTextEquals(balanceAfterWithdraw) }
                }
            }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert the second USDC transfer is signed") {
                assertEquals(signsBefore + 2, countMoneyAccountTransferSignRequests())
            }
        }
    }

    @AllureId("13363")
    @DisplayName("Money account: a new route fee on Pay updates the confirmation and waits for another Pay")
    @Test
    fun newRouteFeeRequiresAnotherPayTest() {
        val higherFeeState = "HigherFee"
        val signScenario = "gasless_transaction_sign"
        val signTimeoutState = "Timeout"
        val higherFeeRoute = TangemPayBankRoutes.ACH.copy(fee = "$3.01")
        val higherFeePayTotal = "$4.01"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Set WireMock scenario '$routeScenario' to '$higherFeeState'") {
                setWireMockScenarioState(routeScenario, higherFeeState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert '${higherFeeRoute.title}' route with the new fee '${higherFeeRoute.fee}' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawPreviewScreen { routeRow(higherFeeRoute).assertIsDisplayed() }
                }
            }
            step("Assert 'Pay $higherFeePayTotal' button is enabled") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { payButton(higherFeePayTotal).assertIsEnabled() } }
            }
            step("Assert no USDC transfer is signed") {
                assertEquals(signsBefore, countMoneyAccountTransferSignRequests())
            }
            step("Click on 'Pay $higherFeePayTotal' button") { clickMoneyAccountBankWithdrawPay(higherFeePayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert one USDC transfer is signed") {
                assertEquals(signsBefore + 1, countMoneyAccountTransferSignRequests())
            }
            step("Close 'Sending' screen") { closeMoneyAccountBankWithdrawSuccess() }
            step("Set WireMock scenario '$routeScenario' to '$basicState'") {
                setWireMockScenarioState(routeScenario, basicState)
            }
            step("Set WireMock scenario '$signScenario' to '$signTimeoutState'") {
                setWireMockScenarioState(signScenario, signTimeoutState)
            }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert 'Unable to perform the operation' screen is displayed after the sign request times out") {
                flakySafely(SIGN_TIMEOUT_MILLIS) { onTangemPayBankWithdrawErrorScreen { title.assertIsDisplayed() } }
            }
            step("Assert 'Retry' button is not displayed") {
                onTangemPayBankWithdrawErrorScreen { retryButton.assertDoesNotExist() }
            }
            step("Assert the transfer with an unknown outcome is signed only once") {
                assertEquals(signsBefore + 2, countMoneyAccountTransferSignRequests())
            }
        }
    }

    @AllureId("13376")
    @DisplayName("Money account: a failed bank transfer shows Retry and sends nothing until it succeeds")
    @Test
    fun failedTransferRetryTest() {
        val routeErrorState = "Error"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Set WireMock scenario '$routeScenario' to '$routeErrorState'") {
                setWireMockScenarioState(routeScenario, routeErrorState)
            }
            val routesBefore = countMoneyAccountBankWithdrawRoutes()
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert '${achRoute.title}' route with fee '${achRoute.fee}' is displayed on 'Confirm transfer'") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { routeRow(achRoute).assertIsDisplayed() } }
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert 'Unable to perform the operation' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayBankWithdrawErrorScreen { title.assertIsDisplayed() } }
            }
            step("Assert 'Please try again later.' description is displayed") {
                onTangemPayBankWithdrawErrorScreen { description.assertIsDisplayed() }
            }
            step("Assert 'Close' button is displayed") {
                onTangemPayBankWithdrawErrorScreen { closeButton.assertIsDisplayed() }
            }
            step("Assert no USDC transfer is signed") {
                assertEquals(signsBefore, countMoneyAccountTransferSignRequests())
            }
            step("Set WireMock scenario '$routeScenario' to '$basicState'") {
                setWireMockScenarioState(routeScenario, basicState)
            }
            step("Click on 'Retry' button") {
                onTangemPayBankWithdrawErrorScreen { retryButton.performClick() }
            }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert the route is created once on 'Retry'") {
                assertEquals(routesBefore + 2, countMoneyAccountBankWithdrawRoutes())
            }
            step("Assert one USDC transfer is signed") {
                assertEquals(signsBefore + 1, countMoneyAccountTransferSignRequests())
            }
        }
    }

    @AllureId("13433")
    @DisplayName("Money account: on Plus the ACH transfer charges only the network fee")
    @Test
    fun plusAchChargesOnlyNetworkFeeTest() {
        val maxAmount = "24.99"
        val notEnoughFundsError = getResourceString(CoreResR.string.tangempay_withdraw_bank_amount_not_enough_funds)
        val zeroFee = "$0.00"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Set WireMock scenario '$optionsScenario' to '$plusState'") {
                setWireMockScenarioState(optionsScenario, plusState)
            }
            step("Set WireMock scenario '$routeScenario' to '$plusState'") {
                setWireMockScenarioState(routeScenario, plusState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Assert '${plusAchRoute.title}' route with fee '${plusAchRoute.fee}' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { routeRow(plusAchRoute).assertIsDisplayed() }
                }
            }
            step("Click on 'Max' button") { clickTangemPayBankWithdrawMax() }
            step("Assert amount is '$maxAmount'") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountInput.assertTextContains(maxAmount) } }
            }
            step("Assert '$notEnoughFundsError' error is not displayed") {
                onTangemPayBankWithdrawAmountScreen { amountError(notEnoughFundsError).assertDoesNotExist() }
            }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert '${plusAchRoute.title}' route with fee '${plusAchRoute.fee}' is displayed on 'Confirm transfer'") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { routeRow(plusAchRoute).assertIsDisplayed() } }
            }
            step("Assert '$zeroFee' is not displayed") {
                onTangemPayBankWithdrawPreviewScreen { detailValue(zeroFee).assertDoesNotExist() }
            }
            step("Click on 'Pay $plusAchPayTotal' button") { clickMoneyAccountBankWithdrawPay(plusAchPayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert the USDC transfer is signed on the first 'Pay'") {
                assertEquals(signsBefore + 1, countMoneyAccountTransferSignRequests())
            }
        }
    }

    @AllureId("13434")
    @DisplayName("Money account: on Plus the Wire transfer charges the fee from the options at cost")
    @Test
    fun plusWireChargesFeeAtCostTest() {
        val plusWireRoute = TangemPayBankRoutes.WIRE.copy(fee = "$7.01")
        val plusWirePayTotal = "$8.01"
        val feesFooter = getResourceString(CoreResR.string.money_account_send_fees_footer, plusWireRoute.title)

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Set WireMock scenario '$optionsScenario' to '$plusState'") {
                setWireMockScenarioState(optionsScenario, plusState)
            }
            step("Set WireMock scenario '$routeScenario' to '$plusState'") {
                setWireMockScenarioState(routeScenario, plusState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${ACME.shortName}'") { openTangemPayRecentRecipient(ACME) }
            step("Assert '${plusWireRoute.title}' route with fee '${plusWireRoute.fee}' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { routeRow(plusWireRoute).assertIsDisplayed() }
                }
            }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert '$feesFooter' footer is displayed") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { detailValue(feesFooter).assertIsDisplayed() } }
            }
            step("Click on 'Pay $plusWirePayTotal' button") { clickMoneyAccountBankWithdrawPay(plusWirePayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert the USDC transfer is signed on the first 'Pay'") {
                assertEquals(signsBefore + 1, countMoneyAccountTransferSignRequests())
            }
        }
    }

    @AllureId("13436")
    @DisplayName("Money account: switching Basic and Plus changes the fee for the saved recipient route")
    @Test
    fun tariffSwitchUpdatesRouteFeeTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Close 'Sending' screen") { closeMoneyAccountBankWithdrawSuccess() }
            step("Set WireMock scenario '$optionsScenario' to '$plusState'") {
                setWireMockScenarioState(optionsScenario, plusState)
            }
            step("Set WireMock scenario '$routeScenario' to '$plusState'") {
                setWireMockScenarioState(routeScenario, plusState)
            }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Assert '${plusAchRoute.title}' route with the Plus fee '${plusAchRoute.fee}' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { routeRow(plusAchRoute).assertIsDisplayed() }
                }
            }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Click on 'Pay $plusAchPayTotal' button") { clickMoneyAccountBankWithdrawPay(plusAchPayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert the Plus transfer is signed on the first 'Pay'") {
                assertEquals(signsBefore + 2, countMoneyAccountTransferSignRequests())
            }
            step("Close 'Sending' screen") { closeMoneyAccountBankWithdrawSuccess() }
            step("Set WireMock scenario '$optionsScenario' to '$basicState'") {
                setWireMockScenarioState(optionsScenario, basicState)
            }
            step("Set WireMock scenario '$routeScenario' to '$basicState'") {
                setWireMockScenarioState(routeScenario, basicState)
            }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Assert '${achRoute.title}' route with the Basic fee '${achRoute.fee}' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { routeRow(achRoute).assertIsDisplayed() }
                }
            }
        }
    }
}

private const val SIGN_TIMEOUT_MILLIS = 90_000L