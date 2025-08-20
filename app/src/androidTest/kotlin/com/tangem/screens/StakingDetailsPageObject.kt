package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.*
import com.tangem.features.tokendetails.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.features.staking.impl.R as StakingImplR
import androidx.compose.ui.test.hasTestTag as withTestTag

class StakingDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StakingDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.SCREEN_CONTAINER)
    }

    val stakingTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        useUnmergedTree = true
    }

    val bannerImage: KNode = child {
        hasTestTag(StakingDetailsScreenTestTags.BANNER_IMAGE)
        useUnmergedTree = true
    }

    val bannerText: KNode = child {
        hasTestTag(StakingDetailsScreenTestTags.BANNER_TEXT)
        useUnmergedTree = true
    }

    val annualPercentageRate: KNode = child {
        hasParent(withTestTag(StakingDetailsScreenTestTags.PARAMETER_BLOCK))
        hasTestTag(StakingDetailsScreenTestTags.PARAMETER_NAME)
        hasText(getResourceString(StakingImplR.string.staking_details_annual_percentage_rate))
        useUnmergedTree = true
    }

    val availableBlock: KNode = child {
        hasParent(withTestTag(StakingDetailsScreenTestTags.PARAMETER_BLOCK))
        hasTestTag(StakingDetailsScreenTestTags.PARAMETER_NAME)
        hasText(getResourceString(StakingImplR.string.staking_details_available))
        useUnmergedTree = true

    }

    val unbondingPeriodBlock: KNode = child {
        hasParent(withTestTag(StakingDetailsScreenTestTags.PARAMETER_BLOCK))
        hasTestTag(StakingDetailsScreenTestTags.PARAMETER_NAME)
        hasText(getResourceString(StakingImplR.string.staking_details_unbonding_period))
        useUnmergedTree = true
    }

    val rewardClaimingBlock: KNode = child {
        hasParent(withTestTag(StakingDetailsScreenTestTags.PARAMETER_BLOCK))
        hasTestTag(StakingDetailsScreenTestTags.PARAMETER_NAME)
        hasText(getResourceString(StakingImplR.string.staking_details_reward_claiming))
        useUnmergedTree = true
    }

    val rewardScheduleBlock: KNode = child {
        hasParent(withTestTag(StakingDetailsScreenTestTags.PARAMETER_BLOCK))
        hasTestTag(StakingDetailsScreenTestTags.PARAMETER_NAME)
        hasText(getResourceString(StakingImplR.string.staking_details_reward_schedule))
        useUnmergedTree = true
    }

    val rewardsBlock: KNode = child {
        hasTestTag(BaseBlockTestTags.BLOCK)
        useUnmergedTree = true
    }

    val rewardsBlockTitle: KNode = child {
        hasTestTag(BaseBlockTestTags.BLOCK_TITLE)
        useUnmergedTree = true
    }

    val rewardsBlockText: KNode = child {
        hasTestTag(BaseBlockTestTags.BLOCK_TEXT)
        useUnmergedTree = true
    }

    val yourStakesTitle: KNode = child {
        hasText(getResourceString(StakingImplR.string.staking_your_stakes))
        useUnmergedTree = true
    }

    val activeStakingBlock: KNode = child {
        hasTestTag(StakingDetailsScreenTestTags.ACTIVE_STAKING_BLOCK)
        useUnmergedTree = true
    }

    val toSText: KNode = child {
        hasTestTag(StakingDetailsScreenTestTags.TOS_TEXT)
        useUnmergedTree = true
    }

    val stakeMoreButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.staking_stake_more))
        useUnmergedTree = true
    }

    val stakeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_stake))
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onStakingDetailsScreen(function: StakingDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)