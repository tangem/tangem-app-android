package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.scenarios.openMainScreen
import com.tangem.screens.onMainScreen
import com.tangem.tap.domain.sdk.mocks.content.DevWalletMockContent
import dagger.hilt.android.testing.HiltAndroidTest
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
}