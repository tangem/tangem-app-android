package com.tangem.features.polymarket.impl.placeprediction.model.transformers

import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.formatPolymarketPrice
import com.tangem.features.polymarket.impl.common.outcomeAccent
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.model.SharesCalculator
import com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate
import com.tangem.utils.transformer.Transformer
import java.time.ZoneId
import java.math.BigDecimal

internal class SetPositionTransformer(private val position: PolymarketPosition.Open) : Transformer<PlacePredictionUM> {

    override fun transform(prevState: PlacePredictionUM): PlacePredictionUM {
        val order = prevState.order as? OrderSideUM.Sell ?: return prevState
        val available = SharesCalculator.available(held = position.size)
        val isResolved = position.resolutionStatus == PolymarketResolutionStatus.RESOLVED

        return prevState.copy(
            market = prevState.market.copy(
                title = position.title,
                imageUrl = position.iconUrl,
                outcomeTitle = position.outcome,
                outcomeAccent = position.outcomeAccent,
                outcomePrice = prevState.market.outcomePrice
                    ?: position.curPrice.formatPolymarketPrice(isResolved = isResolved),
                entryPrice = position.avgPrice.formatPolymarketPrice(isResolved = isResolved),
                resolutionDateMillis = position.endDate
                    ?.atStartOfDay(ZoneId.of(PolymarketConst.EASTERN_TIME_ZONE))
                    ?.toInstant()
                    ?.toEpochMilli(),
            ),
            order = order.copy(position = SellPositionUM.Held(available = available)),
            amountValue = prevState.amountValue.orWholePosition(available = available),
            isMarketAcceptingOrders = position.isAcceptingOrders,
        ).recomputeGate()
    }

    private fun String.orWholePosition(available: BigDecimal): String = when {
        isNotBlank() || available.signum() <= 0 -> this
        else -> available.toPlainString()
    }
}