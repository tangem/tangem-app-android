package com.tangem.tests.swap

import androidx.compose.ui.test.longClick
import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.assertHasBadge
import com.tangem.common.extensions.restartApp
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
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
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
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
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
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
            step("Open 'Markets' token details screen for token '$tokenName'") {
                openMarketTokenDetailsScreen(blockchainName = tokenName, tokenName = tokenName)
            }
            step("Assert 'Swap' button has badge") {
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertIsDisplayed() }
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertContentDescriptionEquals(badgeShown) }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MarketsTokenDetails)
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' button has not badge") {
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertContentDescriptionEquals(badgeHidden) }
            }
        }
    }

    @AllureId("5469")
    @DisplayName("Check unavailable swap stories on 'Main' screen")
    @Test
    fun checkUnavailableSwapStoriesOnMainScreen() {
        val scenarioName = "stories_first_time_swap"
        val scenarioErrorState = "Error"
        val packageName = getTargetContext().packageName

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Swap' button has badge") {
                onMainScreen { swapButton.assertHasBadge(false) }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen, storiesExist = false)
            }
            step("Reset WireMock scenario state") {
                resetWireMockScenarioState(scenarioName)
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Restart app") {
                restartApp(packageName)
            }
            step("Assert 'Swap' button is displayed") {
                waitForIdle()
                onMainScreen { swapButton.assertIsDisplayed() }
            }
            step("Assert 'Swap' button has badge") {
                onMainScreen { swapButton.assertHasBadge() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen, storiesExist = true)
            }
        }
    }

    @AllureId("5471")
    @DisplayName("Check unavailable swap stories on 'Token details' screen")
    @Test
    fun checkUnavailableSwapStoriesOnTokenDetailsScreen() {
        val scenarioName = "stories_first_time_swap"
        val scenarioErrorState = "Error"
        val packageName = getTargetContext().packageName
        val tokenName = "Ethereum"


        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

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
                onTokenDetailsScreen { swapButton().assertHasBadge(false) }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false)
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Reset WireMock scenario state") {
                resetWireMockScenarioState(scenarioName)
            }
            step("Restart app") {
                restartApp(packageName)
            }
            step("Assert 'Swap' button has badge") {
                waitForIdle()
                onMainScreen { swapButton.assertHasBadge() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = true)
            }
        }
    }

    @AllureId("5471")
    @DisplayName("Check unavailable swap stories on 'Markets' token details screen")
    @Test
    fun checkUnavailableSwapStoriesOnMarketsTokenDetailsScreen() {
        val scenarioName = "stories_first_time_swap"
        val scenarioErrorState = "Error"
        val packageName = getTargetContext().packageName
        val tokenName = "Ethereum"
        val badgeShown = "Badge shown"
        val badgeHidden = "Badge hidden"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Markets' token details screen for token '$tokenName'") {
                openMarketTokenDetailsScreen(blockchainName = tokenName, tokenName = tokenName)
            }
            step("Assert 'Swap' button has badge") {
                waitForIdle()
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertIsDisplayed() }
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertContentDescriptionEquals(badgeHidden) }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MarketsTokenDetails, storiesExist = false)
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Reset WireMock scenario state") {
                resetWireMockScenarioState(scenarioName)
            }
            step("Restart app") {
                restartApp(packageName)
            }
            step("Open 'Markets' token details screen for token '$tokenName'") {
                openMarketTokenDetailsScreen(blockchainName = tokenName, tokenName = tokenName)
            }
            step("Assert 'Swap' button has badge") {
                waitForIdle()
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertIsDisplayed() }
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.assertContentDescriptionEquals(badgeShown) }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MarketsTokenDetails, storiesExist = true)
            }
        }
    }

    @AllureId("5474")
    @DisplayName("Check 'Swap' stories on 'Main' screen")
    @Test
    fun checkSwapStoriesOnMainScreenTest() {

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Swap' button on 'Main' screen") {
                onMainScreen { swapButton.performClick() }
            }
            step("Check stories changes") {
                checkStoriesChanges()
            }
            step("Click on 'Close' button") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Open 'Swap' screen without stories") {
                openSwapScreen(from = SwapEntryPoint.MainScreen, storiesExist = false)
            }
        }
    }

    @AllureId("5475")
    @DisplayName("Check 'Swap' stories on 'Token details' screen")
    @Test
    fun checkSwapStoriesOnTokenDetailsScreenTest() {
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
            step("Click on 'Swap' button on 'Token details' screen") {
                onTokenDetailsScreen { swapButton().performClick() }
            }
            step("Check stories changes") {
                checkStoriesChanges()
            }
            step("Click on 'Close' button") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Open 'Swap' screen without stories") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false)
            }
        }
    }

    @AllureId("5476")
    @DisplayName("Check 'Swap' stories on 'Markets' screen")
    @Test
    fun checkSwapStoriesOnMarketTokenDetailsScreenTest() {
        val tokenName = "Ethereum"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Markets' token details screen for token '$tokenName'") {
                openMarketTokenDetailsScreen(blockchainName = tokenName, tokenName = tokenName)
            }
            step("Click on 'Swap' button on 'Markets' token details screen") {
                onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.performClick() }
            }
            step("Check stories changes") {
                checkStoriesChanges()
            }
            step("Click on 'Close' button") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Open 'Swap' screen without stories") {
                openSwapScreen(from = SwapEntryPoint.MarketsTokenDetails, storiesExist = false)
            }
        }
    }

    @AllureId("5477")
    @DisplayName("Check 'Swap' stories on token actions bottom sheet")
    @Test
    fun checkSwapStoriesOnTokenActionsBottomSheetTest() {
        val tokenName = "Ethereum"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenName'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenName).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Click on 'Swap' button") {
                onTokenActionsBottomSheet { swapButton.performClick() }
            }
            step("Check stories changes") {
                checkStoriesChanges()
            }
            step("Click on 'Close' button") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Open 'Swap' screen without stories") {
                openSwapScreen(from = SwapEntryPoint.TokenActionsBottomSheet, storiesExist = false)
            }
        }
    }
}