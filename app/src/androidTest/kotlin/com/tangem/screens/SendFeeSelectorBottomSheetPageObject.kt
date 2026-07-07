package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.SelectNetworkFeeBottomSheetTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

/**
 * Gasless fee selector modal: the `NetworkFee` route (fee-paying token row + selected speed) and the
 * `ChooseToken` route. The `ChooseSpeed` route is covered by [SendSelectNetworkFeeBottomSheetPageObject].
 */
class SendFeeSelectorBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendFeeSelectorBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val networkFeeTitle: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.TITLE)
        hasText(getResourceString(R.string.common_network_fee_title))
        useUnmergedTree = true
    }

    val chooseTokenTitle: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.TITLE)
        hasText(getResourceString(R.string.fee_selector_choose_token_title))
        useUnmergedTree = true
    }

    val feeTokenRow: KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
        useUnmergedTree = true
    }

    fun feeTokenItem(tokenName: String): KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
        hasAnyChild(withText(tokenName))
        useUnmergedTree = true
    }

    fun feeSpeedItemTitle(speed: String): KNode = child {
        hasTestTag(SelectNetworkFeeBottomSheetTestTags.REGULAR_ITEM_TITLE)
        hasText(speed)
        useUnmergedTree = true
    }

    val applyButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_apply)))
        useUnmergedTree = true
    }

    val notEnoughFundsError: KNode = child {
        hasText(getResourceString(R.string.gasless_not_enough_funds_to_cover_token_fee))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSendFeeSelectorBottomSheet(function: SendFeeSelectorBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)