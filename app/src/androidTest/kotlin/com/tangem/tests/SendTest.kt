package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTokenDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SendTest : BaseTestCase() {

    @AllureId("3645")
    @DisplayName("Send: check fee notification")
    @Test
    fun checkFeeNotificationTest() {
        val currencyName = "USDC"
        val feeCurrencyName = "Solana"
        val feeCurrencySymbol = "SOL"
        val balanceScenarioName = "solana_balance"
        val tokensScenarioName = "user_tokens_api"
        val balanceState = "Empty"
        val tokensState = "SolanaUSDC"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(balanceScenarioName)
                resetWireMockScenarioState(tokensScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$tokensScenarioName' to state: '$tokensState'") {
                setWireMockScenarioState(scenarioName = tokensScenarioName, state = tokensState)
            }
            step("Set WireMock scenario: '$balanceScenarioName' to state: '$balanceState'") {
                setWireMockScenarioState(scenarioName = balanceScenarioName, state = balanceState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: $currencyName") {
                onMainScreen { tokenWithTitleAndAddress(currencyName).clickWithAssertion() }
            }
            step("Assert 'Insufficient $feeCurrencyName to cover network fee' notification icon is displayed") {
                onTokenDetailsScreen { networkFeeNotificationIcon(feeCurrencyName).assertIsDisplayed() }
            }
            step("Assert 'Insufficient $feeCurrencyName to cover network fee' notification title is displayed") {
                onTokenDetailsScreen { networkFeeNotificationTitle(feeCurrencyName).assertIsDisplayed() }
            }
            step("Assert 'Insufficient $feeCurrencyName to cover network fee' notification text is displayed") {
                onTokenDetailsScreen {
                    networkFeeNotificationMessage(
                        currencyName,
                        feeCurrencyName,
                        feeCurrencyName,
                        feeCurrencySymbol
                    ).assertIsDisplayed()
                }
            }
            step("Assert 'Go to $feeCurrencySymbol' button is displayed") {
                onTokenDetailsScreen { goToBuyCurrencyButton(feeCurrencySymbol).assertIsDisplayed() }
            }
        }
    }
}