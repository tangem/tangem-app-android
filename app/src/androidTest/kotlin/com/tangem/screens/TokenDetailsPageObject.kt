package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TokenDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TokenDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.SCREEN_CONTAINER)
    }
}

internal fun BaseTestCase.onTokenDetailsScreen(function: TokenDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)