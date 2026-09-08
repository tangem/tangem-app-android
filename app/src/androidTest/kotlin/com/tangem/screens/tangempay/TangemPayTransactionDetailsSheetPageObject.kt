package com.tangem.screens.tangempay

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

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

    val pendingStatus: KNode = statusText(CoreResR.string.tangem_pay_status_pending)

    val declinedStatus: KNode = statusText(CoreResR.string.tangem_pay_status_declined)

    val declinedReason: KNode = statusText(CoreResR.string.tangem_pay_transaction_declined_notification_text)

    val statusIcon: KNode = child {
        hasTestTag(TangemPayTestTags.TRANSACTION_DETAILS_STATUS_ICON)
        useUnmergedTree = true
    }

    val transactionIcon: KNode = child {
        hasTestTag(TangemPayTestTags.TRANSACTION_DETAILS_ICON)
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

    val cardFieldErrorText: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_common_error_loading))
        hasAnyAncestor(withTestTag(TangemPayTestTags.TRANSACTION_DETAILS_CARD_ROW))
        useUnmergedTree = true
    }

    // The refresh control wraps the error text and the icon, and only it carries the click action.
    val cardFieldRetryButton: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.TRANSACTION_DETAILS_CARD_ROW))
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_common_error_loading)))
        hasClickAction()
        useUnmergedTree = true
    }

    /** The transaction date, the merchant name and the category are plain texts, so they are pinned to the sheet. */
    fun date(text: String): KNode = sheetText(text)

    fun merchantName(name: String): KNode = sheetText(name)

    fun category(name: String): KNode = sheetText(name)

    private fun sheetText(text: String): KNode = child {
        hasText(text)
        hasAnyAncestor(withTestTag(BaseBottomSheetTestTags.CONTAINER))
        useUnmergedTree = true
    }

    private fun statusText(@StringRes textRes: Int): KNode = child {
        hasText(getResourceString(textRes))
        hasAnyAncestor(withTestTag(TangemPayTestTags.TRANSACTION_DETAILS_STATUS))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayTransactionDetailsSheet(
    function: TangemPayTransactionDetailsSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)