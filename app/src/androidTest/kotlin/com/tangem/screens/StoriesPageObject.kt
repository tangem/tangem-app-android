package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.StoriesScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class StoriesPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<StoriesPageObject>(semanticsProvider = semanticsProvider) {

    val scanButton: KNode = child {
        hasTestTag(StoriesScreenTestTags.SCAN_BUTTON)
    }

    val orderButton: KNode = child {
        hasTestTag(StoriesScreenTestTags.ORDER_BUTTON)
    }
}

internal fun BaseTestCase.onStoriesScreen(function: StoriesPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)