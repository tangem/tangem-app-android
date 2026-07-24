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