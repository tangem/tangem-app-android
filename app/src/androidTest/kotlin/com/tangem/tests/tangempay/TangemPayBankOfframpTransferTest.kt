package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankAccounts.DUNDER
import com.tangem.common.constants.TangemPayBankAccounts.EMILY
import com.tangem.common.constants.TangemPayBankAccounts.JANE
import com.tangem.common.constants.TangemPayBankAccounts.JOHN
import com.tangem.common.constants.TangemPayBankAccounts.MICHAEL
import com.tangem.common.constants.TangemPayBankOfframpOrder
import com.tangem.common.constants.TangemPayBankRoutes.ACH
import com.tangem.common.constants.TangemPayBankRoutes.WIRE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.TangemPayBankWithdrawField
import com.tangem.screens.tangempay.onTangemPayBankWithdrawAmountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawErrorScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawFormScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawPreviewScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawSuccessScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayBankOfframpTransferTest : BaseTestCase() {

    private val bankAccountsScenario = "tangem_pay_bank_accounts"
    private val orderScenario = "tangem_pay_bank_offramp_order"
    private val orderEndpoint = "/bff-v2/v1/order"
    private val orderStatusEndpoint = "/bff-v2/v1/order/mock-bank-offramp-order-001"
    private val orderSignatureEndpoint = "/bff-v2/v1/order/mock-bank-offramp-order-001/bank-offramp/signature"
    private val bankOfframpOptionsEndpoint = "/bff-v2/v1/account/bank-offramp/options"
    private val polygonChainId = 137
    private val arbitrumChainId = 42161
    private val usdc = "USDC"
    private val arbitrum = "Arbitrum One"

    private val ownAccount: String
        get() = getResourceString(CoreResR.string.tangempay_withdraw_bank_transfer_type_own)
    private val thirdPartyAccount: String
        get() = getResourceString(CoreResR.string.tangempay_withdraw_bank_transfer_type_third_party)
    private val checkingAccount: String
        get() = getResourceString(CoreResR.string.tangempay_withdraw_bank_account_type_checking)
    private val savingAccount: String
        get() = getResourceString(CoreResR.string.tangempay_withdraw_bank_account_type_saving)

    @AllureId("12185")
    @DisplayName("Tangem Pay: the bank transfer amount is validated against the limits, the balance and the route fee")
    @Test
    fun bankTransferAmountValidationTest() {
        val maxWithAch = "98.5"
        val maxWithWire = "85"
        val belowMinimum = "0.5"
        val aboveMaximum = "10001"
        val aboveBalance = "5000"
        val validAmount = "50"
        val outOfRangeError = getResourceString(
            CoreResR.string.tangempay_withdraw_bank_amount_error_range,
            "$1",
            "$10,000",
        )
        val notEnoughFundsError = getResourceString(CoreResR.string.tangempay_withdraw_bank_amount_not_enough_funds)

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To business account' button") { openTangemPayBankWithdrawToBusinessAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Click on 'Max' button") {
                onTangemPayBankWithdrawAmountScreen { maxButton.performClick() }
            }
            step("Assert amount '$maxWithAch' is entered") {
                flakySafely {
                    onTangemPayBankWithdrawAmountScreen { amountInput.assertTextContainsSafe(maxWithAch, substring = true) }
                }
            }
            step("Assert 'Continue' button is enabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsEnabled() }
            }
            step("Choose '${WIRE.title}' route") { chooseTangemPayBankWithdrawRoute(WIRE) }
            step("Assert '$notEnoughFundsError' is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountError(notEnoughFundsError).assertIsDisplayed() } }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsNotEnabled() }
            }
            step("Click on 'Max' button") {
                onTangemPayBankWithdrawAmountScreen { maxButton.performClick() }
            }
            step("Assert amount '$maxWithWire' is entered") {
                flakySafely {
                    onTangemPayBankWithdrawAmountScreen { amountInput.assertTextContainsSafe(maxWithWire, substring = true) }
                }
            }
            step("Assert 'Continue' button is enabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsEnabled() }
            }
            step("Enter amount '$belowMinimum'") { enterTangemPayBankWithdrawAmount(belowMinimum) }
            step("Assert '$outOfRangeError' is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountError(outOfRangeError).assertIsDisplayed() } }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsNotEnabled() }
            }
            step("Enter amount '$aboveMaximum'") { enterTangemPayBankWithdrawAmount(aboveMaximum) }
            step("Assert '$outOfRangeError' is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountError(outOfRangeError).assertIsDisplayed() } }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsNotEnabled() }
            }
            step("Enter amount '$aboveBalance'") { enterTangemPayBankWithdrawAmount(aboveBalance) }
            step("Assert '$notEnoughFundsError' is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountError(notEnoughFundsError).assertIsDisplayed() } }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsNotEnabled() }
            }
            step("Enter amount '$validAmount'") { enterTangemPayBankWithdrawAmount(validAmount) }
            step("Assert 'Continue' button is enabled") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { continueButton.assertIsEnabled() } }
            }
        }
    }

    @AllureId("12186")
    @DisplayName("Tangem Pay: the bank transfer routes from the off-ramp options are offered for selection")
    @Test
    fun bankTransferRoutesSelectionTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay") { openTangemPay() }
            val optionsRequestsBefore = getWireMockRequestCount("GET", bankOfframpOptionsEndpoint)
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Assert bank off-ramp options are requested") {
                assertTangemPayRequestSent("GET", bankOfframpOptionsEndpoint, optionsRequestsBefore)
            }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert '${ACH.title}' route with '${ACH.eta}' and '${ACH.fee}' is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { routeRow(ACH).assertIsDisplayed() } }
            }
            step("Open route selector") { openTangemPayBankWithdrawRouteSelector() }
            step("Assert '${ACH.title}' route with '${ACH.eta}' and '${ACH.fee}' is listed") {
                onTangemPayBankWithdrawAmountScreen { routeOption(ACH).assertIsDisplayed() }
            }
            step("Assert '${WIRE.title}' route with '${WIRE.eta}' and '${WIRE.fee}' is listed") {
                onTangemPayBankWithdrawAmountScreen { routeOption(WIRE).assertIsDisplayed() }
            }
            step("Assert '${ACH.title}' route is checked") {
                onTangemPayBankWithdrawAmountScreen { routeOptionCheckmark(ACH).assertIsOn() }
            }
            step("Assert '${WIRE.title}' route is not checked") {
                onTangemPayBankWithdrawAmountScreen { routeOptionCheckmark(WIRE).assertIsOff() }
            }
            step("Click on '${WIRE.title}' route") {
                onTangemPayBankWithdrawAmountScreen { routeOption(WIRE).performClick() }
            }
            step("Assert '${WIRE.title}' route with '${WIRE.eta}' and '${WIRE.fee}' is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { routeRow(WIRE).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12187")
    @DisplayName("Tangem Pay: a bank transfer to the user's own personal checking account")
    @Test
    fun transferToOwnPersonalCheckingAccountTest() {
        val amount = "5"
        val expectedOrder = TangemPayBankOfframpOrder(
            amountInCents = 500,
            chainId = polygonChainId,
            rail = ACH.rail,
            bankAccountType = "CHECKING",
            beneficiaryType = "INDIVIDUAL",
            isThirdParty = false,
            accountNumber = MICHAEL.accountNumber,
            bankName = MICHAEL.bankName,
        )

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$ownAccount' '$checkingAccount' recipient '${MICHAEL.fullName}'") {
                fillTangemPayNewBankRecipient(ownAccount, checkingAccount, MICHAEL)
            }
            val ordersBefore = countTangemPayBankOfframpOrders(expectedOrder)
            val pollsBefore = getWireMockRequestCount("GET", orderStatusEndpoint)
            step("Send the bank transfer") { sendTangemPayBankWithdrawal(TANGEM_PAY_ACCESS_CODE) }
            step("Assert the order is created with $expectedOrder") {
                assertTangemPayBankOfframpOrderSent(expectedOrder, ordersBefore)
            }
            step("Assert the order is polled") { assertTangemPayRequestSent("GET", orderStatusEndpoint, pollsBefore) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Close 'Sending' screen") { closeTangemPayBankWithdrawSuccess() }
        }
    }

    @AllureId("12188")
    @DisplayName("Tangem Pay: a bank transfer to a third party's personal savings account by wire")
    @Test
    fun transferToThirdPartyPersonalSavingsAccountTest() {
        val amount = "5"
        val expectedOrder = TangemPayBankOfframpOrder(
            amountInCents = 500,
            chainId = polygonChainId,
            rail = WIRE.rail,
            bankAccountType = "SAVINGS",
            beneficiaryType = "INDIVIDUAL",
            isThirdParty = true,
            accountNumber = EMILY.accountNumber,
            bankName = EMILY.bankName,
        )

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Choose '${WIRE.title}' route") { chooseTangemPayBankWithdrawRoute(WIRE) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$thirdPartyAccount' '$savingAccount' recipient '${EMILY.fullName}'") {
                fillTangemPayNewBankRecipient(thirdPartyAccount, savingAccount, EMILY)
            }
            val ordersBefore = countTangemPayBankOfframpOrders(expectedOrder)
            val pollsBefore = getWireMockRequestCount("GET", orderStatusEndpoint)
            step("Send the bank transfer") { sendTangemPayBankWithdrawal(TANGEM_PAY_ACCESS_CODE) }
            step("Assert the order is created with $expectedOrder") {
                assertTangemPayBankOfframpOrderSent(expectedOrder, ordersBefore)
            }
            step("Assert the order is polled") { assertTangemPayRequestSent("GET", orderStatusEndpoint, pollsBefore) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Close 'Sending' screen") { closeTangemPayBankWithdrawSuccess() }
        }
    }

    @AllureId("12189")
    @DisplayName("Tangem Pay: a bank transfer to a business savings account")
    @Test
    fun transferToBusinessSavingsAccountTest() {
        val amount = "5"
        val expectedOrder = TangemPayBankOfframpOrder(
            amountInCents = 500,
            chainId = polygonChainId,
            rail = ACH.rail,
            bankAccountType = "SAVINGS",
            beneficiaryType = "BUSINESS",
            isThirdParty = true,
            accountNumber = DUNDER.accountNumber,
            bankName = DUNDER.bankName,
        )

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To business account' button") { openTangemPayBankWithdrawToBusinessAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$savingAccount' business recipient '${DUNDER.fullName}'") {
                fillTangemPayNewBankRecipient(transferType = null, accountType = savingAccount, recipient = DUNDER)
            }
            val ordersBefore = countTangemPayBankOfframpOrders(expectedOrder)
            val pollsBefore = getWireMockRequestCount("GET", orderStatusEndpoint)
            step("Send the bank transfer") { sendTangemPayBankWithdrawal(TANGEM_PAY_ACCESS_CODE) }
            step("Assert the order is created with $expectedOrder") {
                assertTangemPayBankOfframpOrderSent(expectedOrder, ordersBefore)
            }
            step("Assert the order is polled") { assertTangemPayRequestSent("GET", orderStatusEndpoint, pollsBefore) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Close 'Sending' screen") { closeTangemPayBankWithdrawSuccess() }
        }
    }

    @AllureId("12190")
    @DisplayName("Tangem Pay: a bank transfer to a business checking account by wire")
    @Test
    fun transferToBusinessCheckingAccountTest() {
        val amount = "5"
        val expectedOrder = TangemPayBankOfframpOrder(
            amountInCents = 500,
            chainId = polygonChainId,
            rail = WIRE.rail,
            bankAccountType = "CHECKING",
            beneficiaryType = "BUSINESS",
            isThirdParty = true,
            accountNumber = DUNDER.accountNumber,
            bankName = DUNDER.bankName,
        )

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To business account' button") { openTangemPayBankWithdrawToBusinessAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Choose '${WIRE.title}' route") { chooseTangemPayBankWithdrawRoute(WIRE) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$checkingAccount' business recipient '${DUNDER.fullName}'") {
                fillTangemPayNewBankRecipient(transferType = null, accountType = checkingAccount, recipient = DUNDER)
            }
            val ordersBefore = countTangemPayBankOfframpOrders(expectedOrder)
            val pollsBefore = getWireMockRequestCount("GET", orderStatusEndpoint)
            step("Send the bank transfer") { sendTangemPayBankWithdrawal(TANGEM_PAY_ACCESS_CODE) }
            step("Assert the order is created with $expectedOrder") {
                assertTangemPayBankOfframpOrderSent(expectedOrder, ordersBefore)
            }
            step("Assert the order is polled") { assertTangemPayRequestSent("GET", orderStatusEndpoint, pollsBefore) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Close 'Sending' screen") { closeTangemPayBankWithdrawSuccess() }
        }
    }

    @AllureId("12191")
    @DisplayName("Tangem Pay: a failed bank transfer order shows the error screen with Retry")
    @Test
    fun bankTransferOrderCreationErrorTest() {
        val orderErrorState = "Error"
        val amount = "5"

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Set WireMock scenario '$orderScenario' to '$orderErrorState'") {
                setWireMockScenarioState(orderScenario, orderErrorState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$ownAccount' '$savingAccount' recipient '${EMILY.fullName}'") {
                fillTangemPayNewBankRecipient(ownAccount, savingAccount, EMILY)
            }
            val ordersBefore = getWireMockRequestCount("POST", orderEndpoint)
            step("Click on 'Send' button") { clickTangemPayBankWithdrawSend() }
            step("Assert the order request is sent") { assertTangemPayRequestSent("POST", orderEndpoint, ordersBefore) }
            step("Assert 'Unable to perform the operation' title is displayed") {
                flakySafely { onTangemPayBankWithdrawErrorScreen { title.assertIsDisplayed() } }
            }
            step("Assert 'Please try again later.' description is displayed") {
                onTangemPayBankWithdrawErrorScreen { description.assertIsDisplayed() }
            }
            step("Assert 'Retry' button is displayed") {
                onTangemPayBankWithdrawErrorScreen { retryButton.assertIsDisplayed() }
            }
            step("Assert 'Close' button is displayed") {
                onTangemPayBankWithdrawErrorScreen { closeButton.assertIsDisplayed() }
            }
            val ordersBeforeRetry = getWireMockRequestCount("POST", orderEndpoint)
            step("Click on 'Retry' button") {
                onTangemPayBankWithdrawErrorScreen { retryButton.performClick() }
            }
            step("Assert the order request is sent again") {
                assertTangemPayRequestSent("POST", orderEndpoint, ordersBeforeRetry)
            }
            step("Assert 'Unable to perform the operation' title is displayed") {
                flakySafely { onTangemPayBankWithdrawErrorScreen { title.assertIsDisplayed() } }
            }
            step("Click on 'Close' button") {
                onTangemPayBankWithdrawErrorScreen { closeButton.performClick() }
            }
            step("Assert payment account balance is displayed") {
                flakySafely { onTangemPayMainScreen { balance.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12193")
    @DisplayName("Tangem Pay: the Sending screen shows the amount, the date and the recipient's bank details")
    @Test
    fun bankTransferSendingScreenTest() {
        val amount = "5"
        val recipientLabel = getResourceString(CoreResR.string.send_recipient)
        val bankNameLabel = getResourceString(CoreResR.string.tangempay_withdraw_bank_field_bank_name)
        val accountNumberLabel = getResourceString(CoreResR.string.tangempay_withdraw_bank_field_account_number)

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$ownAccount' '$savingAccount' recipient '${MICHAEL.fullName}'") {
                fillTangemPayNewBankRecipient(ownAccount, savingAccount, MICHAEL)
            }
            step("Send the bank transfer") { sendTangemPayBankWithdrawal(TANGEM_PAY_ACCESS_CODE) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert today's date is displayed") { assertTangemPayBankWithdrawSendingDateToday() }
            listOf(
                recipientLabel to MICHAEL.fullName,
                bankNameLabel to MICHAEL.bankName,
                accountNumberLabel to MICHAEL.accountNumber,
            ).forEach { (label, value) ->
                step("Assert '$label' '$value' is displayed") {
                    onTangemPayBankWithdrawSuccessScreen {
                        detailText(label).assertIsDisplayed()
                        detailText(value).assertIsDisplayed()
                    }
                }
            }
            step("Close 'Sending' screen") { closeTangemPayBankWithdrawSuccess() }
        }
    }

    @AllureId("12195")
    @DisplayName("Tangem Pay: the recent bank recipients are updated after a successful transfer")
    @Test
    fun recentRecipientsUpdatedAfterTransferTest() {
        val twoAccountsState = "TwoAccounts"
        val twoAccountsAfterTransferState = "TwoAccountsAfterTransfer"
        val amount = "5"

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Assert recent recipient '${JOHN.shortName}' is displayed") {
                assertTangemPayRecentRecipientDisplayed(JOHN)
            }
            step("Assert recent recipient '${JANE.shortName}' is not displayed") {
                onTangemPayWithdrawHubScreen { recentRecipientWithName(JANE.shortName).assertDoesNotExist() }
            }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$thirdPartyAccount' '$checkingAccount' recipient '${JANE.fullName}'") {
                fillTangemPayNewBankRecipient(thirdPartyAccount, checkingAccount, JANE)
            }
            step("Send the bank transfer") { sendTangemPayBankWithdrawal(TANGEM_PAY_ACCESS_CODE) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Close 'Sending' screen") { closeTangemPayBankWithdrawSuccess() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsAfterTransferState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsAfterTransferState)
            }
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Assert recent recipient '${JANE.shortName}' is displayed") {
                assertTangemPayRecentRecipientDisplayed(JANE)
            }
            step("Assert recent recipient '${JOHN.shortName}' is displayed") {
                assertTangemPayRecentRecipientDisplayed(JOHN)
            }
        }
    }

    @AllureId("12196")
    @DisplayName("Tangem Pay: the bank transfer confirmation shows the entered data and edits each block")
    @Test
    fun confirmationScreenEditsTransferDataTest() {
        val amount = "5"
        val editedAmount = "7"
        val polygonBalance = "$100"
        val arbitrumBalance = "$50"
        val editedAccountNumber = EMILY.accountNumber
        val editedBankName = EMILY.bankName
        val expectedOrder = TangemPayBankOfframpOrder(
            amountInCents = 700,
            chainId = arbitrumChainId,
            rail = WIRE.rail,
            bankAccountType = "CHECKING",
            beneficiaryType = "INDIVIDUAL",
            isThirdParty = false,
            accountNumber = editedAccountNumber,
            bankName = editedBankName,
        )

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$ownAccount' '$checkingAccount' recipient '${MICHAEL.fullName}'") {
                fillTangemPayNewBankRecipient(ownAccount, checkingAccount, MICHAEL)
            }
            step("Assert amount block '$$amount' from '$polygonBalance' is clickable") {
                onTangemPayBankWithdrawPreviewScreen { amountCard("$$amount", polygonBalance).assertHasClickAction() }
            }
            step("Assert '${ACH.title}' route block is clickable") {
                onTangemPayBankWithdrawPreviewScreen { routeRow(ACH).assertHasClickAction() }
            }
            listOf(MICHAEL.accountNumber, MICHAEL.city, MICHAEL.bankName).forEach { value ->
                step("Assert recipient info block with '$value' is clickable") {
                    onTangemPayBankWithdrawPreviewScreen {
                        recipientCard(value).performScrollTo()
                        recipientCard(value).assertHasClickAction()
                    }
                }
            }
            step("Click on amount block") {
                onTangemPayBankWithdrawPreviewScreen {
                    amountCard("$$amount", polygonBalance).performScrollTo()
                    amountCard("$$amount", polygonBalance).performClick()
                }
            }
            step("Enter amount '$editedAmount'") { enterTangemPayBankWithdrawAmount(editedAmount) }
            step("Choose '$usdc' on '$arbitrum' source") { chooseTangemPayBankWithdrawSource(usdc, arbitrum) }
            step("Choose '${WIRE.title}' route") { chooseTangemPayBankWithdrawRoute(WIRE) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert amount block '$$editedAmount' from '$arbitrumBalance' is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawPreviewScreen { amountCard("$$editedAmount", arbitrumBalance).assertIsDisplayed() }
                }
            }
            step("Assert '${WIRE.title}' route block is displayed") {
                onTangemPayBankWithdrawPreviewScreen { routeRow(WIRE).assertIsDisplayed() }
            }
            step("Click on recipient info block with '${MICHAEL.accountNumber}'") {
                onTangemPayBankWithdrawPreviewScreen {
                    recipientCard(MICHAEL.accountNumber).performScrollTo()
                    recipientCard(MICHAEL.accountNumber).performClick()
                }
            }
            step("Assert recipient details step is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawFormScreen {
                        valueOf(TangemPayBankWithdrawField.AccountNumber, MICHAEL.accountNumber).assertIsDisplayed()
                    }
                }
            }
            step("Change account number to '$editedAccountNumber'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.AccountNumber, editedAccountNumber)
            }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawFormContinue() }
            step("Assert recipient info block with '$editedAccountNumber' is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawPreviewScreen {
                        recipientCard(editedAccountNumber).performScrollTo()
                        recipientCard(editedAccountNumber).assertIsDisplayed()
                    }
                }
            }
            step("Click on recipient bank block with '${MICHAEL.bankName}'") {
                onTangemPayBankWithdrawPreviewScreen {
                    recipientCard(MICHAEL.bankName).performScrollTo()
                    recipientCard(MICHAEL.bankName).performClick()
                }
            }
            step("Assert bank details step is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawFormScreen {
                        valueOf(TangemPayBankWithdrawField.BankName, MICHAEL.bankName).assertIsDisplayed()
                    }
                }
            }
            step("Change bank name to '$editedBankName'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.BankName, editedBankName)
            }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawFormContinue() }
            step("Assert recipient bank block with '$editedBankName' is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawPreviewScreen {
                        recipientCard(editedBankName).performScrollTo()
                        recipientCard(editedBankName).assertIsDisplayed()
                    }
                }
            }
            val ordersBefore = countTangemPayBankOfframpOrders(expectedOrder)
            step("Click on 'Send' button") { clickTangemPayBankWithdrawSend() }
            step("Assert the order is created with $expectedOrder") {
                assertTangemPayBankOfframpOrderSent(expectedOrder, ordersBefore)
            }
        }
    }

    @AllureId("12197")
    @DisplayName("Tangem Pay: a recipient picked from recents keeps the route and the bank details locked")
    @Test
    fun recentRecipientDetailsNotEditableTest() {
        val twoAccountsState = "TwoAccounts"
        val johnBankAccountId = "mock-bank-account-john"
        val amount = "5"
        val editedAmount = "7"
        val polygon = "Polygon"
        val polygonBalance = "$100"
        val arbitrumBalance = "$50"
        val expectedOrder = TangemPayBankOfframpOrder(
            amountInCents = 700,
            chainId = arbitrumChainId,
            bankAccountId = johnBankAccountId,
        )

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Assert amount input is displayed") {
                onTangemPayBankWithdrawAmountScreen { amountInput.assertIsDisplayed() }
            }
            step("Assert source row is clickable") {
                onTangemPayBankWithdrawAmountScreen { sourceRow.assertHasClickAction() }
            }
            step("Assert '${ACH.title}' route is displayed and not clickable") {
                onTangemPayBankWithdrawAmountScreen {
                    routeRow(ACH).assertIsDisplayed()
                    routeRow(ACH).assertHasNoClickAction()
                }
            }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Choose '$usdc' on '$polygon' source") { chooseTangemPayBankWithdrawSource(usdc, polygon) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert 'Confirm transfer' screen is displayed") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { title.assertIsDisplayed() } }
            }
            step("Assert amount block '$$amount' from '$polygonBalance' is clickable") {
                onTangemPayBankWithdrawPreviewScreen { amountCard("$$amount", polygonBalance).assertHasClickAction() }
            }
            step("Assert '${ACH.title}' route block is not clickable") {
                onTangemPayBankWithdrawPreviewScreen { routeRow(ACH).assertHasNoClickAction() }
            }
            listOf(JOHN.accountNumber, JOHN.bankName).forEach { value ->
                step("Assert recipient info block with '$value' is not clickable") {
                    onTangemPayBankWithdrawPreviewScreen {
                        recipientCard(value).performScrollTo()
                        recipientCard(value).assertHasNoClickAction()
                    }
                }
            }
            step("Assert the recipient info matches '${JOHN.fullName}'") {
                onTangemPayBankWithdrawPreviewScreen {
                    listOf(
                        JOHN.accountNumber,
                        JOHN.routingNumber,
                        JOHN.firstName,
                        JOHN.lastName,
                        JOHN.bankName,
                    ).forEach { value ->
                        detailValue(value).performScrollTo()
                        detailValue(value).assertIsDisplayed()
                    }
                }
            }
            step("Click on amount block") {
                onTangemPayBankWithdrawPreviewScreen {
                    amountCard("$$amount", polygonBalance).performScrollTo()
                    amountCard("$$amount", polygonBalance).performClick()
                }
            }
            step("Enter amount '$editedAmount'") { enterTangemPayBankWithdrawAmount(editedAmount) }
            step("Choose '$usdc' on '$arbitrum' source") { chooseTangemPayBankWithdrawSource(usdc, arbitrum) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert amount block '$$editedAmount' from '$arbitrumBalance' is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawPreviewScreen { amountCard("$$editedAmount", arbitrumBalance).assertIsDisplayed() }
                }
            }
            val ordersBefore = countTangemPayBankOfframpOrders(expectedOrder)
            step("Click on 'Send' button") { clickTangemPayBankWithdrawSend() }
            step("Assert the order is created with $expectedOrder") {
                assertTangemPayBankOfframpOrderSent(expectedOrder, ordersBefore)
            }
        }
    }

    @AllureId("12198")
    @DisplayName("Tangem Pay: the bank transfer order is polled until it asks for a signature")
    @Test
    fun bankTransferOrderPollingTest() {
        val orderPollingState = "Polling"
        val amount = "5"

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Set WireMock scenario '$orderScenario' to '$orderPollingState'") {
                setWireMockScenarioState(orderScenario, orderPollingState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To business account' button") { openTangemPayBankWithdrawToBusinessAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$savingAccount' business recipient '${DUNDER.fullName}'") {
                fillTangemPayNewBankRecipient(transferType = null, accountType = savingAccount, recipient = DUNDER)
            }
            val ordersBefore = getWireMockRequestCount("POST", orderEndpoint)
            val pollsBefore = getWireMockRequestCount("GET", orderStatusEndpoint)
            val signaturesBefore = getWireMockRequestCount("POST", orderSignatureEndpoint)
            step("Send the bank transfer") { sendTangemPayBankWithdrawal(TANGEM_PAY_ACCESS_CODE) }
            step("Assert the order request is sent") { assertTangemPayRequestSent("POST", orderEndpoint, ordersBefore) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert the order is polled until it awaits a signature") {
                assertTangemPayRequestSent("GET", orderStatusEndpoint, pollsBefore + 1)
            }
            step("Assert the signature is submitted once") {
                assertTangemPayRequestSentOnce("POST", orderSignatureEndpoint, signaturesBefore)
            }
            step("Close 'Sending' screen") { closeTangemPayBankWithdrawSuccess() }
        }
    }

    @AllureId("12200")
    @DisplayName("Tangem Pay: the bank account details fields are validated")
    @Test
    fun bankAccountDetailsValidationTest() {
        val geoCountriesScenario = "tangem_pay_geo_countries"
        val geoWithGermanyState = "WithGermany"
        val amount = "5"
        val unitedStates = "United States"
        val california = "California"
        val germany = "Germany"
        val invalidFormat = getResourceString(CoreResR.string.tangempay_withdraw_bank_field_error_invalid_format)
        val invalidRoutingNumber = getResourceString(CoreResR.string.tangempay_withdraw_bank_field_error_routing_number)
        val latinOnly = getResourceString(CoreResR.string.tangempay_order_data_field_latin_only)

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Set WireMock scenario '$geoCountriesScenario' to '$geoWithGermanyState'") {
                setWireMockScenarioState(geoCountriesScenario, geoWithGermanyState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert the default source and route") { assertTangemPayBankWithdrawDefaults() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Choose '$ownAccount' '$checkingAccount' account") {
                chooseTangemPayBankWithdrawAccountTypes(ownAccount, checkingAccount)
            }
            step("Assert recipient details step is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawFormScreen { field(TangemPayBankWithdrawField.AccountNumber).assertIsDisplayed() }
                }
            }
            listOf("123", "123456789012345678", "12345-678").forEach { value ->
                step("Assert account number '$value' is rejected") {
                    assertTangemPayBankWithdrawFieldError(
                        field = TangemPayBankWithdrawField.AccountNumber,
                        value = value,
                        blurField = TangemPayBankWithdrawField.RoutingNumber,
                        error = invalidFormat,
                    )
                }
            }
            listOf("02600959", "0260095931", "02600959A").forEach { value ->
                step("Assert routing number '$value' is rejected") {
                    assertTangemPayBankWithdrawFieldError(
                        field = TangemPayBankWithdrawField.RoutingNumber,
                        value = value,
                        blurField = TangemPayBankWithdrawField.FirstName,
                        error = invalidRoutingNumber,
                    )
                }
            }
            step("Select '$germany' residency") {
                selectTangemPayBankWithdrawResidency(TangemPayBankWithdrawField.Country, germany)
            }
            step("Assert region 'Бавария' is rejected") {
                assertTangemPayBankWithdrawFieldError(
                    field = TangemPayBankWithdrawField.Region,
                    value = "Бавария",
                    blurField = TangemPayBankWithdrawField.City,
                    error = latinOnly,
                )
            }
            step("Assert region 'Bayern' is accepted") {
                assertTangemPayBankWithdrawFieldValid(
                    field = TangemPayBankWithdrawField.Region,
                    value = "Bayern",
                    blurField = TangemPayBankWithdrawField.City,
                    error = latinOnly,
                )
            }
            step("Assert region 'Württemberg' is accepted") {
                assertTangemPayBankWithdrawFieldValid(
                    field = TangemPayBankWithdrawField.Region,
                    value = "Württemberg",
                    blurField = TangemPayBankWithdrawField.City,
                    error = latinOnly,
                )
            }
            step("Assert non-US postal code '10115AB' is accepted") {
                assertTangemPayBankWithdrawFieldValid(
                    field = TangemPayBankWithdrawField.PostalCode,
                    value = "10115AB",
                    blurField = TangemPayBankWithdrawField.City,
                    error = invalidFormat,
                )
            }
            step("Select '$unitedStates', '$california' residency") {
                selectTangemPayBankWithdrawResidency(TangemPayBankWithdrawField.Country, unitedStates, california)
            }
            listOf(
                TangemPayBankWithdrawField.FirstName to "Иван",
                TangemPayBankWithdrawField.LastName to "Иванов",
                TangemPayBankWithdrawField.City to "Москва",
                TangemPayBankWithdrawField.AddressLine1 to "Ленина 1",
            ).forEach { (field, value) ->
                step("Assert '${field.name}' '$value' is rejected") {
                    assertTangemPayBankWithdrawFieldError(
                        field = field,
                        value = value,
                        blurField = TangemPayBankWithdrawField.AccountNumber,
                        error = latinOnly,
                    )
                }
            }
            val diacriticValues = listOf(
                TangemPayBankWithdrawField.FirstName to "José",
                TangemPayBankWithdrawField.LastName to "Müller",
                TangemPayBankWithdrawField.City to "São Paulo",
                TangemPayBankWithdrawField.AddressLine1 to "Straße 1",
            )
            diacriticValues.forEach { (field, value) ->
                step("Assert '${field.name}' '$value' is accepted") {
                    assertTangemPayBankWithdrawFieldValid(
                        field = field,
                        value = value,
                        blurField = TangemPayBankWithdrawField.AccountNumber,
                        error = latinOnly,
                    )
                }
            }
            step("Assert US postal code 'ABCDE' is rejected") {
                assertTangemPayBankWithdrawFieldError(
                    field = TangemPayBankWithdrawField.PostalCode,
                    value = "ABCDE",
                    blurField = TangemPayBankWithdrawField.City,
                    error = invalidFormat,
                )
            }
            step("Assert US postal code '${MICHAEL.postalCode}' is accepted") {
                assertTangemPayBankWithdrawFieldValid(
                    field = TangemPayBankWithdrawField.PostalCode,
                    value = MICHAEL.postalCode,
                    blurField = TangemPayBankWithdrawField.City,
                    error = invalidFormat,
                )
            }
            step("Enter account number '${MICHAEL.accountNumber}'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.AccountNumber, MICHAEL.accountNumber)
            }
            step("Enter routing number '${MICHAEL.routingNumber}'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.RoutingNumber, MICHAEL.routingNumber)
            }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawFormContinue() }
            step("Assert bank details step is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawFormScreen { field(TangemPayBankWithdrawField.BankName).assertIsDisplayed() }
                }
            }
        }
    }
}