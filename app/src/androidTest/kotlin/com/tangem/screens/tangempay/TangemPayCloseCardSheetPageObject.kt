package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TangemPayCloseCardSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCloseCardSheetPageObject>(semanticsProvider = semanticsProvider) {

    val icon: KNode = child {
        hasTestTag(TangemPayTestTags.CLOSE_CARD_SHEET_ICON)
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasTestTag(TangemPayTestTags.CLOSE_CARD_SHEET_TITLE)
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasTestTag(TangemPayTestTags.CLOSE_CARD_SHEET_DESCRIPTION)
        useUnmergedTree = true
    }

    val cancelButton: KNode = child {
        hasTestTag(TangemPayTestTags.CLOSE_CARD_SHEET_CANCEL_BUTTON)
        useUnmergedTree = true
    }

    val closeCardButton: KNode = child {
        hasTestTag(TangemPayTestTags.CLOSE_CARD_SHEET_CONFIRM_BUTTON)
        useUnmergedTree = true
    }

    val dismissButton: KNode = child {
        hasTestTag(TangemPayTestTags.CLOSE_CARD_SHEET_DISMISS_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayCloseCardSheet(function: TangemPayCloseCardSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)