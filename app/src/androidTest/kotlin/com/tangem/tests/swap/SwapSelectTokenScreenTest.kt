package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSwapSelectTokenScreen
import com.tangem.screens.onSwapStoriesScreen
import com.tangem.screens.onSwapTokenScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapSelectTokenScreenTest : BaseTestCase() {

    @AllureId("2829")
    @DisplayName("Open 'Swap select token' screen from 'Main' screen")
    @Test
    fun openSwapSelectTokenScreenFromMainScreenTest() {
        val swapTokenName = "Ethereum"
        val receiveTokenName = "Polygon"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Swap' button") {
                onMainScreen { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap select token' screen title is displayed") {
                onSwapSelectTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert 'You swap' title is displayed") {
                onSwapSelectTokenScreen { youSwapTitle.assertIsDisplayed() }
            }
            step("Assert 'You swap' block is displayed") {
                onSwapSelectTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Assert search icon is displayed") {
                onSwapSelectTokenScreen { searchBarIcon.assertIsDisplayed() }
            }
            step("Assert search placeholder is displayed") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.assertIsDisplayed() }
            }
            step("Click on token with name '$swapTokenName'") {
                onSwapSelectTokenScreen { tokenWithName(swapTokenName).performClick() }
            }
            step("Assert 'You receive' title is displayed") {
                onSwapSelectTokenScreen { youReceiveTitle.assertIsDisplayed() }
            }
            step("Assert 'You receive' block is displayed") {
                onSwapSelectTokenScreen { youReceiveBlock.assertIsDisplayed() }
            }
            step("Click on token with name '$receiveTokenName'") {
                onSwapSelectTokenScreen { tokenWithName(receiveTokenName).performClick() }
            }
            step("Assert 'Swap token' screen is opened") {
                onSwapTokenScreen { container.assertIsDisplayed() }
            }
        }
    }
}