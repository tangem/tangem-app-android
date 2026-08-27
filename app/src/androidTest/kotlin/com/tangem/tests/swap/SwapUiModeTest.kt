package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.openSwapAmountScreen
import com.tangem.scenarios.switchSwapMode
import com.tangem.screens.onSwapTokenScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapUiModeTest : BaseTestCase() {

    private val fromTokenName = "USDC"
    private val receiveTokenName = "Ethereum"
    private val inputAmount = "50"
    private val userTokensState = "PolygonUSDCEthereum"
    private val quotesState = "PolygonUSDC"
    private val assetsScenarioName = "express_api_assets"
    private val assetsExchangeEnabledState = "BitcoinExchangeEnabled"

    @AllureId("9984")
    @DisplayName("Swap: toggling the swap mode switches the provider block between the detailed and simple layouts")
    @Test
    fun checkTurnOnDetailedModeTest() {
        val simpleMode = getResourceString(R.string.swap_simple_mode)
        val detailedMode = getResourceString(R.string.swap_detailed_mode)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
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

            step("Open the swap amount screen for '$fromTokenName' -> '$receiveTokenName'") {
                openSwapAmountScreen(
                    fromTokenName = fromTokenName,
                    receiveTokenName = receiveTokenName,
                    amount = inputAmount,
                )
            }
            step("Assert the detailed provider block is displayed by default") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertIsDisplayed() }
                }
            }
            step("Switch the swap mode to '$simpleMode'") {
                switchSwapMode(simpleMode)
            }
            step("Assert the simple provider block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { simpleProvidersBlock.assertIsDisplayed() }
                }
            }
            step("Assert the detailed provider block is not displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertDoesNotExist() }
                }
            }
            step("Switch the swap mode to '$detailedMode'") {
                switchSwapMode(detailedMode)
            }
            step("Assert the detailed provider block is displayed again") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertIsDisplayed() }
                }
            }
            step("Assert the simple provider block is not displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { simpleProvidersBlock.assertDoesNotExist() }
                }
            }
        }
    }
}