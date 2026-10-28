package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.AmountFractionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.BuyAmountCalculator
import com.tangem.utils.transformer.Transformer

internal class SetAmountFractionTransformer(
    private val fraction: AmountFractionUM,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val balance = (prevState.order as? OrderSideUM.Buy)?.balance?.takeIf { it.signum() > 0 } ?: return prevState
        val amount = BuyAmountCalculator.fraction(
            balance = balance,
            fraction = fraction,
            feeRate = prevState.lastFeeRate,
        )

        return SetAmountTransformer(value = amount.stripTrailingZeros().toPlainString())
            .transform(prevState)
            .copy(isAmountFromFraction = true)
    }
}