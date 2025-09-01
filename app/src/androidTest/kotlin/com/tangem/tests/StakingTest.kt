package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.OpenMainScreenScenario
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class StakingTest : BaseTestCase() {

    @AllureId("3558")
    @DisplayName("Staking: validate staking block on 'Token details' screen")
    @Test
    fun validateStakingBlockTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val balance = TOTAL_BALANCE
        val scenarioName = "staking_eth_pol_balances_android"
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
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = $balance") {
                onMainScreen { walletBalance().assertTextContains(balance) }
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
                onTokenDetailsScreen { stakingTitle.assertIsDisplayed() }
            }
            step("Assert 'Staking fiat amount' is displayed") {
                onTokenDetailsScreen { stakingFiatAmount.assertIsDisplayed() }
            }
            step("Assert 'Staking dot' is displayed") {
                onTokenDetailsScreen { stakingDot.assertIsDisplayed() }
            }
            step("Assert 'Staking token amount' is displayed") {
                onTokenDetailsScreen { stakingTokenAmount.assertIsDisplayed() }
            }
            step("Assert 'Staking block chevron icon' is displayed") {
                onTokenDetailsScreen { stakingChevronIcon.assertIsDisplayed() }
            }
        }
    }

    @AllureId("3550")
    @DisplayName("Staking: validate staking more screens")
    @Test
    fun validateStakingMoreScreensTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val balance = TOTAL_BALANCE
        val scenarioName = "staking_eth_pol_balances_android"
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
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = $balance") {
                onMainScreen { walletBalance().assertTextContains(balance) }
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
            step("Assert 'Title' is displayed") {
                onStakingDetailsScreen { stakingTitle.assertIsDisplayed() }
            }
            step("Assert 'Annual percentage rate' is displayed") {
                onStakingDetailsScreen { annualPercentageRate.assertIsDisplayed() }
            }
            step("Assert 'Available' block is displayed") {
                onStakingDetailsScreen { availableBlock.assertIsDisplayed() }
            }
            step("Assert 'Unbonding Period' block is displayed") {
                onStakingDetailsScreen { unbondingPeriodBlock.assertIsDisplayed() }
            }
            step("Assert 'Reward claiming' block is displayed") {
                onStakingDetailsScreen { rewardClaimingBlock.assertIsDisplayed() }
            }
            step("Assert 'Reward schedule' block is displayed") {
                onStakingDetailsScreen { rewardScheduleBlock.assertIsDisplayed() }
            }
            step("Assert 'Rewards block' is displayed") {
                onStakingDetailsScreen { rewardsBlock.assertIsDisplayed() }
            }
            step("Assert 'Rewards block' title is displayed") {
                onStakingDetailsScreen { rewardsBlockTitle.assertIsDisplayed() }
            }
            step("Assert 'Rewards block' text is displayed") {
                onStakingDetailsScreen { rewardsBlockText.assertIsDisplayed() }
            }
            step("Assert 'Active staking block' is displayed") {
                onStakingDetailsScreen { activeStakingBlock.assertIsDisplayed() }
            }
            step("Assert 'Your stakes' title is displayed") {
                onStakingDetailsScreen { yourStakesTitle.assertIsDisplayed() }
            }
            step("Assert 'ToS' text is displayed") {
                onStakingDetailsScreen { toSText.assertIsDisplayed() }
            }
            step("Assert 'Stake more' button is displayed") {
                onStakingDetailsScreen { stakeMoreButton.assertIsDisplayed() }
            }
            step("Click 'Stake more' button") {
                onStakingDetailsScreen { stakeMoreButton.performClick() }
            }
            step("Assert 'Send' screen is displayed") {
                onStakingSendScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Send' screen title is displayed") {
                onStakingSendScreen { title.assertIsDisplayed() }
            }
            step("Assert amount container title is displayed") {
                onStakingSendScreen { amountContainerTitle.assertIsDisplayed() }
            }
            step("Assert amount container text is displayed") {
                onStakingSendScreen { amountContainerText.assertIsDisplayed() }
            }
            step("Assert input text field is displayed") {
                onStakingSendScreen { amountInputTextField.assertIsDisplayed() }
            }
            step("Assert secondary amount is displayed") {
                onStakingSendScreen { secondaryAmount.assertIsDisplayed() }
            }
            step("Type '$stakingAmount' in input text field") {
                onStakingSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(stakingAmount)
                }
            }
            step("Assert input text field has value: '$stakingAmount'") {
                onStakingSendScreen { amountInputTextField.assertTextContains(value = stakingAmount, substring = true) }
            }
            step("Assert currency button is displayed") {
                onStakingSendScreen { currencyButton.assertIsDisplayed() }
            }
            step("Assert fiat button is displayed") {
                onStakingSendScreen { fiatButton.assertIsDisplayed() }
            }
            step("Assert currency button is displayed") {
                onStakingSendScreen { currencyButton.assertIsDisplayed() }
            }
            step("Assert fiat button is displayed") {
                onStakingSendScreen { fiatButton.assertIsDisplayed() }
            }
            step("Assert 'Max' button is displayed") {
                onStakingSendScreen { maxButton.assertIsDisplayed() }
            }
            step("Assert previous button is displayed") {
                onStakingSendScreen { previousButton.assertIsDisplayed() }
            }
            step("Assert 'Next' button is displayed") {
                onStakingSendScreen { nextButton.assertIsDisplayed() }
            }
            step("Click on 'Next' button") {
                onStakingSendScreen { nextButton.performClick() }
            }
            step("Assert 'Send details' screen title is displayed") {
                onStakingSendDetailsScreen { title.assertIsDisplayed() }
            }
            step("Assert primary amount is displayed") {
                onStakingSendDetailsScreen { primaryAmount.assertIsDisplayed() }
            }
            step("Assert secondary amount is displayed") {
                onStakingSendDetailsScreen { secondaryAmount.assertIsDisplayed() }
            }
            step("Assert 'Validator' block is displayed") {
                onStakingSendDetailsScreen { validatorBlock.assertIsDisplayed() }
            }
            step("Assert 'Network Fee' block is displayed") {
                onStakingSendDetailsScreen { networkFeeBlock.assertIsDisplayed() }
            }
            step("Assert 'Stake' button is displayed") {
                onStakingSendDetailsScreen { stakeButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("3548")
    @DisplayName("Staking: validate staking screens")
    @Test
    fun validateStakingScreensTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val balance = TOTAL_BALANCE
        val scenarioName = "staking_eth_pol_balances_android"
        val scenarioState = "Started"
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
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = $balance") {
                onMainScreen { walletBalance().assertTextContains(balance) }
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
                onTokenDetailsScreen { availableStakingBlockTitle.assertIsDisplayed() }
            }
            step("Assert 'Available staking block' text is displayed") {
                onTokenDetailsScreen { availableStakingBlockText.assertIsDisplayed() }
            }
            step("Assert 'Available staking block' currency icon is displayed") {
                onTokenDetailsScreen { availableStakingBlockCurrencyIcon.assertIsDisplayed() }
            }
            step("Click on 'Stake' button") {
                onTokenDetailsScreen { stakeButton.clickWithAssertion() }
            }
            step("Assert 'Title' is displayed") {
                onStakingDetailsScreen { stakingTitle.assertIsDisplayed() }
            }
            step("Assert banner image is displayed") {
                onStakingDetailsScreen { bannerImage.assertIsDisplayed() }
            }
            step("Assert banner text is displayed") {
                onStakingDetailsScreen { bannerText.assertIsDisplayed() }
            }
            step("Assert 'Annual percentage rate' is displayed") {
                onStakingDetailsScreen { annualPercentageRate.assertIsDisplayed() }
            }
            step("Assert 'Available' block is displayed") {
                onStakingDetailsScreen { availableBlock.assertIsDisplayed() }
            }
            step("Assert 'Unbonding Period' block is displayed") {
                onStakingDetailsScreen { unbondingPeriodBlock.assertIsDisplayed() }
            }
            step("Assert 'Reward claiming' block is displayed") {
                onStakingDetailsScreen { rewardClaimingBlock.assertIsDisplayed() }
            }
            step("Assert 'Reward schedule' block is displayed") {
                onStakingDetailsScreen { rewardScheduleBlock.assertIsDisplayed() }
            }
            step("Assert 'ToS' text is displayed") {
                onStakingDetailsScreen { toSText.assertIsDisplayed() }
            }
            step("Assert 'Stake' button is displayed") {
                onStakingDetailsScreen { stakeButton.assertIsDisplayed() }
            }
            step("Click 'Stake' button") {
                onStakingDetailsScreen { stakeButton.performClick() }
            }
            step("Assert 'Send' screen is displayed") {
                onStakingSendScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Send' screen title is displayed") {
                onStakingSendScreen { title.assertIsDisplayed() }
            }
            step("Assert amount container title is displayed") {
                onStakingSendScreen { amountContainerTitle.assertIsDisplayed() }
            }
            step("Assert amount container text is displayed") {
                onStakingSendScreen { amountContainerText.assertIsDisplayed() }
            }
            step("Assert input text field is displayed") {
                onStakingSendScreen { amountInputTextField.assertIsDisplayed() }
            }
            step("Assert secondary amount is displayed") {
                onStakingSendScreen { secondaryAmount.assertIsDisplayed() }
            }
            step("Type '$stakingAmount' in input text field") {
                onStakingSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(stakingAmount)
                }
            }
            step("Assert input text field has value: '$stakingAmount'") {
                onStakingSendScreen { amountInputTextField.assertTextContains(value = stakingAmount, substring = true) }
            }
            step("Assert currency button is displayed") {
                onStakingSendScreen { currencyButton.assertIsDisplayed() }
            }
            step("Assert fiat button is displayed") {
                onStakingSendScreen { fiatButton.assertIsDisplayed() }
            }
            step("Assert currency button is displayed") {
                onStakingSendScreen { currencyButton.assertIsDisplayed() }
            }
            step("Assert fiat button is displayed") {
                onStakingSendScreen { fiatButton.assertIsDisplayed() }
            }
            step("Assert 'Max' button is displayed") {
                onStakingSendScreen { maxButton.assertIsDisplayed() }
            }
            step("Assert previous button is displayed") {
                onStakingSendScreen { previousButton.assertIsDisplayed() }
            }
            step("Assert 'Next' button is displayed") {
                onStakingSendScreen { nextButton.assertIsDisplayed() }
            }
            step("Click on 'Next' button") {
                onStakingSendScreen { nextButton.performClick() }
            }
            step("Assert 'Send details' screen title is displayed") {
                onStakingSendDetailsScreen { title.assertIsDisplayed() }
            }
            step("Assert primary amount is displayed") {
                onStakingSendDetailsScreen { primaryAmount.assertIsDisplayed() }
            }
            step("Assert secondary amount is displayed") {
                onStakingSendDetailsScreen { secondaryAmount.assertIsDisplayed() }
            }
            step("Assert 'Validator' block is displayed") {
                onStakingSendDetailsScreen { validatorBlock.assertIsDisplayed() }
            }
            step("Assert 'Network Fee' block is displayed") {
                onStakingSendDetailsScreen { networkFeeBlock.assertIsDisplayed() }
            }
            step("Assert 'Stake' button is displayed") {
                onStakingSendDetailsScreen { stakeButton.assertIsDisplayed() }
            }
        }
    }
}