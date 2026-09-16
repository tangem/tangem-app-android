package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class SetOutcomePriceTransformer(private val price: BigDecimal) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM = prevState.copy(
        market = prevState.market.copy(
            outcomePriceCents = price.movePointRight(2).toInt(),
            isPriceUnavailable = false,
        ),
        priceError = null,
    ).recomputeGate()
}