package com.tangem.tests.main

import androidx.compose.ui.test.performTextInput
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.COINS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openManageTokens
import com.tangem.screens.onDialog
import com.tangem.screens.onManageTokensScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class ManageTokensTest : BaseTestCase() {

    private val richState = "ManageTokensRich"

    @AllureId("765")
    @DisplayName("Manage tokens: network standard labels are shown for token networks")
    @Test
    fun networkStandardLabelsTest() {
        val tokenTitle = "Tether"

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Open 'Manage tokens' screen") { openManageTokens() }
            step("Click on token: '$tokenTitle'") {
                flakySafely { onManageTokensScreen { tokenItem(tokenTitle).assertIsDisplayed() } }
                onManageTokensScreen { tokenItem(tokenTitle).performClick() }
            }
            step("Assert 'Ethereum' network standard 'ERC20' is displayed") {
                flakySafely {
                    onManageTokensScreen { networkStandard(networkName = "Ethereum", standard = "ERC20").assertIsDisplayed() }
                }
            }
            step("Assert 'BNB Smart Chain' network standard 'BEP20' is displayed") {
                flakySafely {
                    onManageTokensScreen {
                        networkStandard(networkName = "BNB Smart Chain", standard = "BEP20").assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Tron' network standard 'TRC20' is displayed") {
                flakySafely {
                    onManageTokensScreen { networkStandard(networkName = "Tron", standard = "TRC20").assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("667")
    @DisplayName("Manage tokens: search by name and ticker filters the list")
    @Test
    fun searchByNameAndTickerTest() {
        val tokenTitle = "Tether"
        val nameQuery = "Tether"
        val tickerQuery = "USDT"
        val emptyQuery = "Zzqnotoken"

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Open 'Manage tokens' screen") { openManageTokens() }
            step("Search by name: '$nameQuery'") {
                onManageTokensScreen {
                    searchField.performClick()
                    searchField.performTextInput(nameQuery)
                }
            }
            step("Assert token: '$tokenTitle' is displayed") {
                flakySafely { onManageTokensScreen { tokenItem(tokenTitle).assertIsDisplayed() } }
            }
            step("Clear search field") {
                onManageTokensScreen { searchClearButton.clickWithAssertion() }
            }
            step("Search by ticker: '$tickerQuery'") {
                onManageTokensScreen { searchField.performTextInput(tickerQuery) }
            }
            step("Assert token: '$tokenTitle' is displayed") {
                flakySafely { onManageTokensScreen { tokenItem(tokenTitle).assertIsDisplayed() } }
            }
            step("Clear search field") {
                onManageTokensScreen { searchClearButton.clickWithAssertion() }
            }
            step("Search by unknown query: '$emptyQuery'") {
                onManageTokensScreen { searchField.performTextInput(emptyQuery) }
            }
            step("Assert token: '$tokenTitle' is not displayed") {
                flakySafely { onManageTokensScreen { tokenItem(tokenTitle).assertDoesNotExist() } }
            }
        }
    }

    @AllureId("763")
    @DisplayName("Manage tokens: enabling Solana network on a modern card shows no warning")
    @Test
    fun solanaNetworkNoWarningTest() {
        val tokenTitle = "USD Coin"
        val networkTitle = "SOLANA"

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Open 'Manage tokens' screen") { openManageTokens() }
            step("Click on token: '$tokenTitle'") {
                flakySafely { onManageTokensScreen { tokenItem(tokenTitle).assertIsDisplayed() } }
                onManageTokensScreen { tokenItem(tokenTitle).performClick() }
            }
            step("Click on '$networkTitle' switch") {
                flakySafely { onManageTokensScreen { networkSwitch(networkTitle).assertIsDisplayed() } }
                onManageTokensScreen { networkSwitch(networkTitle).performClick() }
            }
            step("Assert hide-token alert is not displayed") {
                waitForIdle()
                onDialog { dialogContainer.assertDoesNotExist() }
            }
        }
    }
}