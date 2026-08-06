package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class TangemPayWithdrawTest : BaseTestCase() {

    @AllureId("4972")
    @DisplayName("Tangem Pay: withdraw swaps USDC to Bitcoin and appends withdrawal to history")
    @Test
    fun withdrawSwapsUsdcToBitcoinAppendsToHistoryTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val expressAssetsScenario = "express_api_assets"
        val exchangeStatusScenario = "exchange_status_provider"
        val balanceScenario = "tangem_pay_balance_update"
        val historyScenario = "tangem_pay_transaction_history"
        val eligibilityState = "PaeraCustomer"
        val bitcoinStartedState = "Started"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val exchangeStatusState = "Changelly"
        val balanceInitialState = "InitialBalance"
        val balanceAfterState = "AfterWithdraw"
        val historyInitialState = "InitialEmpty"
        val historyAfterState = "AfterWithdraw"
        val withdrawAmount = "5"
        val receiveToken = "Bitcoin"
        val balanceAfterText = "5"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(bitcoinScenario)
                resetWireMockScenarioState(expressAssetsScenario)
                resetWireMockScenarioState(exchangeStatusScenario)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay withdraw Swap screen") { openTangemPayWithdrawSwapScreen() }
            step("Choose receive token '$receiveToken'") { chooseWithdrawReceiveToken(receiveToken) }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Wait for the receive amount to load (quote ready, HoldToConfirm enabled)") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveAmount.assertIsDisplayed() }
                }
            }
            step("Confirm swap by holding the button") {
                confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE)
            }
            step("Wait for 'Swap in progress' screen") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterState)
            }
            step("Switch WireMock scenario '$historyScenario' to '$historyAfterState'") {
                setWireMockScenarioState(historyScenario, historyAfterState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert balance updated to contain '$balanceAfterText'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe(balanceAfterText, substring = true)
                    }
                }
            }
            step("Assert pending express withdrawal transaction is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { pendingExpressTransaction.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10099")
    @DisplayName("Tangem Pay: Withdraw button is disabled when balance is zero")
    @Test
    fun withdrawButtonDisabledWhenBalanceIsZeroTest() {
        val balanceScenario = "tangem_pay_balance_update"
        val eligibilityState = "PaeraCustomer"
        // Displayed balance follows the balance scenario; unset it and the mocked build shows a hardcoded $123.45.
        val zeroBalanceState = "AfterFullWithdraw"
        val zeroBalanceText = "0.00"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, zeroBalanceState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
            },
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert balance contains '$zeroBalanceText'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe(zeroBalanceText, substring = true) }
            }
            // ACTION_BUTTON exposes no Disabled semantics; a disabled one simply carries no click action.
            step("Assert 'Withdraw' button is disabled") {
                onTangemPayMainScreen { withdrawButton.assertHasNoClickAction() }
            }
        }
    }

    @AllureId("9599")
    @DisplayName("Tangem Pay: withdraw shows insufficient funds error when amount exceeds balance")
    // App bug: withdraw keeps the Swap button enabled and shows no insufficient-funds state when amount > balance.
    @Ignore("[REDACTED_JIRA]")
    @Test
    fun withdrawInsufficientFundsShowsErrorTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val expressAssetsScenario = "express_api_assets"
        val exchangeStatusScenario = "exchange_status_provider"
        val balanceScenario = "tangem_pay_balance_update"
        val historyScenario = "tangem_pay_transaction_history"
        val eligibilityState = "PaeraCustomer"
        val bitcoinStartedState = "Started"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val exchangeStatusState = "Changelly"
        val balanceInitialState = "InitialBalance"
        val historyInitialState = "InitialEmpty"
        // Withdrawable balance is $10 (InitialBalance); 20 exceeds it and triggers the insufficient-funds state.
        val excessiveAmount = "20"
        val receiveToken = "Bitcoin"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(bitcoinScenario)
                resetWireMockScenarioState(expressAssetsScenario)
                resetWireMockScenarioState(exchangeStatusScenario)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay withdraw Swap screen") { openTangemPayWithdrawSwapScreen() }
            step("Choose receive token '$receiveToken'") { chooseWithdrawReceiveToken(receiveToken) }
            step("Enter withdraw amount '$excessiveAmount'") { enterWithdrawAmount(excessiveAmount) }
            step("Assert 'Insufficient funds' error is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { insufficientFundsButton.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9602")
    @DisplayName("Tangem Pay: withdraw displays the correct CEX provider")
    @Test
    fun withdrawDisplaysCorrectCexProviderTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val expressAssetsScenario = "express_api_assets"
        val exchangeStatusScenario = "exchange_status_provider"
        val balanceScenario = "tangem_pay_balance_update"
        val historyScenario = "tangem_pay_transaction_history"
        val eligibilityState = "PaeraCustomer"
        val bitcoinStartedState = "Started"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val exchangeStatusState = "Changelly"
        val balanceInitialState = "InitialBalance"
        val historyInitialState = "InitialEmpty"
        val withdrawAmount = "5"
        val receiveToken = "Bitcoin"
        val providerName = "Changelly"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(bitcoinScenario)
                resetWireMockScenarioState(expressAssetsScenario)
                resetWireMockScenarioState(exchangeStatusScenario)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay withdraw Swap screen") { openTangemPayWithdrawSwapScreen() }
            step("Choose receive token '$receiveToken'") { chooseWithdrawReceiveToken(receiveToken) }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Assert provider '$providerName' is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerWithName(providerName).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9607")
    @DisplayName("Tangem Pay: withdraw submits the exchange exactly once on confirm")
    @Test
    fun withdrawSwapConfirmSubmitsExchangeOnceTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val expressAssetsScenario = "express_api_assets"
        val exchangeStatusScenario = "exchange_status_provider"
        val balanceScenario = "tangem_pay_balance_update"
        val historyScenario = "tangem_pay_transaction_history"
        val eligibilityState = "PaeraCustomer"
        val bitcoinStartedState = "Started"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val exchangeStatusState = "Changelly"
        val balanceInitialState = "InitialBalance"
        val historyInitialState = "InitialEmpty"
        val withdrawAmount = "5"
        val receiveToken = "Bitcoin"
        val expectedSubmissions = 1
        // CEX withdraw submits via the Express swap endpoint — count it to prove a single submission.
        val exchangeSentEndpointPattern = "/v1/exchange-sent"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(bitcoinScenario)
                resetWireMockScenarioState(expressAssetsScenario)
                resetWireMockScenarioState(exchangeStatusScenario)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay withdraw Swap screen") { openTangemPayWithdrawSwapScreen() }
            step("Choose receive token '$receiveToken'") { chooseWithdrawReceiveToken(receiveToken) }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Wait for the receive amount to load (quote ready, HoldToConfirm enabled)") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveAmount.assertIsDisplayed() }
                }
            }
            val submissionsBefore = getWireMockRequestCount("POST", exchangeSentEndpointPattern)
            step("Confirm swap by holding the button") {
                confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE)
            }
            step("Wait for 'Swap in progress' screen") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Assert the exchange was submitted exactly once") {
                val submissionsAfter = getWireMockRequestCount("POST", exchangeSentEndpointPattern)
                assertEquals(
                    "Exchange should be submitted exactly once",
                    expectedSubmissions,
                    submissionsAfter - submissionsBefore,
                )
            }
        }
    }

    @AllureId("9600")
    @DisplayName("Tangem Pay: full withdraw zeroes the balance and appends withdrawal to history")
    @Test
    fun fullWithdrawZeroesBalanceAppendsToHistoryTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val expressAssetsScenario = "express_api_assets"
        val exchangeStatusScenario = "exchange_status_provider"
        val balanceScenario = "tangem_pay_balance_update"
        val historyScenario = "tangem_pay_transaction_history"
        val eligibilityState = "PaeraCustomer"
        val bitcoinStartedState = "Started"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val exchangeStatusState = "Changelly"
        val balanceInitialState = "InitialBalance"
        val balanceAfterState = "AfterFullWithdraw"
        val historyInitialState = "InitialEmpty"
        val historyAfterState = "AfterFullWithdraw"
        val withdrawAmount = "10"
        val receiveToken = "Bitcoin"
        val zeroBalanceText = "0.00"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(bitcoinScenario)
                resetWireMockScenarioState(expressAssetsScenario)
                resetWireMockScenarioState(exchangeStatusScenario)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            step("Open Tangem Pay withdraw Swap screen") { openTangemPayWithdrawSwapScreen() }
            step("Choose receive token '$receiveToken'") { chooseWithdrawReceiveToken(receiveToken) }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Wait for the receive amount to load (quote ready, HoldToConfirm enabled)") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveAmount.assertIsDisplayed() }
                }
            }
            step("Confirm swap by holding the button") {
                confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE)
            }
            step("Wait for 'Swap in progress' screen") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterState)
            }
            step("Switch WireMock scenario '$historyScenario' to '$historyAfterState'") {
                setWireMockScenarioState(historyScenario, historyAfterState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert balance updated to contain '$zeroBalanceText'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe(zeroBalanceText, substring = true)
                    }
                }
            }
            step("Assert pending express withdrawal transaction is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { pendingExpressTransaction.assertIsDisplayed() }
                }
            }
        }
    }
}