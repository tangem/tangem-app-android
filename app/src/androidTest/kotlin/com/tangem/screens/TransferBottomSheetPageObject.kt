package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class TransferBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TransferBottomSheetPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    val sendButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_send)))
        useUnmergedTree = true
    }

    val swapButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_swap)))
        useUnmergedTree = true
    }

    val sellButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_sell)))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_close)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTransferBottomSheet(function: TransferBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)