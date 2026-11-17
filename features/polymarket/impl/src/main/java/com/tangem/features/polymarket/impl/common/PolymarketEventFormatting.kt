package com.tangem.features.polymarket.impl.common

import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object PolymarketEventFormatting {
    private const val EASTERN_TIME_ZONE = "America/New_York"
    private const val RESOLUTION_DATE_PATTERN = "MMM d, yyyy, h:mm a"

    fun transformChange24h(volume: BigDecimal?, volume24h: BigDecimal?): PriceChangeState {
        val ratio = computeChange24hRatio(volume = volume, volume24h = volume24h) ?: return PriceChangeState.Unknown
        return PriceChangeState.Content(
            valueInPercent = ratio.format { percent(minFractionDigits = 0, maxFractionDigits = 2) },
            type = PriceChangeConverter.fromBigDecimal(ratio),
        )
    }

    /**
     * Polymarket states event dates in US Eastern Time, so the raw UTC instant is rendered
     * in that zone with an explicit "ET" suffix (e.g. "Jul 11, 2026, 6:00 PM ET").
     */
    fun formatEasternDate(raw: String?): String {
        raw ?: return DASH_SIGN
        val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return DASH_SIGN
        val formatted = DateTimeFormatter
            // Deliberately English regardless of the device locale: the whole string is a US-style
            // timestamp with an "ET" suffix, exactly as polymarket.com states it.
            .ofPattern(RESOLUTION_DATE_PATTERN, Locale.ENGLISH)
            .withZone(ZoneId.of(EASTERN_TIME_ZONE))
            .format(instant)
        return "$formatted ET"
    }

    /**
     * Growth ratio of the total traded volume over the last 24 hours, derived from the two
     * volumes the BFF serves: `volume24h / (volume - volume24h)`. `null` when either volume is
     * unknown or the event traded its entire volume within the window (no base to grow from).
     *
     * Note this is a volume delta — a price change would need historical probabilities, which
     * the BFF does not serve.
     */
    internal fun computeChange24hRatio(volume: BigDecimal?, volume24h: BigDecimal?): BigDecimal? {
        volume ?: return null
        volume24h ?: return null
        val base = volume - volume24h
        if (base <= BigDecimal.ZERO) return null
        return volume24h.divide(base, MathContext.DECIMAL64)
    }
}