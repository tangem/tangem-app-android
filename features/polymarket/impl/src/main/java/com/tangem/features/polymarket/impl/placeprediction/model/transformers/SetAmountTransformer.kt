package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.isBelowMinAmount
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

/** Drops the quote the previous amount was priced at, so no screen can show a price for a sum nobody entered. */
internal class SetAmountTransformer(
    private val value: String,
    private val isAmountFromFraction: Boolean = false,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val newState = prevState.copy(amountValue = value, isAmountFromFraction = isAmountFromFraction)
        val isQuotable = newState.enteredAmount() != null && !newState.isBelowMinAmount()
        return newState.copy(
            quote = if (isQuotable) QuoteUM.Loading(isAnswerExpected = false) else QuoteUM.Empty,
        ).recomputeGate()
    }
}