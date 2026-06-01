package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AppCurrencyTest : BaseTestCase() {

    @AllureId("781")
    @DisplayName("App Currency: change of equivalent")
    @Test
    fun changeAppCurrencyTest() {
        val currenciesScenario = "currencies_api"
        val appSettingsState = "AppSettings"
        val targetCurrency = "EUR"
        val targetSymbol = "€"
        val token = "Bitcoin"

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(currenciesScenario) },
        ).run {
            step("Set WireMock scenario '$currenciesScenario' to '$appSettingsState'") {
                setWireMockScenarioState(scenarioName = currenciesScenario, state = appSettingsState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            synchronizeAddresses()
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'App settings' button") {
                onDetailsScreen { appSettingsButton.clickWithAssertion() }
            }
            step("Click on 'App currency' button") {
                onAppSettingsScreen { currencyButton.clickWithAssertion() }
            }
            step("Click on search button") {
                onAppCurrencySelectorScreen { searchActionButton.clickWithAssertion() }
            }
            step("Search currency '$targetCurrency'") {
                onAppCurrencySelectorScreen { searchField.performTextInput(targetCurrency) }
            }
            step("Click on currency '$targetCurrency'") {
                onAppCurrencySelectorScreen { currencyItem(targetCurrency).performClick() }
            }
            step("Return to 'Main' screen") {
                var displayed = false
                var attempts = 0
                while (!displayed && attempts < 4) {
                    onMainScreen { displayed = screenContainer.isDisplayedSafely() }
                    if (!displayed) {
                        device.uiDevice.pressBack()
                        waitForIdle()
                        attempts++
                    }
                }
            }
            step("Assert total balance contains '$targetSymbol' on 'Main' screen") {
                // Balance re-loads in the new currency async after the switch — wait for the € equivalent.
                composeTestRule.waitUntil(WAIT_UNTIL_TIMEOUT_LONG) {
                    runCatching {
                        onMainScreen { totalBalanceText.assertTextContains(targetSymbol, substring = true) }
                    }.isSuccess
                }
            }
            step("Click on token '$token'") {
                onMainScreen { tokenWithTitleAndAddress(token).clickWithAssertion() }
            }
            step("Assert token fiat balance contains '$targetSymbol'") {
                composeTestRule.waitUntil(WAIT_UNTIL_TIMEOUT_LONG) {
                    runCatching {
                        onTokenDetailsScreen { fiatBalance.assertTextContains(targetSymbol, substring = true) }
                    }.isSuccess
                }
            }
        }
    }
}