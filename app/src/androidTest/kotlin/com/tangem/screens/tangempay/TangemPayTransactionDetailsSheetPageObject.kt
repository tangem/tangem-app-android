package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

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
}

internal fun BaseTestCase.onTangemPayTransactionDetailsSheet(
    function: TangemPayTransactionDetailsSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)