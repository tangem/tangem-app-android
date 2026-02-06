package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.SwapStoriesScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class SwapStoriesPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapStoriesPageObject>(semanticsProvider = semanticsProvider) {

    val container: KNode = child {
        hasTestTag(SwapStoriesScreenTestTags.SCREEN_CONTAINER)
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(SwapStoriesScreenTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    fun progressBarItem(index: Int): KNode = child {
        hasTestTag(SwapStoriesScreenTestTags.PROGRESS_BAR_ITEM)
        hasPosition(index)
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasTestTag(SwapStoriesScreenTestTags.TITLE)
        useUnmergedTree = true
    }

    val subtitle: KNode = child {
        hasTestTag(SwapStoriesScreenTestTags.SUBTITLE)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSwapStoriesScreen(function: SwapStoriesPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)