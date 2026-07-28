package com.tangem.tests.main

import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.COINS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertClipboardIsEmpty
import com.tangem.common.utils.assertClipboardTextEquals
import com.tangem.common.utils.clearClipboard
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openManageTokens
import com.tangem.scenarios.toggleTokenNetworkInManageTokens
import com.tangem.screens.onDialog
import com.tangem.screens.onManageTokensScreen
import com.tangem.tap.domain.sdk.mocks.content.Firmware451MockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2NoEd25519Slip0010MockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import com.tangem.core.res.R as CoreResR

@HiltAndroidTest
class ManageTokensTest : BaseTestCase() {

    private val richState = "ManageTokensRich"
    private val solanaTokenTitle = "USD Coin"
    private val solanaNetworkTitle = "SOLANA"
    private val solanaName = "Solana"

    @AllureId("765")
    @DisplayName("Manage tokens: network standard labels are shown for token networks")
    @Test
    fun networkStandardLabelsTest() {
        val tokenTitle = "Tether"
        val ethereum = "Ethereum"
        val bnbSmartChain = "BNB Smart Chain"
        val tron = "Tron"
        val erc20 = "ERC20"
        val bep20 = "BEP20"
        val trc20 = "TRC20"

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
            step("Assert '$ethereum' network standard '$erc20' is displayed") {
                flakySafely {
                    onManageTokensScreen { networkStandard(networkName = ethereum, standard = erc20).assertIsDisplayed() }
                }
            }
            step("Assert '$bnbSmartChain' network standard '$bep20' is displayed") {
                flakySafely {
                    onManageTokensScreen {
                        networkStandard(networkName = bnbSmartChain, standard = bep20).assertIsDisplayed()
                    }
                }
            }
            step("Assert '$tron' network standard '$trc20' is displayed") {
                flakySafely {
                    onManageTokensScreen { networkStandard(networkName = tron, standard = trc20).assertIsDisplayed() }
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
        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Open 'Manage tokens' screen") { openManageTokens() }
            step("Enable '$solanaNetworkTitle' network for token: '$solanaTokenTitle'") {
                toggleTokenNetworkInManageTokens(tokenTitle = solanaTokenTitle, networkTitle = solanaNetworkTitle)
            }
            step("Assert hide-token alert is not displayed") {
                waitForIdle()
                onDialog { dialogContainer.assertDoesNotExist() }
            }
        }
    }

    @AllureId("719")
    @DisplayName("Manage tokens: long tap on a token network copies the contract address, main network does not")
    @Test
    fun copyContractAddressOnNetworkLongTapTest() {
        val tokenTitle = "Tether"
        val tokenNetworkTitle = "ETHEREUM"
        val coinTitle = "Bitcoin"
        val coinNetworkTitle = "BITCOIN"
        val contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7"

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
            step("Long click on '$tokenNetworkTitle' network row") {
                flakySafely { onManageTokensScreen { networkName(tokenNetworkTitle).assertIsDisplayed() } }
                onManageTokensScreen {
                    networkName(tokenNetworkTitle).performTouchInput {
                        longClick(position = center, durationMillis = 1000L)
                    }
                }
            }
            step("Assert contract-address-copied message is displayed") {
                flakySafely { onManageTokensScreen { contractAddressCopiedMessage.assertIsDisplayed() } }
            }
            step("Assert clipboard contains '$tokenTitle' contract address") {
                assertClipboardTextEquals(contractAddress)
            }
            step("Clear clipboard") { clearClipboard() }
            step("Click on token: '$coinTitle'") {
                flakySafely { onManageTokensScreen { tokenItem(coinTitle).assertIsDisplayed() } }
                onManageTokensScreen { tokenItem(coinTitle).performClick() }
            }
            step("Long click on '$coinNetworkTitle' main network row") {
                flakySafely { onManageTokensScreen { networkName(coinNetworkTitle).assertIsDisplayed() } }
                onManageTokensScreen {
                    networkName(coinNetworkTitle).performTouchInput {
                        longClick(position = center, durationMillis = 1000L)
                    }
                }
            }
            step("Assert clipboard is empty after long click on main network") {
                waitForIdle()
                assertClipboardIsEmpty()
            }
        }
    }

    @AllureId("737")
    @DisplayName("Manage tokens: enabling Solana on an old-firmware card shows firmware-limitation warning")
    @Test
    fun solanaFirmwareLimitationWarningTest() {
        val warningMessage = getResourceString(CoreResR.string.alert_manage_tokens_unsupported_message, solanaName)

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen(mockContent = Firmware451MockContent) }
            step("Open 'Manage tokens' screen") { openManageTokens() }
            step("Enable '$solanaNetworkTitle' network for token: '$solanaTokenTitle'") {
                toggleTokenNetworkInManageTokens(tokenTitle = solanaTokenTitle, networkTitle = solanaNetworkTitle)
            }
            step("Assert firmware-limitation warning is displayed") {
                flakySafely { onDialog { containerWithText(warningMessage).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("767")
    @DisplayName("Manage tokens: enabling Solana on a card missing its curve shows unsupported-curve warning")
    @Test
    fun solanaUnsupportedCurveWarningTest() {
        val warningMessage =
            getResourceString(CoreResR.string.alert_manage_tokens_unsupported_curve_message, solanaName)

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen(mockContent = Wallet2NoEd25519Slip0010MockContent) }
            step("Open 'Manage tokens' screen") { openManageTokens() }
            step("Enable '$solanaNetworkTitle' network for token: '$solanaTokenTitle'") {
                toggleTokenNetworkInManageTokens(tokenTitle = solanaTokenTitle, networkTitle = solanaNetworkTitle)
            }
            step("Assert unsupported-curve warning is displayed") {
                flakySafely { onDialog { containerWithText(warningMessage).assertIsDisplayed() } }
            }
        }
    }
}