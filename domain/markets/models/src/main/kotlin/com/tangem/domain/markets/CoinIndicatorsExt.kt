package com.tangem.domain.markets

/**
 * The reading of the given [type] for the selected [timeframe]: RSI and MACD match the reading of
 * that timeframe; the timeframe-agnostic indicators (MA Cross, Galaxy score, Sentiment — `null`
 * timeframe) match any selection
 */
fun CoinIndicators.findReading(
    type: CoinIndicators.Reading.Type,
    timeframe: CoinIndicators.Reading.Timeframe,
): CoinIndicators.Reading? {
    return readings.firstOrNull { it.type == type && (it.timeframe == null || it.timeframe == timeframe) }
}

/**
 * Overall sentiment score of the coin for the selected [timeframe]: each bullish indicator adds one
 * point, each bearish subtracts one — range -5..5 over the 5 indicators. Non-actionable signals
 * (insufficient data / not applicable / not available) contribute zero. The single source of truth
 * shared by the token summary sentiment section and the portfolio-review row badge
 */
fun CoinIndicators.totalSentimentScore(timeframe: CoinIndicators.Reading.Timeframe): Int {
    val signals = CoinIndicators.Reading.Type.entries.mapNotNull { type -> findReading(type, timeframe)?.signal }

    return signals.count { it == CoinIndicators.Reading.Signal.BULLISH } -
        signals.count { it == CoinIndicators.Reading.Signal.BEARISH }
}

/** The signals that count as a successfully loaded indicator: they hold a position on the sentiment scale. */
private val LOADED_SIGNALS = setOf(
    CoinIndicators.Reading.Signal.BULLISH,
    CoinIndicators.Reading.Signal.BEARISH,
    CoinIndicators.Reading.Signal.NEUTRAL,
)

/**
 * The symmetric max magnitude `M` of the sentiment scale bar for the selected [timeframe]: the number of
 * **loaded** indicators (a reading exists AND its signal is BULLISH / BEARISH / NEUTRAL), clamped to at
 * least 1. Each indicator that can't load — a missing reading, or a non-actionable signal
 * (NOT_AVAILABLE / INSUFFICIENT_DATA / NOT_APPLICABLE) — drops the `-M..M` scale by one on each end, i.e.
 * removes 2 of its positions. Pairs with [totalSentimentScore] (the value plotted on that scale), so the
 * main-line pointer reflects only the indicators that actually loaded.
 *
 * Note: MA_CROSS carries a real signal but a null numeric value, so it counts as loaded here — consistent
 * with [totalSentimentScore], which also counts its signal.
 */
fun CoinIndicators.sentimentScaleMax(timeframe: CoinIndicators.Reading.Timeframe): Int =
    CoinIndicators.Reading.Type.entries
        .count { type -> findReading(type, timeframe)?.signal in LOADED_SIGNALS }
        .coerceAtLeast(1)