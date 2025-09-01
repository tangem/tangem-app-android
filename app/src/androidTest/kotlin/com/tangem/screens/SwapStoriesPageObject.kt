package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.SwapStoriesScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class SwapStoriesPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapStoriesPageObject>(semanticsProvider = semanticsProvider) {

    val closeButton: KNode = child {
        hasTestTag(SwapStoriesScreenTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSwapStoriesScreen(function: SwapStoriesPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)