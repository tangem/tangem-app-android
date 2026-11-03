package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

internal object SetQuoteRequestEndedTransformer : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val quote = prevState.quote as? QuoteUM.Loading ?: return prevState
        if (!quote.isAnswerExpected) return prevState

        return prevState.copy(quote = QuoteUM.Loading(isAnswerExpected = false)).recomputeGate()
    }
}