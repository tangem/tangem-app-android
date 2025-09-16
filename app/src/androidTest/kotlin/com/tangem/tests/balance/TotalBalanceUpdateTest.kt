package com.tangem.tests.balance

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.extensions.*
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TotalBalanceUpdateTest : BaseTestCase() {

    @Test
    @AllureId("150")
    @DisplayName("Total balance: check balance update after pull to refresh")
    fun afterPullToRefreshTest() {
        val scenarioName = "eth_network_balance"
        val scenarioState = "Empty"
        val updatedBalance = "$763.55"
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(TOTAL_BALANCE)
            }
            step("Assert $TOTAL_BALANCE is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(TOTAL_BALANCE) }
            }
            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }
            step("Perform pull to refresh") {
                pullToRefresh()
            }
            step("Assert Total balance is updated from $TOTAL_BALANCE to $updatedBalance") {
                onMainScreen { totalBalanceText.assertTextContains(updatedBalance) }
            }
        }
    }

    @Test
    @AllureId("3997")
    @DisplayName("Total balance: check balance update after token added")
    fun afterAddTokenTest() {
        val tokenTitle = "XRP"
        val scenarioName = "quotes_api"
        val scenarioState = "Ripple"
        val updatedBalance = "$3,307.18"
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(TOTAL_BALANCE)
            }
            step("Assert $TOTAL_BALANCE is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(TOTAL_BALANCE) }
            }
            step("Open 'Markets screen'") {
                swipeMarketsBlock(SwipeDirection.UP)
                composeTestRule.waitForIdle()
            }
            step("Click on $tokenTitle token") {
                onMarketsScreen { tokenWithTitle(tokenTitle).clickWithAssertion() }
            }
            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }
            step("Click on 'Add to portfolio' button") {
                onMarketsScreen { addToPortfolioButton.clickWithAssertion() }
            }
            step("Toggle the main network switch") {
                onMarketsScreen { mainNetworkSwitch.performClick() }
            }
            step("Click on 'Continue' button") {
                onDialog { continueButton.clickWithAssertion() }
            }
            step("Assert 'Continue' is not displayed") {
                onDialog { continueButton.assertIsNotDisplayed() }
            }
            step("Go back to 'Markets: tokens list'") {
                composeTestRule.waitForIdle()
                onMarketsScreen { topBarBackButton.clickWithAssertion() }
            }
            step("Close 'Markets screen'") {
                onSearchBar { searchField.assertIsDisplayed() }
                swipeMarketsBlock(SwipeDirection.DOWN)
            }
            step("Assert $updatedBalance is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(updatedBalance) }
            }
        }
    }

    @Test
    @AllureId("4000")
    @DisplayName("Total balance: check balance update after collapse/expand")
    fun afterCollapseAndExpandTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(TOTAL_BALANCE)
            }
            step("Assert $TOTAL_BALANCE is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(TOTAL_BALANCE) }
            }
            step("Press 'Home' to collapse the app") {
                device.uiDevice.pressHome()
            }
            step("Open the app from recent apps") {
                openTheAppFromRecents()
            }
            step("Assert $TOTAL_BALANCE is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(TOTAL_BALANCE) }
            }
        }
    }

    @Test
    @AllureId("3998")
    @DisplayName("Total balance: check balance update after hide token")
    fun afterHideTokenTest() {
        val tokenTitle = "Polygon"
        val updatedBalance = "$3,114.34"
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(TOTAL_BALANCE)
            }
            step("Assert $TOTAL_BALANCE is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(TOTAL_BALANCE) }
            }
            step("Long click on token with name: '$tokenTitle'") {
                composeTestRule.waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Click 'Hide token' button") {
                onBottomSheet { hideButton.clickWithAssertion() }
            }
            step("Click 'Hide' button in dialog") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    okButton.clickWithAssertion()
                }
            }
            step("Assert $updatedBalance is displayed in total balance") {
                swipeVertical(SwipeDirection.DOWN)
                onMainScreen { totalBalanceText.assertTextContains(updatedBalance) }
            }
        }
    }

    @Test
    @AllureId("4001")
    @DisplayName("Total balance: check balance update after navigation")
    fun afterNavigationTest() {
        val tokenTitle = "Polygon"
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(TOTAL_BALANCE)
            }
            step("Assert $TOTAL_BALANCE is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(TOTAL_BALANCE) }
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Click 'More' button") {
                onTokenDetailsTopBar { backButton.clickWithAssertion() }
            }
            step("Assert $TOTAL_BALANCE is displayed in total balance") {
                onMainScreen { totalBalanceText.assertTextContains(TOTAL_BALANCE) }
            }
        }
    }
}