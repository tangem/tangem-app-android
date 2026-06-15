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

class AddFundsBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddFundsBottomSheetPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    val buyButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_buy)))
        useUnmergedTree = true
    }

    val swapButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_swap)))
        useUnmergedTree = true
    }

    val receiveButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_receive)))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_close)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onAddFundsBottomSheet(function: AddFundsBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)