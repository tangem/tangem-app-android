package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.scenarios.OpenMainScreenScenario
import dagger.hilt.android.testing.HiltAndroidTest
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