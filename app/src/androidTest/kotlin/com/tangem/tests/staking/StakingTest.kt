package com.tangem.tests.staking

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.pullToRefresh
import com.tangem.common.extensions.swipeVertical
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertNotEquals
import org.junit.Test

@HiltAndroidTest
class StakingTest : BaseTestCase() {

    @AllureId("3558")
    @DisplayName("Staking: validate staking block on 'Token details' screen")
    @Test
    fun validateStakingBlockTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val scenarioName = "staking_eth_pol_balances"
        val scenarioState = "Staked"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Add & Manage' button is displayed") {
                onMainScreen { addAndManageButton().assertIsDisplayed() }
            }
            step("Swipe up") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Staking block' is displayed") {
                onTokenDetailsScreen { stakingBlock.assertIsDisplayed() }
            }
            step("Assert 'Staking title' is displayed") {
                onTokenDetailsScreen { stakingEnabledTitle.assertIsDisplayed() }
            }
            step("Assert 'Staking fiat amount' is displayed") {
                onTokenDetailsScreen { stakingFiatAmount.assertIsDisplayed() }
            }
            step("Assert 'Staking token amount' is displayed") {
                onTokenDetailsScreen { stakingTokenAmount.assertIsDisplayed() }
            }
        }
    }

    @AllureId("3550")
    @DisplayName("Staking: validate staking more screens")
    @Test
    fun validateStakingMoreScreensTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val scenarioName = "staking_eth_pol_balances"
        val scenarioState = "Staked"
        val stakingAmount = "1"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Add & Manage' button is displayed") {
                onMainScreen { addAndManageButton().assertIsDisplayed() }
            }
            step("Swipe up") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Check 'Staking details' screen") {
                checkStakingDetailsScreen(withStaking = true)
            }
            step("Click 'Stake more' button") {
                onStakingDetailsScreen { stakeMoreButton.performClick() }
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
        }
    }

    @AllureId("3548")
    @DisplayName("Staking: validate staking screens")
    @Test
    fun validateStakingScreensTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val scenarioName = "staking_eth_pol_balances"
        val scenarioState = "Started"
        val stakingAmount = "1"
        val stakingApy = "2.84%"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Add & Manage' button is displayed") {
                onMainScreen { addAndManageButton().assertIsDisplayed() }
            }
            step("Swipe up") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Available staking block' is displayed") {
                onTokenDetailsScreen { availableStakingBlock.assertIsDisplayed() }
            }
            step("Assert 'Available staking block' title is displayed") {
                onTokenDetailsScreen { stakingTitle.assertIsDisplayed() }
            }
            step("Assert 'Available staking block' text is displayed") {
                onTokenDetailsScreen { availableStakingBlockText(stakingApy).assertIsDisplayed() }
            }
            step("Click on 'Stake' button") {
                onTokenDetailsScreen { stakeButton.clickWithAssertion() }
            }
            step("Check 'Staking details' screen") {
                checkStakingDetailsScreen(withStaking = false)
            }
            step("Click 'Stake' button") {
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

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(portfolioScenario)
                resetWireMockScenarioState(balancesScenario)
                resetWireMockScenarioState(stakingScenario)
            }
        ).run {

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
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
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

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(portfolioScenario)
                resetWireMockScenarioState(balancesScenario)
                resetWireMockScenarioState(stakingScenario)
            }
        ).run {

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
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
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
            step("Assert 'Rewards' block shows no rewards to claim after claiming") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { noRewardsToClaimText.assertIsDisplayed() }
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

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(portfolioScenario)
                resetWireMockScenarioState(balancesScenario)
                resetWireMockScenarioState(stakingScenario)
            }
        ).run {

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
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
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

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(portfolioScenario)
                resetWireMockScenarioState(balancesScenario)
                resetWireMockScenarioState(stakingScenario)
            }
        ).run {

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
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
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
            step("Check 'Staking details' screen after initial stake") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { yourStakesTitle.assertIsDisplayed() }
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