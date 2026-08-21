package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import arrow.core.Either
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

internal class SetQuoteResultTransformer(
    private val result: Either<PredictionOrderQuoteError, PredictionOrderQuote>,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM = prevState.copy(
        quote = result.fold(ifLeft = ::toError, ifRight = ::toQuote),
    ).recomputeGate()

    private fun toError(error: PredictionOrderQuoteError): QuoteUM.Error = QuoteUM.Error(
        reason = when (error) {
            PredictionOrderQuoteError.Network -> QuoteErrorUM.Network
            is PredictionOrderQuoteError.Unknown -> QuoteErrorUM.Unknown
        },
    )

    /**
     * A quote whose status forbids placing carries zeros in every figure, so it becomes [QuoteUM.Unavailable]
     * rather than content the screen would render as prices. `BELOW_MIN_ORDER_SIZE` is the exception the
     * contract makes: it blocks the order but keeps the figures, so the user can see what was attempted.
     */
    private fun toQuote(quote: PredictionOrderQuote): QuoteUM = when (quote.status) {
        PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY,
        PredictionQuoteStatus.MARKET_CLOSED,
        -> QuoteUM.Unavailable(status = quote.status)
        PredictionQuoteStatus.FULL,
        PredictionQuoteStatus.PARTIAL,
        PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE,
        -> QuoteUM.Content(
            status = quote.status,
            expectedShares = quote.expectedExecutionAmount,
            guaranteedShares = quote.shares,
            feeTotal = quote.fees.total,
            total = quote.total,
            minOrderSize = quote.minOrderSize,
        )
    }
}