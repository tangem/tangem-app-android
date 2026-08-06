package com.tangem.scenarios

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTouchInput
import com.tangem.common.BaseTestCase
import com.tangem.common.R as CommonR
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.extractText
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.screens.onStakingConfirmScreen
import com.tangem.screens.onStakingDetailsScreen
import com.tangem.screens.onStakingSuccessScreen
import com.tangem.screens.onSendScreen
import com.tangem.screens.onTokenDetailsScreen
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step

/**
 * Waits on the token-details screen until the coin balance is actually loaded (the fiat balance shows a
 * digit). The StakingModel freezes the currency-status snapshot on entry, so entering staking before the
 * balance is loaded breaks the sign/confirm flow. Wrap the call in a step at the call site.
 */
fun BaseTestCase.waitUntilTokenBalanceIsLoaded() {
    awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) {
        var balance = ""
        onTokenDetailsScreen { balance = fiatBalance.extractText() }
        if (balance.none(Char::isDigit)) error("Token balance is not loaded yet: '$balance'")
    }
}

fun BaseTestCase.checkStakingDetailsScreen(withStaking: Boolean) {
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
    step("Assert 'ToS' text is displayed") {
        onStakingDetailsScreen { toSText.assertIsDisplayed() }
    }
    if (withStaking) {
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
        step("Assert 'Stake more' button is displayed") {
            onStakingDetailsScreen { stakeMoreButton.assertIsDisplayed() }
        }
    } else {
        step("Assert banner image is displayed") {
            onStakingDetailsScreen { bannerImage.assertIsDisplayed() }
        }
        step("Assert banner text is displayed") {
            onStakingDetailsScreen { bannerText.assertIsDisplayed() }
        }
        step("Assert 'Stake' button is displayed") {
            onStakingDetailsScreen { stakeButton.assertIsDisplayed() }
        }
    }

}
fun BaseTestCase.checkStakingScreen(stakingAmount: String) {
    step("Assert 'Staking' screen is displayed") {
        onSendScreen { screenContainer.assertIsDisplayed() }
    }
    step("Assert top app bar 'Close' button is displayed") {
        onSendScreen { closeButton.assertIsDisplayed() }
    }
    step("Assert 'Send' screen title is displayed") {
        onSendScreen { title.assertIsDisplayed() }
    }
    step("Assert amount container title is displayed") {
        onSendScreen { amountContainerTitle.assertIsDisplayed() }
    }
    step("Assert input text field is displayed") {
        onSendScreen { amountInputTextField.assertIsDisplayed() }
    }
    step("Assert token name is displayed") {
        onSendScreen { tokenName.assertIsDisplayed() }
    }
    step("Assert primary amount is displayed") {
        onSendScreen { primaryAmount.assertIsDisplayed() }
    }
    step("Assert secondary amount is displayed") {
        onSendScreen { secondaryAmount.assertIsDisplayed() }
    }
    step("Type '$stakingAmount' in input text field") {
        onSendScreen {
            amountInputTextField.performClick()
            amountInputTextField.performTextReplacement(stakingAmount)
        }
    }
    step("Assert input text field has value: '$stakingAmount'") {
        onSendScreen { amountInputTextField.assertTextContains(value = stakingAmount, substring = true) }
    }
    step("Assert 'Max' button is displayed") {
        onSendScreen { maxButton.assertIsDisplayed() }
    }
    step("Assert 'Next' button is displayed") {
        onSendScreen { nextButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkStakingConfirmScreen() {
    step("Assert 'Staking confirm' screen title is displayed") {
        onStakingConfirmScreen { title.assertIsDisplayed() }
    }
    step("Assert primary amount is displayed") {
        onStakingConfirmScreen { primaryAmount.assertIsDisplayed() }
    }
    step("Assert secondary amount is displayed") {
        onStakingConfirmScreen { secondaryAmount.assertIsDisplayed() }
    }
    step("Assert 'Validator' block is displayed") {
        onStakingConfirmScreen { validatorBlock.assertIsDisplayed() }
    }
    step("Assert 'Network Fee' block is displayed") {
        onStakingConfirmScreen { networkFeeBlock.assertIsDisplayed() }
    }
    step("Assert 'Stake' button is displayed") {
        onStakingConfirmScreen { stakeButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkStakingConfirmClickabilityAndHint() {
    step("Assert 'Amount' block is displayed and clickable") {
        onStakingConfirmScreen {
            amountBlock.assertIsDisplayed()
            amountBlock.assertHasClickAction()
        }
    }
    step("Assert 'Validator' block is clickable") {
        onStakingConfirmScreen { validatorBlock.assertHasClickAction() }
    }
    step("Assert 'Network Fee' block is not clickable") {
        onStakingConfirmScreen { networkFeeBlock.assertHasNoClickAction() }
    }
    step("Assert staking summary hint is displayed") {
        onStakingConfirmScreen { hintText.assertIsDisplayed() }
    }
}

/**
 * Signs and sends the stake by holding the confirm button (hot wallet). Mirrors [confirmSwapByHolding]:
 * a single long-click, then wait on the result marker (the success 'Explore' button) — NOT on the
 * button disappearing, since its 'Stake' label vanishes the moment the hold loader spins.
 */
fun BaseTestCase.confirmStakingByHolding() {
    // The validator must be loaded before holding — otherwise the sign flow throws
    // 'No validator provided' silently and the loader hangs. The block renders only when the
    // validator state is Data (it early-returns otherwise), so 'displayed' == loaded.
    awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) { onStakingConfirmScreen { validatorBlock.assertIsDisplayed() } }
    // Hold-to-confirm needs a SINGLE stable SemanticsNodeInteraction for a continuous down→up gesture.
    // A KNode / semanticsProvider.onNode re-resolves the node on each performTouchInput, which splits
    // the press from the release; onAllNodes(...)[i] after fetchSemanticsNodes pins one node. The
    // confirm screen has exactly one BASE_BUTTON (the hold button); its 'Stake' label disappears once
    // the loader spins, so match by tag only. onConfirm fires only when the fill animation (~1.5s,
    // frame clock) completes — hold until the success 'Explore' button appears, then release.
    val buttons = composeTestRule.onAllNodes(hasTestTag(BaseButtonTestTags.BUTTON))
    val stakeButton = buttons[buttons.fetchSemanticsNodes().lastIndex]
    val exploreButton = hasText(getResourceString(CommonR.string.common_explore))
    stakeButton.performTouchInput { down(center) }
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
        composeTestRule.onAllNodes(exploreButton, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    stakeButton.performTouchInput { up() }
}

fun BaseTestCase.checkStakingSuccessScreen() {
    step("Assert 'Staking success' screen title is displayed") {
        onStakingSuccessScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Amount' block is displayed") {
        onStakingSuccessScreen { amountBlock.assertIsDisplayed() }
    }
    step("Assert 'Validator' block is displayed") {
        onStakingSuccessScreen { validatorBlock.assertIsDisplayed() }
    }
    step("Assert 'Network Fee' block is displayed") {
        onStakingSuccessScreen { networkFeeBlock.assertIsDisplayed() }
    }
    step("Assert 'Explore' button is displayed") {
        onStakingSuccessScreen { exploreButton.assertIsDisplayed() }
    }
    step("Assert 'Share' button is displayed") {
        onStakingSuccessScreen { shareButton.assertIsDisplayed() }
    }
    step("Assert 'Close' button is displayed") {
        onStakingSuccessScreen { closeButton.assertIsDisplayed() }
    }
}

/**
 * Signs and sends an unstake / claim-rewards / withdraw action by holding the confirm button (hot wallet).
 * Unlike [confirmStakingByHolding], these exit/pending confirm screens hide the validator block
 * (isVisibleOnConfirmation is false for anything but enter/restake), so we wait for the hold button to
 * become enabled (fee loaded) instead of the validator block, then hold until the success 'Explore' button
 * appears.
 */
fun BaseTestCase.confirmStakingActionByHolding() {
    val holdButtonMatcher = hasTestTag(BaseButtonTestTags.BUTTON)
    awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) {
        val nodes = composeTestRule.onAllNodes(holdButtonMatcher)
        nodes[nodes.fetchSemanticsNodes().lastIndex].assertIsEnabled()
    }
    val buttons = composeTestRule.onAllNodes(holdButtonMatcher)
    val unstakeButton = buttons[buttons.fetchSemanticsNodes().lastIndex]
    val exploreButton = hasText(getResourceString(CommonR.string.common_explore))
    unstakeButton.performTouchInput { down(center) }
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
        composeTestRule.onAllNodes(exploreButton, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    unstakeButton.performTouchInput { up() }
}

fun BaseTestCase.checkUnstakeScreen() {
    step("Assert 'Unstake' screen title is displayed") {
        onStakingConfirmScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Amount' block is displayed and not editable") {
        onStakingConfirmScreen {
            amountBlock.assertIsDisplayed()
            amountBlock.assertIsNotEnabled()
        }
    }
    step("Assert 'Network Fee' block is displayed and not clickable") {
        onStakingConfirmScreen {
            networkFeeBlock.assertIsDisplayed()
            networkFeeBlock.assertHasNoClickAction()
        }
    }
    step("Assert 'Unstake' notification is displayed") {
        onStakingConfirmScreen { unstakeNotificationTitle.assertIsDisplayed() }
    }
    step("Assert 'Unstake' button is displayed") {
        onStakingConfirmScreen { unstakeButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkUnstakeSuccessScreen() {
    step("Assert 'Unstake success' screen title is displayed") {
        onStakingSuccessScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Amount' block is displayed") {
        onStakingSuccessScreen { amountBlock.assertIsDisplayed() }
    }
    step("Assert 'Network Fee' block is displayed") {
        onStakingSuccessScreen { networkFeeBlock.assertIsDisplayed() }
    }
    step("Assert 'Explore' button is displayed") {
        onStakingSuccessScreen { exploreButton.assertIsDisplayed() }
    }
    step("Assert 'Share' button is displayed") {
        onStakingSuccessScreen { shareButton.assertIsDisplayed() }
    }
    step("Assert 'Close' button is displayed") {
        onStakingSuccessScreen { closeButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkRentFeeWarningAndDisabledButton() {
    step("Assert 'Invalid amount' rent-fee notification title is displayed") {
        awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) {
            onStakingConfirmScreen { invalidAmountNotificationTitle.assertIsDisplayed() }
        }
    }
    step("Assert rent-fee notification message is displayed") {
        onStakingConfirmScreen { invalidAmountNotificationMessage.assertIsDisplayed() }
    }
    step("Assert action button is disabled") {
        onStakingConfirmScreen { confirmHoldButton.assertIsNotEnabled() }
    }
}

fun BaseTestCase.checkClaimRewardsScreen() {
    step("Assert 'Claim rewards' screen title is displayed") {
        onStakingConfirmScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Amount' block is displayed and not editable") {
        onStakingConfirmScreen {
            amountBlock.assertIsDisplayed()
            amountBlock.assertIsNotEnabled()
        }
    }
    step("Assert 'Network Fee' block is displayed and not clickable") {
        onStakingConfirmScreen {
            networkFeeBlock.assertIsDisplayed()
            networkFeeBlock.assertHasNoClickAction()
        }
    }
    step("Assert 'Claim rewards' button is displayed") {
        onStakingConfirmScreen { claimRewardsButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkWithdrawScreen() {
    step("Assert 'Withdraw' screen title is displayed") {
        onStakingConfirmScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Amount' block is displayed and not editable") {
        onStakingConfirmScreen {
            amountBlock.assertIsDisplayed()
            amountBlock.assertIsNotEnabled()
        }
    }
    step("Assert 'Network Fee' block is displayed and not clickable") {
        onStakingConfirmScreen {
            networkFeeBlock.assertIsDisplayed()
            networkFeeBlock.assertHasNoClickAction()
        }
    }
    step("Assert 'Withdraw' button is displayed") {
        onStakingConfirmScreen { withdrawButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkWithdrawSuccessScreen() {
    step("Assert 'Withdraw success' screen title is displayed") {
        onStakingSuccessScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Amount' block is displayed") {
        onStakingSuccessScreen { amountBlock.assertIsDisplayed() }
    }
    step("Assert 'Network Fee' block is displayed") {
        onStakingSuccessScreen { networkFeeBlock.assertIsDisplayed() }
    }
    step("Assert 'Explore' button is displayed") {
        onStakingSuccessScreen { exploreButton.assertIsDisplayed() }
    }
    step("Assert 'Share' button is displayed") {
        onStakingSuccessScreen { shareButton.assertIsDisplayed() }
    }
    step("Assert 'Close' button is displayed") {
        onStakingSuccessScreen { closeButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkClaimRewardsSuccessScreen() {
    step("Assert 'Claim rewards success' screen title is displayed") {
        onStakingSuccessScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Amount' block is displayed") {
        onStakingSuccessScreen { amountBlock.assertIsDisplayed() }
    }
    step("Assert 'Network Fee' block is displayed") {
        onStakingSuccessScreen { networkFeeBlock.assertIsDisplayed() }
    }
    step("Assert 'Explore' button is displayed") {
        onStakingSuccessScreen { exploreButton.assertIsDisplayed() }
    }
    step("Assert 'Share' button is displayed") {
        onStakingSuccessScreen { shareButton.assertIsDisplayed() }
    }
    step("Assert 'Close' button is displayed") {
        onStakingSuccessScreen { closeButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkStakingDetailsAfterInitialStake() {
    step("Assert 'Your Stakes' title is displayed") {
        onStakingDetailsScreen { yourStakesTitle.assertIsDisplayed() }
    }
    step("Assert 'Active staking' block is displayed") {
        onStakingDetailsScreen { activeStakingBlock.assertIsDisplayed() }
    }
    step("Assert 'Rewards' block is displayed") {
        onStakingDetailsScreen { rewardsBlock.assertIsDisplayed() }
    }
    step("Assert 'Rewards' block shows no accrued rewards yet") {
        onStakingDetailsScreen { rewardsBlockText.assertIsDisplayed() }
    }
}