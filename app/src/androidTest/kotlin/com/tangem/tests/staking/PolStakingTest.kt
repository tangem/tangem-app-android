package com.tangem.tests.staking

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.HOLD_DURATION_MS
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.common.extensions.swipeVertical
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

@HiltAndroidTest
class PolStakingTest : BaseTestCase() {

    @AllureId("3558")
    @DisplayName("Staking: validate staking block on 'Token details' screen")
    @Test
    fun validateStakingBlockTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val scenarioName = "staking_eth_pol_balances"
        val scenarioState = "Staked"

        setupHooks().run {

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
                openTokenDetailsFromTokenList(tokenTitle)
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
    @DisplayName("Staking: stake more POL with another validator")
    @Test
    fun validateStakingMoreScreensTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val stakingAmount = "1"
        val firstValidator = "Meria"
        val secondValidator = "Staking4All"
        val stakedPosition = "1.00"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingStakedState = "Staked"
        val actionsScenario = "staking_eth_pol_actions"
        val actionsProcessingState = "StakeProcessing"

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
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { stakingBlock.clickWithAssertion() }
                }
            }
            step("Assert '$firstValidator' position is displayed") {
                onStakingDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { activeStakingBlockOfValidator(firstValidator).assertIsDisplayed() }
                }
            }
            step("Click 'Stake more' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onStakingDetailsScreen { stakeMoreButton.performClick() }
                    onSendScreen { amountInputTextField.assertIsDisplayed() }
                }
            }
            step("Assert 'Staking' screen is displayed") {
                checkStakingScreen(stakingAmount)
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.performClick() }
            }
            step("Assert validator '$firstValidator' is selected on 'Staking confirm' screen") {
                onStakingConfirmScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { validatorBlockWithName(firstValidator).assertIsDisplayed() }
                }
            }
            step("Click on validator block") {
                onStakingConfirmScreen { validatorBlock.performClick() }
            }
            step("Click on validator '$secondValidator'") {
                onStakingConfirmScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { validatorListItem(secondValidator).performClick() }
                }
            }
            step("Click on 'Continue' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendScreen { continueButton.performClick() }
                    onStakingConfirmScreen { validatorBlock.assertIsDisplayed() }
                }
            }
            step("Assert validator '$secondValidator' is selected on 'Staking confirm' screen") {
                onStakingConfirmScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { validatorBlockWithName(secondValidator).assertIsDisplayed() }
                }
            }
            step("Hold 'Stake' button to sign and send the transaction") {
                confirmStakingByHolding()
            }
            step("Assert 'Staking success' screen is displayed") {
                checkStakingSuccessScreen()
            }
            step("Set WireMock scenario: '$actionsScenario' to state: '$actionsProcessingState'") {
                setWireMockScenarioState(scenarioName = actionsScenario, state = actionsProcessingState)
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert a separate '$secondValidator' position is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    var shown = false
                    onStakingDetailsScreen { shown = activeStakingBlockOfValidator(secondValidator).isDisplayedSafely() }
                    if (!shown) {
                        reopenStakingDetails()
                        throw AssertionError("'$secondValidator' position is still missing")
                    }
                }
            }
            step("Assert '$secondValidator' position amount is '$stakedPosition'") {
                onStakingDetailsScreen { activeStakingAmountOfValidator(secondValidator, stakedPosition).assertIsDisplayed() }
            }
            step("Assert '$secondValidator' position shows the pending indicator") {
                onStakingDetailsScreen { activeStakingPendingIconOfValidator(secondValidator).assertIsDisplayed() }
            }
            step("Assert '$firstValidator' position amount is still '$stakedPosition'") {
                onStakingDetailsScreen { activeStakingAmountOfValidator(firstValidator, stakedPosition).assertIsDisplayed() }
            }
            step("Assert '$firstValidator' position has no pending indicator") {
                onStakingDetailsScreen { activeStakingPendingIconOfValidator(firstValidator).assertDoesNotExist() }
            }
        }
    }

    @AllureId("3548")
    @DisplayName("Staking: stake POL with granting the token approval")
    @Test
    fun validateStakingScreensTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val stakingAmount = "1"
        val stakingApy = "2.84%"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingStartedState = "Started"
        val stakingStakedState = "Staked"
        val allowanceScenario = "staking_eth_pol_allowance"
        val allowanceNotApprovedState = "NotApproved"
        val allowanceApprovedState = "Approved"

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
            step("Set WireMock scenario: '$allowanceScenario' to state: '$allowanceNotApprovedState'") {
                setWireMockScenarioState(scenarioName = allowanceScenario, state = allowanceNotApprovedState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert 'Available staking block' is displayed") {
                onTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { availableStakingBlock.assertIsDisplayed() }
                }
            }
            step("Assert 'Available staking block' title is displayed") {
                onTokenDetailsScreen { stakingTitle.assertIsDisplayed() }
            }
            step("Assert 'Available staking block' text is displayed") {
                onTokenDetailsScreen { availableStakingBlockText(stakingApy).assertIsDisplayed() }
            }
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Click on 'Stake' button") {
                onTokenDetailsScreen { stakeButton.clickWithAssertion() }
            }
            step("Assert 'Staking details' screen is displayed") {
                checkStakingDetailsScreen(withStaking = false)
            }
            step("Click on 'Stake' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onStakingDetailsScreen { stakeButton.performClick() }
                    onSendScreen { amountInputTextField.assertIsDisplayed() }
                }
            }
            step("Assert 'Staking' screen is displayed") {
                checkStakingScreen(stakingAmount)
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.performClick() }
            }
            step("Assert amount block is displayed on 'Staking confirm' screen") {
                onStakingConfirmScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { amountBlock.assertIsDisplayed() }
                }
            }
            step("Assert validator block is displayed on 'Staking confirm' screen") {
                onStakingConfirmScreen { validatorBlock.assertIsDisplayed() }
            }
            step("Assert network fee block is displayed on 'Staking confirm' screen") {
                onStakingConfirmScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { networkFeeBlock.assertIsDisplayed() }
                }
            }
            step("Assert 'Give Permission' button is displayed") {
                onStakingConfirmScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { givePermissionButton.assertIsDisplayed() }
                }
            }
            step("Click on 'Give Permission' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onStakingConfirmScreen { givePermissionButton.performClick() }
                    onStakingConfirmScreen { approveButton.assertIsDisplayed() }
                }
            }
            step("Hold 'Approve' button to sign the approval") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingConfirmScreen {
                        if (approveButton.isDisplayedSafely()) {
                            approveButton.assertIsEnabled()
                            approveButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
                        }
                        approveButton.assertIsNotDisplayed()
                    }
                }
            }
            step("Set WireMock scenario: '$allowanceScenario' to state: '$allowanceApprovedState'") {
                setWireMockScenarioState(scenarioName = allowanceScenario, state = allowanceApprovedState)
            }
            step("Assert 'Stake' button is displayed after the allowance is updated") {
                onStakingConfirmScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { stakeButton.assertIsDisplayed() }
                }
            }
            step("Assert 'Approval in Progress' notification is not displayed") {
                onStakingConfirmScreen { approvalInProgressNotification.assertDoesNotExist() }
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingStakedState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingStakedState)
            }
            step("Hold 'Stake' button to sign and send the transaction") {
                confirmStakingByHolding()
            }
            step("Assert 'Staking success' screen is displayed") {
                checkStakingSuccessScreen()
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Staking details' screen after the stake is displayed") {
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
            step("Assert staked token amount is displayed in 'Staking' banner") {
                onTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { stakingTokenAmount.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("3556")
    @DisplayName("Staking POL: claim rewards greater than 1 POL")
    @Test
    fun claimRewardsOverOnePolTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingInitialState = "RewardsOverOne"
        val stakingClaimedState = "StakedLarge"
        val rewardsAmount = "1.50"

        setupHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingInitialState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingInitialState)
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
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Assert 'Rewards' block shows '$rewardsAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { rewardsClaimAmount(rewardsAmount).assertIsDisplayed() }
                }
            }
            step("Click on 'Rewards' block") {
                onStakingDetailsScreen { rewardsClaimBlock.performClick() }
            }
            step("Assert 'Claim rewards' screen is displayed") {
                checkClaimRewardsScreen()
            }
            step("Hold 'Claim rewards' button to sign and send the transaction") {
                confirmStakingActionByHolding()
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingClaimedState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingClaimedState)
            }
            step("Assert 'Claim rewards success' screen is displayed") {
                checkClaimRewardsSuccessScreen()
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
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

    @AllureId("3553")
    @DisplayName("Staking POL: withdraw the unstaked position")
    @Test
    fun withdrawUnstakedPolTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingInitialState = "Withdrawable"
        val stakingWithdrawnState = "Started"

        setupHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingInitialState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingInitialState)
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
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Assert 'Unstaked' record shows 'Tap to withdraw'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { withdrawEntrySubtitle.assertIsDisplayed() }
                }
            }
            step("Click on 'Unstaked' record") {
                onStakingDetailsScreen { activeStakingBlock.performClick() }
            }
            step("Assert 'Withdraw' screen is displayed") {
                checkWithdrawScreen()
            }
            step("Hold 'Withdraw' button to sign and send the transaction") {
                confirmStakingActionByHolding()
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingWithdrawnState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingWithdrawnState)
            }
            step("Assert 'Withdraw success' screen is displayed") {
                checkWithdrawSuccessScreen()
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Staking' screen without positions is displayed after the last position is withdrawn") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    var shown = false
                    onStakingDetailsScreen { shown = annualPercentageRate.isDisplayedSafely() && stakeButton.isDisplayedSafely() }
                    if (!shown) {
                        reopenStakingDetails()
                        throw AssertionError("'Stake' button is still missing — the staking balance has not reloaded")
                    }
                }
            }
            step("Assert 'Unstaked' record is not displayed") {
                onStakingDetailsScreen { withdrawEntryTitle.assertDoesNotExist() }
            }
        }
    }

    @AllureId("3552")
    @DisplayName("Staking POL: full unstake")
    @Test
    fun fullUnstakePolTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingInitialState = "StakedLarge"
        val stakingUnstakingState = "UnstakingLarge"
        val yieldScenario = "staking_eth_pol_yield"
        val yieldState = "PartialUnstake"
        val ethBalanceScenario = "eth_network_balance"
        val ethBalanceAfterState = "AfterUnstake"
        val stakedAmount = "5.00"
        var totalBalanceBefore = ""
        var availableBalanceBefore = ""
        var ethFiatBefore = ""

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(yieldScenario, yieldState)
            }
        ).run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingInitialState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingInitialState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Remember 'Ethereum' fiat balance") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { ethFiatBefore = tokenFiatAmountText("Ethereum").extractText() }
                    if (ethFiatBefore.none(Char::isDigit)) throw AssertionError("'Ethereum' balance is not loaded yet")
                }
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Remember 'Total' and 'Available' balances") {
                val balances = readTotalAndAvailableBalance()
                totalBalanceBefore = balances.first
                availableBalanceBefore = balances.second
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Assert staked position amount is '$stakedAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { activeStakingAmount(stakedAmount).assertIsDisplayed() }
                }
            }
            step("Click on active staked balance block") {
                onStakingDetailsScreen { activeStakingBlock.performClick() }
            }
            step("Select the whole staked amount") {
                selectMaxUnstakeAmount()
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.performClick() }
            }
            step("Assert 'Unstake' screen is displayed") {
                checkUnstakeScreen(isAmountEditable = true)
            }
            step("Hold 'Unstake' button to sign and send the transaction") {
                confirmStakingActionByHolding()
            }
            step("Assert 'Unstake success' screen is displayed") {
                checkUnstakeSuccessScreen()
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingUnstakingState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingUnstakingState)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$ethBalanceAfterState'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = ethBalanceAfterState)
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Unstaking' record is displayed in 'Your stakes'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    var shown = false
                    onStakingDetailsScreen { shown = unstakingEntryTitle.isDisplayedSafely() }
                    if (!shown) {
                        reopenStakingDetails()
                        throw AssertionError("'Unstaking' record is still missing — the staking balance has not reloaded")
                    }
                }
            }
            step("Assert 'Unstaking' record amount is '$stakedAmount'") {
                onStakingDetailsScreen { unstakingEntryAmount(stakedAmount).assertIsDisplayed() }
            }
            step("Assert no active staked position is left in 'Your stakes'") {
                onStakingDetailsScreen { stakedEntry.assertDoesNotExist() }
            }
            step("Click on 'Back' button to return to 'Token details' screen") {
                onSendScreen { closeButton.performClick() }
            }
            step("Refresh 'Token details' screen and assert 'Total' and 'Available' balances are unchanged") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    pullToRefreshTokenDetails()
                    val balances = readTotalAndAvailableBalance()
                    assertEquals(totalBalanceBefore, balances.first)
                    assertEquals(availableBalanceBefore, balances.second)
                }
            }
            step("Click on 'Back' button to return to 'Main' screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Ethereum' fiat balance is updated after the fee") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    var ethFiatAfter = ""
                    onMainScreen { ethFiatAfter = tokenFiatAmountText("Ethereum").extractText() }
                    if (ethFiatAfter.none(Char::isDigit)) throw AssertionError("'Ethereum' balance is not loaded yet")
                    assertNotEquals(ethFiatBefore, ethFiatAfter)
                }
            }
        }
    }

    @AllureId("3551")
    @DisplayName("Staking POL: partial unstake")
    @Test
    fun partialUnstakePolTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingInitialState = "StakedLarge"
        val stakingUnstakingState = "PartiallyUnstaking"
        val yieldScenario = "staking_eth_pol_yield"
        val yieldState = "PartialUnstake"
        val ethBalanceScenario = "eth_network_balance"
        val ethBalanceAfterState = "AfterUnstake"
        val stakedAmount = "5.00"
        val unstakeAmount = "2"
        val unstakedAmountDisplayed = "2.00"
        val remainingStakedAmount = "3.00"
        var totalBalanceBefore = ""
        var availableBalanceBefore = ""
        var ethFiatBefore = ""

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(yieldScenario, yieldState)
            }
        ).run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingInitialState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingInitialState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }
            step("Remember 'Ethereum' fiat balance") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { ethFiatBefore = tokenFiatAmountText("Ethereum").extractText() }
                    if (ethFiatBefore.none(Char::isDigit)) throw AssertionError("'Ethereum' balance is not loaded yet")
                }
            }
            step("Click on token with name: '$tokenTitle'") {
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Wait until token balance is loaded before entering staking") {
                waitUntilTokenBalanceIsLoaded()
            }
            step("Remember 'Total' and 'Available' balances") {
                val balances = readTotalAndAvailableBalance()
                totalBalanceBefore = balances.first
                availableBalanceBefore = balances.second
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Assert staked position amount is '$stakedAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { activeStakingAmount(stakedAmount).assertIsDisplayed() }
                }
            }
            step("Click on active staked balance block") {
                onStakingDetailsScreen { activeStakingBlock.performClick() }
            }
            step("Enter unstake amount") {
                enterUnstakeAmount(unstakeAmount)
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.performClick() }
            }
            step("Assert 'Unstake' screen is displayed") {
                checkUnstakeScreen(isAmountEditable = true)
            }
            step("Hold 'Unstake' button to sign and send the transaction") {
                confirmStakingActionByHolding()
            }
            step("Assert 'Unstake success' screen is displayed") {
                checkUnstakeSuccessScreen()
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingUnstakingState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingUnstakingState)
            }
            step("Set WireMock scenario: '$ethBalanceScenario' to state: '$ethBalanceAfterState'") {
                setWireMockScenarioState(scenarioName = ethBalanceScenario, state = ethBalanceAfterState)
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Unstaking' record is displayed in 'Your stakes'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    var shown = false
                    onStakingDetailsScreen { shown = unstakingEntryTitle.isDisplayedSafely() }
                    if (!shown) {
                        reopenStakingDetails()
                        throw AssertionError("'Unstaking' record is still missing — the staking balance has not reloaded")
                    }
                }
            }
            step("Assert 'Unstaking' record amount is '$unstakedAmountDisplayed'") {
                onStakingDetailsScreen { unstakingEntryAmount(unstakedAmountDisplayed).assertIsDisplayed() }
            }
            step("Assert remaining staked position amount is '$remainingStakedAmount'") {
                onStakingDetailsScreen { activeStakingAmount(remainingStakedAmount).assertIsDisplayed() }
            }
            step("Click on 'Back' button to return to 'Token details' screen") {
                onSendScreen { closeButton.performClick() }
            }
            step("Refresh 'Token details' screen and assert 'Total' and 'Available' balances are unchanged") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    pullToRefreshTokenDetails()
                    val balances = readTotalAndAvailableBalance()
                    assertEquals(totalBalanceBefore, balances.first)
                    assertEquals(availableBalanceBefore, balances.second)
                }
            }
            step("Click on 'Back' button to return to 'Main' screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Ethereum' fiat balance is updated after the fee") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    var ethFiatAfter = ""
                    onMainScreen { ethFiatAfter = tokenFiatAmountText("Ethereum").extractText() }
                    if (ethFiatAfter.none(Char::isDigit)) throw AssertionError("'Ethereum' balance is not loaded yet")
                    assertNotEquals(ethFiatBefore, ethFiatAfter)
                }
            }
        }
    }

    @AllureId("11579")
    @DisplayName("Staking POL: unstake an amount between the minimum and the staked amount")
    @Test
    fun unstakeAmountPolTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "HotWalletSvS"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "PolStakingEthereum"
        val stakingScenario = "staking_eth_pol_balances"
        val stakingInitialState = "StakedLarge"
        val stakingUnstakingState = "PartiallyUnstaking"
        val yieldScenario = "staking_eth_pol_yield"
        val yieldState = "PartialUnstake"
        val unstakeAmount = "2"
        val remainingStakedAmount = "3.00"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(yieldScenario, yieldState)
            }
        ).run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingInitialState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingInitialState)
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
            step("Assert staked token amount is displayed in 'Staking' banner") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { stakingTokenAmount.assertIsDisplayed() }
                }
            }
            step("Click on 'Staking block'") {
                onTokenDetailsScreen { stakingBlock.clickWithAssertion() }
            }
            step("Click on active staked balance block") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { activeStakingBlock.assertIsDisplayed() }
                }
                onStakingDetailsScreen { activeStakingBlock.performClick() }
            }
            step("Enter unstake amount") {
                enterUnstakeAmount(unstakeAmount)
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.performClick() }
            }
            step("Assert 'Unstake' screen is displayed") {
                checkUnstakeScreen(isAmountEditable = true)
            }
            step("Hold 'Unstake' button to sign and send the transaction") {
                confirmStakingActionByHolding()
            }
            step("Assert 'Unstake success' screen is displayed") {
                checkUnstakeSuccessScreen()
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingUnstakingState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingUnstakingState)
            }
            step("Click on 'Close' button") {
                onStakingSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Unstaking' record is displayed in 'Your stakes'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG, intervalMs = 5_000) {
                    var shown = false
                    onStakingDetailsScreen { shown = unstakingEntryTitle.isDisplayedSafely() }
                    if (!shown) {
                        reopenStakingDetails()
                        throw AssertionError("'Unstaking' record is still missing — the staking balance has not reloaded")
                    }
                }
            }
            step("Assert remaining staked position amount is '$remainingStakedAmount'") {
                onStakingDetailsScreen { activeStakingAmount(remainingStakedAmount).assertIsDisplayed() }
            }
        }
    }
}