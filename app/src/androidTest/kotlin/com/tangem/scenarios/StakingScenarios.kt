package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.screens.onSendScreen
import com.tangem.screens.onStakingConfirmScreen
import com.tangem.screens.onStakingDetailsScreen
import io.qameta.allure.kotlin.Allure.step

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