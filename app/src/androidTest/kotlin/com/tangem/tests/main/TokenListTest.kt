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

}