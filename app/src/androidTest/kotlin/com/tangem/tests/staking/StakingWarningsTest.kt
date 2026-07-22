package com.tangem.tests.staking

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickAndWaitFor
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class StakingWarningsTest : BaseTestCase() {

    @AllureId("2196")
    @DisplayName("Staking warnings: rent fee warning on unstake, button blocked (SOLANA)")
    @Test
    fun unstakeRentFeeWarningTest() {
        val tokenTitle = "Solana"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "Solana"
        val stakingScenario = "staking_sol_balances"
        val stakingState = "Staked"
        val solanaBalanceScenario = "solana_get_account_info_recipient"
        val solanaBalanceState = "RentBalance"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(portfolioScenario)
                resetWireMockScenarioState(stakingScenario)
                resetWireMockScenarioState(solanaBalanceScenario)
            }
        ).run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingState)
            }
            step("Set WireMock scenario: '$solanaBalanceScenario' to state: '$solanaBalanceState'") {
                setWireMockScenarioState(scenarioName = solanaBalanceScenario, state = solanaBalanceState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
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
            step("Tap the active staked balance block to open unstaking") {
                onStakingDetailsScreen { activeStakingBlock.performClick() }
            }
            step("Check 'Invalid amount' rent-fee warning and blocked button") {
                checkRentFeeWarningAndDisabledButton()
            }
        }
    }

    @AllureId("2233")
    @DisplayName("Staking warnings: rent fee warning on stake, button blocked (SOLANA)")
    @Test
    fun stakeRentFeeWarningTest() {
        val tokenTitle = "Solana"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "Solana"
        val stakingScenario = "staking_sol_balances"
        val stakingState = "Empty"
        val stakeAmount = "0.0375"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(portfolioScenario)
                resetWireMockScenarioState(stakingScenario)
            }
        ).run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
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
            step("Open 'Amount' screen via 'Stake' on 'Staking details'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onStakingDetailsScreen { stakeButton.assertIsDisplayed() }
                }
                onStakingDetailsScreen {
                    stakeButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onSendScreen { amountInputTextField.assertIsDisplayed() }
                        },
                    )
                }
            }
            step("Type '$stakeAmount' in amount input leaving less than the rent on balance") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(stakeAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.performClick() }
            }
            step("Check 'Invalid amount' rent-fee warning and blocked button") {
                checkRentFeeWarningAndDisabledButton()
            }
        }
    }

    @AllureId("10331")
    @DisplayName("Staking warnings: rent fee warning on withdraw, button blocked (SOLANA)")
    @Test
    fun withdrawRentFeeWarningTest() {
        val tokenTitle = "Solana"
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "Solana"
        val stakingScenario = "staking_sol_balances"
        val stakingState = "Withdrawable"
        val solanaBalanceScenario = "solana_get_account_info_recipient"
        val solanaBalanceState = "RentBalance"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(portfolioScenario)
                resetWireMockScenarioState(stakingScenario)
                resetWireMockScenarioState(solanaBalanceScenario)
            }
        ).run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$stakingScenario' to state: '$stakingState'") {
                setWireMockScenarioState(scenarioName = stakingScenario, state = stakingState)
            }
            step("Set WireMock scenario: '$solanaBalanceScenario' to state: '$solanaBalanceState'") {
                setWireMockScenarioState(scenarioName = solanaBalanceScenario, state = solanaBalanceState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
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
            step("Tap the 'Unstaked' record to open withdrawal") {
                onStakingDetailsScreen { activeStakingBlock.performClick() }
            }
            step("Check 'Invalid amount' rent-fee warning and blocked button") {
                checkRentFeeWarningAndDisabledButton()
            }
        }
    }
}