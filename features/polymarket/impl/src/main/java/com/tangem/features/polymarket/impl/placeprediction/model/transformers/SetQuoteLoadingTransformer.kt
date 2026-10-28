package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

internal class SetQuoteLoadingTransformer(
    private val isAwaitingFreshQuote: Boolean,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val isQuoteKept = prevState.quote !is QuoteUM.Empty

        return when {
            isQuoteKept && prevState.isAwaitingFreshQuote == isAwaitingFreshQuote -> prevState
            isQuoteKept -> prevState.copy(isAwaitingFreshQuote = isAwaitingFreshQuote).recomputeGate()
            else -> prevState.copy(quote = QuoteUM.Loading, isAwaitingFreshQuote = isAwaitingFreshQuote).recomputeGate()
        }
    }
}