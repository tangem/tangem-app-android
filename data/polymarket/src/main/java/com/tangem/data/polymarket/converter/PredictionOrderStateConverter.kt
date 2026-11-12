package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobOrderFormat
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketOrderResponse
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal object PredictionOrderStateConverter : Converter<PolymarketOrderResponse, PredictionOrderState> {

    override fun convert(value: PolymarketOrderResponse): PredictionOrderState = PredictionOrderState(
        status = value.status.toOrderStatus(),
        price = value.price?.toBigDecimalOrNull(),
        originalSize = value.originalSize?.toShares(),
        sizeMatched = value.sizeMatched?.toShares(),
    )

    private fun String?.toOrderStatus(): PredictionOrderStatus {
        val status = this?.trim() ?: return PredictionOrderStatus.Other(raw = null)
        return when (status) {
            PolymarketClobOrderFormat.STATUS_MATCHED -> PredictionOrderStatus.Matched
            in PolymarketClobOrderFormat.REJECTED_STATUSES -> PredictionOrderStatus.Rejected(raw = status)
            else -> PredictionOrderStatus.Other(raw = this)
        }
    }

    private fun String.toShares(): BigDecimal? =
        toBigDecimalOrNull()?.movePointLeft(PolymarketClobOrderFormat.SHARE_DECIMALS)
}