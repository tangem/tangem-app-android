package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayAddFundsSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayAddFundsSheetPageObject>(semanticsProvider = semanticsProvider) {

    val swapOption: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_topup_swap_title))
        useUnmergedTree = true
    }

    val receiveOption: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_topup_receive_title))
        useUnmergedTree = true
    }

    val bankTransferOption: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_topup_bank_transfer_title))
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_card_details_add_funds))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TangemPayTestTags.ADD_FUNDS_CLOSE_BUTTON)
        useUnmergedTree = true
    }

    /**
     * Every option row carries the same tag, so a specific one is addressed by its position — which is
     * what makes the sheet's option order assertable. Matched on the merged tree on purpose: the row
     * merges its title and description, so both are readable from the returned node.
     */
    fun optionAt(index: Int): KNode = child {
        hasTestTag(TangemPayTestTags.ADD_FUNDS_OPTION)
        hasPosition(index)
    }
}

internal fun BaseTestCase.onTangemPayAddFundsSheet(function: TangemPayAddFundsSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)