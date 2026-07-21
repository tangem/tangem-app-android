package com.tangem.features.foryou.impl.tokensummary.model.transformer

import com.tangem.domain.markets.CoinIndicators
import com.tangem.features.foryou.impl.model.converter.ForYouPeriod
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryUm
import com.tangem.features.foryou.impl.tokensummary.model.converter.TokenSentimentConverter
import com.tangem.utils.transformer.Transformer

internal class SetTokenSentimentTransformer(
    private val coinIndicators: CoinIndicators?,
    private val periodId: String?,
) : Transformer<TokenSummaryUm> {

    override fun transform(prevState: TokenSummaryUm): TokenSummaryUm {
        return prevState.copy(
            tokenSentiment = when {
                coinIndicators != null -> TokenSentimentConverter(
                    timeframe = ForYouPeriod.fromId(periodId).timeframe,
                ).convert(coinIndicators)
                else -> TokenSentimentUM.Empty
            },
        )
    }
}