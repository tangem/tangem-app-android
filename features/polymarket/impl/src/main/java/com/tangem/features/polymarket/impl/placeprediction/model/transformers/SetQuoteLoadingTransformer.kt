package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

/** Only the first quote of a sum shows as loading; a refresh of an already priced sum leaves the numbers up. */
internal object SetQuoteLoadingTransformer : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM = if (prevState.quote is QuoteUM.Content) {
        prevState
    } else {
        prevState.copy(quote = QuoteUM.Loading).recomputeGate()
    }
}