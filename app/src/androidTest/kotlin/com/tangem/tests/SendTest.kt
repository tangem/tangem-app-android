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
        val currencyName = "POL (ex-MATIC)"
        val feeCurrencyName = "Ethereum"
        val feeCurrencySymbol = "ETH"
        val balance = "$763.55"
        val scenarioName = "eth_network_balance"
        val scenarioState = "Empty"

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
                synchronizeAddresses(balance)
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