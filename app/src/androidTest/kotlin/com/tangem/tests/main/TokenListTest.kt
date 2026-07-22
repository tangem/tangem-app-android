package com.tangem.tests.main

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.addNewCardWalletWithoutSync
import com.tangem.scenarios.getMainScreenTokensOrder
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.tap.domain.sdk.mocks.content.Wallet2MockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TokenListTest : BaseTestCase() {

    @AllureId("180")
    @DisplayName("Token list: hide token by long tap")
    @Test
    fun checkCustomDerivationIconOnTokenAndNetworkTest() {
        val networkTitle = "Ethereum"
        val customTokenTitle = "Myria"
        val scenarioState = "CustomDerivation"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert token: '$networkTitle' is displayed") {
                onMainScreen { tokenWithTitleAndAddress(networkTitle).assertIsDisplayed() }
            }
            step("Assert token with custom derivation icon: '$customTokenTitle' is displayed") {
                onMainScreen { tokenWithCustomDerivationIcon(customTokenTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("177")
    @DisplayName("Main: token list differs after switching to a second wallet")
    @Test
    fun tokenListChangedAfterSwitchingWalletTest() {
        val userTokensState = "ReducedTokens"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }

            val firstWalletTokens = getMainScreenTokensOrder()

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensState)
            }
            step("Add a second card wallet") {
                addNewCardWalletWithoutSync(Wallet2MockContent)
            }

            val secondWalletTokens = getMainScreenTokensOrder()

            step("Assert both wallets exposed a non-empty token list") {
                assertTrue(
                    "First wallet token list should not be empty",
                    firstWalletTokens.isNotEmpty(),
                )
                assertTrue(
                    "Second wallet token list should not be empty",
                    secondWalletTokens.isNotEmpty(),
                )
            }
            step("Assert the two wallets show different token lists") {
                assertNotEquals(
                    "Expected the second wallet's token list to differ from the first " +
                        "(first=$firstWalletTokens, second=$secondWalletTokens)",
                    firstWalletTokens,
                    secondWalletTokens,
                )
            }
        }
    }

}