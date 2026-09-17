package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal object SetPositionUnreadableTransformer : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val order = prevState.order as? OrderSideUM.Sell ?: return prevState

        return prevState.copy(
            order = order.copy(available = BigDecimal.ZERO, isPositionReadable = false),
        ).recomputeGate()
    }
}