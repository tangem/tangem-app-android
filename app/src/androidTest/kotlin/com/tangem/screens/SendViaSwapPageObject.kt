package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseSearchBarTestTags
import com.tangem.core.ui.test.ManageTokensScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasText as withText

class SendViaSwapPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendViaSwapPageObject>(semanticsProvider = semanticsProvider) {

    val closeButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }


    val searchField: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
    }

    fun tokenItem(tokenName: String): KNode = child {
        hasTestTag(ManageTokensScreenTestTags.NETWORK_NAME)
        hasAnyChild(withText(tokenName))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSendViaSwapScreen(function: SendViaSwapPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)