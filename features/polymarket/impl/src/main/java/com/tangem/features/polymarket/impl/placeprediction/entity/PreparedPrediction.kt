package com.tangem.features.polymarket.impl.placeprediction.entity

import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute

/** The conditions accepted on Amount, shared by the summary display and the signing request. */
internal data class PreparedPrediction(
    val params: PlacePredictionComponent.Params,
    val state: PlacePredictionUM,
    val isNegRisk: Boolean,
    val positionId: PolymarketPositionId?,
) {

    val quote: SignableQuoteUM = requireNotNull((state.quote as? QuoteUM.Content)?.signable)

    init {
        require(state.isPrimaryButtonEnabled)
    }
}

internal fun PreparedPrediction.toStatusRoute(result: PlaceResultUM): PlacePredictionRoute.Status =
    PlacePredictionRoute.Status(
        result = result,
        market = state.market,
        side = params.side,
        userWalletId = params.userWalletId,
        positionId = positionId,
    )