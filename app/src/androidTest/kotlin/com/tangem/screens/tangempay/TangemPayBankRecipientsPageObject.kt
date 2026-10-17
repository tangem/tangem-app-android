package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasSetTextAction as withSetTextAction
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayBankRecipientsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayBankRecipientsPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_withdraw_bank_recipients_title))
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_RECIPIENTS_SCREEN))
        useUnmergedTree = true
    }

    val searchField: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_RECIPIENTS_SEARCH))
        addSemanticsMatcher(withSetTextAction())
        useUnmergedTree = true
    }

    val noResults: KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_RECIPIENTS_NO_RESULTS)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_withdraw_bank_recipients_search_empty)))
        useUnmergedTree = true
    }

    fun recipientRow(initial: String, name: String, subtitle: String): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_RECIPIENT_ROW)
        hasAnyDescendant(withText(initial))
        hasAnyDescendant(withText(name))
        hasAnyDescendant(withText(subtitle))
        useUnmergedTree = true
    }

    fun recipientRowWithName(name: String): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_RECIPIENT_ROW)
        hasAnyDescendant(withText(name))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayBankRecipientsScreen(function: TangemPayBankRecipientsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)