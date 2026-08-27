package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayViewPinSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayViewPinSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TangemPayTestTags.VIEW_PIN_TITLE)
        useUnmergedTree = true
    }

    val loader: KNode = child {
        hasTestTag(TangemPayTestTags.VIEW_PIN_LOADER)
        useUnmergedTree = true
    }

    // Digits are drawn by transparent-text boxes over a BasicTextField — the PIN itself lives in its edit text.
    val pin: KNode = child {
        hasTestTag(TangemPayTestTags.VIEW_PIN_VALUE)
        useUnmergedTree = true
    }

    // Merged tree: TangemButton carries both the tag and the Disabled semantics on the same merged node.
    val changePinButton: KNode = child {
        hasTestTag(TangemPayTestTags.VIEW_PIN_CHANGE_BUTTON)
    }

    val errorTitle: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.common_error))
        useUnmergedTree = true
    }

    val errorMessage: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.MESSAGE)
        hasText(getResourceString(CoreResR.string.common_unknown_error))
        useUnmergedTree = true
    }

    val errorGotItButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_SECONDARY)
        hasText(getResourceString(CoreResR.string.common_got_it))
    }
}

internal fun BaseTestCase.onTangemPayViewPinSheet(function: TangemPayViewPinSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)