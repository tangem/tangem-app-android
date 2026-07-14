package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TokenMarketBlockTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TokenMarketBlockPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TokenMarketBlockPageObject>(semanticsProvider = semanticsProvider) {

    val block: KNode = child {
        hasTestTag(TokenMarketBlockTestTags.BLOCK)
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasTestTag(TokenMarketBlockTestTags.TITLE)
        useUnmergedTree = true
    }

    val price: KNode = child {
        hasTestTag(TokenMarketBlockTestTags.PRICE)
        useUnmergedTree = true
    }

    val priceChange: KNode = child {
        hasTestTag(TokenMarketBlockTestTags.PRICE_CHANGE)
        useUnmergedTree = true
    }

    val chart: KNode = child {
        hasTestTag(TokenMarketBlockTestTags.CHART)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTokenMarketBlock(function: TokenMarketBlockPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)