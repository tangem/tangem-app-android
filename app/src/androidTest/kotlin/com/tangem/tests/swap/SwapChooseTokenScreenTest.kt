package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.*
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.SwapEntryPoint
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSwapScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapChooseTokenScreenTest : BaseTestCase() {

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("8505")
    @DisplayName("Swap: check available to swap tokens list")
    @Test
    fun checkAvailableToSwapTokensListTest() {
        val tokenTitle = "Polygon"
        val inputAmount = "100"
        val ethereum = "Ethereum"
        val polExMatic = "POL (ex-MATIC)"
        val bitcoin = "Bitcoin"
        val scenarioState = "CustomTokenAndJesusAdded"
        val jesusCoin = "Jesus Coin"
        val salam = "Salam"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Click on 'Select token' icon") {
                onSwapTokenScreen { selectTokenIcon.performClick() }
            }
            step("Assert '$ethereum' is displayed") {
                onSwapChooseTokenScreen { tokenWithTitle(ethereum).assertIsDisplayed() }
            }
            step("Assert '$polExMatic' is displayed") {
                onSwapChooseTokenScreen { tokenWithTitle(polExMatic).assertIsDisplayed() }
            }
            step("Assert '$bitcoin' is not displayed") {
                onSwapChooseTokenScreen { tokenWithTitle(bitcoin).assertIsNotDisplayed() }
            }
            step("Assert '$jesusCoin' is displayed and unavailable for swap") {
                onSwapChooseTokenScreen { tokenWithTitle(tokenTitle = jesusCoin).assertIsDisplayed() }
            }
            step("Assert custom token without backend id '$salam' is not displayed") {
                onSwapChooseTokenScreen { tokenWithTitle(salam).assertIsNotDisplayed() }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("8506")
    @DisplayName("Swap: check search on choose swap token screen")
    @Test
    fun checkSearchOnSwapChooseTokenScreenTest() {
        val tokenTitle = "Polygon"
        val inputAmount = "100"
        val ethereum = "Ethereum"
        val polExMatic = "POL (ex-MATIC)"
        val polExMaticSymbol = "POL"
        val invalidSearchText = "f"
        val validSearchText = "pol"

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
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
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
            step("Type invalid search text: '$invalidSearchText' in 'Search' text field") {
                onSwapChooseTokenScreen { searchTextField.performTextReplacement(invalidSearchText) }
            }
            step("Assert '$ethereum' is not displayed") {
                onSwapChooseTokenScreen { tokenWithTitle(ethereum).assertIsNotDisplayed() }
            }
            step("Assert '$polExMatic' is not displayed") {
                onSwapChooseTokenScreen { tokenWithTitle(polExMatic).assertIsNotDisplayed() }
            }
            step("Press 'Delete' button") {
                device.uiDevice.pressDelete()
            }
            step("Type valid search text: '$validSearchText' in 'Search' text field") {
                onSwapChooseTokenScreen { searchTextField.performTextReplacement(validSearchText) }
            }
            step("Assert '$ethereum' is not displayed") {
                onSwapChooseTokenScreen { tokenWithTitle(ethereum).assertIsNotDisplayed() }
            }
            step("Assert '$polExMatic' is displayed") {
                onSwapChooseTokenScreen { tokenWithTitle(polExMatic).assertIsDisplayed() }
            }
            step("Select new receive token: $polExMatic") {
                onSwapChooseTokenScreen { tokenWithTitle(polExMatic).performClick() }
            }
            step("Assert new receive token symbol: '$polExMaticSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(polExMaticSymbol).assertIsDisplayed() }
            }
        }
    }
}