package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.DisclaimerTestScreen
import com.tangem.screens.MainTestScreen
import com.tangem.screens.StoriesTestScreen
import com.tangem.tap.domain.sdk.mocks.MockProvider
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.Test

@HiltAndroidTest
class ScanErrorTest : BaseTestCase() {

    @Test
    fun goToMain() =
        setupHooks().run {
            ComposeScreen.onComposeScreen<DisclaimerTestScreen>(composeTestRule) {
                step("Click on \"Accept\" button") {
                    acceptButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<StoriesTestScreen>(composeTestRule) {
                step("Click on \"Scan\" button emulating scan error") {
                    MockProvider.setEmulateError()
                    scanButton.clickWithAssertion()
                }
                step("Click on \"Scan\" button again without emulating error") {
                    MockProvider.resetEmulateError()
                    scanButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<MainTestScreen>(composeTestRule) {
                step("Make sure wallet screen is visible") {
                    assertIsDisplayed()
                }
            }
        }
}