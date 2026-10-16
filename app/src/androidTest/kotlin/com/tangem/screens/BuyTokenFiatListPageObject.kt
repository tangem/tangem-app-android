package com.tangem.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.res.R
import com.tangem.core.ui.test.BuyTokenFiatListTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class BuyTokenFiatListPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<BuyTokenFiatListPageObject>(semanticsProvider = semanticsProvider) {


    private val lazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BuyTokenFiatListTestTags.LAZY_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    val errorTitle: KNode = child {
        hasText(getResourceString(R.string.markets_loading_error_title))
        useUnmergedTree = true
    }

    val tryAgainButton: KNode = child {
        hasText(getResourceString(R.string.try_to_load_data_again_button_title))
        useUnmergedTree = true
    }

    fun fiatListItemWithTitle(title: String): KNode {
        return lazyList.child<KNode> {
            hasText(title)
        }
    }
}

internal fun BaseTestCase.onBuyTokenFiatListBottomSheet(function: BuyTokenFiatListPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)