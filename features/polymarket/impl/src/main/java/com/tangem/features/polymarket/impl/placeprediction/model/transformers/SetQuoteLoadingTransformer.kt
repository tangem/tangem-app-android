package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

internal object SetQuoteLoadingTransformer : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        return if (prevState.quote is QuoteUM.Empty) {
            prevState.copy(quote = QuoteUM.Loading).recomputeGate()
        } else {
            prevState
        }
    }
}