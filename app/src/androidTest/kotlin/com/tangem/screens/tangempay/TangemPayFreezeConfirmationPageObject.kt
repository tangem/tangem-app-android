package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayFreezeConfirmationPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayFreezeConfirmationPageObject>(semanticsProvider = semanticsProvider) {

    val freezeTitle: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangem_pay_freeze_card_alert_title))
        useUnmergedTree = true
    }

    val unfreezeTitle: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangem_pay_unfreeze_card_alert_title))
        useUnmergedTree = true
    }

    val submitButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayFreezeConfirmation(function: TangemPayFreezeConfirmationPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)