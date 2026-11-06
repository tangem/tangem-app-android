package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketPricePoint
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPriceHistoryResponse
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

internal object PolymarketPriceHistoryConverter :
    Converter<PolymarketPriceHistoryResponse, List<PolymarketPricePoint>> {

    private val maxTimestampSeconds = Long.MAX_VALUE / TimeUnit.SECONDS.toMillis(1)

    override fun convert(value: PolymarketPriceHistoryResponse): List<PolymarketPricePoint> = value.history
        .mapNotNull { point ->
            val seconds = point.timestampSeconds?.takeIf { it in 0..maxTimestampSeconds } ?: return@mapNotNull null
            val price = point.price?.toBigDecimalOrNull()
                ?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal.ONE } ?: return@mapNotNull null
            PolymarketPricePoint(TimeUnit.SECONDS.toMillis(seconds), price)
        }
        .associateBy { it.timestampMillis }
        .values
        .sortedBy { it.timestampMillis }
}