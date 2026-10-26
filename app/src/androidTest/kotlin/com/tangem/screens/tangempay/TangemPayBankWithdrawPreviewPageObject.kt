package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankRoute
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayBankWithdrawPreviewPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayBankWithdrawPreviewPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_withdraw_bank_confirm_title))
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_PREVIEW_SCREEN))
        useUnmergedTree = true
    }

    val sendButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_send))
        hasClickAction()
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_PREVIEW_SCREEN))
    }

    fun amountCard(amount: String, balance: String): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_PREVIEW_AMOUNT_CARD)
        hasAnyDescendant(withText(amount))
        hasAnyDescendant(withText(balance))
        useUnmergedTree = true
    }

    fun routeRow(route: TangemPayBankRoute): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_ROUTE_ROW)
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_PREVIEW_SCREEN))
        hasAnyDescendant(withText(route.title))
        hasAnyDescendant(withText(route.eta))
        hasAnyDescendant(withText(route.fee))
        useUnmergedTree = true
    }

    fun recipientCard(value: String): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_PREVIEW_RECIPIENT_CARD)
        hasAnyDescendant(withText(value))
        useUnmergedTree = true
    }

    fun detailValue(value: String): KNode = child {
        hasText(value)
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_PREVIEW_SCREEN))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayBankWithdrawPreviewScreen(
    function: TangemPayBankWithdrawPreviewPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)