package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.StakingSendDetailsScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class StakingSendDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StakingSendDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        useUnmergedTree = true
    }

    val primaryAmount: KNode = child {
        hasTestTag(StakingSendDetailsScreenTestTags.PRIMARY_AMOUNT)
        useUnmergedTree = true
    }

    val secondaryAmount: KNode = child {
        hasTestTag(StakingSendDetailsScreenTestTags.SECONDARY_AMOUNT)
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

}

internal fun BaseTestCase.onStakingSendDetailsScreen(function: StakingSendDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)