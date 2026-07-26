package com.tangem.tests.main

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.parseNumericBalance
import com.tangem.common.extensions.tapBackButton
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onAppCurrencySelectorScreen
import com.tangem.screens.onAppSettingsScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.utils.StringsSigns.DASH_SIGN
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

@HiltAndroidTest
class TotalBalanceTest : BaseTestCase() {

    @AllureId("151")
    @DisplayName("Total balance: Balance is under shimmer with slow connection")
    @Test
    fun totalBalanceHideUnderShimmerWithSlowConnectionTest() {
        val quoteDelayScenario = "quotes_api"
        val quoteDelayState = "SlowResponse"
        val coinsListDelayScenario = "coins_list_api"
        val coinsListDelayState = "SlowResponse"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set WireMock scenario: '$quoteDelayScenario' to state: '$quoteDelayState'") {
                setWireMockScenarioState(quoteDelayScenario, quoteDelayState)
            }
            step("Set WireMock scenario: '$coinsListDelayScenario' to state: '$coinsListDelayState'") {
                setWireMockScenarioState(coinsListDelayScenario, coinsListDelayState)
            }
            step("Open 'Main Screen'" ) {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(assertBalance = false)
            }
            step("Assert shimmer instead of Total balance is displayed") {
                flakySafely {
                    onMainScreen { totalBalanceShimmer.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("164")
    @DisplayName("Total balance: Zero balance displayed for all tokens except custom")
    @Test
    fun totalBalanceZeroBalancesExceptCustomTokensTest() {
        val customToken = "SuperCustomToken"
        val userTokensScenario = "user_tokens_api"
        val userTokensState = "TOTAL_BALANCE_TOKENS_ZERO"
        val rippleAccountInfoScenario = "ripple_account_info"
        val rippleAccountInfoState = "Empty"
        val ethCallScenario = "eth_call_api"
        val ethCallState = "Empty"
        val ethBalanceScenario = "eth_network_balance"
        val ethBalanceState = "Empty"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenario' to state: '$userTokensState'") {
                setWireMockScenarioState(userTokensScenario, userTokensState)
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
            step("Open 'Main Screen'" ) {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert total balance equals 0") {
                flakySafely {
                    var totalBalance = ""
                    onMainScreen { totalBalance = totalBalanceText.extractText() }
                    assertEquals(0, totalBalance.parseNumericBalance().signum())
                }
            }
            step("Assert every token shows a zero balance except '$customToken' which shows a dash") {
                var tokenBalances: List<Pair<String, String>> = emptyList()
                onMainScreen { tokenBalances = getDisplayedTokenBalances() }
                assertTrue("No token rows found on the main screen", tokenBalances.isNotEmpty())
                assertTrue(
                    "Custom token '$customToken' should be displayed",
                    tokenBalances.any { (title, _) -> title == customToken },
                )
                tokenBalances.forEach { (title, fiatAmount) ->
                    if (title == customToken) {
                        assertEquals("Custom token '$title' should show a dash", DASH_SIGN, fiatAmount)
                    } else {
                        assertEquals(
                            "Token '$title' should have a zero balance",
                            0,
                            fiatAmount.parseNumericBalance().signum(),
                        )
                    }
                }
            }
        }
    }

    @AllureId("166")
    @DisplayName("Total balance: Balance includes Staked amounts")
    @Test
    fun totalBalanceStakedAmountsIncluded() {
        val tokenName = "POL (ex-MATIC)"
        val stakingEthScenario = "staking_eth_pol_balances"
        val stakingEthState = "Staked"
        var totalBalance = BigDecimal.ZERO
        var stakedAmount = BigDecimal.ZERO
        var availableBalance = BigDecimal.ZERO

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set WireMock scenario: '$stakingEthScenario' to state: '$stakingEthState'") {
                setWireMockScenarioState(stakingEthScenario, stakingEthState)
            }
            step("Open 'Main Screen'" ) {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on '$tokenName' token") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Get Total balance") {
                flakySafely {
                    onTokenDetailsScreen {
                        totalBalanceSwitcher.assertIsDisplayed()
                        totalBalance = fiatBalance.extractText().parseNumericBalance()
                    }
                }
            }
            step("Get staked amount") {
                flakySafely {
                    onTokenDetailsScreen {
                        stakingFiatAmount.assertIsDisplayed()
                        stakedAmount = stakingFiatAmount.extractText().parseNumericBalance()
                    }
                }
            }
            step("Click on 'Total balance' switcher") {
                onTokenDetailsScreen { totalBalanceSwitcher.clickWithAssertion() }
            }
            step("Get Available balance") {
                flakySafely {
                    onTokenDetailsScreen {
                        availableBalanceSwitcher.assertIsDisplayed()
                        availableBalance = fiatBalance.extractText().parseNumericBalance()
                    }
                }
            }
            step("Assert Available balance plus staked amount equals Total balance") {
                val diff = (availableBalance + stakedAmount - totalBalance).abs()
                assertTrue(
                    "Available ($availableBalance) + staked ($stakedAmount) should equal " +
                        "total ($totalBalance) within $BALANCE_TOLERANCE",
                    diff <= BALANCE_TOLERANCE,
                )
            }
        }
    }

    @AllureId("3966")
    @DisplayName("Total balance: Positive balance displayed for all tokens except custom")
    @Test
    fun totalBalancePositiveBalancesExceptCustomTokensTest() {
        val customToken = "SuperCustomToken"
        val userTokensScenario = "user_tokens_api"
        val userTokensState = "TOTAL_BALANCE_TOKENS_POSITIVE"
        val quotesScenario = "quotes_api"
        val quotesState = "Ripple"
        val rippleCustomDerivationScenario = "ripple_custom_derivation"
        val rippleCustomDerivationState = "Positive"
        val rippleAccountInfoScenario = "ripple_account_info"
        val rippleAccountInfoState = "Started"
        val rippleAccountLinesScenario = "ripple_account_lines"
        val rippleAccountLinesState = "Started"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenario' to state: '$userTokensState'") {
                setWireMockScenarioState(userTokensScenario, userTokensState)
            }
            step("Set WireMock scenario: '$quotesScenario' to state: '$quotesState'") {
                setWireMockScenarioState(quotesScenario, quotesState)
            }
            step("Set WireMock scenario: '$rippleCustomDerivationScenario' to state: '$rippleCustomDerivationState'") {
                setWireMockScenarioState(rippleCustomDerivationScenario, rippleCustomDerivationState)
            }
            step("Set WireMock scenario: '$rippleAccountInfoScenario' to state: '$rippleAccountInfoState'") {
                setWireMockScenarioState(rippleAccountInfoScenario, rippleAccountInfoState)
            }
            step("Set WireMock scenario: '$rippleAccountLinesScenario' to state: '$rippleAccountLinesState'") {
                setWireMockScenarioState(rippleAccountLinesScenario, rippleAccountLinesState)
            }
            step("Open 'Main Screen'" ) {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert total balance greater than 0") {
                flakySafely {
                    var totalBalance = ""
                    onMainScreen { totalBalance = totalBalanceText.extractText() }
                    assertEquals(1, totalBalance.parseNumericBalance().signum())
                }
            }
            step("Assert every token shows a positive balance except '$customToken' which shows a dash") {
                var tokenBalances: List<Pair<String, String>> = emptyList()
                onMainScreen { tokenBalances = getDisplayedTokenBalances() }
                assertTrue("No token rows found on the main screen", tokenBalances.isNotEmpty())
                assertTrue(
                    "Custom token '$customToken' should be displayed",
                    tokenBalances.any { (title, _) -> title == customToken },
                )
                tokenBalances.forEach { (title, fiatAmount) ->
                    if (title == customToken) {
                        assertEquals("Custom token '$title' should show a dash", DASH_SIGN, fiatAmount)
                    } else {
                        assertEquals(
                            "Token '$title' should have a positive balance",
                            1,
                            fiatAmount.parseNumericBalance().signum(),
                        )
                    }
                }
            }
        }
    }

    @AllureId("3996")
    @DisplayName("Total balance: Currency symbol changing")
    @Test
    fun totalBalanceCurrencySymbolChangingTest() {
        val defaultCurrencyCode = "USD"
        val newCurrecnyCode = "RUB"
        val currenciesScenario = "currencies_api"
        val currenciesState = "AppSettings"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set WireMock scenario: '$currenciesScenario' to state: '$currenciesState'") {
                setWireMockScenarioState(currenciesScenario, currenciesState)
            }
            step("Open 'Main Screen'" ) {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Details' screen") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'App settings' screen") {
                onDetailsScreen { appSettingsButton.clickWithAssertion() }
            }
            step("Open 'App currency screen'") {
                onAppSettingsScreen { currencyButton.clickWithAssertion() }
            }
            step("Change default currency to '$newCurrecnyCode'") {
                onAppCurrencySelectorScreen {
                    searchActionButton.clickWithAssertion()
                    searchField.performTextInput(newCurrecnyCode)
                    currencyItem(newCurrecnyCode).clickWithAssertion()
                }
            }
            step("Return to the 'Details' screen") {
                tapBackButton()
                onDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Return to the 'Main' screen") {
                tapBackButton()
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert balance is displayed with '₽' currency symbol") {
                onMainScreen { totalBalanceText.assertTextContains("₽", true ) }
            }
            step("Open 'Details' screen") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'App settings' screen") {
                onDetailsScreen { appSettingsButton.clickWithAssertion() }
            }
            step("Open 'App currency screen'") {
                onAppSettingsScreen { currencyButton.clickWithAssertion() }
            }
            step("Change default currency to '$defaultCurrencyCode'") {
                onAppCurrencySelectorScreen {
                    searchActionButton.clickWithAssertion()
                    searchField.performTextInput(defaultCurrencyCode)
                    currencyItem(defaultCurrencyCode).clickWithAssertion()
                }
            }
            step("Return to the 'Details' screen") {
                tapBackButton()
                onDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Return to the 'Main' screen") {
                tapBackButton()
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert balance is displayed with '$' currency symbol") {
                onMainScreen { totalBalanceText.assertTextContains("$", true ) }
            }
        }
    }

    private companion object {
        val BALANCE_TOLERANCE: BigDecimal = BigDecimal("0.01")
    }
}