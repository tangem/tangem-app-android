package com.tangem.tests.transfer

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R as CoreUiR
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import com.tangem.tap.domain.sdk.mocks.content.Wallet2WithDerivationsMockContent
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
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

    @AllureId("9841")
    @DisplayName("App transfers: full transfer reaches 'Transfer in progress' screen")
    @Test
    fun fullTransferReachesTransferInProgressScreenTest() {
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

            step("Open Swap in Transfer mode for '$token' with existing hot wallet") {
                openSwapInTransferModeWithHotWallet(tokenName = token, seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
            step("Hold to confirm the transfer") { holdToConfirmTransfer() }
            step("Assert 'Transfer in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapSuccessScreen { transferInProgressTitle.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9999")
    @DisplayName("App transfers: broadcast error shows alert without finish screen")
    @Test
    fun broadcastErrorShowsAlertWithoutFinishScreenTest() {
        val token = "Ethereum"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameToken"
        val sendRawTransactionScenario = "eth_sendRawTransaction"
        val broadcastErrorState = "BroadcastError"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
                resetWireMockScenarioState(sendRawTransactionScenario)
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
            step("Set WireMock scenario: '$sendRawTransactionScenario' to state: '$broadcastErrorState'") {
                setWireMockScenarioState(scenarioName = sendRawTransactionScenario, state = broadcastErrorState)
            }

            step("Open Swap in Transfer mode for '$token' with existing hot wallet") {
                openSwapInTransferModeWithHotWallet(tokenName = token, seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
            step("Hold to confirm the transfer") { holdToConfirmTransfer() }
            step("Assert 'Transaction failed' dialog is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onFailedTransactionDialog { dialogContainer.assertIsDisplayed() }
                }
            }
            step("Assert 'Transfer in progress' screen is not displayed") {
                onSwapSuccessScreen { transferInProgressTitle.assertDoesNotExist() }
            }
        }
    }

    @AllureId("9989")
    @DisplayName("App transfers: receive list allows identical token on another account")
    @Test
    fun receiveListAllowsIdenticalTokenOnAnotherAccountTest() {
        val token = "Ethereum"
        val receiveAccountName = "Account 2"
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

            step("Open Swap for '$token' in 'Account 1'") { openSwapForTokenInAccount(token) }
            step("Open receive token selector") {
                onSwapTokenScreen { chooseTokenButton.performClick() }
            }
            step("Expand account '$receiveAccountName' in receive selector") {
                onSwapSelectTokenScreen { tokenWithName(receiveAccountName).performClick() }
            }
            step("Assert token '$token' is displayed in receive selector") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapSelectTokenScreen { tokenWithName(token).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9998")
    @DisplayName("App transfers: fee calculation error disables Transfer")
    @Test
    fun feeCalculationErrorDisablesTransferTest() {
        val token = "Ethereum"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameToken"
        val feeHistoryScenario = "eth_fee_history"
        val estimateGasScenario = "eth_estimate_gas"
        val unreachable = "Unreachable"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
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
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = started)
            }
            step("Set WireMock scenario: '$feeHistoryScenario' to state: '$unreachable'") {
                setWireMockScenarioState(scenarioName = feeHistoryScenario, state = unreachable)
            }
            step("Set WireMock scenario: '$estimateGasScenario' to state: '$unreachable'") {
                setWireMockScenarioState(scenarioName = estimateGasScenario, state = unreachable)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            // Unreachable fee endpoints leave the fee unresolved (shown as '—'); the transfer stays blocked.
            step("Assert 'Transfer' button is disabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { transferButton.assertIsNotEnabled() }
                }
            }
        }
    }

    @AllureId("9994")
    @DisplayName("App transfers: mode switches reactively without screen reload")
    @Test
    fun modeSwitchesReactivelyWithoutScreenReloadTest() {
        val token = "Solana"
        val swapReceiveToken = "USDC"
        val userTokensState = "TwoAccountsSameSolanaWithUsdc"
        val solanaBalanceScenario = "solana_balance"
        val assetsScenario = "express_api_assets"
        val fromPairsScenario = "solana_from_pairs"
        val dexProviderState = "DexProvider"
        val quotesSolanaState = "Solana"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(solanaBalanceScenario)
                resetWireMockScenarioState(assetsScenario)
                resetWireMockScenarioState(fromPairsScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$solanaBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = solanaBalanceScenario, state = started)
            }
            step("Set WireMock scenario: '$assetsScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = assetsScenario, state = started)
            }
            step("Set WireMock scenario: '$fromPairsScenario' to state: '$dexProviderState'") {
                setWireMockScenarioState(scenarioName = fromPairsScenario, state = dexProviderState)
            }
            // Non-zero SOL price keeps total fiat > 0 so the empty-wallet banner doesn't push the account list under the Markets sheet.
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesSolanaState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesSolanaState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Change receive token to '$swapReceiveToken' to switch to Swap mode") {
                changeReceiveToken(swapReceiveToken)
            }
            step("Assert 'Swap' button is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { swapButton.assertIsDisplayed() }
                }
            }
            step("Change receive token back to identical '$token' to switch to Transfer mode") {
                changeReceiveToken(token)
            }
            step("Assert Transfer mode is ready") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { assertTransferReady() }
            }
        }
    }

    @AllureId("10001")
    @DisplayName("App transfers: memo field is not entered manually in Transfer mode")
    @Test
    fun memoFieldIsNotEnteredManuallyInTransferModeTest() {
        val token = "XRP Ledger"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameXRP"
        val rippleAccountInfoScenario = "ripple_account_info"
        val quotesRippleState = "Ripple"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(rippleAccountInfoScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$rippleAccountInfoScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = rippleAccountInfoScenario, state = started)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesRippleState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesRippleState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert manual 'Destination tag' field is not displayed") {
                onSwapTokenScreen { destinationTagField.assertDoesNotExist() }
            }
        }
    }

    @AllureId("10009")
    @DisplayName("App transfers: XRP network fee")
    @Test
    fun xrpNetworkFeeTest() {
        val token = "XRP Ledger"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameXRP"
        val rippleAccountInfoScenario = "ripple_account_info"
        val quotesRippleState = "Ripple"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(rippleAccountInfoScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$rippleAccountInfoScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = rippleAccountInfoScenario, state = started)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesRippleState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesRippleState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    @AllureId("10011")
    @DisplayName("App transfers: Stellar network fee")
    @Test
    fun stellarNetworkFeeTest() {
        val token = "Stellar"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameXLM"
        val quotesXlmState = "XLM"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesXlmState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesXlmState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    @AllureId("10005")
    @DisplayName("App transfers: Tron network fee")
    @Test
    fun tronNetworkFeeTest() {
        val token = "Tron"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameTron"
        val networksProvidersScenario = "networks_providers"
        val appTransfersNetworksState = "AppTransfersNetworks"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(storiesScenario, storiesErrorState)
                // networks_providers configures SDK RPC hosts at launch — must be set before the activity starts.
                setWireMockScenarioState(networksProvidersScenario, appTransfersNetworksState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(networksProvidersScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            // Non-zero prices keep total fiat > 0 so the empty-wallet banner doesn't push the account list under the Markets sheet.
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$appTransfersNetworksState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = appTransfersNetworksState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    // blockchain SDK TonProvidersBuilder drops public providers, so TON has no provider in the mocked build.
    @Ignore("[REDACTED_JIRA]")
    @AllureId("10012")
    @DisplayName("App transfers: TON network fee")
    @Test
    fun tonNetworkFeeTest() {
        // The SDK names TON's coin "Gram" (Blockchain.TON.getCoinName), so the portfolio row shows "Gram", not "Toncoin".
        val token = "Gram"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameTON"
        val networksProvidersScenario = "networks_providers"
        val appTransfersNetworksState = "AppTransfersNetworks"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(storiesScenario, storiesErrorState)
                // networks_providers configures SDK RPC hosts at launch — must be set before the activity starts.
                setWireMockScenarioState(networksProvidersScenario, appTransfersNetworksState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(networksProvidersScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            // Non-zero prices keep total fiat > 0 so the empty-wallet banner doesn't push the account list under the Markets sheet.
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$appTransfersNetworksState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = appTransfersNetworksState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    @AllureId("10013")
    @DisplayName("App transfers: Cosmos network fee")
    @Test
    fun cosmosNetworkFeeTest() {
        val token = "Cosmos"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameCosmos"
        val networksProvidersScenario = "networks_providers"
        val appTransfersNetworksState = "AppTransfersNetworks"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(storiesScenario, storiesErrorState)
                // networks_providers configures SDK RPC hosts at launch — must be set before the activity starts.
                setWireMockScenarioState(networksProvidersScenario, appTransfersNetworksState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(networksProvidersScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            // Non-zero prices keep total fiat > 0 so the empty-wallet banner doesn't push the account list under the Markets sheet.
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$appTransfersNetworksState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = appTransfersNetworksState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    @AllureId("10015")
    @DisplayName("App transfers: Aptos network fee")
    @Test
    fun aptosNetworkFeeTest() {
        val token = "Aptos"
        val amount = "0.001"
        val userTokensState = "TwoAccountsSameAptos"
        val networksProvidersScenario = "networks_providers"
        val appTransfersNetworksState = "AppTransfersNetworks"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(storiesScenario, storiesErrorState)
                // networks_providers configures SDK RPC hosts at launch — must be set before the activity starts.
                setWireMockScenarioState(networksProvidersScenario, appTransfersNetworksState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(networksProvidersScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            // Non-zero prices keep total fiat > 0 so the empty-wallet banner doesn't push the account list under the Markets sheet.
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$appTransfersNetworksState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = appTransfersNetworksState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
        }
    }

    @AllureId("9856")
    @DisplayName("App transfers: Transfer mode is available from a Tangem Pay account")
    @Test
    fun transferModeAvailableFromTangemPayAccountTest() {
        val token = "USDC"
        val receiveAccountName = "Main account"
        val eligibilityState = "PaeraCustomer"
        val balanceScenario = "tangem_pay_balance_update"
        val balanceInitialState = "InitialBalance"
        val historyScenario = "tangem_pay_transaction_history"
        val historyInitialState = "InitialEmpty"
        val userTokensState = "TangemPayTransferUsdc"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
                resetWireMockScenarioState(historyScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$balanceScenario' to state: '$balanceInitialState'") {
                setWireMockScenarioState(scenarioName = balanceScenario, state = balanceInitialState)
            }
            step("Set WireMock scenario: '$historyScenario' to state: '$historyInitialState'") {
                setWireMockScenarioState(scenarioName = historyScenario, state = historyInitialState)
            }
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }

            step("Open Tangem Pay") { openTangemPay() }
            step("Click on 'Withdraw' button") {
                onTangemPayMainScreen { withdrawButton.clickWithAssertion() }
            }
            step("Acknowledge withdrawal note sheet") {
                onTangemPayWithdrawNoteSheet {
                    title.assertIsDisplayed()
                    gotItButton.clickWithAssertion()
                }
            }
            step("Choose identical receive token '$token' from '$receiveAccountName'") {
                chooseIdenticalReceiveToken(tokenName = token, receiveAccountName = receiveAccountName)
            }
            // Withdraw-entry swap keeps recalculating — use flakySafely rather than assertTransferReady's waitUntil.
            step("Assert Transfer mode is ready") {
                flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) {
                    onSwapTokenScreen { transferTitle.assertIsDisplayed() }
                }
                onSwapTokenScreen { providersBlock.assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("9995")
    @DisplayName("App transfers: transfer between different wallets reaches 'Transfer in progress' screen")
    @Test
    fun transferBetweenDifferentWalletsReachesFinishTest() {
        val token = "Ethereum"
        val amount = "0.001"
        val secondWalletName = "Wallet 2"
        val userTokensState = "EthereumWithSecondToken"

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

            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Generate missing addresses") { generateMissingHotWalletAddresses() }
            step("Wait for addresses to be generated") { waitForAddressesGenerated() }
            step("Add a second card wallet '$secondWalletName'") {
                addNewCardWallet(WalletMockContent)
            }
            step("Switch back to the hot wallet") { switchToPreviousWallet() }
            step("Click on token with name: '$token'") { clickDisplayedTokenOnMain(token) }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false)
            }
            step("Select identical receive token '$token' on '$secondWalletName'") {
                selectReceiveTokenOnWallet(token = token, walletName = secondWalletName)
            }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert Transfer mode is ready") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
            step("Hold to confirm the transfer") { holdToConfirmTransfer() }
            step("Assert 'Transfer in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapSuccessScreen { transferInProgressTitle.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9996")
    @DisplayName("App transfers: adding a missing token to the recipient wallet enables Transfer")
    @Test
    fun addMissingTokenToRecipientWalletEnablesTransferTest() {
        val token = "Ethereum"
        val bitcoinToken = "Bitcoin"
        val recipientWalletName = "Wallet"
        val recipientWithoutEthereumState = "RecipientWithoutEthereum"
        val ethereumWithSecondTokenState = "EthereumWithSecondToken"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(ethCallScenario)
                resetWireMockScenarioState(ethBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$recipientWithoutEthereumState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = recipientWithoutEthereumState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethCallScenario, state = started)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = started)
            }

            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Generate missing addresses") { generateMissingHotWalletAddresses() }
            step("Wait for addresses to be generated") { waitForAddressesGenerated() }
            step("Assert token '$bitcoinToken' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { tokenWithTitleAndAddress(bitcoinToken).assertIsDisplayed() }
                }
            }
            // Switch the user-tokens mock so the second wallet loads with Ethereum while the recipient stays Ethereum-less.
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$ethereumWithSecondTokenState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = ethereumWithSecondTokenState)
            }
            step("Add a second card wallet") {
                addNewCardWallet(WalletMockContent)
            }
            step("Click on token with name: '$token'") { clickDisplayedTokenOnMain(token) }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false)
            }
            step("Add missing token '$token' to recipient wallet '$recipientWalletName'") {
                addMissingReceiveTokenToWallet(token = token, recipientWalletName = recipientWalletName)
            }
            step("Assert Transfer mode is ready") { assertTransferReady() }
        }
    }

    // [REDACTED_TASK_KEY]: transfer mode never runs tx validation, so the destination rent-exemption notification never shows.
    @Ignore("[REDACTED_JIRA]")
    @AllureId("9852")
    @DisplayName("App transfers: amount below destination reserve disables Transfer")
    @Test
    fun amountBelowDestinationReserveDisablesTransferTest() {
        val token = "Solana"
        val belowReserveAmount = "0.0001"
        val userTokensState = "TwoAccountsSameSolana"
        val solanaBalanceScenario = "solana_balance"
        val recipientAccountScenario = "solana_recipient_account"
        val notExistState = "NotExist"
        val quotesSolanaState = "Solana"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(solanaBalanceScenario)
                resetWireMockScenarioState(recipientAccountScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$solanaBalanceScenario' to state: '$started'") {
                setWireMockScenarioState(scenarioName = solanaBalanceScenario, state = started)
            }
            step("Set WireMock scenario: '$recipientAccountScenario' to state: '$notExistState'") {
                setWireMockScenarioState(scenarioName = recipientAccountScenario, state = notExistState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesSolanaState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesSolanaState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$belowReserveAmount'") { inputAmount(belowReserveAmount) }
            step("Assert error notification is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { errorNotificationTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Transfer' button is disabled") {
                onSwapTokenScreen { transferButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("9997")
    @DisplayName("App transfers: amount below minimum disables Transfer")
    @Test
    fun amountBelowMinimumDisablesTransferTest() {
        val token = "Kaspa"
        val belowMinimumAmount = "0.00000001"
        val userTokensState = "TwoAccountsSameKaspa"
        val kaspaUtxoScenario = "kaspa_utxo"
        // Android-specific UTXO body — addresses differ from the iOS fixture (see kaspa-utxo.json).
        val kaspaUtxoState = "more_than_84_android"
        val quotesKaspaState = "Kaspa"
        val invalidAmountTitle = getResourceString(CoreUiR.string.send_notification_invalid_amount_title)
        val minimumAmountMessagePrefix =
            getResourceString(CoreUiR.string.send_notification_invalid_minimum_amount_text).substringBefore("%1")

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalAfterSection = {
                resetWireMockScenarioState(storiesScenario)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(kaspaUtxoScenario)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario: '$kaspaUtxoScenario' to state: '$kaspaUtxoState'") {
                setWireMockScenarioState(scenarioName = kaspaUtxoScenario, state = kaspaUtxoState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesKaspaState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesKaspaState)
            }

            step("Open Swap in Transfer mode for '$token'") { openSwapInTransferMode(token) }
            step("Enter amount '$belowMinimumAmount'") { inputAmount(belowMinimumAmount) }
            step("Assert '$invalidAmountTitle' notification title is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { warningTitle(invalidAmountTitle).assertIsDisplayed() }
                }
            }
            step("Assert notification message contains the minimum-amount text") {
                onSwapTokenScreen { errorNotificationText.assertTextContains(minimumAmountMessagePrefix, substring = true) }
            }
            step("Assert 'Transfer' button is disabled") {
                onSwapTokenScreen { transferButton.assertIsNotEnabled() }
            }
        }
    }
}