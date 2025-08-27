package com.tangem.tests

import android.content.Intent.ACTION_VIEW
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onDisclaimerScreen
import com.tangem.screens.onStoriesScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.intent.KIntent

@HiltAndroidTest
class StoriesTest : BaseTestCase() {

    // @Test
    fun clickOnOrderButtonTest() =
        setupHooks().run {
            val buyWalletUrl = "https://buy.tangem.com/"
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
                        hasData { toString().startsWith(buyWalletUrl) }
                    }
                    expectedIntent.intended()
                    device.uiDevice.pressBack()
                }
            }
        }
}