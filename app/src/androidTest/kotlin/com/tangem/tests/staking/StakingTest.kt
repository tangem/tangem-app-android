package com.tangem.tests.staking

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.pullToRefresh
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.math.BigDecimal

@HiltAndroidTest
class StakingTest : BaseTestCase() {

    @AllureId("11513")
    @DisplayName("Staking ETH P2P: staking is unavailable when the vault limit is reached")
    @Test
    fun ethStakingUnavailableWhenVaultLimitReachedTest() {
        val tokenTitle = "Ethereum"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val capacityScenario = "p2p_eth_pool_capacity"
        val capacityFullState = "Full"
        val capacityAvailableState = "Available"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(capacityScenario, capacityFullState)
            },
        ).run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Assert '$tokenTitle' token row is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { tokenWithTitleAndAddress(tokenTitle).assertIsDisplayed() }
                }
            }
            step("Assert 'APY' badge is not displayed on the '$tokenTitle' token row") {
                onMainScreen { tokenEarnApyBadge(tokenTitle).assertDoesNotExist() }
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Wait until token balance is loaded") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Assert 'Available staking block' is not displayed") {
                onTokenDetailsScreen { availableStakingBlock.assertDoesNotExist() }
            }
            step("Assert 'Staking block' is not displayed") {
                onTokenDetailsScreen { stakingBlock.assertDoesNotExist() }
            }
            step("Set WireMock scenario: '$capacityScenario' to state: '$capacityAvailableState'") {
                setWireMockScenarioState(scenarioName = capacityScenario, state = capacityAvailableState)
            }
            step("Pull to refresh until 'Available staking block' is displayed once the vault has free capacity") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    pullToRefreshTokenDetails()
                    onTokenDetailsScreen { availableStakingBlock.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("11574")
    @DisplayName("Staking SOL: stake more with the same validator")
    @Test
    fun stakeMoreWithSameValidatorTest() {
        val tokenTitle = "Solana"
        val stakingAmount = "0.1"
        val stakedFiatAmount = BigDecimal("20.00")
        val validator = "Tangem Validator"
        val positionAmount = "1.00"
        val pendingPositionAmount = "0.10"
        val stakedAfterOperation = "1.10"
        val parseFiat = { fiat: String -> BigDecimal(fiat.filter { it.isDigit() || it == '.' }) }
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "SolanaHotWalletSvS"
        val walletBalanceScenario = "solana_balance"
        val walletBalanceBeforeState = "StakeMoreFunded"
        val walletBalanceAfterState = "StakeMoreDone"
        val stakingScenario = "staking_sol_balances"
        val stakingBeforeState = "HotWalletStaked"
        val stakingAfterState = "HotWalletStakedMore"
        val actionsScenario = "staking_sol_actions"
        val actionsProcessingState = "StakeProcessing"
        val actionsCompletedState = "Started"

        setupHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$walletBalanceScenario' to state: '$walletBalanceBeforeState'") {
                setWireMockScenarioState(scenarioName = walletBalanceScenario, state = walletBalanceBeforeState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingBeforeState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingBeforeState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }

            var totalBefore = ""
            var availableBefore = ""
            step("Read 'Total' balance") {
                onTokenDetailsScreen {
                    totalBalanceLabel.assertIsDisplayed()
                    totalBefore = fiatBalance.extractText()
                }
            }
            step("Switch balance type to 'Available'") {
                onTokenDetailsScreen { totalBalanceLabel.performClick() }
            }
            step("Read 'Available' balance") {
                onTokenDetailsScreen {
                    availableBalanceLabel.assertIsDisplayed()
                    availableBefore = fiatBalance.extractText()
                }
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { stakingBlock.clickWithAssertion() }
                }
            }
            step("Assert rewards are added to the staking balance automatically") {
                onStakingDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { autoClaimedRewardsText.assertIsDisplayed() }
                }
            }
            step("Click 'Stake more' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onStakingDetailsScreen { stakeMoreButton.performClick() }
                    onSendScreen { amountInputTextField.assertIsDisplayed() }
                }
            }
            step("Assert 'Staking' screen is displayed and enter '$stakingAmount'") {
                checkStakingScreen(stakingAmount)
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.performClick() }
            }
            step("Assert validator '$validator' is selected on 'Staking confirm' screen") {
                onStakingConfirmScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { validatorBlockWithName(validator).assertIsDisplayed() }
                }
            }
            step("Hold 'Stake' button to sign and send the transaction") {
                confirmStakingByHolding()
            }
            step("Assert 'Staking success' screen is displayed") {
                checkStakingSuccessScreen()
            }
            step("Set WireMock scenario: '$walletBalanceScenario' to state: '$walletBalanceAfterState'") {
                setWireMockScenarioState(scenarioName = walletBalanceScenario, state = walletBalanceAfterState)
            }
            step("Set WireMock scenario: '$actionsScenario' to state: '$actionsProcessingState'") {
                setWireMockScenarioState(scenarioName = actionsScenario, state = actionsProcessingState)
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert a pending '$validator' position with '$pendingPositionAmount' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    var shown = false
                    onStakingDetailsScreen {
                        shown = pendingStakeAmountOfValidator(validator, pendingPositionAmount).isDisplayedSafely()
                    }
                    if (!shown) {
                        reopenStakingDetails()
                        throw AssertionError("The pending '$validator' position is still missing")
                    }
                }
            }
            step("Assert the pending indicator is displayed") {
                onStakingDetailsScreen { activeStakingPendingIcon.assertIsDisplayed() }
            }
            step("Assert the settled '$validator' position is still '$positionAmount'") {
                onStakingDetailsScreen { settledStakeAmountOfValidator(validator, positionAmount).assertIsDisplayed() }
            }
            step("Click on 'Close' button to return to 'Token details' screen") {
                onSendScreen { closeButton.performClick() }
            }
            step("Wait until token balance is loaded") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Assert 'Available' balance is decreased by the staked amount '$stakingAmount'") {
                onTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        availableBalanceLabel.assertIsDisplayed()
                        assertEquals(0, stakedFiatAmount.compareTo(parseFiat(availableBefore) - parseFiat(fiatBalance.extractText())))
                    }
                }
            }
            step("Switch balance type to 'Total'") {
                onTokenDetailsScreen {
                    availableBalanceLabel.performClick()
                    totalBalanceLabel.assertIsDisplayed()
                }
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingAfterState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingAfterState)
            }
            step("Set WireMock scenario: '$actionsScenario' to state: '$actionsCompletedState'") {
                setWireMockScenarioState(scenarioName = actionsScenario, state = actionsCompletedState)
            }
            step("Pull to refresh until the staked amount in 'Staking' banner is updated") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    pullToRefreshTokenDetails()
                    onTokenDetailsScreen { stakingTokenAmount.assertTextContains(stakedAfterOperation, substring = true) }
                }
            }
            step("Assert 'Total' balance is unchanged after the operation is completed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTokenDetailsScreen { assertEquals(totalBefore, fiatBalance.extractText()) }
                }
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Assert '$validator' position is merged into '$stakedAfterOperation'") {
                onStakingDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        settledStakeAmountOfValidator(validator, stakedAfterOperation).assertIsDisplayed()
                    }
                }
            }
            step("Assert the pending indicator is not displayed") {
                onStakingDetailsScreen { activeStakingPendingIcon.assertDoesNotExist() }
            }
        }
    }

    @AllureId("2188")
    @DisplayName("Staking: withdraw and validate result")
    @Test
    fun withdrawStakingTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingWithdrawableState = "Withdrawable"
        val stakingStartedState = "Started"

        setupHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingWithdrawableState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingWithdrawableState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Staking block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { stakingBlock.assertIsDisplayed() }
                }
            }
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Assert 'Your stakes' title is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { yourStakesTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Unstaked' record title is displayed") {
                onStakingDetailsScreen { withdrawEntryTitle.assertIsDisplayed() }
            }
            step("Assert 'Unstaked' record shows 'Tap to withdraw'") {
                onStakingDetailsScreen { withdrawEntrySubtitle.assertIsDisplayed() }
            }
            step("Tap the 'Unstaked' record to open withdrawal") {
                onStakingDetailsScreen { activeStakingBlock.performClick() }
            }
            step("Check 'Withdraw' screen") {
                checkWithdrawScreen()
            }
            step("Hold 'Withdraw' button to sign and send the transaction") {
                confirmStakingActionByHolding()
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingStartedState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingStartedState)
            }
            step("Check 'Withdraw success' screen") {
                checkWithdrawSuccessScreen()
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Unstaked' record is removed from 'Your stakes' after withdrawal") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { withdrawEntryTitle.assertDoesNotExist() }
                }
            }
        }
    }

    @AllureId("2190")
    @DisplayName("Staking: claim rewards and validate result")
    @Test
    fun claimRewardsTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingRewardsState = "Rewards"
        val stakingStakedState = "Staked"

        setupHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingRewardsState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingRewardsState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Staking block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { stakingBlock.assertIsDisplayed() }
                }
            }
            step("Assert 'Staking' fiat amount is displayed") {
                onTokenDetailsScreen { stakingFiatAmount.assertIsDisplayed() }
            }
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Check 'Staking details' screen") {
                checkStakingDetailsScreen(withStaking = true)
            }
            step("Tap the 'Rewards' block to claim rewards") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { rewardsClaimBlock.assertIsDisplayed() }
                }
                onStakingDetailsScreen { rewardsClaimBlock.performClick() }
            }
            step("Check 'Claim rewards' screen") {
                checkClaimRewardsScreen()
            }
            step("Hold 'Claim rewards' button to sign and send the transaction") {
                confirmStakingActionByHolding()
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingStakedState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingStakedState)
            }
            step("Check 'Claim rewards success' screen") {
                checkClaimRewardsSuccessScreen()
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            // Re-enters the screen instead of only waiting: 'Staking details' has no pull-to-refresh, so if the
            // staking balance was already cached before the scenario switched, nothing re-reads it and waiting
            // alone never succeeds (green locally, red on CI). Leaving and opening it again reloads the data.
            // Re-enters the screen between attempts instead of only waiting: 'Staking details' has no
            // pull-to-refresh, so when the staking balance was cached before the scenario switched, nothing
            // re-reads it and waiting alone never succeeds (green locally, red on CI). Re-opening reloads it.
            step("Assert 'Rewards' block shows no rewards to claim after claiming") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    var noRewards = false
                    onStakingDetailsScreen { noRewards = noRewardsToClaimText.isDisplayedSafely() }
                    if (!noRewards) {
                        reopenStakingDetails()
                        throw AssertionError("Rewards are still claimable — the staking balance has not reloaded")
                    }
                }
            }
        }
    }

    @AllureId("2192")
    @DisplayName("Staking: unstake and validate result")
    @Test
    fun unstakeStakingTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingStakedState = "Staked"
        val stakingUnstakingState = "Unstaking"

        setupHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingStakedState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingStakedState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Staking block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { stakingBlock.assertIsDisplayed() }
                }
            }
            step("Assert 'Staking' fiat amount is displayed") {
                onTokenDetailsScreen { stakingFiatAmount.assertIsDisplayed() }
            }
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Check 'Staking details' screen") {
                checkStakingDetailsScreen(withStaking = true)
            }
            step("Tap the active staked balance block to open unstaking") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { activeStakingBlock.assertIsDisplayed() }
                }
                onStakingDetailsScreen { activeStakingBlock.performClick() }
            }
            step("Check 'Unstake' screen") {
                checkUnstakeScreen()
            }
            // Switched before the hold — the transaction's own refetch would race it and cache the stale state.
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingUnstakingState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingUnstakingState)
            }
            step("Hold 'Unstake' button to sign and send the transaction") {
                confirmStakingActionByHolding()
            }
            step("Check 'Unstake success' screen") {
                checkUnstakeSuccessScreen()
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Unstaking' record is displayed in 'Your stakes'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { unstakingEntryTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Unstaking' record shows the 'Today' date") {
                onStakingDetailsScreen { unstakingEntryDate.assertIsDisplayed() }
            }
        }
    }

    @AllureId("2193")
    @DisplayName("Staking: enter staking, stake and validate result")
    @Test
    fun enterStakingAndValidateResultTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val stakingAmount = "1"
        val stakingApr = "APR 2.84%"
        val tokenRowFiatBalance = "578.89"
        val totalWalletBalance = "3,299.37"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingStartedState = "Started"
        val stakingStakedState = "Staked"

        setupHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingStartedState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingStartedState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Available staking block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { availableStakingBlock.assertIsDisplayed() }
                }
            }
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Click on 'Stake' button") {
                onTokenDetailsScreen { stakeButton.clickWithAssertion() }
            }
            step("Check 'Staking details' screen") {
                checkStakingDetailsScreen(withStaking = false)
            }
            step("Click on 'Stake' button") {
                onStakingDetailsScreen { stakeButton.performClick() }
            }
            step("Check 'Staking' screen") {
                checkStakingScreen(stakingAmount)
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.performClick() }
            }
            step("Check 'Staking confirm' screen") {
                checkStakingConfirmScreen()
            }
            step("Check 'Staking confirm' blocks clickability and hint") {
                checkStakingConfirmClickabilityAndHint()
            }
            // Switched before the hold for the same reason as in unstakeStakingTest.
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingStakedState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingStakedState)
            }
            step("Hold 'Stake' button to sign and send the transaction") {
                confirmStakingByHolding()
            }
            step("Check 'Staking success' screen") {
                checkStakingSuccessScreen()
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            // Re-enters the screen between attempts, the same way claimRewardsTest does: 'Staking details' has
            // no pull-to-refresh, so a staking balance cached before the scenario switched is never re-read and
            // 'Your stakes' never appears no matter how long the wait is.
            step("Check 'Staking details' screen after initial stake") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    var staked = false
                    onStakingDetailsScreen { staked = yourStakesTitle.isDisplayedSafely() }
                    if (!staked) {
                        reopenStakingDetails()
                        throw AssertionError("'Your stakes' is still missing — the staking balance has not reloaded")
                    }
                }
                checkStakingDetailsAfterInitialStake()
            }
            step("Click on 'Back' button to return to 'Token details' screen") {
                onSendScreen { closeButton.performClick() }
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Perform pull to refresh") {
                pullToRefresh()
            }
            step("Assert staked fiat amount is displayed in 'Staking' banner") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { stakingFiatAmount.assertIsDisplayed() }
                }
            }
            step("Assert staked token amount is displayed in 'Staking' banner") {
                onTokenDetailsScreen { stakingTokenAmount.assertIsDisplayed() }
            }
            step("Assert 'no rewards' info is displayed in 'Staking' banner") {
                onTokenDetailsScreen { stakingNoRewards.assertIsDisplayed() }
            }

            var allBalance = ""
            var availableBalance = ""
            step("Read 'All' balance") {
                onTokenDetailsScreen {
                    totalBalanceLabel.assertIsDisplayed()
                    allBalance = fiatBalance.extractText()
                }
            }
            step("Switch balance type to 'Available'") {
                onTokenDetailsScreen { totalBalanceLabel.performClick() }
            }
            step("Read 'Available' balance") {
                onTokenDetailsScreen {
                    availableBalanceLabel.assertIsDisplayed()
                    availableBalance = fiatBalance.extractText()
                }
            }
            step("Assert 'All' balance differs from 'Available' balance") {
                assertNotEquals(availableBalance, allBalance)
            }
            step("Click on 'Back' button to return to 'Main Screen'") {
                onTokenDetailsTopBar { backButton.clickWithAssertion() }
            }
            step("Assert 'APR' badge is displayed on the '$tokenTitle' token row") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { tokenEarnApyBadge(tokenTitle).assertTextContains(stakingApr, substring = true) }
                }
            }
            step("Assert '$tokenTitle' token row fiat balance includes the staked amount") {
                onMainScreen { tokenFiatAmountText(tokenTitle).assertTextContains(tokenRowFiatBalance, substring = true) }
            }
            step("Assert wallet 'Total balance' equals the expected fiat value") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { totalBalanceText.assertTextContains(totalWalletBalance, substring = true) }
                }
            }
        }
    }
}