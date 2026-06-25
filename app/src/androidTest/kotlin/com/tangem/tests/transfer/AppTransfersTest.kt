package com.tangem.tests.transfer

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.content.Wallet2WithDerivationsMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AppTransfersTest : BaseTestCase() {

    private val ethCallScenario = "eth_call_api"
    private val ethBalanceScenario = "eth_network_balance"
    private val started = "Started"
    // Disable the first-time-swap stories (500 → not shown); their auto-advancing animation keeps Compose non-idle and flakes the close.
    private val storiesScenario = "stories_first_time_swap_v2"
    private val storiesErrorState = "Error"

    @AllureId("9838")
    @DisplayName("App transfers: identical pair switches to Transfer mode")
    @Test
    fun identicalPairSwitchesToTransferModeTest() {
        val token = "Ethereum"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameToken"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethCallScenario, state = started)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = started)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    @AllureId("9843")
    @DisplayName("App transfers: zero amount keeps Transfer button disabled")
    @Test
    fun zeroAmountKeepsTransferButtonDisabledTest() {
        val token = "Ethereum"
        val userTokensState = "TwoAccountsSameToken"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethCallScenario, state = started)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = started)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Assert provider block is not displayed") {
                onSwapTokenScreen { providersBlock.assertIsNotDisplayed() }
            }
            step("Assert 'Transfer' button is disabled") {
                onSwapTokenScreen { transferButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("9992")
    @DisplayName("App transfers: reversing tokens keeps Transfer mode")
    @Test
    fun reversingTokensKeepsTransferModeTest() {
        val token = "Ethereum"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameToken"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethCallScenario, state = started)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = started)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
            step("Click on 'Swap tokens' (reverse) button") {
                onSwapTokenScreen { replaceTokensButton.performClick() }
            }
            step("Assert Transfer mode is ready") { assertTransferReady() }
        }
    }

    @AllureId("9847")
    @DisplayName("App transfers: Max amount keeps Transfer enabled and subtracts fee")
    @Test
    fun maxAmountFractionSubtractsFeeTest() {
        val token = "Ethereum"
        val userTokensState = "TwoAccountsSameToken"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethCallScenario, state = started)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = started)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Focus amount field to reveal predefined amount buttons") {
                waitForIdle()
                onSwapTokenScreen { textInput.clickWithAssertion() }
            }
            step("Click on 'Max' amount button") {
                onSwapTokenScreen { maxAmountButton.performClick() }
            }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert 'Transfer' button is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { transferButton.assertIsEnabled() }
                }
            }
        }
    }

    @AllureId("9844")
    @DisplayName("App transfers: amount above balance disables Transfer")
    @Test
    fun amountAboveBalanceDisablesTransferTest() {
        val token = "Ethereum"
        val aboveBalanceAmount = "100"
        val userTokensState = "TwoAccountsSameToken"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethCallScenario, state = started)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = started)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$aboveBalanceAmount'") { inputAmount(aboveBalanceAmount) }
            // Above-balance recalculates the fee forever (Compose never idles), so assert the "Insufficient funds" title, not button state.
            step("Assert 'Insufficient funds' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { insufficientFundsErrorTitle.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10003")
    @DisplayName("App transfers: EVM network fee speed options")
    @Test
    fun evmNetworkFeeSpeedOptionsTest() {
        val token = "Ethereum"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameToken"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethCallScenario, state = started)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = started)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
            step("Assert Transfer mode is ready") { assertTransferReady() }

            var marketFee = ""
            step("Read displayed 'Market' fee amount") {
                onSwapTokenScreen { marketFee = feeAmount.extractText() }
            }
            step("Open 'Network fee' selector via 'Select fee' icon") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { selectFeeIcon.performClick() }
                    onSwapSelectNetworkFeeBottomSheet { fastSelectorItem.assertIsDisplayed() }
                }
            }
            step("Click on 'Fast' fee option") {
                onSwapSelectNetworkFeeBottomSheet { fastSelectorItem.clickWithAssertion() }
            }
            step("Assert fee amount changed from Market fee") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { feeAmount.assertIsDisplayed() }
                    check(swapFeeDiffersFrom(marketFee)) { "Network fee did not change from '$marketFee'" }
                }
            }
        }
    }

    @AllureId("10002")
    @DisplayName("App transfers: UTXO network fee")
    @Test
    fun utxoNetworkFeeTest() {
        val token = "Bitcoin"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameBitcoin"
        val bitcoinUtxoScenario = "bitcoin_utxo"
        val bitcoinUtxoState = "BalanceAnyAddress"
        val assetsScenario = "express_api_assets"
        val assetsBitcoinState = "BitcoinExchangeEnabled"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(bitcoinUtxoScenario)
                resetWireMockScenarioState(assetsScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$bitcoinUtxoScenario' to state: '$bitcoinUtxoState'") {
                setWireMockScenarioState(scenarioName = bitcoinUtxoScenario, state = bitcoinUtxoState)
            }
            // Bitcoin swap must be exchange-enabled or the token-details Swap button stays disabled.
            step("Set WireMock scenario: '$assetsScenario' to state: '$assetsBitcoinState'") {
                setWireMockScenarioState(scenarioName = assetsScenario, state = assetsBitcoinState)
            }

            // V3 card: Bitcoin's default path is m/84' (matches the stub) so the coin isn't custom — else the Swap button stays disabled.
            step("Open Swap in Transfer mode for '$token'") {
                openSwapInTransferMode(token, mockContent = Wallet2WithDerivationsMockContent)
            }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    @AllureId("10004")
    @DisplayName("App transfers: Solana network fee")
    @Test
    fun solanaNetworkFeeTest() {
        val token = "Solana"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameSolana"
        val solanaBalanceScenario = "solana_balance"
        val quotesSolanaState = "Solana"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(solanaBalanceScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$solanaBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = solanaBalanceScenario, state = started)
            }
            // Non-zero SOL price keeps total fiat > 0 so the empty-wallet banner doesn't push the account list under the Markets sheet.
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesSolanaState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesSolanaState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    @AllureId("9845")
    @DisplayName("App transfers: insufficient native coin for fee disables Transfer")
    @Test
    fun insufficientNativeCoinForFeeDisablesTransferTest() {
        val token = "Tether"
        val feeCoinName = "Ethereum"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameUsdt"
        // Zero native ETH (coin present in the mock so the fee still estimates) → fee exceeds balance.
        val ethBalanceState = "EmptyAnyId"
        val quotesUsdtState = "USDTHotWalletSvS"
        val feeHistoryScenario = "eth_fee_history"
        val estimateGasScenario = "eth_estimate_gas"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(feeHistoryScenario)
                resetWireMockScenarioState(estimateGasScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethCallScenario, state = started)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$ethBalanceState'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = ethBalanceState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesUsdtState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesUsdtState)
            }
            step("Set WireMock scenario: '$feeHistoryScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = feeHistoryScenario, state = started)
            }
            step("Set WireMock scenario: '$estimateGasScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = estimateGasScenario, state = started)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert 'Insufficient $feeCoinName to cover network fee' notification is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen {
                        insufficientFeeForTransferNotificationTitle(feeCoinName).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("9990")
    @DisplayName("App transfers: search filters receive token list")
    @Test
    fun searchFiltersReceiveTokenListTest() {
        val sourceToken = "Polygon"
        val ethereumToken = "Ethereum"
        val polygonReceiveName = "POL (ex-MATIC)"
        val noMatchQuery = "f"
        val polygonQuery = "pol"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = { resetWireMockScenarioState(storiesScenario) },
        ).run {
            step("Open 'Main' screen") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Click on token with name: '$sourceToken'") {
                onMainScreen { tokenWithTitleAndAddress(sourceToken).clickWithAssertion() }
            }
            step("Open 'Swap' screen") { openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false) }
            step("Open receive token selector") {
                onSwapTokenScreen { chooseTokenButton.performClick() }
            }
            step("Type '$noMatchQuery' in search field") {
                onSwapSelectTokenScreen {
                    searchBarBlock.performClick()
                    searchBarBlock.performTextInput(noMatchQuery)
                }
            }
            step("Assert '$ethereumToken' is not displayed") {
                onSwapSelectTokenScreen { tokenWithName(ethereumToken).assertIsNotDisplayed() }
            }
            step("Assert '$polygonReceiveName' is not displayed") {
                onSwapSelectTokenScreen { tokenWithName(polygonReceiveName).assertIsNotDisplayed() }
            }
            step("Replace search text with '$polygonQuery'") {
                onSwapSelectTokenScreen { searchBarBlock.performTextReplacement(polygonQuery) }
            }
            step("Assert '$polygonReceiveName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapSelectTokenScreen { tokenWithName(polygonReceiveName).assertIsDisplayed() }
                }
            }
            step("Assert '$ethereumToken' is not displayed") {
                onSwapSelectTokenScreen { tokenWithName(ethereumToken).assertIsNotDisplayed() }
            }
        }
    }
}