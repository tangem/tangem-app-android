package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class BuyTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<BuyTokenPageObject>(semanticsProvider = semanticsProvider) {

    val topAppBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.common_buy))
        useUnmergedTree = true
    }

    private val lazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BuyTokenScreenTestTags.LAZY_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    @OptIn(ExperimentalTestApi::class)
    fun tokenWithTitleAndFiatAmount(tokenTitle: String): KNode {
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
            hasText(tokenTitle)
            useUnmergedTree = true
        }.child<KNode> {
            hasTestTag(TokenElementsTestTags.TOKEN_FIAT_AMOUNT)
            useUnmergedTree = true
        }
    }
}

internal fun BaseTestCase.onBuyTokenScreen(function: BuyTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)