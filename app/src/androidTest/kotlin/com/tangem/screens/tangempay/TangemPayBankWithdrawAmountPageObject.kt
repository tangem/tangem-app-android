package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.SendScreenTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayBankWithdrawAmountPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayBankWithdrawAmountPageObject>(semanticsProvider = semanticsProvider) {

    val amountInput: KNode = child {
        hasTestTag(SendScreenTestTags.INPUT_TEXT_FIELD)
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_AMOUNT_SCREEN))
        useUnmergedTree = true
    }

    val sourceRow: KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_SOURCE_ROW)
        useUnmergedTree = true
    }

    // The DS3 button keeps its click and Disabled semantics on the surface above the BASE_BUTTON tag.
    val continueButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_continue))
        hasClickAction()
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_AMOUNT_SCREEN))
    }

    val tokenSheetTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.common_choose_network))
        useUnmergedTree = true
    }

    fun headerTitle(title: String): KNode = child {
        hasText(title)
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_AMOUNT_SCREEN))
        useUnmergedTree = true
    }

    fun sourceRowWith(symbol: String, network: String): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_SOURCE_ROW)
        hasAnyDescendant(withText(symbol))
        hasAnyDescendant(withText(network, substring = true))
        useUnmergedTree = true
    }

    fun tokenRow(symbol: String, subtitlePart: String): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_TOKEN_ROW)
        hasAnyDescendant(withText(symbol))
        hasAnyDescendant(withText(subtitlePart, substring = true))
        useUnmergedTree = true
    }

    fun selectedTokenRow(symbol: String, subtitlePart: String): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_TOKEN_ROW)
        hasAnyDescendant(withText(symbol))
        hasAnyDescendant(withText(subtitlePart, substring = true))
        hasAnyDescendant(withTestTag(TangemPayTestTags.BANK_WITHDRAW_TOKEN_CHECKMARK))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayBankWithdrawAmountScreen(
    function: TangemPayBankWithdrawAmountPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)