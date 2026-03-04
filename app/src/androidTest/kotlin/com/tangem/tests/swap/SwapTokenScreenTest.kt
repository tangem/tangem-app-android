package com.tangem.tests.swap

import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.*
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapTokenScreenTest : BaseTestCase() {

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("3546")
    @DisplayName("Swap: network fee")
    @Test
    fun networkFeeTest() {
        val inputAmount = "400"
        val tokenTitle = "Polygon"

        setupHooks().run {

            resetWireMockScenarios()
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
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert 'Close' button is displayed") {
                onSwapTokenScreen { closeButton.assertIsDisplayed() }
            }
            step("Assert 'Swap tokens on screen' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        swapTokensOnscreenButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert receive amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        receiveAmount.assertIsDisplayed()
                    }
                }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert input amount = '$inputAmount'") {
                onSwapTokenScreen { textInput.assertTextEquals(inputAmount) }
            }
            step("Assert 'Providers' block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        providersBlock.assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Network fee' block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        networkFeeBlock.assertIsDisplayed()
                    }
                }
            }
            step("Assert receive amount is not equal to '0'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        waitForIdle()
                        receiveAmount.assert(!hasText("0"))
                    }
                }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("3549")
    @DisplayName("Swap: network error test")
    @Test
    fun networkErrorSwapTest() {
        val tokenTitle = "Polygon"

        setupHooks(
            additionalAfterSection = {
                enableWiFi()
                enableMobileData()
            }
        ).run {

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
            step("Turn off Wi-Fi and Mobile Data") {
                disableWiFi()
                disableMobileData()
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert error notification title is displayed") {
                onSwapTokenScreen {
                    waitForIdle()
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        errorNotificationTitle.assertIsDisplayed()
                    }
                }
            }
            step("Assert error notification text is displayed") {
                onSwapTokenScreen { errorNotificationText.assertIsDisplayed() }
            }
            step("Assert 'Refresh' button is displayed") {
                onSwapTokenScreen { refreshButton.assertIsDisplayed() }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("3547")
    @DisplayName("Swap: change network fee")
    @Test
    fun changeNetworkFeeTest() {
        val inputAmount = "400"

        setupHooks().run {
            val tokenTitle = "Polygon"

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
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert 'Swap tokens on screen' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        swapTokensOnscreenButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert receive amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        receiveAmount.assertIsDisplayed()
                    }
                }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert input amount = '$inputAmount'") {
                onSwapTokenScreen { textInput.assertTextEquals(inputAmount) }
            }
            step("Assert 'Network fee' block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        networkFeeBlock.assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Swap' button is enabled") {
                onSwapTokenScreen { swapButton.assertIsEnabled() }
            }
            step("Click on 'Network fee' block") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        selectFeeIcon.performClick()
                    }
                }
            }
            step("Assert 'Select fee' bottom sheet title is displayed") {
                onSwapSelectNetworkFeeBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert 'Market' item is displayed") {
                onSwapSelectNetworkFeeBottomSheet { marketSelectorItem.assertIsDisplayed() }
            }
            step("Assert 'Fast' item is displayed") {
                onSwapSelectNetworkFeeBottomSheet { fastSelectorItem.assertIsDisplayed() }
            }
            step("Assert 'Read more' text block is displayed") {
                onSwapSelectNetworkFeeBottomSheet { readMoreTextBlock.assertIsDisplayed() }
            }
            step("Click on 'Fast' item") {
                onSwapSelectNetworkFeeBottomSheet { fastSelectorItem.assertIsDisplayed() }
            }
            step("Assert 'Network fee' block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        networkFeeBlock.assertIsDisplayed()
                    }
                }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("2828")
    @DisplayName("Swap: network fee")
    @Test
    fun goToTokenSwapTest() {
        val swapTokenSymbol = "POL"
        val receiveTokenSymbol = "ETH"
        val tokenTitle = "Polygon"

        setupHooks().run {

            resetWireMockScenarios()
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
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert 'Close' button is displayed") {
                onSwapTokenScreen { closeButton.assertIsDisplayed() }
            }
            step("Assert 'Swap tokens on screen' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        swapTokensOnscreenButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Assert token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("575")
    @DisplayName("Swap: check UI")
    @Test
    fun checkSwapUiTest() {
        val swapTokenSymbol = "POL"
        val receiveTokenSymbol = "ETH"
        val newReceiveToken = "POL (ex-MATIC)"
        val tokenTitle = "Polygon"
        val inputAmount = "1"

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
            step("Assert 'Close' button is displayed") {
                onSwapTokenScreen { closeButton.assertIsDisplayed() }
            }
            step("Assert swap token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
            step("Click on 'Select token' icon") {
                onSwapTokenScreen { selectTokenIcon.performClick() }
            }
            step("Select new receive token: $newReceiveToken") {
                onSwapChooseTokenScreen { tokenWithTitle(newReceiveToken).performClick() }
            }
            step("Assert new receive token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert input amount = '$inputAmount'") {
                onSwapTokenScreen { textInput.assertTextEquals(inputAmount) }
            }
            step("Press 'Delete' button on keyboard") {
                device.uiDevice.pressDelete()
                waitForIdle()
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Assert 'You receive' block is displayed") {
                onSwapTokenScreen { youReceiveBlock }
            }
            step("Assert send token fiat amount is displayed") {
                onSwapTokenScreen { swapFiatAmount.assertIsDisplayed() }
            }
            step("Assert receive token fiat amount is displayed") {
                onSwapTokenScreen { receiveFiatAmount.assertIsDisplayed() }
            }
            step("Assert 'Swap tokens on screen' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        swapTokensOnscreenButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Swap' button is disabled") {
                onSwapTokenScreen { swapButton.assertIsNotEnabled() }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("5162")
    @DisplayName("Swap: check swap tokens switch")
    @Test
    fun checkSwapTokensSwitchTest() {
        val swapTokenSymbol = "POL"
        val receiveTokenSymbol = "ETH"
        val tokenTitle = "Polygon"

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
            step("Assert swap token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
            step("Click on 'Swap tokens on screen' button") {
                onSwapTokenScreen { swapTokensOnscreenButton.performClick() }
                waitForIdle()
            }
            step("Assert new swap token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
            step("Assert new receive token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
        }
    }

    @AllureId("573")
    @DisplayName("Swap: check 'Swap' button availability")
    @Test
    fun checkSwapButtonAvailabilityTest() {
        val polygon = "Polygon"
        val bitcoin = "Bitcoin"
        val salam = "Salam"
        val scenarioState = "CustomTokenAndJesusAdded"

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
            step("Click on token with name: '$polygon'") {
                onMainScreen { tokenWithTitleAndAddress(polygon).clickWithAssertion() }
            }
            step("Assert 'Swap' button is not dimmed. Swap available") {
                onTokenDetailsScreen { swapButton().assertIsDimmed(false) }
            }
            step("Press 'Back' button") {
                device.uiDevice.pressBack()
            }
            step("Click on token with name: '$bitcoin'. Swap unavailable") {
                onMainScreen { tokenWithTitleAndAddress(bitcoin).clickWithAssertion() }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton().assertIsDimmed(true) }
            }
            step("Press 'Back' button") {
                device.uiDevice.pressBack()
            }
            step("Click on unknown custom token with name: '$salam'. Swap unavailable") {
                onMainScreen { tokenWithTitleAndAddress(salam).clickWithAssertion() }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton().assertIsDimmed(true) }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("583")
    @DisplayName("Swap: check switch fee type (enable to cover 'Market' and 'Fast' fee)")
    @Test
    fun enableToCoverMarketAndFastFeeTest() {
        val tokenName = "Ethereum"
        val inputAmount = "0.99"
        val market = "Market"
        val fast = "Fast"
        val marketFeeAmount = "$1.12"
        val fastFeeAmount = "$1.43"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
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
            step("Select '$market' fee type") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    selectFeeType(FeeType.Market, selectedFeeAmount = marketFeeAmount)
                }
            }
            step("Select '$fast' fee type") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    selectFeeType(FeeType.Fast, selectedFeeAmount = fastFeeAmount)
                }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("8536")
    @DisplayName("Swap: check switch fee type (unable to cover 'Market' and 'Fast' fee)")
    @Test
    fun unableToCoverMarketAndFastFeeTest() {
        val tokenName = "POL (ex-MATIC)"
        val inputAmount = "0.0001"
        val marketFeeType = "Market"
        val fastFeeType = "Fast"
        val feeAmount = "$"
        val scenarioName = "eth_network_balance"
        val scenarioState = "LessThanDollar"
        val networkName = "Ethereum"
        val currencySymbol = "ETH"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
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
            step("Select '$marketFeeType' fee type") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    selectFeeType(feeType = FeeType.Market, feeAmount)
                }
            }
            step("Check 'Unable to cover '$networkName' fee notification") {
                chackUnableToCoverFeeNotification(networkName = networkName, currencySymbol = currencySymbol)
            }
            step("Assert 'Swap' button is disabled") {
                onSwapTokenScreen { swapButton.assertIsNotEnabled() }
            }
            step("Select '$fastFeeType' fee type") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    selectFeeType(feeType = FeeType.Fast, feeAmount)
                }
            }
            step("Check 'Unable to cover '$networkName' fee notification") {
                chackUnableToCoverFeeNotification(networkName = networkName, currencySymbol = currencySymbol)
            }
            step("Assert 'Swap' button is disabled") {
                onSwapTokenScreen { swapButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("8537")
    @DisplayName("Swap: check switch fee type (unable to cover 'Fast' fee)")
    @Test
    fun unableToCoverFastFeeTest() {
        val tokenName = "POL (ex-MATIC)"
        val inputAmount = "3000"
        val fastFeeType = "Fast"
        val fastFeeAmount = "$2,"
        val marketFeeType = "Market"
        val marketFeeAmount = "$1."
        val scenarioName = "eth_fee_history"
        val scenarioState = "UnableToCoverFastFee"
        val networkName = "Ethereum"
        val currencySymbol = "ETH"


        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
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
            step("Assert 'Swap' button is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { swapButton.assertIsEnabled() }
                }
            }
            step("Select '$fastFeeType' fee type") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    selectFeeTypeWithGasless(feeType = FeeType.Fast, fastFeeAmount)
                }
            }
            step("Assert fee amount is equal to '$fastFeeType' fee:'$fastFeeAmount'") {
                onSwapTokenScreen { feeAmount.assertTextContains(fastFeeAmount, substring = true) }
            }
            step("Check 'Unable to cover '$networkName' fee notification") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    chackUnableToCoverFeeNotification(networkName = networkName, currencySymbol = currencySymbol)
                }
            }
            step("Assert 'Swap' button is disabled") {
                onSwapTokenScreen { swapButton.assertIsNotEnabled() }
            }
            step("Select '$marketFeeType' fee type") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    selectFeeTypeWithGasless(feeType = FeeType.Market, marketFeeAmount)
                }
            }
            step("Assert 'Swap' button is enabled") {
                waitForIdle()
                onSwapTokenScreen { swapButton.assertIsEnabled() }
            }
        }
    }
}