package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.utils.transformer.Transformer

internal class SetResolutionDateTransformer(private val resolutionDateMillis: Long) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM =
        prevState.copy(market = prevState.market.copy(resolutionDateMillis = resolutionDateMillis))
}