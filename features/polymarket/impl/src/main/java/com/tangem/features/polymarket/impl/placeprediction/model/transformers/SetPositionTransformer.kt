package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.model.SharesCalculator
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.time.ZoneId

internal class SetPositionTransformer(private val position: PolymarketPosition.Open) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val order = prevState.order as? OrderSideUM.Sell ?: return prevState

        return prevState.copy(
            market = prevState.market.copy(
                title = position.title,
                imageUrl = position.iconUrl,
                outcomeTitle = position.outcome,
                outcomePriceCents = prevState.market.outcomePriceCents ?: position.curPrice.movePointRight(2).toInt(),
                resolutionDateMillis = position.endDate
                    ?.atStartOfDay(ZoneId.of(PolymarketConst.EASTERN_TIME_ZONE))
                    ?.toInstant()
                    ?.toEpochMilli(),
            ),
            order = order.copy(available = SharesCalculator.available(held = position.size)),
            isMarketAcceptingOrders = position.isAcceptingOrders,
        ).recomputeGate()
    }
}