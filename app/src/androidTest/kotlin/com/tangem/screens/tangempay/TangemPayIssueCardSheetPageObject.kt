package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayIssueCardSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayIssueCardSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_issue_additional_card_title))
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_issue_additional_card_description))
        useUnmergedTree = true
    }

    // The 'Issue fee' label is also rendered by the order-type screen underneath, so the row is
    // addressed by its tagged value instead of the shared label text.
    val feeValue: KNode = child {
        hasTestTag(TangemPayTestTags.ISSUE_ADDITIONAL_CARD_FEE_VALUE)
        useUnmergedTree = true
    }

    val issueCardButton: KNode = child {
        hasTestTag(TangemPayTestTags.ISSUE_ADDITIONAL_CARD_CONFIRM_BUTTON)
        useUnmergedTree = true
    }

    val insufficientFundsTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_issue_additional_card_insufficient_funds_title))
        useUnmergedTree = true
    }

    val insufficientFundsSubtitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_issue_additional_card_insufficient_funds_subtitle))
        useUnmergedTree = true
    }

    val addFundsButton: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_card_details_add_funds))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayIssueCardSheet(function: TangemPayIssueCardSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)