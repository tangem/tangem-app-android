package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
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
    @DisplayName("Swap: switching from the simple to the detailed mode shows the detailed provider block")
    @Test
    fun checkTurnOnDetailedModeTest() {
        val simpleMode = getResourceString(R.string.swap_simple_mode)
        val detailedMode = getResourceString(R.string.swap_detailed_mode)

        setupHooks().run {
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
            step("Assert the provider block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertIsDisplayed() }
                }
            }
            step("Switch the swap mode to '$simpleMode' as the precondition") {
                switchSwapMode(simpleMode)
            }
            step("Assert the simple provider block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { simpleProvidersBlock.assertIsDisplayed() }
                }
            }
            step("Click on 'More' button") {
                onSwapTokenScreen { moreButton.clickWithAssertion() }
            }
            step("Assert '$simpleMode' mode is displayed in the menu") {
                onSwapTokenScreen { swapModeMenuRow(simpleMode).assertIsDisplayed() }
            }
            step("Assert '$detailedMode' mode is displayed in the menu") {
                onSwapTokenScreen { swapModeMenuRow(detailedMode).assertIsDisplayed() }
            }
            step("Assert '$simpleMode' mode is selected") {
                onSwapTokenScreen { swapModeMenuRow(simpleMode).assertIsSelected() }
            }
            step("Assert '$detailedMode' mode is not selected") {
                onSwapTokenScreen { swapModeMenuRow(detailedMode).assertIsNotSelected() }
            }
            step("Click on '$detailedMode' mode") {
                onSwapTokenScreen { swapModeMenuItem(detailedMode).clickWithAssertion() }
            }
            step("Assert the swap mode menu is closed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { swapModeMenuRow(simpleMode).assertDoesNotExist() }
                }
            }
            step("Assert the receive fiat amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveFiatAmount.assertIsDisplayed() }
                }
            }
            step("Assert the detailed provider block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertIsDisplayed() }
                }
            }
            step("Assert the provider logo is displayed") {
                onSwapTokenScreen { providerIcon.assertIsDisplayed() }
            }
            step("Assert the provider name is displayed") {
                onSwapTokenScreen { providerNameText.assertIsDisplayed() }
            }
            step("Assert the provider type is displayed") {
                onSwapTokenScreen { providerTypeText.assertIsDisplayed() }
            }
            step("Assert the 'Best rate' badge is displayed") {
                onSwapTokenScreen { bestRateText.assertIsDisplayed() }
            }
            step("Assert the provider rate is displayed") {
                onSwapTokenScreen { providerRateText.assertIsDisplayed() }
            }
        }
    }
}