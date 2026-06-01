package com.tangem.tests.swap

import androidx.compose.ui.test.longClick
import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
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

    @AllureId("5469")
    @DisplayName("Check unavailable swap stories on 'Main' screen")
    @Test
    fun checkUnavailableSwapStoriesOnMainScreen() {
        val scenarioName = "stories_first_time_swap_v2"
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
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen, storiesExist = true)
            }
        }
    }

    @AllureId("5471")
    @DisplayName("Check unavailable swap stories on 'Token details' screen")
    @Test
    fun checkUnavailableSwapStoriesOnTokenDetailsScreen() {
        val scenarioName = "stories_first_time_swap_v2"
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
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = true)
            }
        }
    }

    @AllureId("5470")
    @DisplayName("Check unavailable swap stories on 'Markets' token details screen")
    @Test
    fun checkUnavailableSwapStoriesOnMarketsTokenDetailsScreen() {
        val scenarioName = "stories_first_time_swap_v2"
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
            step("Open 'Token details' from 'Markets' screen for token '$tokenName'") {
                openTokenDetailsFromMarketsScreen(blockchainName = tokenName, tokenName = tokenName)
            }
            step("Assert 'Swap' button is displayed") {
                waitForIdle()
                onTokenDetailsScreen { swapButton.assertIsDisplayed() }
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
            step("Assert 'Swap' button is displayed") {
                waitForIdle()
                onTokenDetailsScreen { swapButton.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = true)
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
            step("Click on 'Swap' button on 'Token details' screen") {
                onTokenDetailsScreen { swapButton.performClick() }
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
            step("Open 'Token details' from 'Markets' screen for token '$tokenName'") {
                openTokenDetailsFromMarketsScreen(blockchainName = tokenName, tokenName = tokenName)
            }
            step("Click on 'Swap' button on 'Markets' token details screen") {
                onTokenDetailsScreen { swapButton.performClick() }
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
            step("Long click on token with name: '$tokenName' again to reopen actions menu") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenName).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L,
                        )
                    }
                }
            }
            step("Open 'Swap' screen without stories") {
                openSwapScreen(from = SwapEntryPoint.TokenActionsBottomSheet, storiesExist = false)
            }
        }
    }
}