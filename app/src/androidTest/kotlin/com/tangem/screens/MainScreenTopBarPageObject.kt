package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.MainScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class MainScreenTopBarPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainScreenTopBarPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(MainScreenTestTags.TOP_BAR) }
    ) {

    val moreButton: KNode = child {
        hasTestTag(MainScreenTestTags.MORE_BUTTON)
        hasPosition(1)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onMainScreenTopBar(function: MainScreenTopBarPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)