package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseAmountBlockTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.SendScreenTestTags
import com.tangem.core.ui.test.StakingSendDetailsScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class StakingConfirmPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StakingConfirmPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        useUnmergedTree = true
    }

    val amountBlock: KNode = child {
        hasTestTag(StakingSendDetailsScreenTestTags.AMOUNT_BLOCK)
        useUnmergedTree = true
    }

    val hintText: KNode = child {
        hasTestTag(SendScreenTestTags.FOOTER_TEXT)
        useUnmergedTree = true
    }

    val primaryAmount: KNode = child {
        hasTestTag(BaseAmountBlockTestTags.PRIMARY_AMOUNT)
        useUnmergedTree = true
    }

    val secondaryAmount: KNode = child {
        hasTestTag(BaseAmountBlockTestTags.SECONDARY_AMOUNT)
        useUnmergedTree = true
    }

    val validatorBlock: KNode = child {
        hasTestTag(StakingSendDetailsScreenTestTags.VALIDATOR_BLOCK)
        useUnmergedTree = true
    }

    val networkFeeBlock: KNode = child {
        hasTestTag(StakingSendDetailsScreenTestTags.NETWORK_FEE_BLOCK)
        useUnmergedTree = true
    }

    val stakeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_stake))
        useUnmergedTree = true
    }

    val unstakeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_unstake))
        useUnmergedTree = true
    }

    val unstakeNotificationTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.common_unstake))
        useUnmergedTree = true
    }

    val claimRewardsButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_claim_rewards))
        useUnmergedTree = true
    }

    val confirmHoldButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        useUnmergedTree = true
    }

    val invalidAmountNotificationTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.send_notification_invalid_amount_title))
        useUnmergedTree = true
    }

    val invalidAmountNotificationMessage: KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(
            getResourceString(R.string.send_notification_invalid_amount_rent_fee).substringBefore("%1\$s"),
            substring = true,
        )
        hasText(text = "SOL", substring = true)
        useUnmergedTree = true
    }

    val withdrawButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.staking_withdraw))
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onStakingConfirmScreen(function: StakingConfirmPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)