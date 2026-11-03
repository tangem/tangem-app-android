package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

internal class SetQuoteLoadingTransformer(
    private val isAnswerExpected: Boolean,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val loading = QuoteUM.Loading(isAnswerExpected = isAnswerExpected)

        return when (prevState.quote) {
            loading -> prevState
            QuoteUM.Empty, is QuoteUM.Loading -> prevState.copy(quote = loading).recomputeGate()
            else -> prevState
        }
    }
}