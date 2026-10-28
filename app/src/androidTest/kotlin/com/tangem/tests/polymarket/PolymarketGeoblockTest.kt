package com.tangem.tests.polymarket

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.scenarios.POLYMARKET_AB_TEST_ON
import com.tangem.scenarios.POLYMARKET_AB_TEST_PROPERTY
import com.tangem.scenarios.POLYMARKET_BRIDGE_DEPOSIT_PATH
import com.tangem.scenarios.POLYMARKET_BRIDGE_SCENARIO
import com.tangem.scenarios.POLYMARKET_DEPLOY_PATH
import com.tangem.scenarios.POLYMARKET_FEED_SCENARIO
import com.tangem.scenarios.POLYMARKET_GEOBLOCK_PATH
import com.tangem.scenarios.POLYMARKET_GEOBLOCK_SCENARIO
import com.tangem.scenarios.POLYMARKET_WALLET_SCENARIO
import com.tangem.scenarios.awaitPolymarketMainScreen
import com.tangem.scenarios.clickPolymarketOnboardingActionButton
import com.tangem.scenarios.openMainScreenWithPolymarketHotWallet
import com.tangem.scenarios.openPolymarketFromMarketsBanner
import com.tangem.screens.onPolymarketHistoryScreen
import com.tangem.screens.onPolymarketMainScreen
import com.tangem.screens.onPolymarketOnboardingScreen
import com.tangem.screens.onPolymarketPlacePredictionScreen
import com.tangem.screens.onPolymarketPositionDetailsScreen
import com.tangem.screens.onPolymarketWithdrawScreen
import com.tangem.screens.onSwapStoriesScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class PolymarketGeoblockTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.AND_16204_POLYMARKET_ENABLED.rawName to true,
    )

    @Test
    @Ignore("[REDACTED_JIRA]")
    @AllureId("12903")
    @DisplayName("Polymarket: geoblock. Region restrictions are shown instead of onboarding for a new account")
    fun regionBlockedNewAccountTest() {
        val blockedState = "Blocked"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)

            step("Set WireMock scenario '$POLYMARKET_GEOBLOCK_SCENARIO' to '$blockedState'") {
                setWireMockScenarioState(POLYMARKET_GEOBLOCK_SCENARIO, blockedState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert 'Region restrictions' sheet is displayed") {
                flakySafely {
                    onPolymarketOnboardingScreen { regionRestrictionsSheet.assertIsDisplayed() }
                }
            }
            step("Assert onboarding 'Welcome' screen is not displayed") {
                onPolymarketOnboardingScreen { welcomeScreen.assertDoesNotExist() }
            }
            step("Assert the account was not deployed") {
                assertEquals(deployRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH))
            }
        }
    }

    @Test
    @AllureId("13356")
    @DisplayName("Polymarket: geoblock. Ready account without L2 credentials reconnects, then sees Region restrictions")
    fun regionBlockedReadyAccountReconnectsTest() {
        val blockedState = "Blocked"
        val walletState = "ReadyToTrade"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            step("Set WireMock scenario '$POLYMARKET_GEOBLOCK_SCENARIO' to '$blockedState'") {
                setWireMockScenarioState(POLYMARKET_GEOBLOCK_SCENARIO, blockedState)
            }
            step("Set WireMock scenario '$POLYMARKET_WALLET_SCENARIO' to '$walletState'") {
                setWireMockScenarioState(POLYMARKET_WALLET_SCENARIO, walletState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Reconnect' screen is displayed") {
                flakySafely { onPolymarketOnboardingScreen { reconnectScreen.assertIsDisplayed() } }
            }
            step("Assert 'Region restrictions' sheet is not displayed") {
                onPolymarketOnboardingScreen { regionRestrictionsSheet.assertDoesNotExist() }
            }
            step("Click on 'Continue predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert 'Region restrictions' banner is displayed") {
                flakySafely { onPolymarketMainScreen { regionRestrictedBanner.assertIsDisplayed() } }
            }
            step("Assert 'Region restrictions' banner title is displayed") {
                flakySafely { onPolymarketMainScreen { regionRestrictedBannerTitle.assertIsDisplayed() } }
            }
            step("Assert 'Region restrictions' banner subtitle is displayed") {
                flakySafely { onPolymarketMainScreen { regionRestrictedBannerSubtitle.assertIsDisplayed() } }
            }
        }
    }

    @Test
    @AllureId("12929")
    @DisplayName("Polymarket: geoblock. No restrictions open the account with full access")
    fun fullAccessTest() {
        val feedState = "FeedLoaded"
        val bridgeState = "Available"
        val userTokensState = "PolygonUSDCHotWallet"
        val quotesState = "PolygonUSDC"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val geoblockRequestsBefore = getWireMockRequestCount("GET", POLYMARKET_GEOBLOCK_PATH)
            val depositRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_BRIDGE_DEPOSIT_PATH)

            step("Set WireMock scenario '$POLYMARKET_FEED_SCENARIO' to '$feedState'") {
                setWireMockScenarioState(POLYMARKET_FEED_SCENARIO, feedState)
            }
            step("Set WireMock scenario '$POLYMARKET_BRIDGE_SCENARIO' to '$bridgeState'") {
                setWireMockScenarioState(POLYMARKET_BRIDGE_SCENARIO, bridgeState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(QUOTES_API_SCENARIO, quotesState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Click on 'Start predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert the region was checked") {
                flakySafely {
                    assertTrue(getWireMockRequestCount("GET", POLYMARKET_GEOBLOCK_PATH) > geoblockRequestsBefore)
                }
            }
            step("Assert 'Region restrictions' banner is not displayed") {
                onPolymarketMainScreen { regionRestrictedBanner.assertDoesNotExist() }
            }
            step("Assert available balance is displayed") {
                flakySafely { onPolymarketMainScreen { availableBalance.assertIsDisplayed() } }
            }
            step("Assert 'Withdraw' button is enabled") {
                flakySafely { onPolymarketMainScreen { withdrawButton.assertIsEnabled() } }
            }
            step("Assert active positions are displayed") {
                flakySafely { onPolymarketMainScreen { viewAllActivePositionsButton.assertIsDisplayed() } }
            }
            step("Click on 'More' button") {
                onPolymarketMainScreen { moreButton.clickWithAssertion() }
            }
            step("Click on 'History' menu item") {
                flakySafely { onPolymarketMainScreen { historyMenuItem.clickWithAssertion() } }
            }
            step("Assert 'History' screen is displayed") {
                flakySafely { onPolymarketHistoryScreen { screen.assertIsDisplayed() } }
            }
            step("Go back to 'Polymarket main' screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Polymarket main' screen is displayed after the history") {
                flakySafely { onPolymarketMainScreen { screen.assertIsDisplayed() } }
            }
            step("Assert the funding requisites were requested") {
                flakySafely {
                    assertTrue(
                        getWireMockRequestCount("POST", POLYMARKET_BRIDGE_DEPOSIT_PATH) > depositRequestsBefore,
                    )
                }
            }
            step("Click on 'Add funds' button") {
                onPolymarketMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Assert 'Swap' option of the 'Add funds' sheet is displayed") {
                flakySafely { onPolymarketMainScreen { addFundsSwapOption.assertIsDisplayed() } }
            }
            step("Assert 'Receive' option of the 'Add funds' sheet is displayed") {
                flakySafely { onPolymarketMainScreen { addFundsReceiveOption.assertIsDisplayed() } }
            }
            step("Go back to 'Polymarket main' screen from the 'Add funds' sheet") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Polymarket main' screen is displayed after the 'Add funds' sheet") {
                flakySafely { onPolymarketMainScreen { screen.assertIsDisplayed() } }
            }
            step("Click on 'Withdraw' button") {
                onPolymarketMainScreen { withdrawButton.clickWithAssertion() }
            }
            step("Click on 'Close' button of the stories") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapStoriesScreen { closeButton.clickWithAssertion() } }
            }
            step("Assert 'Withdraw' swap screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onPolymarketWithdrawScreen { sendFromTitle.assertIsDisplayed() } }
            }
            step("Go back to 'Polymarket main' screen from the 'Withdraw' screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Polymarket main' screen is displayed after the 'Withdraw' screen") {
                flakySafely { onPolymarketMainScreen { screen.assertIsDisplayed() } }
            }
            step("Click on the first position") {
                flakySafely { onPolymarketMainScreen { firstPositionCard.clickWithAssertion() } }
            }
            step("Assert 'Buy' button of the position is enabled") {
                flakySafely { onPolymarketPositionDetailsScreen { buyButton.assertIsEnabled() } }
            }
            step("Click on 'Buy' button of the position") {
                onPolymarketPositionDetailsScreen { buyButton.clickWithAssertion() }
            }
            step("Assert 'Place prediction' screen is displayed for 'Buy'") {
                flakySafely { onPolymarketPlacePredictionScreen { screen.assertIsDisplayed() } }
            }
            step("Go back to the position screen from 'Buy'") {
                device.uiDevice.pressBack()
            }
            step("Click on 'Close' button of the position") {
                flakySafely { onPolymarketPositionDetailsScreen { closePositionButton.clickWithAssertion() } }
            }
            step("Assert 'Place prediction' screen is displayed for 'Close'") {
                flakySafely { onPolymarketPlacePredictionScreen { screen.assertIsDisplayed() } }
            }
            step("Go back to the position screen from 'Close'") {
                device.uiDevice.pressBack()
            }
            step("Go back to 'Polymarket main' screen from the position screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Polymarket main' screen is displayed after the position") {
                flakySafely { onPolymarketMainScreen { screen.assertIsDisplayed() } }
            }
            step("Click on the redeemable position") {
                flakySafely { onPolymarketMainScreen { redeemablePosition.clickWithAssertion() } }
            }
            step("Assert 'Claim reward' button is displayed") {
                flakySafely { onPolymarketMainScreen { claimRewardButton.assertIsDisplayed() } }
            }
            step("Go back to 'Polymarket main' screen from the redeemable position") {
                device.uiDevice.pressBack()
            }
            step("Scroll to the first outcome button") {
                flakySafely { onPolymarketMainScreen { scrollToFirstOutcomeButton() } }
            }
            step("Click on the first outcome button") {
                flakySafely { onPolymarketMainScreen { firstOutcomeButton.clickWithAssertion() } }
            }
            step("Assert 'Place prediction' screen is displayed") {
                flakySafely { onPolymarketPlacePredictionScreen { screen.assertIsDisplayed() } }
            }
        }
    }

    @Test
    @AllureId("12930")
    @DisplayName("Polymarket: geoblock. Restricted region opens the account in read-only mode")
    fun travelerReadOnlyTest() {
        val blockedState = "Blocked"
        val feedState = "FeedLoaded"
        val bridgeState = "Available"
        val userTokensState = "PolygonUSDCHotWallet"
        val quotesState = "PolygonUSDC"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            step("Set WireMock scenario '$POLYMARKET_FEED_SCENARIO' to '$feedState'") {
                setWireMockScenarioState(POLYMARKET_FEED_SCENARIO, feedState)
            }
            step("Set WireMock scenario '$POLYMARKET_BRIDGE_SCENARIO' to '$bridgeState'") {
                setWireMockScenarioState(POLYMARKET_BRIDGE_SCENARIO, bridgeState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(QUOTES_API_SCENARIO, quotesState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Click on 'Start predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Go back to 'Main' screen") {
                device.uiDevice.pressBack()
            }
            step("Set WireMock scenario '$POLYMARKET_GEOBLOCK_SCENARIO' to '$blockedState'") {
                setWireMockScenarioState(POLYMARKET_GEOBLOCK_SCENARIO, blockedState)
            }
            step("Open Polymarket from the 'Predictions' banner again") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert 'Region restrictions' banner is displayed") {
                flakySafely { onPolymarketMainScreen { regionRestrictedBanner.assertIsDisplayed() } }
            }
            step("Assert 'Region restrictions' banner title is displayed") {
                flakySafely { onPolymarketMainScreen { regionRestrictedBannerTitle.assertIsDisplayed() } }
            }
            step("Assert 'Region restrictions' banner subtitle is displayed") {
                flakySafely { onPolymarketMainScreen { regionRestrictedBannerSubtitle.assertIsDisplayed() } }
            }
            step("Assert available balance label is displayed") {
                flakySafely { onPolymarketMainScreen { availableBalanceLabel.assertIsDisplayed() } }
            }
            step("Assert available balance is displayed") {
                flakySafely { onPolymarketMainScreen { availableBalance.assertIsDisplayed() } }
            }
            step("Assert 'Withdraw' button is enabled") {
                flakySafely { onPolymarketMainScreen { withdrawButton.assertIsEnabled() } }
            }
            step("Assert active positions are displayed") {
                flakySafely { onPolymarketMainScreen { viewAllActivePositionsButton.assertIsDisplayed() } }
            }
            step("Scroll to the first outcome button") {
                flakySafely { onPolymarketMainScreen { scrollToFirstOutcomeButton() } }
            }
            step("Assert the first outcome button is displayed") {
                flakySafely { onPolymarketMainScreen { firstOutcomeButton.assertIsDisplayed() } }
            }
            step("Click on 'More' button") {
                onPolymarketMainScreen { moreButton.clickWithAssertion() }
            }
            step("Click on 'History' menu item") {
                flakySafely { onPolymarketMainScreen { historyMenuItem.clickWithAssertion() } }
            }
            step("Assert 'History' screen is displayed") {
                flakySafely { onPolymarketHistoryScreen { screen.assertIsDisplayed() } }
            }
            step("Go back to 'Polymarket main' screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Polymarket main' screen is displayed after the history") {
                flakySafely { onPolymarketMainScreen { screen.assertIsDisplayed() } }
            }
            val geoblockRequestsBeforeClick = getWireMockRequestCount("GET", POLYMARKET_GEOBLOCK_PATH)
            step("Click on 'Add funds' button") {
                onPolymarketMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Assert 'Region restrictions' message is displayed") {
                flakySafely { onPolymarketMainScreen { regionRestrictionsModalTitle.assertIsDisplayed() } }
            }
            step("Click on 'Close' button of the message") {
                onPolymarketMainScreen { regionRestrictionsModalClose.clickWithAssertion() }
            }
            step("Assert 'Polymarket main' screen is displayed after the message is closed") {
                flakySafely { onPolymarketMainScreen { screen.assertIsDisplayed() } }
            }
            step("Assert 'Swap' option of the 'Add funds' sheet is not displayed") {
                onPolymarketMainScreen { addFundsSwapOption.assertDoesNotExist() }
            }
            step("Click on the first position") {
                flakySafely { onPolymarketMainScreen { firstPositionCard.clickWithAssertion() } }
            }
            step("Assert 'Buy' button of the position is enabled") {
                flakySafely { onPolymarketPositionDetailsScreen { buyButton.assertIsEnabled() } }
            }
            step("Click on 'Buy' button of the position") {
                onPolymarketPositionDetailsScreen { buyButton.clickWithAssertion() }
            }
            step("Assert 'Region restrictions' message is displayed for 'Buy'") {
                flakySafely { onPolymarketMainScreen { regionRestrictionsModalTitle.assertIsDisplayed() } }
            }
            step("Click on 'Close' button of the message for 'Buy'") {
                onPolymarketMainScreen { regionRestrictionsModalClose.clickWithAssertion() }
            }
            step("Assert 'Place prediction' screen is not displayed after 'Buy'") {
                flakySafely { onPolymarketPositionDetailsScreen { buyButton.assertIsDisplayed() } }
                onPolymarketPlacePredictionScreen { screen.assertDoesNotExist() }
            }
            step("Click on 'Close' button of the position") {
                onPolymarketPositionDetailsScreen { closePositionButton.clickWithAssertion() }
            }
            step("Assert 'Region restrictions' message is displayed for 'Close'") {
                flakySafely { onPolymarketMainScreen { regionRestrictionsModalTitle.assertIsDisplayed() } }
            }
            step("Click on 'Close' button of the message for 'Close'") {
                onPolymarketMainScreen { regionRestrictionsModalClose.clickWithAssertion() }
            }
            step("Assert 'Place prediction' screen is not displayed after 'Close'") {
                flakySafely { onPolymarketPositionDetailsScreen { buyButton.assertIsDisplayed() } }
                onPolymarketPlacePredictionScreen { screen.assertDoesNotExist() }
            }
            step("Go back to 'Polymarket main' screen from the position screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Polymarket main' screen is displayed after the position") {
                flakySafely { onPolymarketMainScreen { screen.assertIsDisplayed() } }
            }
            step("Assert no new geoblock request was sent") {
                assertEquals(geoblockRequestsBeforeClick, getWireMockRequestCount("GET", POLYMARKET_GEOBLOCK_PATH))
            }
            step("Click on 'Withdraw' button") {
                onPolymarketMainScreen { withdrawButton.clickWithAssertion() }
            }
            step("Click on 'Close' button of the stories") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapStoriesScreen { closeButton.clickWithAssertion() } }
            }
            step("Assert 'Withdraw' swap screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onPolymarketWithdrawScreen { sendFromTitle.assertIsDisplayed() } }
            }
            step("Assert 'Region restrictions' message is not displayed for 'Withdraw'") {
                onPolymarketMainScreen { regionRestrictionsModalTitle.assertDoesNotExist() }
            }
            step("Go back to 'Polymarket main' screen from the 'Withdraw' screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Polymarket main' screen is displayed after the 'Withdraw' screen") {
                flakySafely { onPolymarketMainScreen { screen.assertIsDisplayed() } }
            }
            step("Click on the redeemable position") {
                flakySafely { onPolymarketMainScreen { redeemablePosition.clickWithAssertion() } }
            }
            step("Click on 'Claim reward' button") {
                flakySafely { onPolymarketMainScreen { claimRewardButton.clickWithAssertion() } }
            }
            step("Assert 'Region restrictions' message is not displayed") {
                onPolymarketMainScreen { regionRestrictionsModalTitle.assertDoesNotExist() }
            }
        }
    }
}