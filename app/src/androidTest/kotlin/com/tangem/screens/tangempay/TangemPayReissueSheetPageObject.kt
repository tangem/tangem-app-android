package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayReissueSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayReissueSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_reissue_card_title))
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_reissue_card_description))
        useUnmergedTree = true
    }

    val feeLabel: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_reissue_card_fee_label))
        useUnmergedTree = true
    }

    val feeValue: KNode = child {
        hasTestTag(TangemPayTestTags.REISSUE_SHEET_FEE_VALUE)
        useUnmergedTree = true
    }

    val confirmButton: KNode = child {
        hasTestTag(TangemPayTestTags.REISSUE_SHEET_CONFIRM_BUTTON)
        useUnmergedTree = true
    }

    val feeErrorTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_reissue_card_fee_unreachable_error_title))
        useUnmergedTree = true
    }

    val refreshButton: KNode = child {
        hasText(getResourceString(CoreResR.string.warning_button_refresh))
        useUnmergedTree = true
    }

    val insufficientFundsTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_reissue_card_insufficient_funds_title))
        useUnmergedTree = true
    }

    val addFundsButton: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_card_details_add_funds))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayReissueSheet(function: TangemPayReissueSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)