package com.tangem.tests.markets

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.openMarketsScreen
import com.tangem.scenarios.searchInMarkets
import com.tangem.screens.MarketsExtractors
import com.tangem.screens.onMarketsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class MarketsSearchTest : BaseTestCase() {

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("37")
    @DisplayName("Markets: search by ticker returns matching results")
    fun marketsSearchByTickerTest() {
        val ticker = "BTC"
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Search for '$ticker'") { searchInMarkets(ticker) }
            step("Assert at least one result is shown and contains '$ticker' / 'Bitcoin'") {
                assertVisibleResultsContain(substring = ticker, fallback = "Bitcoin")
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("36")
    @DisplayName("Markets: search by token name returns matching results")
    fun marketsSearchByTokenNameTest() {
        val query = "Wrapped"
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Search for '$query'") { searchInMarkets(query) }
            step("Assert all visible results contain '$query'") {
                assertVisibleResultsContain(substring = query)
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("40")
    @DisplayName("Markets: 'tokens under cap' section is collapsed and expandable")
    fun marketsSearchTokensUnderCapTest() {
        val query = "kok"
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Search for '$query'") { searchInMarkets(query) }
            step("Tap 'Show tokens under cap' button") {
                onMarketsScreen { showTokensUnderCapButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert all visible results contain '$query'") {
                assertVisibleResultsContain(substring = query)
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("41")
    @DisplayName("Markets: 'no results' state is displayed for unknown query")
    fun marketsSearchNoResultsStateTest() {
        val query = "rublo"
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Search for '$query'") { searchInMarkets(query) }
            step("Assert 'No results' state is displayed") {
                onMarketsScreen { noResultsLabel.assertIsDisplayed() }
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("39")
    @DisplayName("Markets: default list shows top-cap coins in expected order")
    fun marketsDefaultListFirstCoinsOrderTest() {
        val expected = listOf(
            "Bitcoin",
            "Ethereum",
            "Tether",
            "XRP",
            "BNB",
            "Solana",
            "USDC",
            "TRON",
            "Dogecoin",
            "Lido Staked Ether",
        )
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Tap 'See all' button") {
                onMarketsScreen { seeAllButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert first ${expected.size} coins match expected order") {
                onMarketsScreen {
                    val actual = firstTokenNames(count = expected.size)
                    assertTrue(
                        "First coins order mismatch. Expected=$expected, actual=$actual",
                        actual == expected,
                    )
                }
            }
        }
    }

    @Test
    @AllureId("38")
    @DisplayName("Markets: search by smart contract address returns matching token (Tether)")
    fun marketsSearchBySmartContractAddressTest() {
        val contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7"
        val expectedToken = "Tether"
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Search by contract address") { searchInMarkets(contractAddress) }
            step("Assert '$expectedToken' is present in search results") {
                onMarketsScreen { tokenItemNameByText(expectedToken).assertIsDisplayed() }
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("35")
    @DisplayName("Markets: search field type/delete/clear actions")
    fun marketsSearchTypeDeleteClearTest() {
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Tap 'Search' field") {
                onMarketsScreen { searchField.performClick() }
                waitForIdle()
            }
            step("Type 'b'") {
                onMarketsScreen { searchField.performTextInput("b") }
                waitForIdle()
            }
            step("Assert visible results contain 'b'") {
                assertVisibleResultsContain(substring = "b")
            }
            step("Type 'tc'") {
                onMarketsScreen { searchField.performTextInput("tc") }
                waitForIdle()
            }
            step("Assert visible results contain 'BTC' (or Bitcoin)") {
                assertVisibleResultsContain(substring = "BTC", fallback = "Bitcoin")
            }
            step("Delete 1 character (text now: 'bt')") {
                onMarketsScreen { searchField.performTextReplacement("bt") }
                waitForIdle()
            }
            step("Assert visible results contain 'bt'") {
                assertVisibleResultsContain(substring = "bt")
            }
            step("Clear search field") {
                onMarketsScreen { searchClearButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert clear button is no longer displayed (search field is empty)") {
                onMarketsScreen { searchClearButton.assertDoesNotExist() }
            }
        }
    }

    /**
     * Asserts that at least one visible token name matches [substring] (case-insensitive),
     * or — if the query is a ticker — that [fallback] appears in the visible results.
     */
    private fun assertVisibleResultsContain(substring: String, fallback: String? = null) {
        onMarketsScreen {
            val names = allTokenNameNodes()
                .mapNotNull { MarketsExtractors.extractTextRecursively(it) }
            assertFalse("Expected at least one search result, found none", names.isEmpty())

            val matched = names.any { name ->
                name.contains(substring, ignoreCase = true) ||
                    (fallback != null && name.contains(fallback, ignoreCase = true))
            }
            assertTrue(
                "Expected visible results to contain '$substring' (fallback: $fallback). Got: $names",
                matched,
            )
        }
    }
}