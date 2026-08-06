package com.tangem.tests.main

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.parseNumericBalance
import com.tangem.common.extensions.pullToRefresh
import com.tangem.common.extensions.tapBackButton
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onOutdatedDataBanner
import com.tangem.screens.onTokenDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

@HiltAndroidTest
class BalanceCachingTest : BaseTestCase() {

    @AllureId("140")
    @DisplayName("Balance caching: 'Balances may be outdated' banner displaying")
    @Test
    fun balanceCachingOutdatedBalancesBannerDisplayingTest() {
        val bitcoinBalanceScenario = "bitcoin_utxo"
        val bitcoinBalanceState = "BalanceError"
        val providersScenario = "networks_providers"
        val bitcoinMockOnlyState = "BitcoinMockOnly"

        // Bitcoin has five providers: a failing mock alone just makes the SDK fall through to the real ones.
        // The provider list is read at app launch, so it has to be selected before the app starts.
        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(providersScenario, bitcoinMockOnlyState)
            },
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Set WireMock scenario: '$bitcoinBalanceScenario' to state: '$bitcoinBalanceState'") {
                setWireMockScenarioState(bitcoinBalanceScenario, bitcoinBalanceState)
            }
            step("Pull to refresh to trigger a failing balance update over cached data") {
                pullToRefresh()
            }
            step("Assert 'Balances may be outdated' notification is displayed") {
                flakySafely {
                    onOutdatedDataBanner {
                        title.assertIsDisplayed()
                        message.assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("143")
    @DisplayName("Balance caching: Actions with tokens with outdated balances")
    @Test
    fun balanceCachingActionsWithTokensWithOutdatedBalancesTest() {
        val bitcoinBalanceScenario = "bitcoin_utxo"
        val bitcoinBalanceState = "BalanceError"
        val providersScenario = "networks_providers"
        val bitcoinMockOnlyState = "BitcoinMockOnly"
        val bitcoinBalanceDefaultState = "Started"
        val tokenWithOutdatedBalance = "Bitcoin"

        // Bitcoin has five providers: a failing mock alone just makes the SDK fall through to the real ones.
        // The provider list is read at app launch, so it has to be selected before the app starts.
        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(providersScenario, bitcoinMockOnlyState)
            },
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Set WireMock scenario: '$bitcoinBalanceScenario' to state: '$bitcoinBalanceState'") {
                setWireMockScenarioState(bitcoinBalanceScenario, bitcoinBalanceState)
            }
            step("Pull to refresh to trigger a failing balance update over cached data") {
                pullToRefresh()
            }
            step("Click on '$tokenWithOutdatedBalance' token with outdated balance") {
                onMainScreen { tokenWithTitleAndAddress(tokenWithOutdatedBalance).clickWithAssertion() }
            }
            step("Assert 'Balances may be outdated' banner is displayed on 'Token details' screen") {
                flakySafely {
                    onOutdatedDataBanner {
                        title.assertIsDisplayed()
                        message.assertIsDisplayed()
                    }
                }
            }
            step("Return to the 'Main screen'") {
                tapBackButton()
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Set WireMock scenario: '$bitcoinBalanceScenario' to state: '$bitcoinBalanceDefaultState'") {
                setWireMockScenarioState(bitcoinBalanceScenario, bitcoinBalanceDefaultState)
            }
            step("Pull to refresh to trigger a balance update") {
                pullToRefresh()
            }
            step("Assert 'Balances may be outdated' banner is NOT displayed on 'Main' screen") {
                flakySafely {
                    onOutdatedDataBanner {
                        title.assertDoesNotExist()
                        message.assertDoesNotExist()
                    }
                }
            }
        }
    }

    @AllureId("144")
    @DisplayName("Balance caching: Correct balance displaying with active staking")
    @Test
    fun balanceCachingCorrectBalanceDisplayingWithStakingTest() {
        val tokenName = "POL (ex-MATIC)"
        val stakingEthScenario = "staking_eth_pol_balances"
        val stakingEthState = "Staked"
        var totalBalance = BigDecimal.ZERO
        var stakedAmount = BigDecimal.ZERO

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set WireMock scenario: '$stakingEthScenario' to state: '$stakingEthState'") {
                setWireMockScenarioState(stakingEthScenario, stakingEthState)
            }

            step("Open 'Main Screen'") {
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
            step("Assert Total balance != Staked amount") {
                assertTrue(
                    "Total balance $totalBalance and Staked amount $stakedAmount should not be equal",
                    totalBalance.compareTo(stakedAmount) != 0
                )
            }
        }
    }
}