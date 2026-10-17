package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.StatusScreenTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag

class TangemPayBankWithdrawSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayBankWithdrawSuccessPageObject>(semanticsProvider = semanticsProvider) {

    fun title(text: String): KNode = child {
        hasTestTag(StatusScreenTestTags.TITLE)
        hasText(text)
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_SUCCESS_SCREEN))
        useUnmergedTree = true
    }

    fun subtitle(text: String): KNode = child {
        hasTestTag(StatusScreenTestTags.DESCRIPTION)
        hasText(text, substring = true)
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_SUCCESS_SCREEN))
        useUnmergedTree = true
    }

    fun detailText(text: String): KNode = child {
        hasText(text)
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_SUCCESS_SCREEN))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_close))
        hasClickAction()
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_SUCCESS_SCREEN))
    }
}

internal fun BaseTestCase.onTangemPayBankWithdrawSuccessScreen(
    function: TangemPayBankWithdrawSuccessPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)