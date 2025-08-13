package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.MarketTooltipTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class MarketsTooltipPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MarketsTooltipPageObject>(semanticsProvider = semanticsProvider) {

    val contentContainer: KNode = child {
        hasTestTag(MarketTooltipTestTags.CONTAINER)
    }
}

internal fun BaseTestCase.onMarketsTooltipScreen(function: MarketsTooltipPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)