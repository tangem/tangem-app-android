package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.MARKETS_MAIN_NETWORK_SUFFIX
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class MarketsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MarketsPageObject>(semanticsProvider = semanticsProvider) {

    val addToPortfolioButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_add_to_portfolio))
        useUnmergedTree = true
    }

    val mainNetworkSuffix: KNode = child {
        hasText(MARKETS_MAIN_NETWORK_SUFFIX)
    }

    val topBarBackButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val searchThroughMarketPlaceholder: KNode = child {
        hasText(getResourceString(R.string.markets_search_header_title))
        useUnmergedTree = true
    }

    fun tokenWithTitle(title: String): KNode {
        return child {
            hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
            hasText(title)
        }
    }
}

internal fun BaseTestCase.onMarketsScreen(function: MarketsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)