package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.polymarket.impl.placeprediction.entity.CollateralUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.utils.transformer.Transformer

internal class SetCollateralTransformer(private val currency: CryptoCurrency) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM = prevState.copy(
        collateral = CollateralUM(
            tokenSymbol = currency.symbol,
            iconState = CryptoCurrencyToIconStateConverter().convert(currency),
        ),
    )
}