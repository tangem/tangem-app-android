package com.tangem.features.feed.components.v2

import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.foryou.TokenSummaryComponent

internal fun TokenSummaryComponent.Token.toRouteToken(): FeedRoute.TokenSummary.Token {
    return when (this) {
        is TokenSummaryComponent.Token.Portfolio -> FeedRoute.TokenSummary.Token.Portfolio(
            cryptoCurrency = cryptoCurrency,
        )
        is TokenSummaryComponent.Token.Market -> FeedRoute.TokenSummary.Token.Market(
            cryptoCurrencyRawId = cryptoCurrencyRawId,
            symbol = symbol,
            title = title,
            tangemIconUrl = tangemIconUrl,
            networks = networks,
        )
    }
}

internal fun FeedRoute.TokenSummary.Token.toComponentToken(): TokenSummaryComponent.Token {
    return when (this) {
        is FeedRoute.TokenSummary.Token.Portfolio -> TokenSummaryComponent.Token.Portfolio(
            cryptoCurrency = cryptoCurrency,
        )
        is FeedRoute.TokenSummary.Token.Market -> TokenSummaryComponent.Token.Market(
            cryptoCurrencyRawId = cryptoCurrencyRawId,
            symbol = symbol,
            title = title,
            tangemIconUrl = tangemIconUrl,
            networks = networks,
        )
    }
}