package com.tangem.tests.send.addressScreen

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.DOGECOIN_ADDRESS
import com.tangem.common.constants.TestConstants.ETHEREUM_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSendAddressScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onSendAddressScreen
import com.tangem.screens.onSendConfirmScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MyWalletsBlockTest : BaseTestCase() {
    private val txHistoryScenarioName = "dogecoin_tx_history"

    @AllureId("4579")
    @DisplayName("Send (address screen): click on 'My wallet address'")
    @Test
    fun clickOnMyWalletAddressTest() {
        val tokenName = "Dogecoin"
        val sendAmount = "1"
        val txHistoryScenarioState = "OutgoingTransaction"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(txHistoryScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$txHistoryScenarioName' to state: '$txHistoryScenarioState'") {
                setWireMockScenarioState(scenarioName = txHistoryScenarioName, state = txHistoryScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Click on 'My wallet' address") {
                onSendAddressScreen {
                    recentAddressItem(recipientAddress = DOGECOIN_ADDRESS, isMyWallet = true).performClick()
                }
            }
            step("Assert recipient address is displayed on confirm screen") {
                onSendConfirmScreen { recipientAddress(DOGECOIN_ADDRESS).assertIsDisplayed() }
            }
        }
    }

    @AllureId("4599")
    @DisplayName("Send (address screen): 'My wallets' block is not displayed for account networks")
    @Test
    fun myWalletsBlockForAccountNetworksNotDisplayedTest() {
        val tokenName = "Ethereum"
        val sendAmount = "1"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Assert 'My wallets' title is not displayed") {
                onSendAddressScreen { myWalletsTitle.assertIsNotDisplayed() }
            }
            step("Assert 'My wallets' item is not displayed") {
                onSendAddressScreen {
                    recentAddressItem(
                        recipientAddress = ETHEREUM_ADDRESS,
                        isMyWallet = true
                    ).assertIsNotDisplayed()
                }
            }
        }
    }

    @AllureId("4737")
    @DisplayName("Send (address screen): assert 'My wallet address is correct'")
    @Test
    fun checkMyWalletsBlockTest() {
        val tokenName = "Dogecoin"
        val sendAmount = "1"
        val txHistoryScenarioState = "OutgoingTransaction"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(txHistoryScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$txHistoryScenarioName' to state: '$txHistoryScenarioState'") {
                setWireMockScenarioState(scenarioName = txHistoryScenarioName, state = txHistoryScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Address' screen") {
                openSendAddressScreen(tokenName, sendAmount)
            }
            step("Assert 'My wallets' title is displayed") {
                onSendAddressScreen { myWalletsTitle.assertIsDisplayed() }
            }
            step("Assert 'My wallet' address is displayed") {
                onSendAddressScreen {
                    recentAddressItem(recipientAddress = DOGECOIN_ADDRESS, isMyWallet = true).performClick()
                }
            }
        }
    }
}