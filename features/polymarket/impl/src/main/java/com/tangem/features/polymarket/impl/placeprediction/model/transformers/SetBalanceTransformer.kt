package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class SetBalanceTransformer(
    private val balance: BigDecimal?,
    private val tokenSymbol: String,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM = prevState.copy(
        payment = prevState.payment.copy(balance = balance, tokenSymbol = tokenSymbol),
    ).recomputeGate()
}