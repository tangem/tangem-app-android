package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.screens.DisclaimerScreen
import com.tangem.screens.StoriesScreen
import com.tangem.screens.WalletScreen
import com.tangem.tap.domain.sdk.mocks.MockProvider
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.Test

@HiltAndroidTest
class ScanErrorTest : BaseTestCase() {

    @Test
    fun goToMain() =
        setupHooks().run {
            ComposeScreen.onComposeScreen<StoriesScreen>(composeTestRule) {
                step("Click on \"Scan\" button emulating scan error") {
                    MockProvider.setEmulateError()
                    scanButton {
                        assertIsDisplayed()
                        performClick()
                    }
                }
                step("Click on \"Scan\" button again without emulating error") {
                    MockProvider.resetEmulateError()
                    scanButton {
                        assertIsDisplayed()
                        performClick()
                    }
                }
            }
            DisclaimerScreen {
                step("Click on \"Accept\" button") {
                    acceptButton {
                        isVisible()
                        click()
                    }
                }
            }
            ComposeScreen.onComposeScreen<WalletScreen>(composeTestRule) {
                step("Make sure wallet screen is visible") {
                    assertIsDisplayed()
                }
            }
        }
}
