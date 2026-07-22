package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasText as withText

class TangemPayKycSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayKycSheetPageObject>(semanticsProvider = semanticsProvider) {

    fun title(text: String): KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(text)
        useUnmergedTree = true
    }

    fun primaryButtonWithText(text: String): KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        hasAnyDescendant(withText(text))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayKycSheet(function: TangemPayKycSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)