package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.StakingSendDetailsScreenTestTags
import com.tangem.core.ui.test.TransactionSuccessScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class StakingSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StakingSuccessPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TITLE)
        useUnmergedTree = true
    }

    val transactionDate: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TRANSACTION_DATE)
        useUnmergedTree = true
    }

    val amountBlock: KNode = child {
        hasTestTag(StakingSendDetailsScreenTestTags.AMOUNT_BLOCK)
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

    val exploreButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_explore))
    }

    val shareButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_share))
    }

    val closeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_close))
    }
}

internal fun BaseTestCase.onStakingSuccessScreen(function: StakingSuccessPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)