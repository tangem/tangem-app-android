package com.tangem.tests

import com.atiurin.ultron.allure.step.step
import com.atiurin.ultron.extensions.assertIsDisplayed
import com.atiurin.ultron.extensions.assertIsNotDisplayed
import com.atiurin.ultron.extensions.click
import com.tangem.common.BaseTestCase
import com.tangem.screens.DisclaimerPage
import com.tangem.screens.MainPage
import com.tangem.screens.StoriesPage
import com.tangem.tap.domain.sdk.mocks.MockProvider
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class ScanErrorTest : BaseTestCase() {

    @Test
    fun goToMain() {
        step("Click on \"Accept\" button") {
            DisclaimerPage.acceptButton.click()
        }
        step("Emulate scan error") {
            MockProvider.setEmulateError()
        }
        step("Click on \"Scan\" button emulating scan error") {
            StoriesPage.scanButton.click()
        }
        step("Assert: Error is displayed") {
            MainPage.container.assertIsNotDisplayed()
        }
        step("Emulate success scan") {
            MockProvider.resetEmulateError()
        }
        step("Click on \"Scan\" button") {
            StoriesPage.scanButton.click()
        }
        step("Assert: wallet screen is displayed") {
            MainPage.container.assertIsDisplayed()
        }
    }
}