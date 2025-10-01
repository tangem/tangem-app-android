package com.tangem.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.MARKETS_MAIN_NETWORK_SUFFIX
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class MarketsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MarketsPageObject>(semanticsProvider = semanticsProvider) {

    private val lazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(MarketsTestTags.TOKENS_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    val addToPortfolioButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_add_to_portfolio))
        useUnmergedTree = true
    }

    val mainNetworkSwitch: KNode = child<KNode> {
        hasAnyDescendant(withText(MARKETS_MAIN_NETWORK_SUFFIX))
        useUnmergedTree = true
    }.child { hasTestTag(MarketsTestTags.ADD_TO_PORTFOLIO_SWITCH) }

    val topBarBackButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    fun tokenWithTitle(title: String): KNode {
        return lazyList.child<KNode> {
            hasText(title)
            useUnmergedTree = true
        }
    }
}

internal fun BaseTestCase.onMarketsScreen(function: MarketsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)