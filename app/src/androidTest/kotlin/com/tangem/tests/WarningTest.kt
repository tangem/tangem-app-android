package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.screens.onMainScreen
import com.tangem.tap.domain.sdk.mocks.content.DevWalletMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2WithSeedPhraseMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class WarningTest : BaseTestCase() {

    @AllureId("898")
    @DisplayName("Warnings: check 'Dev card' warning")
    @Test
    fun devCardWarningTest() {
        setupHooks().run {
            step("Open 'Main' screen") {
                openMainScreen(mockContent = DevWalletMockContent)
            }
            step("Assert 'Dev card' notification title is displayed") {
                onMainScreen { devCardNotificationTitle.assertIsDisplayed() }
            }
            step("Assert 'Dev card' notification message is displayed") {
                onMainScreen { devCardNotificationMessage.assertIsDisplayed() }
            }
            step("Assert 'Dev card' notification icon is displayed") {
                onMainScreen { devCardNotificationIcon.assertIsDisplayed() }
            }
        }
    }

    @AllureId("3991")
    @DisplayName("Warnings: check 'Dev card' warning is not displayed for release card")
    @Test
    fun releaseCardWarningTest() {
        setupHooks().run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Assert 'Dev card' notification title is not displayed") {
                onMainScreen { devCardNotificationTitle.assertIsNotDisplayed() }
            }
            step("Assert 'Dev card' notification message is not displayed") {
                onMainScreen { devCardNotificationMessage.assertIsNotDisplayed() }
            }
            step("Assert 'Dev card' notification icon is not displayed") {
                onMainScreen { devCardNotificationIcon.assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("227")
    @DisplayName("Seed notify: check warning for wallet with seed phrase")
    @Test
    fun checkWarningForWalletWithSeedPhraseTest() {
        val scenarioName = "seedphrase_notification"
        val scenarioState = "Notified"
        setupHooks(
            additionalBeforeSection = {
                step("Setup WireMock scenario '$scenarioName' for '$scenarioState' state") {
                    setWireMockScenarioState(scenarioName, scenarioState)
                }
            },
            additionalAfterSection = {
                step("Reset WireMock scenario '$scenarioName' state") {
                    resetWireMockScenarioState(scenarioName)
                }
            }
        ).run {
            step("Open 'Main' screen") {
                openMainScreen(mockContent = Wallet2WithSeedPhraseMockContent, alreadyActivatedDialogIsShown = true)
            }
            step("Assert 'Seed phrase' notification icon is displayed") {
                onMainScreen { seedPhraseNotificationIcon.assertIsDisplayed() }
            }
            step("Assert 'Seed phrase' notification title is displayed") {
                onMainScreen { seedPhraseNotificationTitle.assertIsDisplayed() }
            }
            step("Assert 'Seed phrase' notification message is displayed") {
                onMainScreen { seedPhraseNotificationMessage.assertIsDisplayed() }
            }
            step("Assert notification 'Yes' button is displayed") {
                onMainScreen { notificationYesButton.assertIsDisplayed() }
            }
            step("Assert notification 'No' button is displayed") {
                onMainScreen { notificationNoButton.assertIsDisplayed() }
            }
        }
    }
}