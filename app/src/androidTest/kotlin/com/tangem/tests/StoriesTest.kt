package com.tangem.tests

import android.content.Intent.ACTION_VIEW
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import com.atiurin.ultron.allure.step.step
import com.atiurin.ultron.core.config.UltronConfig.UiAutomator.Companion.uiDevice
import com.atiurin.ultron.extensions.click
import com.tangem.common.BaseTestCase
import com.tangem.screens.DisclaimerPage
import com.tangem.screens.StoriesPage
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.core.AllOf.allOf
import org.junit.Test

@HiltAndroidTest
class StoriesTest : BaseTestCase() {

    @Test
    fun checkOrderButton() {
        Intents.init()
        step("Click Accept on ToS") {
            DisclaimerPage.acceptButton.click()
        }
        step("Click order button ") {
            StoriesPage.orderButton.click()
        }
        step("Assert: browser is opened ") {
            Intents.intended(allOf(hasAction(ACTION_VIEW), hasData(NEW_BUY_WALLET_URL)))
            uiDevice.pressBack()
            Intents.release()
        }
    }
}