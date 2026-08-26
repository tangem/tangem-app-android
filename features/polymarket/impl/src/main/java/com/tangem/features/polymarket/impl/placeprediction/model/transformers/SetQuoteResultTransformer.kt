package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import arrow.core.Either
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

internal class SetQuoteResultTransformer(
    private val result: Either<PredictionOrderQuoteError, PredictionOrderQuote>,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM = prevState.copy(
        quote = result.fold(ifLeft = ::toError, ifRight = ::toContent),
    ).recomputeGate()

    private fun toError(error: PredictionOrderQuoteError): QuoteUM.Error = QuoteUM.Error(
        reason = when (error) {
            PredictionOrderQuoteError.Network -> QuoteErrorUM.Network
            is PredictionOrderQuoteError.Unknown -> QuoteErrorUM.Unknown
        },
    )

    private fun toContent(quote: PredictionOrderQuote): QuoteUM.Content = QuoteUM.Content(
        status = quote.status,
        shares = quote.shares,
        toWin = quote.shares,
        feeTotal = quote.fees.total,
        total = quote.total,
        minOrderSize = quote.minOrderSize,
    )
}