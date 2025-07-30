package com.tangem.tests

import android.content.Intent.ACTION_VIEW
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.DisclaimerTestScreen
import com.tangem.screens.StoriesTestScreen
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.intent.KIntent

@HiltAndroidTest
class StoriesTest : BaseTestCase() {

    // @Test
    fun clickOnOrderButtonTest() =
        setupHooks().run {
            onDisclaimerScreen {
                step("Click on 'Accept' button") {
                    acceptButton.clickWithAssertion()
                }
            }
            onStoriesScreen {
                step("Click on 'Order' button") {
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