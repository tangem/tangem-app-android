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
class WarningsTest : BaseTestCase() {

    @AllureId("184")
    @DisplayName("Token list: hide token by long tap")
    @Test
    fun checkUnavailableNetworksWarningTest() {
        val scenarioState = "MissingDerivation"
        val networkCount = 1

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
                synchronizeAddresses(isBalanceAvailable = false)
            }
            step("Assert 'Missing addresses' notification icon is displayed") {
                onMainScreen { missingAddressNotificationIcon.assertIsDisplayed() }
            }
            step("Assert 'Missing addresses' notification title is displayed") {
                onMainScreen { missingAddressNotificationTitle.assertIsDisplayed() }
            }
            step("Assert 'Missing addresses' notification message is displayed") {
                onMainScreen { missingAddressNotificationMessage(networkCount).assertIsDisplayed() }
            }
        }
    }
}