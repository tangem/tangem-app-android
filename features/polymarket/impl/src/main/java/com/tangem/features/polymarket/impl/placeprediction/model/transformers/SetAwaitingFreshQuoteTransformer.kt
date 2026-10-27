package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

internal class SetAwaitingFreshQuoteTransformer(
    private val isAwaitingFreshQuote: Boolean,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM =
        prevState.copy(isAwaitingFreshQuote = isAwaitingFreshQuote).recomputeGate()
}