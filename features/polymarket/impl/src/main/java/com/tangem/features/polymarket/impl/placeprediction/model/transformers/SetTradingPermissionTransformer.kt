package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer

/**
 * Records the answer of the region check the flow runs for itself.
 *
 * Until it arrives the state says [TradingPermissionUM.Unknown], which keeps the button shut without
 * telling the user they are restricted — a claim only [TradingPermissionUM.Restricted] may make.
 */
internal class SetTradingPermissionTransformer(
    private val permission: TradingPermissionUM,
) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM =
        prevState.copy(tradingPermission = permission).recomputeGate()
}