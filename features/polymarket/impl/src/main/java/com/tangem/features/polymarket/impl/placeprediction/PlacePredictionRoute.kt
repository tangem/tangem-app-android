package com.tangem.features.polymarket.impl.placeprediction

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import java.math.BigDecimal

/**
 * Steps of the place-prediction flow, an inner stack owned by [PlacePredictionComponent].
 *
 * `serializer = null` is used in the stack, so no `@Serializable` is required here.
 */
internal sealed interface PlacePredictionRoute : Route {

    data object Amount : PlacePredictionRoute

    data class Slippage(val value: BigDecimal, val onConfirm: (BigDecimal) -> Unit) : PlacePredictionRoute

    data class Summary(val order: PreparedPrediction) : PlacePredictionRoute

    /** Terminal: must be entered via `replaceAll`, so back never returns to [Summary]. */
    data class Status(
        val result: PlaceResultUM,
        val market: MarketHeaderUM,
        val side: PredictionOrderSide,
        val userWalletId: UserWalletId,
        val positionId: PolymarketPositionId?,
    ) : PlacePredictionRoute
}