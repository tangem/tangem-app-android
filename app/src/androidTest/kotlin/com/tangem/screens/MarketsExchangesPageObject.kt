package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class MarketsExchangesPageObject(private val provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MarketsExchangesPageObject>(semanticsProvider = provider) {

    fun allTradingVolumeNodes(): List<SemanticsNode> =
        provider
            .onAllNodes(hasTestTag(TokenElementsTestTags.TOKEN_FIAT_AMOUNT_TEXT))
            .fetchSemanticsNodes()

    fun allExchangeTypeNodes(): List<SemanticsNode> =
        provider
            .onAllNodes(hasParent(hasParent(hasTestTag(TokenElementsTestTags.TOKEN_PRICE))))
            .fetchSemanticsNodes()

    fun allTrustScoreNodes(): List<SemanticsNode> =
        provider
            .onAllNodes(hasParent(hasTestTag(TokenElementsTestTags.TOKEN_CRYPTO_AMOUNT)))
            .fetchSemanticsNodes()

    val exchangesTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.markets_token_details_exchanges_title))
        useUnmergedTree = true
    }

    val exchangeName: KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
    }

    val exchangeLogo: KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_ICON)
    }

    val tradingVolume: KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_FIAT_AMOUNT)
    }

    val exchangeType: KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_PRICE)
    }

    val trustScore: KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_CRYPTO_AMOUNT)
    }

    val tryAgainButton: KNode = child {
        hasText(getResourceString(R.string.alert_button_try_again))
    }

    val errorMessage: KNode = child {
        hasText(getResourceString(R.string.markets_loading_error_title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onMarketsExchangesScreen(function: MarketsExchangesPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)