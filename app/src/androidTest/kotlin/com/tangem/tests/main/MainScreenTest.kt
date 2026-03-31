package com.tangem.tests.main

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MainScreenTest : BaseTestCase() {

    @AllureId("66")
    @DisplayName("Main: check 'Organize tokens' button with multiple tokens no accounts")
    @Test
    fun checkOrganizeTokensButtonWithMultipleTokensNoAccountsTest() {

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Organize tokens' button is displayed") {
                onMainScreen { organizeTokensButton().assertIsDisplayed() }
            }
        }
    }

    @AllureId("8748")
    @DisplayName("Main: check 'Organize tokens' button with single token no accounts")
    @Test
    fun checkOrganizeTokensButtonWithSingleTokenNoAccountsTest() {
        val scenarioState = "Cardano"

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
            step("Assert 'Organize tokens' button is not displayed") {
                onMainScreen { organizeTokensButtonNode.assertIsNotDisplayed()}
            }
        }
    }

    @AllureId("8749")
    @DisplayName("Main: check 'Organize tokens' button with single token two accounts")
    @Test
    fun checkOrganizeTokensButtonWithSingleTokenMultiAccountsTest() {
        val scenarioState = "TwoAccountsSingleTokenEach"

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
            step("Assert 'Organize tokens' button is not displayed") {
                onMainScreen { organizeTokensButtonNode.assertIsNotDisplayed()}
            }
        }
    }

    @AllureId("8750")
    @DisplayName("Main: check 'Organize tokens' button with multiple tokens two accounts")
    @Test
    fun checkOrganizeTokensButtonWithMultipleTokensMultiAccountsTest() {
        val scenarioState = "TwoAccountsMixed"

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
            step("Assert 'Organize tokens' button is not displayed") {
                onMainScreen { organizeTokensButtonNode.assertIsDisplayed()}
            }
        }
    }
}