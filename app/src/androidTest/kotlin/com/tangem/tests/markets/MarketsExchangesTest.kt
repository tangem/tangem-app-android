package com.tangem.tests.markets

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.extensions.*
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.assertMarketsExchangesScreen
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openMarketsExchangesScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMarketsExchangesScreen
import com.tangem.screens.onMarketsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MarketsExchangesTest : BaseTestCase() {

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("58")
    @DisplayName("Markets: verify exchanges list screen")
    fun marketsExchangesListTest() {
        val tokenName = "Solana"
        setupHooks().run {
            step("Open 'Markets Exhanges Screen with token: $tokenName'") {
                openMarketsExchangesScreen(tokenName = tokenName, shouldClickSeeAllButton = true)
            }
            step("Assert 'Exchanges' list screen is displayed") {
                assertMarketsExchangesScreen()
            }
        }
    }

    @Test
    @AllureId("56")
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @DisplayName("Markets: verify exchanges block is displayed in token details")
    fun marketsExchangesBlockDisplayedTest() {
        val tokenName = "Bitcoin"
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Markets' screen") {
                swipeMarketsBlock(SwipeDirection.UP)
                waitForIdle()
            }
            step("Click on '$tokenName' token") {
                onMarketsScreen { tokenWithTitle(tokenName).clickWithAssertion() }
                waitForIdle()
            }
            step("Scroll down") {
                swipeVertical(SwipeDirection.UP)
                swipeVertical(SwipeDirection.UP)
            }
            step("Assert 'Listed on exchanges' block has title") {
                onMarketsScreen { listedOnBlockContainer.assertIsDisplayed() }
            }
            step("Assert 'Listed on exchanges' block has exchanges count") {
                onMarketsScreen { listedOnExchangesCount.assertIsDisplayed() }
            }
            step("Assert 'Listed on exchanges' block has arrow button") {
                onMarketsScreen { listedOnBlockContainer.assertHasClickAction() }
            }
            step("Tap on 'Listed on exchanges' block and navigate to exchanges list") {
                onMarketsScreen { listedOnBlockContainer.performClick() }
                waitForIdle()
            }
            step("Assert 'Exchanges' list screen is displayed") {
                assertMarketsExchangesScreen()
            }
        }
    }

    @Test
    @AllureId("60")
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @DisplayName("Markets: verify exchanges list is sorted by volume descending")
    fun marketsExchangesListSortedByVolumeTest() {
        val tokenName = "Bitcoin"
        setupHooks().run {
            step("Open 'Markets Exhanges Screen with token: $tokenName'") {
                openMarketsExchangesScreen(tokenName)
            }
            step("Assert exchanges are sorted by trading volume in descending order") {
                flakySafely {
                    onMarketsExchangesScreen { allTradingVolumeNodes().assertSortedByVolumeDescending() }
                }
            }
        }
    }

    @Test
    @AllureId("61")
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @DisplayName("Markets: verify exchange types are CEX or DEX")
    fun marketsExchangesTypeTest() {
        val tokenName = "Bitcoin"
        setupHooks().run {
            step("Open 'Markets Exhanges Screen with token: $tokenName'") {
                openMarketsExchangesScreen(tokenName)
            }
            step("Assert exchange types list is not empty and all types are 'CEX' or 'DEX'") {
                flakySafely {
                    onMarketsExchangesScreen { allExchangeTypeNodes().assertExchangeTypesAreCexOrDex() }
                }
            }
        }
    }

    @Test
    @AllureId("62")
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @DisplayName("Markets: verify exchange trust scores are valid")
    fun marketsExchangesTrustScoreTest() {
        val tokenName = "Bitcoin"
        setupHooks().run {
            step("Open 'Markets Exhanges Screen with token: $tokenName'") {
                openMarketsExchangesScreen(tokenName)
            }
            step("Assert trust scores list is not empty and all scores are valid") {
                flakySafely {
                    onMarketsExchangesScreen { allTrustScoreNodes().assertTrustScoresValid() }
                }
            }
        }
    }

    @Test
    @AllureId("57")
    @DisplayName("Markets: verify empty exchanges state")
    fun marketsExchangesEmptyTest() {
        val tokenName = "Bitcoin"
        val scenarioName = "coins_bitcoin"
        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(scenarioName = scenarioName, state = "EmptyExchanges")
            },
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            },
        ).run {
            step("Open 'Markets Exhanges Screen with token: $tokenName'") {
                openMarketsExchangesScreen(tokenName)
            }
            step("Assert 'Listed on exchanges' block shows no exchanges") {
                onMarketsScreen { listedOnEmptyText.assertIsDisplayed() }
            }
        }
    }

    @Test
    @AllureId("59")
    @DisplayName("Markets: verify exchanges error state and retry")
    fun marketsExchangesErrorStateTest() {
        val tokenName = "Bitcoin"
        val scenarioName = "bitcoin_exchange"
        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(scenarioName = scenarioName, state = "Unreachable")
            },
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            },
        ).run {
            step("Open 'Markets Exhanges Screen with token: $tokenName'") {
                openMarketsExchangesScreen(tokenName)
            }
            step("Assert 'Unable to load data' error state is displayed") {
                step("Assert 'Error message' is displayed") {
                    onMarketsExchangesScreen { errorMessage.assertIsDisplayed() }
                }
                step("Assert 'Try again button' is displayed") {
                    onMarketsExchangesScreen { tryAgainButton.assertIsDisplayed() }
                }

            }
            step("Set WireMock scenario '$scenarioName' to state 'Started'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = "Started")
            }
            step("Tap 'Try again' button") {
                onMarketsExchangesScreen { tryAgainButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert 'Exchanges' list screen is displayed after retry") {
                assertMarketsExchangesScreen()
            }
        }
    }
}