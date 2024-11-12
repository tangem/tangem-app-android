package com.tangem.tests

import android.content.Intent.ACTION_VIEW
import androidx.test.espresso.intent.matcher.UriMatchers
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.DisclaimerTestScreen
import com.tangem.screens.StoriesTestScreen
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.kakao.intent.KIntent
import org.hamcrest.Matchers
import org.junit.Test

@HiltAndroidTest
class StoriesTest : BaseTestCase() {

    @Test
    fun clickOnOrderButton() =
        setupHooks().run {
            ComposeScreen.onComposeScreen<DisclaimerTestScreen>(composeTestRule) {
                step("Click on \"Accept\" button") {
                    acceptButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<StoriesTestScreen>(composeTestRule) {
                step("Click on \"Order\" button") {
                    orderButton.clickWithAssertion()
                }
                step("Assert: browser opened") {
                    val expectedIntent = KIntent {
                        hasAction(ACTION_VIEW)
                        hasData { toString().startsWith(NEW_BUY_WALLET_URL) }
                    }
                    expectedIntent.intended()
                    device.uiDevice.pressBack()
                }
            }
        }
}