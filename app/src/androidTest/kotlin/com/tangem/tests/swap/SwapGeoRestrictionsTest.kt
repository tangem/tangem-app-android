package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.ui.R
import com.tangem.scenarios.SwapEntryPoint
import com.tangem.scenarios.chooseReceiveToken
import com.tangem.scenarios.inputAmount
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSwapScreen
import com.tangem.scenarios.openTokenDetailsFromTokenList
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onChooseProviderBottomSheet
import com.tangem.screens.onSwapTokenScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapGeoRestrictionsTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1643_EXPRESS_CATEGORIES_GEO_BLOCKING_ENABLED.rawName to true,
    )

    @AllureId("11595")
    @DisplayName("Swap: a region-restricted provider is unavailable while the other providers stay selectable")
    @Test
    fun restrictionForOneProviderTest() {
        val tokenTitle = "Ethereum"
        val receiveTokenName = "POL (ex-MATIC)"
        // The wallet holds POL on two networks; only the Ethereum one has several providers, 1inch among them.
        val receiveTokenNetwork = "Ethereum network"
        val inputAmount = "0.1"
        val restrictedProviderName = "1Inch"
        val availableProviderName = "LI.FI"
        val restrictionsTitle = getResourceString(R.string.express_swap_restrictions_title)
        val quoteScenarioName = "exchange_quote_ethereum"
        val oneInchRestrictedState = "OneInchRestricted"

        setupHooks().run {
            step("Set WireMock scenario '$quoteScenarioName' to '$oneInchRestrictedState'") {
                setWireMockScenarioState(scenarioName = quoteScenarioName, state = oneInchRestrictedState)
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
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Choose receive token '$receiveTokenName' on '$receiveTokenNetwork'") {
                chooseReceiveToken(receiveTokenName, receiveTokenNetwork)
            }
            step("Input swap amount '$inputAmount'") {
                inputAmount(inputAmount)
            }
            step("Assert the receive amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveAmount.assertIsDisplayed() }
                }
            }
            step("Assert the available provider '$availableProviderName' is selected") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerNameText.assertTextContains(availableProviderName) }
                }
            }
            step("Assert '$restrictionsTitle' warning is not displayed") {
                onSwapTokenScreen { warningTitle(restrictionsTitle).assertDoesNotExist() }
            }
            step("Assert 'Swap' button is available") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapButton.assertHasClickAction() }
                }
            }
            step("Click on 'Providers' block") {
                onSwapTokenScreen { providersBlock.performClick() }
            }
            step("Assert the restricted provider '$restrictedProviderName' is listed as not available") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        unavailableProviderItem(restrictedProviderName).assertIsDisplayed()
                    }
                }
            }
        }
    }
}