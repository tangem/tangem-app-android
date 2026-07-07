package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.MarketTokenDetailsBottomSheetTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText
import com.tangem.core.ui.R as CoreUiR

class MarketsTokenDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MarketsTokenDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val swapPortfolioQuickActionButton: KNode = child {
        hasTestTag(MarketTokenDetailsBottomSheetTestTags.PORTFOLIO_QUICK_ACTION_BUTTON)
        hasText(getResourceString(R.string.common_swap), substring = true)
    }

    val inYourPortfolioBlock: KNode = child {
        hasText(getResourceString(CoreUiR.string.markets_portfolio_block_subtitle), substring = true)
        useUnmergedTree = true
    }

    fun tokenWithTitle(title: String): KNode = child {
        hasAnyChild(withText(title))
        hasClickAction()
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onMarketsTokenDetailsScreen(function: MarketsTokenDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)