package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag

class TangemPayTransactionDetailsSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayTransactionDetailsSheetPageObject>(semanticsProvider = semanticsProvider) {

    val feeTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangem_pay_fee_title))
        useUnmergedTree = true
    }

    val serviceFeesCategory: KNode = child {
        hasText(getResourceString(CoreResR.string.tangem_pay_fee_subtitle))
        useUnmergedTree = true
    }

    val amount: KNode = child {
        hasTestTag(TangemPayTestTags.TRANSACTION_DETAILS_AMOUNT)
        useUnmergedTree = true
    }

    val purchaseTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangem_pay_purchase))
        useUnmergedTree = true
    }

    val completedStatus: KNode = child {
        hasText(getResourceString(CoreResR.string.tangem_pay_status_completed))
        useUnmergedTree = true
    }

    val getHelpButton: KNode = child {
        hasText(getResourceString(CoreResR.string.tangem_pay_get_help))
        useUnmergedTree = true
    }

    val cardFieldTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_common_card))
        hasAnyAncestor(withTestTag(TangemPayTestTags.TRANSACTION_DETAILS_CARD_ROW))
        useUnmergedTree = true
    }

    // The row stacks its subvalue under its value, so scoping to the Card row pins the name to the number.
    fun cardFieldValue(text: String): KNode = child {
        hasText(text)
        hasAnyAncestor(withTestTag(TangemPayTestTags.TRANSACTION_DETAILS_CARD_ROW))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayTransactionDetailsSheet(
    function: TangemPayTransactionDetailsSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)