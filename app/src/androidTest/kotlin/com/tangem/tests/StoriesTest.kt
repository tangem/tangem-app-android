package com.tangem.tests

import android.content.Intent.ACTION_VIEW
import com.tangem.common.BaseTestCase
import com.tangem.screens.StoriesScreen
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.kakao.intent.KIntent
import org.junit.Test

@HiltAndroidTest
class StoriesTest : BaseTestCase() {

    @Test
    fun clickOnButtons() =
        setupHooks().run {
            ComposeScreen.onComposeScreen<StoriesScreen>(composeTestRule) {
                step("Click on \"Scan\" button") {
                    scanButton {
                        assertIsDisplayed()
                        performClick()
                    }
                }
                step("Assert: \"Scan card\" popup opened") {
                    enableNFCAlert.isDisplayed()
                    cancelButton.click()
                    device.uiDevice.pressBack()
                }
                step("Click on \"Order\" button") {
                    orderButton.performClick()
                }
                step("Assert: browser opened") {
                    val expectedIntent = KIntent {
                        hasAction(ACTION_VIEW)
                        hasData(NEW_BUY_WALLET_URL)
                    }
                    expectedIntent.intended()
                    device.uiDevice.pressBack()
                }
            }
        }
}
