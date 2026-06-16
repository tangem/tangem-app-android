package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText as withText
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TransactionSuccessScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

// Legacy swap feature's success screen — lacks CONTAINER testTag that SendSuccessPageObject relies on.
class SwapSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapSuccessPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TITLE)
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_close)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSwapSuccessScreen(function: SwapSuccessPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)