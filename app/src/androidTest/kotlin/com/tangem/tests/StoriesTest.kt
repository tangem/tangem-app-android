package com.tangem.tests

import android.content.Intent.ACTION_VIEW
import androidx.test.espresso.intent.Intents
import com.tangem.helpers.base.BaseAutoTestCase
import com.tangem.screens.StoriesScreen
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.kakao.intent.KIntent
import org.junit.Test

class StoriesTest : BaseAutoTestCase() {

    @Test
    fun clickOnButtons() = before {
        Intents.init()
    }.after {
        Intents.release()
    }.run {
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