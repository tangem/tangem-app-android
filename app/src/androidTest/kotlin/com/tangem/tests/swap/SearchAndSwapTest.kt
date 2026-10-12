package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.performTextInputInChunks
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R
import com.tangem.scenarios.openTokenDetailsFromTokenList
import com.tangem.scenarios.SwapEntryPoint
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSwapFromTokenSelector
import com.tangem.scenarios.openSwapScreen
import com.tangem.scenarios.selectBalanceFilter
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SearchAndSwapTest : BaseTestCase() {

    private val allFilterLabel = getResourceString(R.string.common_all)
    private val hideZeroBalanceLabel = getResourceString(R.string.swap_token_selector_filter_hide_zero_balance)

    @AllureId("8520")
    @DisplayName("Search and Swap: add token without derivation")
    @Test
    fun addTokenWithoutDerivationTest() {
        val receiveTokenName = "Tether"
        val swapTokenSymbol = "ETH"
        val receiveTokenSymbol = "USDT"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Click on 'Choose token' button") {
                onSwapTokenScreen { chooseTokenButton.performClick() }
            }
            step("Click on 'Search' text field") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.performClick() }
            }
            step("Type '$receiveTokenName' in input text field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInputInChunks(receiveTokenName) }
            }
            step("Click on token with name '$receiveTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { marketsTokenWithName(receiveTokenName).clickWithAssertion() }
                }
            }
            step("Click on 'Confirm' button") {
                onAddTokenBottomSheet { confirmButton.performClick() }
            }
            step("Assert swap token symbol: '$swapTokenSymbol' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("8519")
    @DisplayName("Search and Swap: add token with derivation")
    @Test
    fun addTokenWithDerivationTest() {
        val receiveTokenName = "TRON"
        val swapTokenSymbol = "ETH"
        val receiveTokenSymbol = "TRX"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Click on 'Choose token' button") {
                onSwapTokenScreen { chooseTokenButton.performClick() }
            }
            step("Click on 'Search' text field") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.performClick() }
            }
            step("Type '$receiveTokenSymbol' in input text field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInputInChunks(receiveTokenSymbol) }
            }
            step("Click on token with name '$receiveTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { marketsTokenWithName(receiveTokenName).clickWithAssertion() }
                }
            }
            step("Click on 'Confirm' button") {
                onAddTokenBottomSheet { confirmButton.performClick() }
            }
            step("Assert swap token symbol: '$swapTokenSymbol' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("8523")
    @DisplayName("Search and Swap: Markets error")
    @Test
    fun marketsErrorTest() {
        val scenarioName = "coins_list_api"
        val scenarioState = "Error"

        setupHooks().run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName, scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Click on 'Choose token' button") {
                onSwapTokenScreen { chooseTokenButton.performClick() }
            }
            step("Assert 'Unable to load data...' error is displayed") {
                onSwapSelectTokenScreen { unableToLoadData.assertIsDisplayed() }
            }
            step("Assert 'Try again' button is displayed") {
                onSwapSelectTokenScreen { tryAgainButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("8522")
    @DisplayName("Search and Swap: check 'Unsupported token pair' warning")
    @Test
    fun unsupportedTokenPairTest() {
        val receiveTokenName = "Pepe"
        val warningTitle = getResourceString(R.string.warning_express_unsupported_pair_title)
        val warningMessage = getResourceString(R.string.warning_express_unsupported_pair_description)

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Click on 'Choose token' button") {
                onSwapTokenScreen { chooseTokenButton.performClick() }
            }
            step("Click on 'Search' text field") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.performClick() }
            }
            step("Type '$receiveTokenName' in input text field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInputInChunks(receiveTokenName) }
            }
            step("Click on token with name '$receiveTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { marketsTokenWithName(receiveTokenName).clickWithAssertion() }
                }
            }
            step("Click on 'Confirm' button") {
                onAddTokenBottomSheet { confirmButton.performClick() }
            }
            step("Assert warning title '$warningTitle' is displayed") {
                onSwapTokenScreen { warningTitle(warningTitle).assertIsDisplayed() }
            }
            step("Assert warning message '$warningMessage' is displayed") {
                onSwapTokenScreen { warningMessage(warningMessage).assertIsDisplayed() }
            }
            step("Assert warning icon is displayed'") {
                onSwapTokenScreen { warningIcon(warningMessage).assertIsDisplayed() }
            }
        }
    }

    @AllureId("8521")
    @DisplayName("Swap: search token on Swap token screen")
    @Test
    fun networkFeeTest() {
        val tokenTitle = "Ethereum"
        val swapTokenSymbol = "TRX"
        val receiveTokenSymbol = "ETH"
        val swapTokenName = "TRON"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Click on 'Replace tokens' button") {
                onSwapTokenScreen { replaceTokensButton.performClick() }
            }
            step("Click on 'Choose token' button") {
                onSwapTokenScreen { chooseTokenButton.performClick() }
            }
            step("Click on 'Search' icon") {
                onSwapSelectTokenScreen { searchBarIcon.performClick() }
            }
            step("Type '$swapTokenSymbol' in 'Search' text field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInputInChunks(swapTokenSymbol) }
            }
            step("Click on token with name: '$swapTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { marketsTokenWithName(swapTokenName).performClick() }
                }
            }
            step("Click on 'Confirm' button") {
                onAddTokenBottomSheet { confirmButton.performClick() }
            }
            step("Assert swap token symbol: '$swapTokenSymbol' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { swapTokenSymbol(swapTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert receive token symbol: '$receiveTokenSymbol' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10862")
    @DisplayName("Search and Swap: 'Hide 0 balance tokens' filter is enabled by default")
    @Test
    fun hideZeroBalanceFilterEnabledByDefaultTest() {
        val zeroBalanceScenario = "polygon_coin_balance"
        val zeroBalanceState = "ZeroBalance"
        val fundedTokenName = "POL (ex-MATIC)"
        val zeroBalanceTokenName = "Polygon"

        setupHooks().run {
            step("Set WireMock scenario: '$zeroBalanceScenario' to state: '$zeroBalanceState'") {
                setWireMockScenarioState(zeroBalanceScenario, zeroBalanceState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Open 'From' token selector") {
                openSwapFromTokenSelector()
            }
            step("Assert 'Hide 0 balance' filter is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { balanceFilterChip.assertTextContains(hideZeroBalanceLabel) }
                }
            }
            step("Assert token with balance: '$fundedTokenName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(fundedTokenName).assertIsDisplayed() }
                }
            }
            step("Assert token with 0 balance: '$zeroBalanceTokenName' is not displayed") {
                onSwapSelectTokenScreen { tokenWithName(zeroBalanceTokenName).assertDoesNotExist() }
            }
        }
    }

    @AllureId("10863")
    @DisplayName("Search and Swap: 'Hide 0 balance tokens' filter with all wallet tokens at zero balance")
    @Test
    fun hideZeroBalanceFilterAllTokensZeroBalanceTest() {
        val userTokensState = "TOTAL_BALANCE_TOKENS_ZERO"
        val rippleAccountInfoScenario = "ripple_account_info"
        val rippleAccountInfoState = "Empty"
        val ethCallScenario = "eth_call_api"
        val ethCallState = "Empty"
        val ethBalanceScenario = "eth_network_balance"
        val ethBalanceState = "Empty"
        val fundedTokenName = "Ethereum"

        setupHooks().run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensState)
            }
            step("Set WireMock scenario: '$rippleAccountInfoScenario' to state: '$rippleAccountInfoState'") {
                setWireMockScenarioState(rippleAccountInfoScenario, rippleAccountInfoState)
            }
            step("Set WireMock scenario: '$ethCallScenario' to state: '$ethCallState'") {
                setWireMockScenarioState(ethCallScenario, ethCallState)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$ethBalanceState'") {
                setWireMockScenarioState(ethBalanceScenario, ethBalanceState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Open 'From' token selector") {
                openSwapFromTokenSelector()
            }
            step("Assert 'You don't have any funded tokens' message is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { filteredOutEmptyMessage.assertIsDisplayed() }
                }
            }
            step("Assert 'See all' button is displayed") {
                onSwapSelectTokenScreen { seeAllButton.assertIsDisplayed() }
            }
            step("Click on 'See all' button") {
                onSwapSelectTokenScreen { seeAllButton.clickWithAssertion() }
            }
            step("Assert 'All' filter is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { balanceFilterChip.assertTextContains(allFilterLabel) }
                }
            }
            step("Assert token: '$fundedTokenName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(fundedTokenName).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10869")
    @DisplayName("Search and Swap: 'Hide 0 balance tokens' filter — search")
    @Test
    fun hideZeroBalanceFilterSearchTest() {
        val zeroBalanceScenario = "polygon_coin_balance"
        val zeroBalanceState = "ZeroBalance"
        val fundedTokenName = "POL (ex-MATIC)"
        val zeroBalanceTokenName = "Polygon"

        setupHooks().run {
            step("Set WireMock scenario: '$zeroBalanceScenario' to state: '$zeroBalanceState'") {
                setWireMockScenarioState(zeroBalanceScenario, zeroBalanceState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Open 'From' token selector") {
                openSwapFromTokenSelector()
            }
            step("Click on 'Search' text field") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.performClick() }
            }
            step("Type '$fundedTokenName' in search field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInput(fundedTokenName) }
            }
            step("Assert token with balance: '$fundedTokenName' is found") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(fundedTokenName).assertIsDisplayed() }
                }
            }
            step("Click on 'Clear' search button") {
                onSwapSelectTokenScreen { searchClearButton.performClick() }
            }
            step("Type '$zeroBalanceTokenName' in search field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInput(zeroBalanceTokenName) }
            }
            step("Assert search field contains '$zeroBalanceTokenName'") {
                // '$zeroBalanceTokenName' is hidden by the default filter regardless of search — proving the
                // query itself landed is what makes the next assertion about search, not just the filter.
                onSwapSelectTokenScreen { searchBarBlock.assertTextContains(zeroBalanceTokenName) }
            }
            step("Assert token with 0 balance: '$zeroBalanceTokenName' is hidden") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(zeroBalanceTokenName).assertDoesNotExist() }
                }
            }
        }
    }

    @AllureId("10866")
    @DisplayName("Search and Swap: enable 'All' tokens filter")
    @Test
    fun enableAllTokensFilterTest() {
        val zeroBalanceScenario = "polygon_coin_balance"
        val zeroBalanceState = "ZeroBalance"
        val fundedTokenName = "POL (ex-MATIC)"
        val zeroBalanceTokenName = "Polygon"

        setupHooks().run {
            step("Set WireMock scenario: '$zeroBalanceScenario' to state: '$zeroBalanceState'") {
                setWireMockScenarioState(zeroBalanceScenario, zeroBalanceState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Open 'From' token selector") {
                openSwapFromTokenSelector()
            }
            step("Assert token with balance: '$fundedTokenName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(fundedTokenName).assertIsDisplayed() }
                }
            }
            step("Assert token with 0 balance: '$zeroBalanceTokenName' is not displayed") {
                onSwapSelectTokenScreen { tokenWithName(zeroBalanceTokenName).assertDoesNotExist() }
            }
            step("Select the '$allFilterLabel' balance filter") {
                selectBalanceFilter(allFilterLabel)
            }
            step("Assert token with 0 balance: '$zeroBalanceTokenName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(zeroBalanceTokenName).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10870")
    @DisplayName("Search and Swap: 'All' tokens filter — search")
    @Test
    fun allTokensFilterSearchTest() {
        val zeroBalanceScenario = "polygon_coin_balance"
        val zeroBalanceState = "ZeroBalance"
        val fundedTokenName = "POL (ex-MATIC)"
        val zeroBalanceTokenName = "Polygon"

        setupHooks().run {
            step("Set WireMock scenario: '$zeroBalanceScenario' to state: '$zeroBalanceState'") {
                setWireMockScenarioState(zeroBalanceScenario, zeroBalanceState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.MainScreen)
            }
            step("Open 'From' token selector") {
                openSwapFromTokenSelector()
            }
            step("Select the '$allFilterLabel' balance filter") {
                selectBalanceFilter(allFilterLabel)
            }
            step("Assert token with 0 balance: '$zeroBalanceTokenName' is displayed before searching") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(zeroBalanceTokenName).assertIsDisplayed() }
                }
            }
            step("Click on 'Search' text field") {
                onSwapSelectTokenScreen { searchBarPlaceholderText.performClick() }
            }
            step("Type '$zeroBalanceTokenName' in search field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInput(zeroBalanceTokenName) }
            }
            step("Assert token with 0 balance: '$zeroBalanceTokenName' is found") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(zeroBalanceTokenName).assertIsDisplayed() }
                }
            }
            step("Click on 'Clear' search button") {
                onSwapSelectTokenScreen { searchClearButton.performClick() }
            }
            step("Type '$fundedTokenName' in search field") {
                onSwapSelectTokenScreen { searchBarBlock.performTextInput(fundedTokenName) }
            }
            step("Assert token with balance: '$fundedTokenName' is found") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSwapSelectTokenScreen { tokenWithName(fundedTokenName).assertIsDisplayed() }
                }
            }
        }
    }
}