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
import androidx.compose.ui.test.hasText as withText

class TangemPayBankWithdrawChoicePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayBankWithdrawChoicePageObject>(semanticsProvider = semanticsProvider) {

    val continueButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_continue))
        hasClickAction()
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_CHOICE_SCREEN))
    }

    fun sectionTitle(title: String): KNode = child {
        hasText(title)
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_CHOICE_SCREEN))
        useUnmergedTree = true
    }

    fun option(title: String): KNode = child {
        hasTestTag(TangemPayTestTags.BANK_WITHDRAW_CHOICE_OPTION)
        hasAnyDescendant(withText(title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayBankWithdrawChoiceScreen(
    function: TangemPayBankWithdrawChoicePageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)