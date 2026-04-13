package com.tangem.tests.markets

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.openMarketsScreen
import com.tangem.screens.onMarketsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class MarketsTokenListTest : BaseTestCase() {

    private val sortOptions = listOf(
        "Trending" to "Trending",
        "ExperiencedBuyers" to "Experienced buyers",
        "TopGainers" to "Top gainers",
        "TopLosers" to "Top losers",
    )

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("50")
    @DisplayName("Markets: changing the interval refreshes the price-change values")
    fun marketsIntervalChangeUpdatesDataTest() {
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Tap 'See all'") {
                onMarketsScreen { seeAllButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Switch to '1w' interval") {
                onMarketsScreen { intervalSegment("D7").clickWithAssertion() }
                waitForIdle()
            }
            val priceChange7d = readFirstPriceChangeText()
            step("Switch to '1m' interval") {
                onMarketsScreen { intervalSegment("M1").clickWithAssertion() }
                waitForIdle()
            }
            step("Wait for price-change data to update") {
                waitForPriceChangeNotEqualTo(priceChange7d)
            }
            val priceChange30d = readFirstPriceChangeText()
            step("Assert price change differs between intervals") {
                assertNotEquals(
                    "Price change should update after switching to 1m interval",
                    priceChange7d,
                    priceChange30d,
                )
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("55")
    @DisplayName("Markets: tokens have price-change values, switching intervals refreshes them")
    fun marketsPriceChangeDisplayTest() {
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Tap 'See all'") {
                onMarketsScreen { seeAllButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert at least one token has a non-empty price change value") {
                onMarketsScreen {
                    val texts = allPriceChangeTexts().filter { it.isNotBlank() }
                    assertTrue(
                        "Expected at least one non-empty price change, got=${allPriceChangeTexts()}",
                        texts.isNotEmpty(),
                    )
                }
            }
            val initialPriceChange = readFirstPriceChangeText()
            step("Switch to '1w' interval") {
                onMarketsScreen { intervalSegment("D7").clickWithAssertion() }
                waitForIdle()
            }
            step("Wait for price-change data to update") {
                waitForPriceChangeNotEqualTo(initialPriceChange)
            }
            val updatedPriceChange = readFirstPriceChangeText()
            step("Assert price change updates after changing interval") {
                assertNotEquals(
                    "Price change should update after changing interval",
                    initialPriceChange,
                    updatedPriceChange,
                )
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("49")
    @DisplayName("Markets: changing sort order changes the displayed token list")
    fun marketsSortOrderChangeTest() {
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Tap 'See all'") {
                onMarketsScreen { seeAllButton.clickWithAssertion() }
                waitForIdle()
            }
            val initialOrder = readFirstTokenNames(count = 5)
            step("Assert initial token list has at least 3 items") {
                assertTrue(
                    "Should have at least 3 tokens to compare order, got=${initialOrder.size}",
                    initialOrder.size >= 3,
                )
            }
            sortOptions.forEach { (sortId, displayName) ->
                step("Tap sort button & select '$displayName'") {
                    onMarketsScreen { sortButton.clickWithAssertion() }
                    waitForIdle()
                    onMarketsScreen { sortOption(sortId).clickWithAssertion() }
                    waitForIdle()
                }
                val newOrder = readFirstTokenNames(count = 5)
                step("Assert token order changed after selecting '$displayName'") {
                    assertNotEquals(
                        "Token order should change after selecting '$displayName' sort option",
                        initialOrder,
                        newOrder,
                    )
                }
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("53")
    @DisplayName("Markets: token list items have name + price-change populated")
    fun marketsTokenListDisplayTest() {
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Tap 'See all'") {
                onMarketsScreen { seeAllButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert tokens have non-empty names") {
                onMarketsScreen {
                    val names = firstTokenNames(count = 3)
                    assertTrue("Expected at least one visible token name", names.isNotEmpty())
                    names.forEachIndexed { index, name ->
                        assertTrue("Token name at index $index should not be empty", name.isNotBlank())
                    }
                }
            }
            step("Assert tokens have non-empty price-change values") {
                onMarketsScreen {
                    val priceChanges = allPriceChangeTexts().take(3)
                    assertTrue("Expected at least one visible price change", priceChanges.isNotEmpty())
                    priceChanges.forEachIndexed { index, value ->
                        assertTrue(
                            "Price change at index $index should not be empty, got='$value'",
                            value.isNotBlank(),
                        )
                    }
                }
            }
        }
    }

    private fun readFirstPriceChangeText(): String {
        var text: String? = null
        onMarketsScreen { text = firstPriceChangeText() }
        return text.orEmpty()
    }

    private fun readFirstTokenNames(count: Int): List<String> {
        var names: List<String> = emptyList()
        onMarketsScreen { names = firstTokenNames(count = count) }
        return names
    }

    private fun waitForPriceChangeNotEqualTo(previousValue: String) {
        composeTestRule.waitUntil(WAIT_UNTIL_TIMEOUT_LONG) {
            val current = runCatching { readFirstPriceChangeText() }.getOrNull().orEmpty()
            current.isNotBlank() && current != previousValue
        }
    }
}