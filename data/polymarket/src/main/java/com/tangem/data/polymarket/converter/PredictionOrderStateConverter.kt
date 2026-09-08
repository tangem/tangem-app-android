package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobOrderFormat
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketOrderResponse
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal object PredictionOrderStateConverter : Converter<PolymarketOrderResponse, PredictionOrderState> {

    override fun convert(value: PolymarketOrderResponse): PredictionOrderState = PredictionOrderState(
        status = if (value.status?.trim() == PolymarketClobOrderFormat.STATUS_MATCHED) {
            PredictionOrderStatus.Matched
        } else {
            PredictionOrderStatus.Other(raw = value.status)
        },
        price = value.price?.toBigDecimalOrNull(),
        originalSize = value.originalSize?.toShares(),
        sizeMatched = value.sizeMatched?.toShares(),
    )

    private fun String.toShares(): BigDecimal? =
        toBigDecimalOrNull()?.movePointLeft(PolymarketClobOrderFormat.SHARE_DECIMALS)
}