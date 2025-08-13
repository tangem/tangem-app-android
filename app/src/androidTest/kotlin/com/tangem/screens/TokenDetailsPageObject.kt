package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.features.tokendetails.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TokenDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TokenDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.SCREEN_CONTAINER)
    }

    val title: KNode = child {
        hasTestTag(TokenDetailsScreenTestTags.TOKEN_TITLE)
    }

    private val horizontalActionChips = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TokenDetailsScreenTestTags.HORIZONTAL_ACTION_CHIPS) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    @OptIn(ExperimentalTestApi::class)
    val swapButton: LazyListItemNode = horizontalActionChips.childWith<LazyListItemNode> {
        hasTestTag(TokenDetailsScreenTestTags.ACTION_BUTTON)
        hasText(getResourceString(R.string.common_swap))
    }

    @OptIn(ExperimentalTestApi::class)
    val sellButton: LazyListItemNode = horizontalActionChips.childWith<LazyListItemNode> {
        hasTestTag(TokenDetailsScreenTestTags.ACTION_BUTTON)
        hasText(getResourceString(R.string.common_sell))
    }

    @OptIn(ExperimentalTestApi::class)
    val buyButton: LazyListItemNode = horizontalActionChips.childWith<LazyListItemNode> {
        hasTestTag(TokenDetailsScreenTestTags.ACTION_BUTTON)
        hasText(getResourceString(R.string.common_buy))
    }

}

internal fun BaseTestCase.onTokenDetailsScreen(function: TokenDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)