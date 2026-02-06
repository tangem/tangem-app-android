package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.MarketTokenDetailsBottomSheetTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class MarketsTokenDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MarketsTokenDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val swapPortfolioQuickActionButton: KNode = child {
        hasTestTag(MarketTokenDetailsBottomSheetTestTags.PORTFOLIO_QUICK_ACTION_BUTTON)
        hasText(getResourceString(R.string.common_swap), substring = true)
    }

    fun tokenWithTitle(title: String): KNode = child {
        hasAnyAncestor(withTestTag(MarketTokenDetailsBottomSheetTestTags.PORTFOLIO_TOKEN_ITEM))
        hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
        hasAnySibling(withTestTag(TokenElementsTestTags.TOKEN_ICON))
        hasAnyChild(withText(title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onMarketsTokenDetailsScreen(function: MarketsTokenDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)