package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TangemPayWithdrawNoteSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayWithdrawNoteSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        useUnmergedTree = true
    }

    val gotItButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayWithdrawNoteSheet(function: TangemPayWithdrawNoteSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)