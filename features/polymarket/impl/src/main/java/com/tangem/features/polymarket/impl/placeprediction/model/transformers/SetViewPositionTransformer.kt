package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.ViewPositionUM
import com.tangem.utils.transformer.Transformer

internal class SetViewPositionTransformer(private val viewPosition: ViewPositionUM) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val result = prevState.submit as? SubmitUM.Result ?: return prevState
        return prevState.copy(submit = result.copy(viewPosition = viewPosition))
    }
}