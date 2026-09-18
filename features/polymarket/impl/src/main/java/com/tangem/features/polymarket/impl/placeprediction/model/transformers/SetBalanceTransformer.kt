package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class SetBalanceTransformer(private val balance: BigDecimal?) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val order = when (val order = prevState.order) {
            is OrderSideUM.Buy -> order.copy(balance = balance)
            is OrderSideUM.Sell -> order.copy(balance = balance)
        }

        return prevState.copy(order = order).recomputeGate()
    }
}