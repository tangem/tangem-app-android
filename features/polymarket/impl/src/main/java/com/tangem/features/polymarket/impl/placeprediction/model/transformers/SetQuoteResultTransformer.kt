package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import arrow.core.Either
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

internal class SetQuoteResultTransformer(
    private val result: Either<PredictionOrderQuoteError, PredictionOrderQuote>,
    private val receivedAtMillis: Long,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        if (prevState.submit !is SubmitUM.Idle) return prevState

        val quote = result.fold(ifLeft = ::toError, ifRight = ::toQuote)
        val updatedState = prevState.copy(
            quote = quote,
            market = if (quote is QuoteUM.Error && prevState.order is OrderSideUM.Sell) {
                prevState.market.copy(isPriceUnavailable = true)
            } else {
                prevState.market
            },
            minAmount = result.fold(ifLeft = { prevState.minAmount }, ifRight = { it.minAmount }),
        )
        val pricedQuote = result.getOrNull()?.takeIf { it.isPlaceable }

        return if (pricedQuote != null) {
            SetOutcomePriceTransformer(price = pricedQuote.averagePrice).transform(updatedState)
        } else {
            updatedState.recomputeGate()
        }
    }

    private fun toError(error: PredictionOrderQuoteError): QuoteUM.Error = QuoteUM.Error(
        reason = when (error) {
            PredictionOrderQuoteError.Network -> QuoteErrorUM.Network
            is PredictionOrderQuoteError.Unknown -> QuoteErrorUM.Unknown
        },
    )

    /** Unavailable quotes may provide proceeds for display but never a signable order. */
    private fun toQuote(quote: PredictionOrderQuote): QuoteUM = when (quote.status) {
        PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY -> QuoteUM.Unavailable(
            status = quote.status,
            expected = quote.expectedExecutionAmount,
        )
        PredictionQuoteStatus.MARKET_CLOSED -> QuoteUM.Unavailable(status = quote.status)
        PredictionQuoteStatus.FULL,
        PredictionQuoteStatus.PARTIAL,
        PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE,
        PredictionQuoteStatus.BELOW_MIN_ORDER_AMOUNT,
        -> QuoteUM.Content(
            status = quote.status,
            expected = quote.expectedExecutionAmount,
            feeTotal = quote.fees.total,
            total = quote.total,
            signable = SignableQuoteUM(quote = quote, receivedAtMillis = receivedAtMillis),
        )
    }
}