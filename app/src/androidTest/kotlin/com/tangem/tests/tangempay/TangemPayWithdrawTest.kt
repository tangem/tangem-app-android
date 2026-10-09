package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.utils.findWireMockRequestBodies
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

@HiltAndroidTest
class TangemPayWithdrawTest : BaseTestCase() {

    private val withdrawScenario = "tangem_pay_withdraw"
    private val withdrawDataEndpoint = "/bff-v2/v1/customer/card/withdraw/data"
    private val withdrawEndpoint = "/bff-v2/v1/customer/card/withdraw"
    private val withdrawOrderEndpoint = "/bff-v2/v1/order/mock-tangempay-withdraw-order-001"
    private val exchangeSentEndpoint = "/v1/exchange-sent"

    private val minPollsToProveContinuation = 2

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
        val withdrawalRowTitle = getResourceString(com.tangem.core.res.R.string.tangem_pay_withdrawal)
        val withdrawalAmountText = "-$${withdrawAmount}.00"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            }
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
            step("Assert the withdrawal transaction is appended to history") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        scrollToTransactionWithText(withdrawalRowTitle)
                        transactionRowWithText(withdrawalRowTitle).assertIsDisplayed()
                        transactionRowWithText(withdrawalAmountText).assertIsDisplayed()
                    }
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
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, zeroBalanceState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert balance contains '$zeroBalanceText'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe(zeroBalanceText, substring = true) }
            }
            step("Assert 'Withdraw' button is disabled") {
                onTangemPayMainScreen { withdrawButtonControl.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("9599")
    @DisplayName("Tangem Pay: withdraw shows insufficient funds error when amount exceeds balance")
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
            }
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
            }
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
            }
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
        val withdrawalRowTitle = getResourceString(com.tangem.core.res.R.string.tangem_pay_withdrawal)
        val withdrawalAmountText = "-$${withdrawAmount}.00"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
            }
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
            step("Assert the withdrawal transaction is appended to history") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        scrollToTransactionWithText(withdrawalRowTitle)
                        transactionRowWithText(withdrawalRowTitle).assertIsDisplayed()
                        transactionRowWithText(withdrawalAmountText).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("11386")
    @DisplayName("Tangem Pay: the withdrawal is addressed to the payment-account token that was picked")
    @Test
    fun withdrawFromSelectedPaymentAccountTokenTest() {
        val balanceScenario = "tangem_pay_balance_update"
        val balanceAfterState = "MultiNetworkStatusesAfterWithdraw"
        val initialBalanceText = "17.00"
        val balanceAfterText = "12.00"
        val sourceToken = "USDT"
        val sourceNetwork = "BNB Smart Chain"
        val receiveToken = "Bitcoin"
        val withdrawAmount = "5"
        val withdrawEndpoint = "/bff-v2/v1/customer/card/withdraw"
        val bscChainId = 56
        val usdtBscContractAddress = "0x55d398326f99059ff775485246999027b3197955"

        setupHooks().run {
            step("Set WireMock scenarios for a multichain Tangem Pay withdrawal") {
                setTangemPayMultiNetworkWithdrawScenarios()
            }
            step("Open Tangem Pay withdraw Swap screen") {
                openTangemPayWithdrawSwapScreen(expectedBalance = initialBalanceText)
            }
            step("Choose source token '$sourceToken' on '$sourceNetwork'") {
                chooseWithdrawSourceToken(tokenName = sourceToken, networkName = sourceNetwork)
            }
            step("Choose receive token '$receiveToken'") { chooseWithdrawReceiveToken(receiveToken) }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Wait for the receive amount to load (quote ready, HoldToConfirm enabled)") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveAmount.assertIsDisplayed() }
                }
            }
            val withdrawalsBefore = findWireMockRequestBodies("POST", withdrawEndpoint).size
            step("Confirm swap by holding the button") {
                confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE)
            }
            step("Wait for 'Swap in progress' screen") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Assert the withdrawal names '$sourceToken' on '$sourceNetwork'") {
                val withdrawals = findWireMockRequestBodies("POST", withdrawEndpoint)
                assertEquals(
                    "Exactly one withdrawal should be submitted",
                    1,
                    withdrawals.size - withdrawalsBefore,
                )
                val body = JSONObject(withdrawals.last())
                assertEquals("Withdrawal should name the BNB Smart Chain id", bscChainId, body.getInt("chain_id"))
                assertEquals(
                    "Withdrawal should name the selected USDT contract",
                    usdtBscContractAddress,
                    body.getString("token_contract_address").lowercase(),
                )
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert the balance '$balanceAfterText' is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe(balanceAfterText, substring = true)
                    }
                }
            }
        }
    }

    @AllureId("11385")
    @DisplayName("Tangem Pay: the withdraw source selector hides zero-balance payment-account tokens")
    @Test
    fun withdrawSourceSelectorHidesZeroBalanceTokensTest() {
        val initialBalanceText = "17.00"
        val usdcToken = "USDC"
        val usdtToken = "USDT"
        val polygonNetwork = "Polygon"
        val bscNetwork = "BNB Smart Chain"
        val allFilterLabel = getResourceString(com.tangem.core.res.R.string.common_all)

        setupHooks().run {
            step("Set WireMock scenarios for a multichain Tangem Pay withdrawal") {
                setTangemPayMultiNetworkWithdrawScenarios()
            }
            step("Open Tangem Pay withdraw Swap screen") {
                openTangemPayWithdrawSwapScreen(expectedBalance = initialBalanceText)
            }
            step("Open the source token selector on the 'Payment' account") {
                openWithdrawSourceTokenSelector()
            }
            step("Assert only the funded payment-account tokens are displayed") {
                onSwapSelectTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tokenWithNameAndNetwork(usdcToken, polygonNetwork).assertIsDisplayed()
                    }
                    tokenWithNameAndNetwork(usdtToken, bscNetwork).assertIsDisplayed()
                    tokenWithNameAndNetwork(usdcToken, bscNetwork).assertDoesNotExist()
                }
            }
            step("Select the '$allFilterLabel' balance filter") { selectBalanceFilter(allFilterLabel) }
            step("Assert the zero-balance token is displayed too") {
                onSwapSelectTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tokenWithNameAndNetwork(usdcToken, bscNetwork).assertIsDisplayed()
                    }
                    tokenWithNameAndNetwork(usdcToken, polygonNetwork).assertIsDisplayed()
                    tokenWithNameAndNetwork(usdtToken, bscNetwork).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("11645")
    @DisplayName("Tangem Pay: Withdraw is enabled when the balance is withdrawable on one non-Polygon network only")
    @Test
    fun withdrawEnabledWhenOnlyOneNonPolygonNetworkIsWithdrawableTest() {
        val storiesScenario = "stories_first_time_swap_v2"
        val balanceScenario = "tangem_pay_balance_update"
        val expressAssetsScenario = "express_api_assets"
        val historyScenario = "tangem_pay_transaction_history"
        val storiesErrorState = "Error"
        val eligibilityState = "PaeraCustomer"
        val expressAssetsState = "PayWithdrawAllNetworks"
        val historyState = "InitialEmpty"
        val withdrawableOnlyOnNetwork = listOf(
            "WithdrawableOnlyOnBsc" to "BNB Smart Chain",
            "WithdrawableOnlyOnBase" to "Base",
            "WithdrawableOnlyOnArbitrum" to "Arbitrum One",
        )
        val (firstBalanceState, _) = withdrawableOnlyOnNetwork.first()
        val customerMeEndpoint = "/bff-v2/v1/customer/me"
        val balanceText = "10.00"
        val usdcToken = "USDC"

        setupHooks(
            // The stories request fires at app start and the loop opens Withdraw three times, so keep them off.
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$expressAssetsScenario' to '$expressAssetsState'") {
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
            }
            step("Set WireMock scenario '$historyScenario' to '$historyState'") {
                setWireMockScenarioState(historyScenario, historyState)
            }
            step("Set WireMock scenario '$balanceScenario' to '$firstBalanceState'") {
                setWireMockScenarioState(balanceScenario, firstBalanceState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            withdrawableOnlyOnNetwork.forEachIndexed { index, (balanceState, networkName) ->
                step("Set WireMock scenario '$balanceScenario' to '$balanceState'") {
                    setWireMockScenarioState(balanceScenario, balanceState)
                }
                val customerMeRequestsBefore = getWireMockRequestCount("GET", customerMeEndpoint)
                step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
                // Every state has the same $10 balance, so only the journal proves the refresh reached WireMock.
                step("Assert '$customerMeEndpoint' is requested again") {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        val customerMeRequests = getWireMockRequestCount("GET", customerMeEndpoint)
                        if (customerMeRequests <= customerMeRequestsBefore) {
                            throw AssertionError("'$customerMeEndpoint' was not requested after the refresh")
                        }
                    }
                }
                step("Assert balance contains '$balanceText'") {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(balanceText, substring = true) }
                }
                step("Assert the balance is withdrawable only on '$networkName'") {
                    checkTangemPayWithdrawableOnlyOnNetwork(
                        tokenName = usdcToken,
                        networkName = networkName,
                        otherNetworkNames = withdrawableOnlyOnNetwork.map { it.second } - networkName,
                        expandPaymentAccount = index == 0,
                    )
                }
            }
        }
    }

    @AllureId("9605")
    @DisplayName("Tangem Pay: a failing withdraw data request shows an error and nothing is submitted")
    @Test
    fun withdrawDataErrorShowsErrorTest() {
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
        val withdrawDataErrorState = "WithdrawDataError"
        val unknownErrorText = getResourceString(com.tangem.core.res.R.string.common_unknown_error)

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(withdrawScenario, withdrawDataErrorState)
            }
        ).run {
            step("Open the withdraw to '$receiveToken' ready to confirm") {
                openTangemPayWithdrawReadyToConfirm(receiveToken, withdrawAmount)
            }
            val withdrawDataBefore = getWireMockRequestCount("POST", withdrawDataEndpoint)
            val withdrawBefore = getWireMockRequestCount("POST", withdrawEndpoint)
            val exchangeSentBefore = getWireMockRequestCount("POST", exchangeSentEndpoint)
            step("Confirm swap by holding the button") {
                confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert the withdrawal error dialog is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onDialog { containerWithText(unknownErrorText).assertIsDisplayed() }
                }
            }
            step("Assert the withdrawal stopped before submitting") {
                assertEquals(
                    "Withdrawal data should be requested once",
                    1,
                    getWireMockRequestCount("POST", withdrawDataEndpoint) - withdrawDataBefore,
                )
                assertEquals(
                    "Withdrawal should not be submitted without signing data",
                    withdrawBefore,
                    getWireMockRequestCount("POST", withdrawEndpoint),
                )
                assertEquals(
                    "Failed withdrawal should not be reported to Express",
                    exchangeSentBefore,
                    getWireMockRequestCount("POST", exchangeSentEndpoint),
                )
            }
        }
    }

    @AllureId("9606")
    @DisplayName("Tangem Pay: a withdraw failing after signing shows an error")
    @Test
    fun withdrawErrorAfterSigningShowsErrorTest() {
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
        val withdrawErrorState = "WithdrawError"
        val unknownErrorText = getResourceString(com.tangem.core.res.R.string.common_unknown_error)

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(withdrawScenario, withdrawErrorState)
            }
        ).run {
            step("Open the withdraw to '$receiveToken' ready to confirm") {
                openTangemPayWithdrawReadyToConfirm(receiveToken, withdrawAmount)
            }
            val withdrawBefore = getWireMockRequestCount("POST", withdrawEndpoint)
            val orderBefore = getWireMockRequestCount("GET", withdrawOrderEndpoint)
            val exchangeSentBefore = getWireMockRequestCount("POST", exchangeSentEndpoint)
            step("Confirm swap by holding the button") {
                confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert the withdrawal error dialog is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onDialog { containerWithText(unknownErrorText).assertIsDisplayed() }
                }
            }
            step("Assert the signed withdrawal was submitted once and not followed up") {
                assertEquals(
                    "Signed withdrawal should be submitted once",
                    1,
                    getWireMockRequestCount("POST", withdrawEndpoint) - withdrawBefore,
                )
                assertEquals(
                    "Failed withdrawal should not poll the order",
                    orderBefore,
                    getWireMockRequestCount("GET", withdrawOrderEndpoint),
                )
                assertEquals(
                    "Failed withdrawal should not be reported to Express",
                    exchangeSentBefore,
                    getWireMockRequestCount("POST", exchangeSentEndpoint),
                )
            }
        }
    }

    @AllureId("9658")
    @DisplayName("Tangem Pay: the withdraw order without a transaction hash keeps being polled after Close")
    @Test
    fun withdrawOrderWithoutHashKeepsPollingAfterCloseTest() {
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
        val orderWithoutHashState = "OrderWithoutHash"
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(bitcoinScenario, bitcoinStartedState)
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
                setWireMockScenarioState(exchangeStatusScenario, exchangeStatusState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(withdrawScenario, orderWithoutHashState)
            }
        ).run {
            step("Open the withdraw to '$receiveToken' ready to confirm") {
                openTangemPayWithdrawReadyToConfirm(receiveToken, withdrawAmount)
            }
            val exchangeSentBefore = getWireMockRequestCount("POST", exchangeSentEndpoint)
            val ordersBeforeConfirm = getWireMockRequestCount("GET", withdrawOrderEndpoint)
            step("Confirm swap by holding the button") {
                confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert 'Swap in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { onSwapSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Assert the order without a transaction hash was fetched after the withdraw") {
                awaitTangemPayWithdrawOrderPolls(withdrawOrderEndpoint, ordersBeforeConfirm + 1)
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            val ordersAfterClose = getWireMockRequestCount("GET", withdrawOrderEndpoint)
            step("Assert the order keeps being polled after Close") {
                awaitTangemPayWithdrawOrderPolls(withdrawOrderEndpoint, ordersAfterClose + minPollsToProveContinuation)
            }
            step("Assert the withdrawal without a transaction hash was not reported to Express") {
                assertEquals(
                    "Withdrawal without a transaction hash should not be reported to Express",
                    exchangeSentBefore,
                    getWireMockRequestCount("POST", exchangeSentEndpoint),
                )
            }
        }
    }
}