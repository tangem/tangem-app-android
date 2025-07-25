package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TokenDetailsTopBarPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TokenDetailsTopBarPageObject>(semanticsProvider = semanticsProvider) {

    val moreButton: KNode = child {
        hasTestTag(TokenDetailsTopBarTestTags.MORE_BUTTON)
        useUnmergedTree = true
    }

    val backButton: KNode = child {
        hasTestTag(TokenDetailsTopBarTestTags.BACK_BUTTON)
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onTokenDetailsTopBar(function: TokenDetailsTopBarPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)