package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketOrderResponse
import com.tangem.utils.converter.Converter

internal object PredictionOrderStateConverter : Converter<PolymarketOrderResponse, PredictionOrderState> {

    private const val STATUS_MATCHED = "ORDER_STATUS_MATCHED"

    private const val SHARE_DECIMALS = 6

    override fun convert(value: PolymarketOrderResponse): PredictionOrderState = PredictionOrderState(
        status = if (value.status?.trim() == STATUS_MATCHED) {
            PredictionOrderStatus.Matched
        } else {
            PredictionOrderStatus.Other(raw = value.status)
        },
        price = value.price?.toBigDecimalOrNull(),
        originalSize = value.originalSize?.toBigDecimalOrNull()?.movePointLeft(SHARE_DECIMALS),
        sizeMatched = value.sizeMatched?.toBigDecimalOrNull()?.movePointLeft(SHARE_DECIMALS),
    )
}