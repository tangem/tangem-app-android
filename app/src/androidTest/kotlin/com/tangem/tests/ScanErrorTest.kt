package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onDisclaimerScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onStoriesScreen
import com.tangem.tap.domain.sdk.mocks.MockProvider
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class ScanErrorTest : BaseTestCase() {

    @Test
    fun goToMainTest() =
        setupHooks().run {
            onDisclaimerScreen {
                step("Click on 'Accept' button") {
                    acceptButton.clickWithAssertion()
                }
            }
            onStoriesScreen {
                step("Click on 'Scan' button emulating scan error") {
                    MockProvider.setEmulateError()
                    scanButton.clickWithAssertion()
                }
                step("Click on 'Scan' button again without emulating error") {
                    MockProvider.resetEmulateError()
                    scanButton.clickWithAssertion()
                }
            }
            onMainScreen {
                step("Make sure wallet screen is visible") {
                    assertIsDisplayed()
                }
            }
        }
}