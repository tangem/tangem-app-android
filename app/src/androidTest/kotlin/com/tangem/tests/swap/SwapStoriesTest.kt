package com.tangem.tests.swap

import androidx.compose.ui.test.longClick
import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWhenEnabled
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.restartApp
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
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
            }
        ).run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Markets' token details for token '$tokenName'") {
                openMarketsTokenDetailsFromMainScreen(tokenName)
            }
            step("Open 'Swap' screen from 'Markets' token details without stories") {
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
            step("Open 'Markets' token details for token '$tokenName'") {
                openMarketsTokenDetailsFromMainScreen(tokenName)
            }
            step("Click on 'Add funds' button on 'Markets' token details screen") {
                onMarketsTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { addFundsButton.clickWithAssertion() }
                }
            }
            step("Click on 'Swap' action in the bottom sheet") {
                onAddFundsBottomSheet { swapButton.clickWithAssertion() }
            }
            step("Assert 'Stories' screen is displayed") {
                onSwapStoriesScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { closeButton.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
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
    @DisplayName("Check 'Swap' stories on 'Markets' token details screen")
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
            step("Open 'Markets' token details for token '$tokenName'") {
                openMarketsTokenDetailsFromMainScreen(tokenName)
            }
            step("Click on 'Add funds' button on 'Markets' token details screen") {
                onMarketsTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { addFundsButton.clickWithAssertion() }
                }
            }
            step("Click on 'Swap' action in the bottom sheet") {
                onAddFundsBottomSheet { swapButton.clickWithAssertion() }
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
            step("Open 'Swap' screen from 'Markets' token details without stories") {
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

    @AllureId("5453")
    @DisplayName("Swap stories: view stories from 'Main' screen")
    @Test
    fun viewSwapStoriesFromMainScreenTest() {
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
            step("Assert 'Stories' screen is displayed") {
                onSwapStoriesScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { container.assertIsDisplayed() }
                }
            }
            step("View stories to the end") {
                viewSwapStoriesToEnd()
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Assert 'Main' screen is displayed") {
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Open 'Swap' screen without stories") {
                openSwapScreen(from = SwapEntryPoint.MainScreen, storiesExist = false)
            }
        }
    }

    @AllureId("5454")
    @DisplayName("Swap stories: view stories from 'Token details' screen")
    @Test
    fun viewSwapStoriesFromTokenDetailsScreenTest() {
        val tokenName = "Ethereum"
        val storiesEndpoint = "/v1/stories/first-time-swap-v2"
        var storiesRequestsBeforeLaunch = 0

        setupHooks(
            additionalBeforeAppLaunchSection = {
                storiesRequestsBeforeLaunch = getWireMockRequestCount("GET", storiesEndpoint)
            },
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Assert '$storiesEndpoint' is requested on app launch") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    assertTrue(
                        "App launch should request $storiesEndpoint",
                        getWireMockRequestCount("GET", storiesEndpoint) > storiesRequestsBeforeLaunch,
                    )
                }
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                openTokenDetailsFromTokenList(tokenName)
            }
            step("Click on 'Swap' button on 'Token details' screen") {
                onTokenDetailsScreen { swapButton.clickWhenEnabled() }
            }
            step("Assert 'Stories' screen is displayed") {
                onSwapStoriesScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { container.assertIsDisplayed() }
                }
            }
            step("View stories to the end") {
                viewSwapStoriesToEnd()
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapTokenScreen { closeButton.performClick() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Open 'Swap' screen without stories") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false)
            }
        }
    }
}