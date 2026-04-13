package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.MARKETS_MAIN_NETWORK_SUFFIX
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.BaseSearchBarTestTags
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.core.ui.test.SearchBarTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag

class MarketsPageObject(private val provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MarketsPageObject>(semanticsProvider = provider) {

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

    val searchField: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
    }

    val searchClearButton: KNode = child {
        hasTestTag(SearchBarTestTags.CLEAR_BUTTON)
        useUnmergedTree = true
    }

    val listedOnExchangesCount: KNode = child {
        hasTestTag(MarketsTestTags.LISTED_ON_EXCHANGES_COUNT)
        useUnmergedTree = true
    }

    val listedOnBlockContainer: KNode = child {
        hasText(getResourceString(R.string.markets_token_details_listed_on), substring = true)
    }

    val listedOnEmptyText: KNode = child {
        hasText(getResourceString(R.string.markets_token_details_empty_exchanges))
        useUnmergedTree = true
    }

    val seeAllButton: KNode = child {
        hasTestTag(MarketsTestTags.SEE_ALL_BUTTON)
        useUnmergedTree = true
    }

    val sortButton: KNode = child {
        hasTestTag(MarketsTestTags.SORT_BUTTON)
        useUnmergedTree = true
    }

    val noResultsLabel: KNode = child {
        hasTestTag(MarketsTestTags.NO_RESULTS_LABEL)
        useUnmergedTree = true
    }

    val showTokensUnderCapButton: KNode = child {
        hasTestTag(MarketsTestTags.SHOW_TOKENS_UNDER_CAP_BUTTON)
        useUnmergedTree = true
    }

    fun intervalSegment(intervalId: String): KNode = child {
        hasTestTag("${MarketsTestTags.INTERVAL_SEGMENT}_$intervalId")
        useUnmergedTree = true
    }

    fun sortOption(optionId: String): KNode = child {
        hasTestTag("${MarketsTestTags.SORT_OPTION}_$optionId")
        useUnmergedTree = true
    }

    fun tokenWithTitle(title: String): KNode {
        return child {
            hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
            hasText(title)
        }
    }

    fun tokenItemNameByText(name: String): KNode = child {
        hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM_NAME)
        hasText(name, substring = false, ignoreCase = true)
        useUnmergedTree = true
    }

    fun allTokenNameNodes(): List<SemanticsNode> = provider
        .onAllNodes(hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM_NAME), useUnmergedTree = true)
        .fetchSemanticsNodes()

    fun allPriceChangeNodes(): List<SemanticsNode> = provider
        .onAllNodes(hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM_PRICE_CHANGE), useUnmergedTree = true)
        .fetchSemanticsNodes()

    fun firstPriceChangeText(): String? {
        val node = allPriceChangeNodes().firstOrNull() ?: return null
        return MarketsExtractors.extractTextRecursively(node)
    }

    fun allPriceChangeTexts(): List<String> = allPriceChangeNodes()
        .mapNotNull { MarketsExtractors.extractTextRecursively(it) }

    fun firstTokenNames(count: Int): List<String> = allTokenNameNodes()
        .take(count)
        .mapNotNull { MarketsExtractors.extractTextRecursively(it) }

    /** Token-list-item button containing the name as descendant — useful for sub-checks. */
    fun tokenListItemContaining(text: String): KNode = child {
        hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
        hasText(text, substring = true)
    }

    /** Element with name in token list (descendant of TOKENS_LIST_ITEM). */
    fun tokenListItemNameInside(): KNode = child {
        hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM_NAME)
        hasParent(withTestTag(MarketsTestTags.TOKENS_LIST_ITEM))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onMarketsScreen(function: MarketsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)