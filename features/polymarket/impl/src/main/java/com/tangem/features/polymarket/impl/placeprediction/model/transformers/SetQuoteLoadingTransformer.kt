package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

/** Only the first quote of a sum shows as loading; a refresh of an already priced sum leaves the numbers up. */
internal object SetQuoteLoadingTransformer : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val quote = prevState.quote
        val hasDisplayedProceeds = prevState.order is OrderSideUM.Sell &&
            quote is QuoteUM.Unavailable && quote.expected != null

        return if (quote is QuoteUM.Content || hasDisplayedProceeds) {
            prevState
        } else {
            prevState.copy(quote = QuoteUM.Loading).recomputeGate()
        }
    }
}