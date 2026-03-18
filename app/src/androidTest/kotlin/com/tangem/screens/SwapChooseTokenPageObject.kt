package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.AppBarWithSearchTestTags
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.test.MarketsTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class SwapChooseTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapChooseTokenPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(R.string.common_choose_token))
    }

    val myTokensTitle: KNode = child {
        hasText(getResourceString(R.string.exchange_tokens_available_tokens_header))
    }

    val searchIcon: KNode = child {
        hasTestTag(AppBarWithSearchTestTags.SEARCH_ICON)
    }

    val searchTextField: KNode = child {
        hasTestTag(AppBarWithSearchTestTags.TEXT_FIELD)
    }

    val noTokensFoundText: KNode = child {
        hasText(getResourceString(R.string.express_token_list_empty_search))
    }

    fun tokenWithTitle(tokenTitle: String, availableForSwap: Boolean = true): KNode = child {
        hasTestTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
        hasAnyDescendant(withText(tokenTitle))
        if (!availableForSwap) {
            hasAnyDescendant(
                withText(
                    getResourceString(R.string.tokens_list_unavailable_to_swap_source_header)
                )
            )
        }
        useUnmergedTree = true
    }

    fun marketsTokenWithTitle(title: String): KNode {
        return child {
            hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
            hasText(title)
        }
    }
}

internal fun BaseTestCase.onSwapChooseTokenScreen(function: SwapChooseTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)