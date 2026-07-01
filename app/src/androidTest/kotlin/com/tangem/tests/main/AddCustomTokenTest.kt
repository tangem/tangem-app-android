package com.tangem.tests.main

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.COINS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.performTextInputInChunks
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.addCustomTokenWithCustomDerivation
import com.tangem.scenarios.navigateBackToMainFromManageTokens
import com.tangem.scenarios.openAddCustomToken
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onAddCustomTokenScreen
import com.tangem.screens.onMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AddCustomTokenTest : BaseTestCase() {

    private val richState = "ManageTokensRich"
    private val tokenTitle = "Tether"
    private val ethereumNetwork = "Ethereum"
    private val solanaNetwork = "Solana"
    private val ethContract = "0xdac17f958d2ee523a2206206994597c13d831ec7"
    private val solanaContract = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
    private val customDerivationPath = "m/44'/60'/0'/0/1"

    @AllureId("772")
    @DisplayName("Add custom token: added token appears on Main")
    @Test
    fun addCustomTokenAppearsOnMainTest() {
        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") {
                flakySafely { onMainScreen { synchronizeAddressesButton.assertIsDisplayed() } }
                synchronizeAddresses(assertBalance = false)
            }
            step("Open 'Add custom token' screen") { openAddCustomToken() }
            step("Click on network: '$ethereumNetwork'") {
                flakySafely { onAddCustomTokenScreen { scrollToNetwork(ethereumNetwork) } }
                onAddCustomTokenScreen { networkRow(ethereumNetwork).performClick() }
            }
            step("Enter contract address: '$ethContract'") {
                flakySafely { onAddCustomTokenScreen { contractAddressField.assertExists() } }
                // Chunked input spans several validation ticks — the model drops the first sampled form change.
                onAddCustomTokenScreen { contractAddressField.performTextInputInChunks(ethContract) }
            }
            step("Click on 'Add token' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onAddCustomTokenScreen { addTokenButton.assertIsEnabled() } }
                onAddCustomTokenScreen { addTokenButton.performClick() }
            }
            step("Navigate back to 'Main Screen'") { navigateBackToMainFromManageTokens() }
            step("Assert token: '$tokenTitle' is displayed on Main") {
                flakySafely { onMainScreen { tokenWithTitleAndAddress(tokenTitle).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("775")
    @DisplayName("Add custom token: custom derivation path is accepted")
    @Test
    fun addCustomTokenWithCustomDerivationTest() {
        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") {
                flakySafely { onMainScreen { synchronizeAddressesButton.assertIsDisplayed() } }
                synchronizeAddresses(assertBalance = false)
            }
            step("Open 'Add custom token' screen") { openAddCustomToken() }
            step("Add custom token with custom derivation on '$ethereumNetwork'") {
                addCustomTokenWithCustomDerivation(
                    network = ethereumNetwork,
                    contract = ethContract,
                    derivationPath = customDerivationPath,
                )
            }
            step("Assert token: '$tokenTitle' is displayed on Main") {
                flakySafely { onMainScreen { tokenWithTitleAndAddress(tokenTitle).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("771")
    @DisplayName("Add custom token: custom derivation indicator is shown on Main")
    @Test
    fun customDerivationIndicatorOnMainTest() {
        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") {
                flakySafely { onMainScreen { synchronizeAddressesButton.assertIsDisplayed() } }
                synchronizeAddresses(assertBalance = false)
            }
            step("Open 'Add custom token' screen") { openAddCustomToken() }
            step("Add custom token with custom derivation on '$ethereumNetwork'") {
                addCustomTokenWithCustomDerivation(
                    network = ethereumNetwork,
                    contract = ethContract,
                    derivationPath = customDerivationPath,
                )
            }
            step("Assert custom derivation indicator for '$tokenTitle' is displayed on Main") {
                flakySafely { onMainScreen { tokenWithCustomDerivationIcon(tokenTitle).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("770")
    @DisplayName("Add custom token: derivation field is available for a non-EVM network")
    @Test
    fun derivationAvailableForNonEvmNetworkTest() {
        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Open 'Add custom token' screen") { openAddCustomToken() }
            step("Click on network: '$solanaNetwork'") {
                flakySafely { onAddCustomTokenScreen { scrollToNetwork(solanaNetwork) } }
                onAddCustomTokenScreen { networkRow(solanaNetwork).performClick() }
            }
            step("Assert 'Derivation path' field is available") {
                flakySafely { onAddCustomTokenScreen { derivationSelectorField.assertExists() } }
            }
        }
    }

    @AllureId("769")
    @DisplayName("Add custom token: Solana token on a modern card shows no unsupported warning")
    @Test
    fun solanaTokenModernCardNoWarningTest() {
        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(COINS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$COINS_API_SCENARIO' to state: '$richState'") {
                setWireMockScenarioState(COINS_API_SCENARIO, richState)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Open 'Add custom token' screen") { openAddCustomToken() }
            step("Click on network: '$solanaNetwork'") {
                flakySafely { onAddCustomTokenScreen { scrollToNetwork(solanaNetwork) } }
                onAddCustomTokenScreen { networkRow(solanaNetwork).performClick() }
            }
            step("Enter contract address: '$solanaContract'") {
                flakySafely { onAddCustomTokenScreen { contractAddressField.assertExists() } }
                onAddCustomTokenScreen { contractAddressField.performTextInputInChunks(solanaContract) }
            }
            step("Assert 'Add token' button is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onAddCustomTokenScreen { addTokenButton.assertIsEnabled() } }
            }
            step("Assert unsupported-token warning is not displayed") {
                onAddCustomTokenScreen { warningNotification.assertDoesNotExist() }
            }
        }
    }
}