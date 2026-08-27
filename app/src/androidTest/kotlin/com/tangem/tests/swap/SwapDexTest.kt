package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.R as CommonR
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.confirmSwapByHolding
import com.tangem.scenarios.openSwapAmountScreen
import com.tangem.screens.onExpressStatusBottomSheet
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTokenDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapDexTest : BaseTestCase() {

    private val fromTokenName = "POL (ex-MATIC)"
    private val receiveTokenName = "Ethereum"
    private val inputAmount = "50"
    private val providerName = "1Inch"
    private val userTokensState = "HotWalletSvS"
    private val quotesState = "PolygonUSDC"
    private val assetsScenarioName = "express_api_assets"
    private val assetsExchangeEnabledState = "BitcoinExchangeEnabled"
    private val pairsScenarioName = "ethereum_from_pairs"
    private val dexProviderState = "DexProvider"
    private val exchangeStatusScenario = "exchange_status_provider"
    private val oneInchStatusState = "OneInch"

    @AllureId("9459")
    @DisplayName("Swap: after a DEX swap the token details show the DEX exchange block with statuses and data")
    @Test
    fun checkDexSwapStatusBlockTest() {
        val expressStatusItemTitle = getResourceString(CommonR.string.express_exchange_by, providerName)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(pairsScenarioName)
                resetWireMockScenarioState(exchangeStatusScenario)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$dexProviderState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = dexProviderState)
            }
            step("Set WireMock scenario '$exchangeStatusScenario' to '$oneInchStatusState'") {
                setWireMockScenarioState(scenarioName = exchangeStatusScenario, state = oneInchStatusState)
            }

            step("Open the swap amount screen for '$fromTokenName' -> '$receiveTokenName' on an existing hot wallet") {
                openSwapAmountScreen(
                    fromTokenName = fromTokenName,
                    receiveTokenName = receiveTokenName,
                    amount = inputAmount,
                    seedPhrase = SVS_SEED_PHRASE_12,
                )
            }
            step("Assert the DEX provider block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertIsDisplayed() }
                }
            }
            step("Assert the network fee block is displayed (quote and fee are ready)") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { networkFeeBlock.assertIsDisplayed() }
                }
            }
            step("Confirm the swap by holding the 'Swap' button and sign") {
                confirmSwapByHolding()
            }
            step("Assert the 'Swap in progress' screen is displayed") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Assert the DEX exchange block '$expressStatusItemTitle' is displayed on token details") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { expressStatusItem(expressStatusItemTitle).assertIsDisplayed() }
                }
            }
            step("Open the DEX exchange block") {
                onTokenDetailsScreen { expressStatusItem(expressStatusItemTitle).clickWithAssertion() }
            }
            step("Assert the exchange status details are displayed") {
                onExpressStatusBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { title.assertIsDisplayed() }
                }
            }
            step("Assert the DEX provider '$providerName' is shown in the status details") {
                onExpressStatusBottomSheet { providerName(providerName).assertIsDisplayed() }
            }
        }
    }
}