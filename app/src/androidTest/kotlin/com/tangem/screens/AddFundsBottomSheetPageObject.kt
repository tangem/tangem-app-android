package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.performScrollToIndex
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.core.ui.test.TokenActionsTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class AddFundsBottomSheetPageObject(private val provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddFundsBottomSheetPageObject>(
        semanticsProvider = provider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    val buyButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_buy)))
        useUnmergedTree = true
    }

    val swapButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_swap)))
        useUnmergedTree = true
    }

    val receiveButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_receive)))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val buyTokenButton: KNode = child {
        hasTestTag(TokenActionsTestTags.BUY_ACTION)
        useUnmergedTree = true
    }

    fun titleWithTokenName(tokenTitle: String): KNode = child {
        hasText(getResourceString(R.string.get_token_title, tokenTitle))
        useUnmergedTree = true
    }

    val marketPulseTitle: KNode = child {
        hasText(getResourceString(R.string.markets_pulse_common_title))
        useUnmergedTree = true
    }

    fun userTokenWithTitle(tokenTitle: String): KNode = child {
        hasTestTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
        hasAnyDescendant(withTestTag(TokenElementsTestTags.TOKEN_TITLE))
        hasAnyDescendant(withText(tokenTitle))
        useUnmergedTree = true
    }

    private val trendingTokensList = KLazyListNode(
        semanticsProvider = provider,
        viewBuilderAction = { hasTestTag(BuyTokenScreenTestTags.LAZY_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(LazyListItemPositionSemantics, position)
        },
    )

    @OptIn(ExperimentalTestApi::class)
    fun trendingTokenWithTitle(tokenTitle: String): LazyListItemNode =
        trendingTokensList.childWith<LazyListItemNode> {
            hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
            hasText(tokenTitle)
            useUnmergedTree = true
        }

    // Raw Compose on purpose: rows below the fold are not composed, so every list index is scrolled to and collected.
    @OptIn(ExperimentalTestApi::class)
    fun assertMarketBlockTokens(expectedTitles: List<String>) {
        val list = provider.onNode(withTestTag(BuyTokenScreenTestTags.LAZY_LIST), useUnmergedTree = true)
        val itemsCount = list.fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount
        val rows = linkedSetOf<String>()
        repeat(itemsCount) { index ->
            if (runCatching { list.performScrollToIndex(index) }.isFailure) return@repeat
            provider.onAllNodes(withTestTag(MarketsTestTags.TOKENS_LIST_ITEM), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .forEach { rows += it.allTexts() }
        }
        val missing = expectedTitles.filter { title -> rows.none { row -> row.contains(title) } }
        if (missing.isNotEmpty() || rows.size != expectedTitles.size) {
            throw AssertionError("Market block rows: $rows, expected tokens: $expectedTitles")
        }
    }
}

private fun SemanticsNode.allTexts(): List<String> {
    val own = config.getOrElse(SemanticsProperties.Text) { emptyList() }.joinToString(" ") { it.text }
    val nested = children.flatMap { it.allTexts() }
    return listOf((listOf(own) + nested).filter { it.isNotBlank() }.joinToString(" ")).filter { it.isNotBlank() }
}

internal fun BaseTestCase.onAddFundsBottomSheet(function: AddFundsBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)