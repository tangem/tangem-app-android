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

class TangemPayBankWithdrawErrorPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayBankWithdrawErrorPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(StatusScreenTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangempay_withdraw_bank_error_general_title))
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_ERROR_SCREEN))
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasTestTag(StatusScreenTestTags.DESCRIPTION)
        hasText(getResourceString(CoreResR.string.common_try_again_later))
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_ERROR_SCREEN))
        useUnmergedTree = true
    }

    val retryButton: KNode = button(CoreResR.string.common_retry)

    val closeButton: KNode = button(CoreResR.string.common_close)

    private fun button(textRes: Int): KNode = child {
        hasText(getResourceString(textRes))
        hasClickAction()
        hasAnyAncestor(withTestTag(TangemPayTestTags.BANK_WITHDRAW_ERROR_SCREEN))
    }
}

internal fun BaseTestCase.onTangemPayBankWithdrawErrorScreen(
    function: TangemPayBankWithdrawErrorPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)