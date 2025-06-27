package com.tangem.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tangem.common.BaseTestCase
import com.tangem.scenarios.OpenMainScreenScenario
import com.tangem.tap.MainActivity
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainScreenTest : BaseTestCase() {

    @Test
    fun goToMain() {
        setupHooks().run {
            scenario(OpenMainScreenScenario(composeTestRule))
        }
    }

}