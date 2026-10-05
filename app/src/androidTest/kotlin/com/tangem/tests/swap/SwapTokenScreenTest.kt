package com.tangem.tests.swap

import com.tangem.grow.datasource.config.Express

import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.*
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.res.R
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class SwapTokenScreenTest : BaseTestCase() {

    // Express stays on MOCK (the rule's default): pointing it at PROD made the test depend on which
    // providers the live exchange happened to offer for the pair, which is exactly what made it flaky.
    // The mocked ETH -> POL (Ethereum) pair carries 1inch alongside the CEX providers, so the ALL/CEX/DEX
    // filter the case asks for is always there.
    @AllureId("9456")
    @DisplayName("Swap: provider modal shows ALL/CEX/DEX filter and the picked provider appears in the tab")
    @Test
    fun checkProviderTypeFilterTest() {
        val fromTokenSymbol = "ETH"
        val receiveTokenName = "POL (ex-MATIC)"
        // The wallet holds POL on two networks and only the Ethereum one has a DEX provider (1inch) next to
        // Picking by name alone lands on the Polygon one about half the time, whose pair is CEX-only, and
        // then no type filter can ever appear — no amount of reopening the sheet helps.
        val receiveTokenNetwork = "Ethereum network"
        val inputAmount = "0.1"
        val allFilter = getResourceString(R.string.common_all)
        val cexFilter = "CEX"
        val dexFilter = "DEX"
        val dexProviderName = "1Inch"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' from the main screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Assert the most funded token '$fromTokenSymbol' is pre-filled in the 'From' block") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Choose receive token '$receiveTokenName'") {
                chooseReceiveToken(receiveTokenName, receiveTokenNetwork)
            }
            step("Input swap amount '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Wait for the providers block to load") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertIsDisplayed() }
                }
            }
            step("Open the providers bottom sheet with the type filter displayed") {
                openProviderSheetWithTypeFilter(allFilter)
            }
            step("Assert the 'CEX' filter is displayed") {
                onChooseProviderBottomSheet { filterButton(cexFilter).assertIsDisplayed() }
            }
            step("Assert the 'DEX' filter is displayed") {
                onChooseProviderBottomSheet { filterButton(dexFilter).assertIsDisplayed() }
            }
            step("Select the 'DEX' filter") {
                onChooseProviderBottomSheet { filterButton(dexFilter).performClick() }
            }
            step("Assert the DEX provider '$dexProviderName' is displayed under the filter") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerItem(dexProviderName).assertIsDisplayed() }
                }
            }
            step("Select the '$dexProviderName' provider") {
                onChooseProviderBottomSheet { providerItem(dexProviderName).clickWithAssertion() }
            }
            step("Assert the selected provider '$dexProviderName' is shown in the providers tab") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerWithName(dexProviderName).assertIsDisplayed() }
                }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(Express.KEY, ApiEnvironment.PROD)
    )
    @AllureId("9458")
    @DisplayName("Swap: predefined percent buttons (25/50/75/Max) split the balance of the 'From' token")
    @Test
    fun checkPredefinedPercentButtonsTest() {
        val mostFundedTokenSymbol = "ETH"
        val receiveTokenName = "Polygon"
        val percent25 = "PERCENT_25"
        val percent50 = "PERCENT_50"
        val percent75 = "PERCENT_75"
        val max = "MAX"
        // The most funded token on the standard mock is Ethereum with a 1.0 ETH balance,
        // so the predefined-percent amounts are exact fractions of it.
        val amount25 = "0.25"
        val amount50 = "0.5"
        val amount75 = "0.75"
        val amountMax = "1"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' from the main screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Assert the most funded token '$mostFundedTokenSymbol' is pre-filled in the 'From' block") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(mostFundedTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Choose receive token '$receiveTokenName'") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Focus the 'From' amount input") {
                onSwapTokenScreen { textInput.clickWithAssertion() }
            }
            step("Assert the '25%' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { percentButton(percent25).assertIsDisplayed() }
                }
            }
            step("Assert the '50%' button is displayed") {
                onSwapTokenScreen { percentButton(percent50).assertIsDisplayed() }
            }
            step("Assert the '75%' button is displayed") {
                onSwapTokenScreen { percentButton(percent75).assertIsDisplayed() }
            }
            step("Assert the 'Max' button is displayed") {
                onSwapTokenScreen { percentButton(max).assertIsDisplayed() }
            }
            step("Click on '25%' button") {
                onSwapTokenScreen { percentButton(percent25).clickWithAssertion() }
            }
            step("Assert the 'From' amount is '$amount25'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(amount25) }
                }
            }
            step("Click on '50%' button") {
                onSwapTokenScreen { percentButton(percent50).clickWithAssertion() }
            }
            step("Assert the 'From' amount is '$amount50'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(amount50) }
                }
            }
            step("Click on '75%' button") {
                onSwapTokenScreen { percentButton(percent75).clickWithAssertion() }
            }
            step("Assert the 'From' amount is '$amount75'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(amount75) }
                }
            }
            step("Click on 'Max' button") {
                onSwapTokenScreen { percentButton(max).clickWithAssertion() }
            }
            step("Assert the 'From' amount is the full balance '$amountMax'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(amountMax) }
                }
            }
        }
    }

    @AllureId("3546")
    @DisplayName("Swap: network fee is calculated on amount input")
    @Test
    fun networkFeeTest() {
        val inputAmount = "400"
        val changedInputAmount = "200"
        val tokenTitle = "Polygon"
        val receiveTokenName = "Ethereum"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton.performClick() }
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
                        replaceTokensButton.assertIsDisplayed()
                    }
                }
            }
            step("Choose receive token") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Assert 'Swap tokens on screen' button is enabled") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { replaceTokensButton.assertIsEnabled() }
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
            step("Assert receive amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        receiveAmount.assertIsDisplayed()
                    }
                }
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
            step("Assert network fee amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeAmount.assertIsDisplayed() }
                }
            }
            step("Open 'Network fee' selector") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { selectFeeIcon.performClick() }
                    onSwapSelectNetworkFeeBottomSheet { marketFeeItem.assertIsDisplayed() }
                }
            }
            step("Assert 'Market' fee is selected by default") {
                onSwapSelectNetworkFeeBottomSheet { marketFeeItem.assertIsSelected() }
            }
            step("Assert 'Fast' fee is not selected") {
                onSwapSelectNetworkFeeBottomSheet { fastFeeItem.assertIsNotSelected() }
            }
            step("Close 'Network fee' selector") {
                device.uiDevice.pressBack()
                onSwapSelectNetworkFeeBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { title.assertDoesNotExist() }
                }
            }
            var initialReceiveAmount = ""
            step("Read receive amount for '$inputAmount'") {
                onSwapTokenScreen { initialReceiveAmount = receiveAmount.extractText() }
            }
            step("Change swap amount to '$changedInputAmount'") {
                onSwapTokenScreen { textInput.performTextReplacement(changedInputAmount) }
            }
            step("Assert network fee loader is displayed while the fee is recalculated") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { feeLoader.assertIsDisplayed() }
                }
            }
            step("Assert recalculated network fee amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeAmount.assertIsDisplayed() }
                }
            }
            step("Assert receive amount is recalculated for '$changedInputAmount'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        receiveAmount.assertIsDisplayed()
                        receiveAmount.assert(!hasText(initialReceiveAmount))
                    }
                }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(Express.KEY, ApiEnvironment.PROD)
    )
    @AllureId("3549")
    @DisplayName("Swap: network error test")
    @Test
    fun networkErrorSwapTest() {
        val tokenTitle = "Polygon"
        val receiveTokenName = "Ethereum"

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
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Turn off Wi-Fi and Mobile Data") {
                disableWiFi()
                disableMobileData()
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Choose receive token") {
                chooseReceiveToken(receiveTokenName)
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

    @AllureId("3547")
    @DisplayName("Swap: changing the network fee type recalculates the fee")
    @Test
    fun changeNetworkFeeTest() {
        val inputAmount = "400"
        val tokenTitle = "Polygon"
        val receiveTokenName = "Ethereum"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Choose receive token") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Assert 'Swap tokens on screen' button is enabled") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { replaceTokensButton.assertIsEnabled() }
                }
            }
            step("Input swap amount = '$inputAmount'") {
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert network fee amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeAmount.assertIsDisplayed() }
                }
            }
            step("Open 'Network fee' selector") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { selectFeeIcon.performClick() }
                    onSwapSelectNetworkFeeBottomSheet { marketFeeItem.assertIsDisplayed() }
                }
            }
            step("Assert 'Market' fee is selected") {
                onSwapSelectNetworkFeeBottomSheet { marketFeeItem.assertIsSelected() }
            }
            var marketFee = ""
            step("Read 'Market' fee amount") {
                onSwapSelectNetworkFeeBottomSheet { marketFee = marketFeeTokenAmount.extractText() }
            }
            step("Assert 'Fast' fee amount differs from 'Market' fee amount") {
                onSwapSelectNetworkFeeBottomSheet { fastFeeTokenAmount.assert(!hasText(marketFee)) }
            }
            step("Click on 'Fast' fee") {
                onSwapSelectNetworkFeeBottomSheet { fastFeeItem.clickWithAssertion() }
            }
            step("Assert 'Network fee' selector is closed") {
                onSwapSelectNetworkFeeBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { title.assertDoesNotExist() }
                }
            }
            step("Assert network fee amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeAmount.assertIsDisplayed() }
                }
            }
            step("Open 'Network fee' selector") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { selectFeeIcon.performClick() }
                    onSwapSelectNetworkFeeBottomSheet { fastFeeItem.assertIsDisplayed() }
                }
            }
            step("Assert 'Fast' fee is selected") {
                onSwapSelectNetworkFeeBottomSheet { fastFeeItem.assertIsSelected() }
            }
            step("Assert 'Market' fee is not selected") {
                onSwapSelectNetworkFeeBottomSheet { marketFeeItem.assertIsNotSelected() }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(Express.KEY, ApiEnvironment.PROD)
    )
    @AllureId("2828")
    @DisplayName("Swap: go to token swap")
    @Test
    fun goToTokenSwapTest() {
        val swapTokenSymbol = "POL"
        val tokenTitle = "Polygon"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton.performClick() }
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
                        replaceTokensButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Assert 'Choose token' button is displayed") {
                onSwapTokenScreen { chooseTokenButton.assertIsDisplayed() }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(Express.KEY, ApiEnvironment.PROD)
    )
    @AllureId("575")
    @DisplayName("Swap: check UI")
    @Test
    fun checkSwapUiTest() {
        val swapTokenSymbol = "POL"
        val receiveTokenSymbol = "ETH"
        val receiveTokenName = "Ethereum"
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
                openTokenDetailsFromTokenList(tokenTitle)
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
            step("Choose receive token") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
            step("Click on receive 'Select token' icon") {
                onSwapTokenScreen { receiveSelectTokenIcon.performClick() }
            }
            step("Select new receive token: $newReceiveToken") {
                onSwapSelectTokenScreen { tokenWithName(newReceiveToken).performClick() }
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
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        receiveFiatAmount.assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Swap tokens on screen' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        replaceTokensButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Swap' button is disabled") {
                onSwapTokenScreen { swapButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("5162")
    @DisplayName("Swap: switching the swap direction recalculates the quote")
    @Test
    fun checkSwapTokensSwitchTest() {
        val swapTokenSymbol = "POL"
        val receiveTokenSymbol = "ETH"
        val receiveTokenName = "Ethereum"
        val tokenTitle = "Polygon"
        val inputAmount = "100"
        val switchedInputAmount = "0.1"
        val pairsToScenarioName = "polygon_pos_to_pairs"
        val pairsToScenarioState = "DexProvider"
        val reverseQuoteScenarioName = "exchange_quote_ethereum_polygon"
        val reverseQuoteScenarioState = "Native"

        setupHooks().run {

            step("Set WireMock scenario: '$pairsToScenarioName' to state: '$pairsToScenarioState'") {
                setWireMockScenarioState(scenarioName = pairsToScenarioName, state = pairsToScenarioState)
            }
            step("Set WireMock scenario: '$reverseQuoteScenarioName' to state: '$reverseQuoteScenarioState'") {
                setWireMockScenarioState(scenarioName = reverseQuoteScenarioName, state = reverseQuoteScenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
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
            step("Choose receive token") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
            }
            step("Assert 'Swap tokens on screen' button is enabled") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { replaceTokensButton.assertIsEnabled() }
                }
            }
            step("Input swap amount = '$inputAmount'") {
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert receive amount is not equal to '0'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        receiveAmount.assertIsDisplayed()
                        receiveAmount.assert(!hasText("0"))
                    }
                }
            }
            step("Assert provider name is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerNameText.assertIsDisplayed() }
                }
            }
            step("Assert provider rate is displayed") {
                onSwapTokenScreen { providerRateText.assertIsDisplayed() }
            }
            step("Assert network fee is calculated in '$swapTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeBlockCurrency(swapTokenSymbol).assertIsDisplayed() }
                }
            }
            var initialReceiveAmount = ""
            var initialRate = ""
            step("Read receive amount and provider rate") {
                onSwapTokenScreen {
                    initialReceiveAmount = receiveAmount.extractText()
                    initialRate = providerRateText.extractText()
                }
            }
            step("Click on 'Swap tokens on screen' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen {
                        replaceTokensButton.assertIsEnabled()
                        replaceTokensButton.performClick()
                    }
                }
            }
            step("Assert new swap token symbol: '$receiveTokenSymbol' is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { swapTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert new receive token symbol: '$swapTokenSymbol' is displayed") {
                onSwapTokenScreen { receiveTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
            }
            step("Assert 'Swap tokens on screen' button is enabled") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { replaceTokensButton.assertIsEnabled() }
                }
            }
            step("Input swap amount = '$switchedInputAmount'") {
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(switchedInputAmount)
                }
            }
            step("Assert receive amount is recalculated for the new direction") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        receiveAmount.assertIsDisplayed()
                        receiveAmount.assert(!hasText("0"))
                        receiveAmount.assert(!hasText(initialReceiveAmount))
                    }
                }
            }
            step("Assert provider name is displayed for the new direction") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerNameText.assertIsDisplayed() }
                }
            }
            step("Assert provider rate is recalculated for the new direction") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerRateText.assert(!hasText(initialRate)) }
                }
            }
            step("Assert network fee is recalculated in '$receiveTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeBlockCurrency(receiveTokenSymbol).assertIsDisplayed() }
                }
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

        setupHooks().run {

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
                openTokenDetailsFromTokenList(polygon)
            }
            step("Assert 'Swap' button is not dimmed. Swap available") {
                onTokenDetailsScreen { swapButton.assertIsEnabled() }
            }
            step("Press 'Back' button") {
                device.uiDevice.pressBack()
                flakySafely { onMainScreen { screenContainer.assertIsDisplayed() } }
            }
            step("Click on token with name: '$bitcoin'. Swap unavailable") {
                onMainScreen { clickDisplayedToken(bitcoin) }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton.assertIsNotEnabled() }
            }
            step("Press 'Back' button") {
                device.uiDevice.pressBack()
                flakySafely { onMainScreen { screenContainer.assertIsDisplayed() } }
            }
            step("Click on unknown custom token with name: '$salam'. Swap unavailable") {
                onMainScreen { clickDisplayedToken(salam) }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton.assertIsNotEnabled() }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(Express.KEY, ApiEnvironment.PROD)
    )
    @AllureId("583")
    @DisplayName("Swap: check switch fee type (enable to cover 'Market' and 'Fast' fee)")
    @Test
    fun enableToCoverMarketAndFastFeeTest() {
        val tokenName = "Ethereum"
        val receiveTokenName = "Polygon"
        val inputAmount = "0.99"
        val market = "Market"
        val fast = "Fast"

        setupHooks().run {

            var marketFee = 0.0

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                openTokenDetailsFromTokenList(tokenName)
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Choose receive token") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Select '$market' fee type and capture its amount") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    marketFee = parseFeeAmount(selectFeeTypeAndReadFee(FeeType.Market))
                }
            }
            step("Select '$fast' fee type and assert it exceeds the '$market' fee") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    val fastFee = parseFeeAmount(selectFeeTypeAndReadFee(FeeType.Fast))
                    assertTrue(
                        "Expected '$fast' fee ($fastFee) to be greater than '$market' fee ($marketFee)",
                        fastFee > marketFee,
                    )
                }
            }
        }
    }

    private fun parseFeeAmount(raw: String): Double = raw
        .replace(Regex("[^0-9.]"), "")
        .toDoubleOrNull()
        ?: error("Could not parse a numeric fee amount from '$raw'")

    @AllureId("8536")
    @DisplayName("Swap: check switch fee type (unable to cover 'Market' and 'Fast' fee)")
    @Test
    fun unableToCoverMarketAndFastFeeTest() {
        val tokenName = "POL (ex-MATIC)"
        val receiveTokenName = "Ethereum"
        val inputAmount = "0.0001"
        val marketFeeType = "Market"
        val fastFeeType = "Fast"
        val feeAmount = "$"
        val scenarioName = "eth_network_balance"
        val scenarioState = "LessThanDollar"
        val pairsScenarioName = "ethereum_from_pairs"
        val pairsScenarioState = "DexProvider"
        val networkName = "Ethereum"
        val currencySymbol = "ETH"

        setupHooks().run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }
            step("Set WireMock scenario: '$pairsScenarioName' to state: $pairsScenarioState") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = pairsScenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Swipe up") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Click on token with name: '$tokenName'") {
                openTokenDetailsFromTokenList(tokenName)
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Choose receive token") {
                chooseReceiveToken(receiveTokenName)
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
        val receiveTokenName = "Ethereum"
        val inputAmount = "3000"
        val fastFeeType = "Fast"
        val fastFeeAmount = "$2,"
        val marketFeeType = "Market"
        val marketFeeAmount = "$1."
        val scenarioName = "eth_fee_history"
        val scenarioState = "UnableToCoverFastFee"
        val pairsScenarioName = "ethereum_from_pairs"
        val pairsScenarioState = "DexProvider"
        val networkName = "Ethereum"
        val currencySymbol = "ETH"

        setupHooks().run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }
            step("Set WireMock scenario: '$pairsScenarioName' to state: $pairsScenarioState") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = pairsScenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Swipe up") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Click on token with name: '$tokenName'") {
                openTokenDetailsFromTokenList(tokenName)
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Choose receive token") {
                chooseReceiveToken(receiveTokenName)
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
                    selectFeeType(feeType = FeeType.Fast, selectedFeeAmount = fastFeeAmount)
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
                    selectFeeType(feeType = FeeType.Market, selectedFeeAmount = marketFeeAmount)
                }
            }
            step("Assert 'Swap' button is enabled") {
                waitForIdle()
                onSwapTokenScreen { swapButton.assertIsEnabled() }
            }
        }
    }
}