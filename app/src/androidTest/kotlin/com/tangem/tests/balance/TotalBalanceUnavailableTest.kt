package com.tangem.tests.balance

import com.tangem.common.BaseTestCase
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.utils.StringsSigns.DASH_SIGN
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TotalBalanceUnavailableTest : BaseTestCase() {

    @Test
    @AllureId("148")
    @DisplayName("Total balance: check dash sign when addresses are not derived")
    fun whenDerivationsDoesNotSyncedTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Do not synchronize addresses") {
                onMainScreen { synchronizeAddressesButton.assertIsDisplayed() }
            }
            step("Assert dash sign is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(DASH_SIGN) }
            }
        }
    }

    @Test
    @AllureId("3993")
    @DisplayName("Total balance: check dash sign when network is unreachable")
    fun whenNetworkIsUnreachableTest() {
        val scenarioName = "eth_network_balance"
        val scenarioState = "Unreachable"
        val tokenTitle = "Ethereum"
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
                synchronizeAddresses(DASH_SIGN)
            }
            step("Assert 'Synchronize addresses' button does not exist") {
                onMainScreen {
                    flakySafely {
                        synchronizeAddressesButton.assertDoesNotExist()
                    }
                }
            }
            step("Assert dash sign is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(DASH_SIGN) }
            }
            step("Assert $tokenTitle is unreachable") {
                onMainScreen { tokenWithTitleAndPosition(tokenTitle, 1).assertIsUnreachable() }
            }
        }
    }

    @Test
    @AllureId("3994")
    @DisplayName("Total balance: check dash sign when quotes are failed")
    fun whenQuotesAreFailedTest() {
        val scenarioName = "quotes_api"
        val scenarioState = "Error"
        val tokenTitle = "Ethereum"
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
                synchronizeAddresses(DASH_SIGN)
            }
            step("Assert 'Synchronize addresses' button does not exist") {
                onMainScreen {
                    flakySafely {
                        synchronizeAddressesButton.assertDoesNotExist()
                    }
                }
            }
            step("Assert dash sign is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(DASH_SIGN) }
            }
            step("Assert $tokenTitle is unreachable") {
                onMainScreen { tokenWithTitleAndPosition(tokenTitle, 1).assertIsUnreachable() }
            }
        }
    }

    @Test
    @AllureId("3995")
    @DisplayName("Total balance: check dash sign when added custom token without rate")
    fun whenCustomTokenWithoutRateAddedTest() {
        val scenarioName = "user_tokens_api"
        val scenarioState = "CustomTokenAdded"
        val tokenTitle = "Myria"
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
                synchronizeAddresses(DASH_SIGN)
            }
            step("Assert 'Synchronize addresses' button does not exist") {
                onMainScreen {
                    flakySafely {
                        synchronizeAddressesButton.assertDoesNotExist()
                    }
                }
            }
            step("Assert dash sign is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(DASH_SIGN) }
            }
            step("Assert $tokenTitle is unreachable") {
                onMainScreen { tokenWithTitleAndPosition(tokenTitle, 4).assertIsUnreachable() }
            }
        }
    }
}