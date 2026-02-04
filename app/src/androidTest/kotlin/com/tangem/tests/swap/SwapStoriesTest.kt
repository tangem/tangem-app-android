package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.assertHasBadge
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapStoriesTest : BaseTestCase() {

    @AllureId("5453")
    @DisplayName("Check 'Swap' button badge on 'Main' screen")
    @Test
    fun checkMainScreenSwapButtonBadgeTest() {

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Swap' button has badge") {
                onMainScreen { swapButton.assertHasBadge() }
            }
            step("Click on 'Swap' button") {
                onMainScreen { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' button has not badge") {
                onMainScreen { swapButton.assertHasBadge(false) }
            }
        }
    }

    @AllureId("5454")
    @DisplayName("Check 'Swap' button badge on token details screen")
    @Test
    fun checkTokenDetailsScreenSwapButtonTest() {
        val tokenName = "Ethereum"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).performClick() }
            }
            step("Assert 'Swap' button has badge") {
                onTokenDetailsScreen { swapButton().assertHasBadge() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' button has not badge") {
                onTokenDetailsScreen { swapButton().assertHasBadge(false) }
            }
        }
    }

    @AllureId("5455")
    @DisplayName("Check 'Swap' button badge on token details in 'Market' screen")
    @Test
    fun checkMarketTokenDetailsScreenSwapButtonTest() {
        val tokenName = "Ethereum"
        val badgeShown = "Badge shown"
        val badgeHidden = "Badge hidden"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Markets' screen") {
                onMainScreen { searchThroughMarketPlaceholder.performClick() }
                waitForIdle()
            }
            step("") {
                onMarketsScreen { searchThroughMarketPlaceholder.performClick() }
            }
            step("Click on $tokenName token") {
                waitForIdle()
                onMarketsScreen { tokenWithTitle(tokenName).clickWithAssertion() }
            }
            step("Click on $tokenName token") {
                waitForIdle()
                onMarketsTokenDetailsScreen { tokenWithTitle(tokenName).performClick() }
            }
            step("Assert 'Swap' button has badge") {
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertIsDisplayed() }
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertContentDescriptionEquals(badgeShown) }
            }
            step("Click on 'Swap' button") {
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' button has not badge") {
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertContentDescriptionEquals(badgeHidden) }
            }
        }
    }
}