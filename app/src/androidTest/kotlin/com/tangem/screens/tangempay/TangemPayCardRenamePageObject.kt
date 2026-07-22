package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TangemPayCardRenamePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCardRenamePageObject>(semanticsProvider = semanticsProvider) {

    val nameField: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_NAME_TEXT_FIELD)
    }

    val doneButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_RENAME_DONE_BUTTON)
    }

    val closeButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_RENAME_CLOSE_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayCardRenameScreen(function: TangemPayCardRenamePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)