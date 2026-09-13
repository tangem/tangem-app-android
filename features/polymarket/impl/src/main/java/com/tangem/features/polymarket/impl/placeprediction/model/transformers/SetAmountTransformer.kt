package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

/** Drops the quote the previous amount was priced at, so no screen can show a price for a sum nobody entered. */
internal class SetAmountTransformer(private val value: String) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val newState = prevState.copy(amountValue = value)

        return newState.copy(
            quote = if (newState.enteredAmount() == null) QuoteUM.Empty else QuoteUM.Loading,
        ).recomputeGate()
    }
}