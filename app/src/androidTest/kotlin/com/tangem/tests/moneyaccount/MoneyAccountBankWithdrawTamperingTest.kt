package com.tangem.tests.moneyaccount

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankAccounts.JOHN
import com.tangem.common.constants.TangemPayBankRoutes
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.bringAppToForeground
import com.tangem.common.extensions.collapseAppByHomeButton
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.onTangemPayBankWithdrawAmountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawErrorScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawPreviewScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class MoneyAccountBankWithdrawTamperingTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val bankAccountsScenario = "tangem_pay_bank_accounts"
    private val twoAccountsState = "TwoAccounts"
    private val routeScenario = "tangem_pay_bank_offramp_route"
    private val basicState = "Started"
    private val amount = "1"
    private val achRoute = TangemPayBankRoutes.ACH.copy(fee = "$1.51")
    private val achPayTotal = "$2.51"
    private val signedAchAmount = "2.50"
    private val accountBalance = "$25"
    private val depositAddress = "0x934a1c2b3d4e5f60718293a4b5c6d7e8f9018ea0"

    @AllureId("13437")
    @DisplayName("Money account: the bank transfer signs the route deposit address and the amount from Confirm")
    @Test
    fun signsDepositAddressAndConfirmedAmountTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val routesBefore = countMoneyAccountBankWithdrawRoutes()
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Confirm transfer' for '${JOHN.shortName}' with amount '$amount'") {
                openMoneyAccountBankWithdrawPreview(JOHN, amount)
            }
            step("Assert amount '$$amount' and balance '$accountBalance' are displayed on 'Confirm transfer'") {
                flakySafely {
                    onTangemPayBankWithdrawPreviewScreen { amountCard("$$amount", accountBalance).assertIsDisplayed() }
                }
            }
            step("Assert '${achRoute.title}' route with fee '${achRoute.fee}' is displayed on 'Confirm transfer'") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { routeRow(achRoute).assertIsDisplayed() } }
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert one bank-offramp route is created") {
                assertEquals(routesBefore + 1, countMoneyAccountBankWithdrawRoutes())
            }
            step("Assert $signedAchAmount USDC is signed to the route deposit address") {
                assertEquals(
                    listOf(polygonUsdcTransfer(depositAddress, signedAchAmount)),
                    findMoneyAccountTransfersSignedSince(signsBefore),
                )
            }
        }
    }

    @AllureId("13438")
    @DisplayName("Money account: a substituted route deposit address is what the bank transfer signs")
    @Test
    fun substitutedDepositAddressIsSignedTest() {
        val tamperedDepositState = "TamperedDepositAddress"
        val tamperedDepositAddress = "0x00000000000000000000000000000000deadbeef"

        setupHooks(
            additionalBeforeSection = { enableMoneyAccountKeyDerivationWithAccessCode() },
            additionalAfterSection = { disableMoneyAccountKeyDerivationWithAccessCode() },
        ).run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Set WireMock scenario '$routeScenario' to '$tamperedDepositState'") {
                setWireMockScenarioState(routeScenario, tamperedDepositState)
            }
            val routesBefore = countMoneyAccountBankWithdrawRoutes()
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount(accessCode = TANGEM_PAY_ACCESS_CODE) }
            step("Open 'Confirm transfer' for '${JOHN.shortName}' with amount '$amount'") {
                openMoneyAccountBankWithdrawPreview(JOHN, amount)
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Cancel the access code request") { cancelHotWalletUnlock() }
            step("Assert 'Pay $achPayTotal' button is enabled") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { payButton(achPayTotal).assertIsEnabled() } }
            }
            step("Assert recipient account '${JOHN.accountNumber}' is displayed") {
                onTangemPayBankWithdrawPreviewScreen { recipientCard(JOHN.accountNumber).assertIsDisplayed() }
            }
            step("Assert no USDC transfer is signed") {
                assertEquals(signsBefore, countMoneyAccountTransferSignRequests())
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Enter access code") { enterHotWalletUnlockAccessCode(TANGEM_PAY_ACCESS_CODE) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert the route is created once") {
                assertEquals(routesBefore + 1, countMoneyAccountBankWithdrawRoutes())
            }
            step("Assert $signedAchAmount USDC is signed to the substituted address") {
                assertEquals(
                    listOf(polygonUsdcTransfer(tamperedDepositAddress, signedAchAmount)),
                    findMoneyAccountTransfersSignedSince(signsBefore),
                )
            }
        }
    }

    @AllureId("13440")
    @DisplayName("Money account: a route response with an empty or missing field is not signed")
    @Test
    fun incompleteRouteIsNotSignedTest() {
        val incompleteRouteStates = listOf("EmptyDepositAddress", "NoFee", "NoEta")

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            incompleteRouteStates.forEach { state ->
                step("Set WireMock scenario '$routeScenario' to '$state'") {
                    setWireMockScenarioState(routeScenario, state)
                }
                step("Open 'Confirm transfer' for '${JOHN.shortName}' with amount '$amount'") {
                    openMoneyAccountBankWithdrawPreview(JOHN, amount)
                }
                step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
                step("Assert 'Unable to perform the operation' screen is displayed") {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        onTangemPayBankWithdrawErrorScreen { title.assertIsDisplayed() }
                    }
                }
                step("Assert no USDC transfer is signed") {
                    assertEquals(signsBefore, countMoneyAccountTransferSignRequests())
                }
                step("Close the error screen") { closeMoneyAccountBankWithdrawError() }
            }
        }
    }

    @AllureId("13441")
    @DisplayName("Money account: a sub-cent bank transfer amount is cut to cents before signing")
    @Test
    fun subCentAmountIsCutToCentsTest() {
        val subCentAmount = "1.009"
        val centsAmount = "1.00"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Type amount '$subCentAmount'") { typeTangemPayBankWithdrawAmount(subCentAmount) }
            step("Assert amount is '$centsAmount'") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountInput.assertTextContains(centsAmount) } }
            }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert amount '$$amount' and balance '$accountBalance' are displayed on 'Confirm transfer'") {
                flakySafely {
                    onTangemPayBankWithdrawPreviewScreen { amountCard("$$amount", accountBalance).assertIsDisplayed() }
                }
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert $signedAchAmount USDC is signed: the cut amount plus the route fee") {
                assertEquals(
                    listOf(polygonUsdcTransfer(depositAddress, signedAchAmount)),
                    findMoneyAccountTransfersSignedSince(signsBefore),
                )
            }
        }
    }

    @AllureId("13442")
    @DisplayName("Money account: a bank transfer whose amount, fee and gas exceed the balance is not signed")
    @Test
    fun amountWithFeesAboveBalanceIsNotSignedTest() {
        val maxAmount = "23.49"
        val maxPayTotal = "$25"
        val signedMaxAmount = "24.99"
        val aboveMaxAmount = "23.50"
        val notEnoughFundsError = getResourceString(CoreResR.string.tangempay_withdraw_bank_amount_not_enough_funds)

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Confirm transfer' for '${JOHN.shortName}' with amount '$maxAmount'") {
                openMoneyAccountBankWithdrawPreview(JOHN, maxAmount)
            }
            step("Click on 'Pay $maxPayTotal' button") { clickMoneyAccountBankWithdrawPay(maxPayTotal) }
            step("Assert 'Sending $$maxAmount' screen is displayed") {
                assertTangemPayBankWithdrawSendingScreen(maxAmount)
            }
            step("Assert $signedMaxAmount USDC is signed to the route deposit address") {
                assertEquals(
                    listOf(polygonUsdcTransfer(depositAddress, signedMaxAmount)),
                    findMoneyAccountTransfersSignedSince(signsBefore),
                )
            }
            step("Close 'Sending' screen") { closeMoneyAccountBankWithdrawSuccess() }
            val routesBefore = countMoneyAccountBankWithdrawRoutes()
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Enter amount '$aboveMaxAmount'") { enterTangemPayBankWithdrawAmount(aboveMaxAmount) }
            step("Assert '$notEnoughFundsError' error is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountError(notEnoughFundsError).assertIsDisplayed() } }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsNotEnabled() }
            }
            step("Assert no bank-offramp route is created") {
                assertEquals(routesBefore, countMoneyAccountBankWithdrawRoutes())
            }
            step("Assert no other USDC transfer is signed") {
                assertEquals(signsBefore + 1, countMoneyAccountTransferSignRequests())
            }
        }
    }

    @AllureId("13448")
    @DisplayName("Money account: the bank transfer is signed only after the access code, cancel sends nothing")
    @Test
    fun transferNeedsAccessCodeTest() {
        setupHooks(
            additionalBeforeSection = { enableMoneyAccountKeyDerivationWithAccessCode() },
            additionalAfterSection = { disableMoneyAccountKeyDerivationWithAccessCode() },
        ).run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val routesBefore = countMoneyAccountBankWithdrawRoutes()
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount(accessCode = TANGEM_PAY_ACCESS_CODE) }
            step("Open 'Confirm transfer' for '${JOHN.shortName}' with amount '$amount'") {
                openMoneyAccountBankWithdrawPreview(JOHN, amount)
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Cancel the access code request") { cancelHotWalletUnlock() }
            step("Assert 'Pay $achPayTotal' button is enabled") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { payButton(achPayTotal).assertIsEnabled() } }
            }
            step("Assert no USDC transfer is signed") {
                assertEquals(signsBefore, countMoneyAccountTransferSignRequests())
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert access code dialog is displayed") { waitForHotWalletUnlockDialog() }
            step("Collapse the app by 'Home' button") { collapseAppByHomeButton() }
            step("Bring the app to the foreground") { bringAppToForeground(getTargetContext().packageName) }
            step("Cancel the access code request") { cancelHotWalletUnlock() }
            step("Assert no USDC transfer is signed") {
                assertEquals(signsBefore, countMoneyAccountTransferSignRequests())
            }
            step("Assert the route is created once") {
                assertEquals(routesBefore + 1, countMoneyAccountBankWithdrawRoutes())
            }
        }
    }

    @AllureId("13449")
    @DisplayName("Money account: a double Pay and a retry after a lost route response make one transfer each")
    @Test
    fun doublePayAndRouteRetryMakeOneTransferTest() {
        val connectionResetState = "ConnectionReset"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val routesBefore = countMoneyAccountBankWithdrawRoutes()
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Confirm transfer' for '${JOHN.shortName}' with amount '$amount'") {
                openMoneyAccountBankWithdrawPreview(JOHN, amount)
            }
            step("Assert 'Pay $achPayTotal' button is enabled") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { payButton(achPayTotal).assertIsEnabled() } }
            }
            step("Double-click on 'Pay $achPayTotal' button") {
                onTangemPayBankWithdrawPreviewScreen { doubleClickPayButton(achPayTotal) }
            }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert one bank-offramp route is created") {
                assertEquals(routesBefore + 1, countMoneyAccountBankWithdrawRoutes())
            }
            step("Assert one USDC transfer is signed") {
                assertEquals(
                    listOf(polygonUsdcTransfer(depositAddress, signedAchAmount)),
                    findMoneyAccountTransfersSignedSince(signsBefore),
                )
            }
            step("Close 'Sending' screen") { closeMoneyAccountBankWithdrawSuccess() }
            step("Set WireMock scenario '$routeScenario' to '$connectionResetState'") {
                setWireMockScenarioState(routeScenario, connectionResetState)
            }
            val retryRoutesBefore = countMoneyAccountBankWithdrawRoutes()
            step("Open 'Confirm transfer' for '${JOHN.shortName}' with amount '$amount'") {
                openMoneyAccountBankWithdrawPreview(JOHN, amount)
            }
            step("Click on 'Pay $achPayTotal' button") { clickMoneyAccountBankWithdrawPay(achPayTotal) }
            step("Assert 'Unable to perform the operation' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayBankWithdrawErrorScreen { title.assertIsDisplayed() } }
            }
            step("Set WireMock scenario '$routeScenario' to '$basicState'") {
                setWireMockScenarioState(routeScenario, basicState)
            }
            step("Click on 'Retry' button") {
                onTangemPayBankWithdrawErrorScreen { retryButton.performClick() }
            }
            step("Assert 'Sending $$amount' screen is displayed") { assertTangemPayBankWithdrawSendingScreen(amount) }
            step("Assert every route request of the retried transfer has the same idempotency key") {
                val keys = findMoneyAccountRouteIdempotencyKeysSince(retryRoutesBefore)
                assertTrue("Expected the lost route request and its retry, got $keys", keys.size >= 2)
                assertEquals(1, keys.toSet().size)
            }
            step("Assert one more USDC transfer is signed") {
                assertEquals(signsBefore + 2, countMoneyAccountTransferSignRequests())
            }
        }
    }
}