package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
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
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Organize tokens' button is displayed") {
                onMainScreen { organizeTokensButton().assertIsDisplayed() }
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
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Organize tokens' button is displayed") {
                onMainScreen { organizeTokensButton().assertIsDisplayed() }
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
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Organize tokens' button is displayed") {
                onMainScreen { organizeTokensButton().assertIsDisplayed() }
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
}