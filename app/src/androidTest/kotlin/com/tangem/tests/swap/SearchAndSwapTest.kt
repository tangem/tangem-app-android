package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.performTextInputInChunks
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.SwapEntryPoint
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSwapScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class SearchAndSwapTest : BaseTestCase() {

    @Ignore("ToDo: [REDACTED_JIRA]")
    @AllureId("8520")
    @DisplayName("Search and Swap: add token without derivation")
    @Test
    fun addTokenWithoutDerivationTest() {
        val swapTokenName = "Ethereum"
        val receiveTokenName = "Tether"
        val swapTokenSymbol = "ETH"
        val receiveTokenSymbol = "USDT"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Click on token with name '$swapTokenName'") {
                onSwapSelectTokenScreen { tokenWithName(swapTokenName).performClick() }
                waitForIdle()
            }
            step("Click on 'Search' text field") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.performClick() }
            }
            step("Type '$receiveTokenName' in input text field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInputInChunks(receiveTokenName) }
            }
            step("Click on token with name '$receiveTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { marketsTokenWithName(receiveTokenName).clickWithAssertion() }
                }
            }
            step("Click on 'Add' button") {
                onAddTokenBottomSheet { addButton.performClick() }
            }
            step("Assert swap token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
        }
    }

    @Ignore("ToDo: [REDACTED_JIRA]")
    @AllureId("8519")
    @DisplayName("Search and Swap: add token with derivation")
    @Test
    fun addTokenWithDerivationTest() {
        val swapTokenName = "Ethereum"
        val receiveTokenName = "TRON"
        val swapTokenSymbol = "ETH"
        val receiveTokenSymbol = "TRX"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Click on token with name '$swapTokenName'") {
                onSwapSelectTokenScreen { tokenWithName(swapTokenName).performClick() }
                waitForIdle()
            }
            step("Click on 'Search' text field") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.performClick() }
            }
            step("Type '$receiveTokenSymbol' in input text field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInputInChunks(receiveTokenSymbol) }
            }
            step("Click on token with name '$receiveTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { marketsTokenWithName(receiveTokenName).clickWithAssertion() }
                }
            }
            step("Click on 'Add' button") {
                onAddTokenBottomSheet { addButton.performClick() }
            }
            step("Assert swap token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
        }
    }

    @Ignore("ToDo: [REDACTED_JIRA]")
    @AllureId("8523")
    @DisplayName("Search and Swap: Markets error")
    @Test
    fun marketsErrorTest() {
        val swapTokenName = "Ethereum"
        val scenarioName = "coins_list_api"
        val scenarioState = "Error"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName, scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Click on token with name '$swapTokenName'") {
                onSwapSelectTokenScreen { tokenWithName(swapTokenName).performClick() }
                waitForIdle()
            }
            step("Assert 'Unable to load data...' error is displayed") {
                onSwapSelectTokenScreen { unableToLoadData.assertIsDisplayed() }
            }
            step("Assert 'Try again' button is displayed") {
                onSwapSelectTokenScreen { tryAgainButton.assertIsDisplayed() }
            }
        }
    }

    @Ignore("ToDo: [REDACTED_JIRA]")
    @AllureId("8522")
    @DisplayName("Search and Swap: check 'Unsupported token pair' warning")
    @Test
    fun unsupportedTokenPairTest() {
        val swapTokenName = "Ethereum"
        val receiveTokenName = "Pepe"
        val warningTitle = getResourceString(R.string.warning_express_unsupported_pair_title)
        val warningMessage = getResourceString(R.string.warning_express_unsupported_pair_description)

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Click on token with name '$swapTokenName'") {
                onSwapSelectTokenScreen { tokenWithName(swapTokenName).performClick() }
                waitForIdle()
            }
            step("Click on 'Search' text field") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.performClick() }
            }
            step("Type '$receiveTokenName' in input text field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInputInChunks(receiveTokenName) }
            }
            step("Click on token with name '$receiveTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { marketsTokenWithName(receiveTokenName).clickWithAssertion() }
                }
            }
            step("Click on 'Add' button") {
                onAddTokenBottomSheet { addButton.performClick() }
            }
            step("Assert warning title '$warningTitle' is displayed") {
                onSwapTokenScreen { warningTitle(warningTitle).assertIsDisplayed() }
            }
            step("Assert warning message '$warningMessage' is displayed") {
                onSwapTokenScreen { warningMessage(warningMessage).assertIsDisplayed() }
            }
            step("Assert warning icon is displayed'") {
                onSwapTokenScreen { warningIcon(warningMessage).assertIsDisplayed() }
            }
        }
    }

    @Ignore("ToDo: [REDACTED_JIRA]")
    @AllureId("8521")
    @DisplayName("Swap: search token on Swap token screen")
    @Test
    fun networkFeeTest() {
        val tokenTitle = "Ethereum"
        val swapTokenSymbol = "TRX"
        val receiveTokenSymbol = "ETH"
        val swapTokenName = "TRON"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Click on 'Replace tokens' button") {
                onSwapTokenScreen { replaceTokensButton.performClick() }
            }
            step("Click on 'Select token' icon") {
                onSwapTokenScreen { selectTokenIcon.performClick() }
            }
            step("Click on 'Search' icon") {
                onSwapChooseTokenScreen { searchIcon.performClick() }
            }
            step("Click on 'Search' text field") {
                onSwapChooseTokenScreen { searchTextField.performClick() }
            }
            step("Type '$swapTokenSymbol' in 'Search' text field") {
                onSwapChooseTokenScreen { searchTextField.performTextInputInChunks(swapTokenSymbol) }
            }
            step("Click on token with name: '$swapTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapChooseTokenScreen { marketsTokenWithTitle(swapTokenName).performClick() }
                }
            }
            step("Click on 'Add' button") {
                onAddTokenBottomSheet { addButton.performClick() }
            }
            step("Assert swap token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
        }
    }
}