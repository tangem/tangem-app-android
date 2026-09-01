package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class SetSlippageTransformer(private val percent: BigDecimal) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM = prevState.copy(
        slippage = SlippageUM(
            percent = percent,
            isDefault = percent.compareTo(DEFAULT_SLIPPAGE_PERCENT) == 0,
        ),
    ).recomputeGate()
}