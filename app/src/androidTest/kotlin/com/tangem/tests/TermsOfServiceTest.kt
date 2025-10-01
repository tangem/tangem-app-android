package com.tangem.tests

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
import com.tangem.screens.onDisclaimerScreen
import com.tangem.screens.onStoriesScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TermsOfServiceTest : BaseTestCase() {

    @AllureId("3573")
    @DisplayName("ToS: success acceptance")
    @Test
    fun validateTermsOfServiceScreenTest() {
        setupHooks().run {
            val tosUrl = "https://tangem.com/tangem_tos.html"
            step("Assert title of 'Disclaimer screen' is displayed") {
                onDisclaimerScreen { title.assertIsDisplayed() }
            }
            step("Assert title of 'Disclaimer screen' is displayed") {
                onDisclaimerScreen { webView.assertIsDisplayed() }
            }
            step("Verify WebView loads correct URL") {
                onDisclaimerScreen {
                    webView.assertContentDescriptionContains(value = tosUrl, substring = true)
                }
            }
            step("Click on 'Accept' button") {
                onDisclaimerScreen { acceptButton.clickWithAssertion() }
            }
            step("Assert 'Stories' screen is opened") {
                onStoriesScreen {
                    scanButton.assertIsDisplayed()
                    orderButton.assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("3574")
    @DisplayName("ToS: accept after app restart")
    @Test
    fun acceptTermsOfServiceAfterAppRestart() {
        val packageName = getTargetContext().packageName
        setupHooks().run {
            val tosUrl = "https://tangem.com/tangem_tos.html"
            step("Assert title of 'Disclaimer screen' is displayed") {
                onDisclaimerScreen { title.assertIsDisplayed() }
            }
            step("Assert WebView of 'Disclaimer screen' is displayed") {
                onDisclaimerScreen { webView.assertIsDisplayed() }
            }
            step("Verify WebView loads correct URL") {
                onDisclaimerScreen {
                    webView.assertContentDescriptionContains(value = tosUrl, substring = true)
                }
            }
            step("'Accept' button is displayed") {
                onDisclaimerScreen { acceptButton.assertIsDisplayed() }
            }
            step("Open recent apps") {
                device.uiDevice.pressRecentApps()
            }
            step("Stop app by swipe") {
                swipeVertical(SwipeDirection.UP, startHeightRatio = 0.5f)
            }
            step("Launch app") {
                device.apps.launch(packageName)
            }
            step("Assert title of 'Disclaimer screen' is displayed") {
                onDisclaimerScreen { title.assertIsDisplayed() }
            }
            step("Assert WebView of 'Disclaimer screen' is displayed") {
                onDisclaimerScreen { webView.assertIsDisplayed() }
            }
            step("Verify WebView loads correct URL") {
                onDisclaimerScreen {
                    webView.assertContentDescriptionContains(value = tosUrl, substring = true)
                }
            }
            step("Click on 'Accept' button") {
                onDisclaimerScreen { acceptButton.clickWithAssertion() }
            }
            step("Open recent apps") {
                device.uiDevice.pressRecentApps()
            }
            step("Stop app by swipe") {
                swipeVertical(SwipeDirection.UP, startHeightRatio = 0.5f)
            }
            step("Launch app") {
                device.apps.launch(packageName)
            }
            step("Assert 'Stories' screen is opened") {
                onStoriesScreen {
                    scanButton.assertIsDisplayed()
                    orderButton.assertIsDisplayed()
                }
            }
        }
    }
}