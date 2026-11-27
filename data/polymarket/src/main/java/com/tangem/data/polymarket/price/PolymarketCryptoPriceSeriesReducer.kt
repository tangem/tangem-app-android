package com.tangem.data.polymarket.price

import com.tangem.core.socket.SocketState
import com.tangem.domain.polymarket.model.PolymarketCryptoPricePoint
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries.Status
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceMessageDto
import java.math.BigDecimal

/**
 * Folds the frames of the live-data socket into a bounded series.
 *
 * A snapshot arrives on every subscription, a reconnect included, and replaces everything from its first point on;
 * a tick is taken only when it is newer than the last point. The series keeps [HISTORY_MILLIS] behind its newest
 * point: the two minutes the chart shows plus its render delay and overscan.
 */
internal object PolymarketCryptoPriceSeriesReducer {

    private const val HISTORY_MILLIS = 127_500L
    private const val MAX_POINTS = 3_600

    fun reduce(series: PolymarketCryptoPriceSeries, message: PolymarketPriceMessageDto): PolymarketCryptoPriceSeries {
        if (message.op == PolymarketPriceMessageDto.OP_ERROR) return series.copy(status = Status.UNAVAILABLE)
        val payload = message.payload ?: return series
        val snapshot = payload.points

        val points = if (snapshot != null) {
            val fresh = snapshot.mapNotNull { point(it.timestamp, it.fullAccuracyValue, it.value) }
                .sortedBy(PolymarketCryptoPricePoint::timestampMillis)
                .distinctBy(PolymarketCryptoPricePoint::timestampMillis)
            val start = fresh.firstOrNull()?.timestampMillis ?: return series
            series.points.takeWhile { it.timestampMillis < start } + fresh
        } else {
            val tick = point(payload.timestamp, payload.fullAccuracyValue, payload.value) ?: return series
            val last = series.points.lastOrNull()
            if (last != null && tick.timestampMillis <= last.timestampMillis) return series
            series.points + tick
        }

        return PolymarketCryptoPriceSeries(points = points.trimmed(), status = Status.LIVE)
    }

    fun reduce(series: PolymarketCryptoPriceSeries, state: SocketState): PolymarketCryptoPriceSeries {
        return if (state is SocketState.Stopped) series.copy(status = Status.UNAVAILABLE) else series
    }

    private fun point(timestamp: Double?, exactValue: String?, value: Double?): PolymarketCryptoPricePoint? {
        val millis = timestamp?.takeIf { it.isFinite() && it > 0 }?.toLong() ?: return null
        val price = exactValue?.toBigDecimalOrNull()
            ?: value?.takeIf(Double::isFinite)?.let(BigDecimal::valueOf)
            ?: return null
        if (price.signum() <= 0) return null

        return PolymarketCryptoPricePoint(timestampMillis = millis, price = price)
    }

    private fun List<PolymarketCryptoPricePoint>.trimmed(): List<PolymarketCryptoPricePoint> {
        val oldest = (lastOrNull() ?: return this).timestampMillis - HISTORY_MILLIS

        return dropWhile { it.timestampMillis < oldest }.takeLast(MAX_POINTS)
    }
}