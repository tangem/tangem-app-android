package com.tangem.features.polymarket.impl.placeprediction.entity

import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import java.math.BigDecimal

/** The conditions accepted on Amount, shared by the summary display and the signing request. */
internal data class PreparedPrediction(
    val params: PlacePredictionComponent.Params,
    val state: PlacePredictionUM,
    val isNegRisk: Boolean,
    val positionId: PolymarketPositionId?,
    val awaitLiveResolutionDateMillis: suspend () -> Long?,
) {

    val quote: SignableQuoteUM = requireNotNull((state.quote as? QuoteUM.Content)?.signable)

    init {
        require(state.isReadyToPlace)
    }
}

internal suspend fun PreparedPrediction.toStatusRoute(
    result: PlaceResultUM,
    current: PlacePredictionUM,
): PlacePredictionRoute.Status = PlacePredictionRoute.Status(
    result = result,
    market = state.market.copy(
        resolutionDateMillis = state.market.resolutionDateMillis ?: awaitLiveResolutionDateMillis(),
    ),
    side = params.side,
    userWalletId = params.userWalletId,
    positionId = positionId,
    isPositionClosed = current.closesPosition(result = result),
)

private fun PlacePredictionUM.closesPosition(result: PlaceResultUM): Boolean {
    val held = ((order as? OrderSideUM.Sell)?.position as? SellPositionUM.Held)?.available ?: return false
    val sold = when (result) {
        is PlaceResultUM.Filled -> result.shares
        is PlaceResultUM.PartiallyFilled -> result.shares
        is PlaceResultUM.NotFilled,
        is PlaceResultUM.Submitted,
        is PlaceResultUM.StillProcessing,
        -> null
    } ?: return false
    return held - sold < DUST_SHARES
}

private val DUST_SHARES = BigDecimal("0.01")