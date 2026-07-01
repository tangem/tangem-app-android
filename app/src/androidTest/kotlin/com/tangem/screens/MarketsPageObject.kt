package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
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

    val addButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_add))
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
        hasText(getResourceString(R.string.markets_search_title_placeholder))
        useUnmergedTree = true
    }

    val tokenDetailsContent: KNode = child {
        hasTestTag(MarketsTestTags.TOKEN_DETAILS_CONTENT)
        useUnmergedTree = true
    }

    val listedOnExchangesCount: KNode = child {
        hasTestTag(MarketsTestTags.LISTED_ON_EXCHANGES_COUNT)
        useUnmergedTree = true
    }

    val listedOnBlockContainer: KNode = child {
        hasTestTag(MarketsTestTags.LISTED_ON_BLOCK)
        useUnmergedTree = true
    }

    val listedOnEmptyText: KNode = child {
        hasText(getResourceString(R.string.markets_token_details_empty_exchanges))
        useUnmergedTree = true
    }

    val seeAllButton: KNode = child {
        hasText(getResourceString(com.tangem.core.ui.R.string.common_see_all))
        useUnmergedTree = true
    }

    fun tokenWithTitle(title: String): KNode {
        return child {
            hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
            hasText(title)
        }
    }

    @ExperimentalTestApi
    fun scrollToListedOnBlock() {
        tokenDetailsContent {
            performScrollToNode(hasTestTag(MarketsTestTags.LISTED_ON_BLOCK))
        }
    }
}

internal fun BaseTestCase.onMarketsScreen(function: MarketsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)