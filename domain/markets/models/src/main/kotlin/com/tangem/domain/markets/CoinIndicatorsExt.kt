package com.tangem.domain.markets

/**
 * The reading of the given [type] for the selected [timeframe]. Every indicator carries a timeframe,
 * so the match is exact: a type with no reading for that timeframe has nothing to show
 */
fun CoinIndicators.findReading(
    type: CoinIndicators.Reading.Type,
    timeframe: CoinIndicators.Reading.Timeframe,
): CoinIndicators.Reading? {
    return readings.firstOrNull { it.type == type && it.timeframe == timeframe }
}

/**
 * Overall sentiment score of the coin for the selected [timeframe]: each positive indicator adds one
 * point, each negative subtracts one — range -5..5 over the 5 indicators. Non-actionable signals
 * (insufficient data / not available) contribute zero. The single source of truth shared by the token
 * summary sentiment section and the portfolio-review row badge
 */
fun CoinIndicators.totalSentimentScore(timeframe: CoinIndicators.Reading.Timeframe): Int {
    val signals = CoinIndicators.Reading.Type.entries.mapNotNull { type -> findReading(type, timeframe)?.signal }

    return signals.count { it == CoinIndicators.Reading.Signal.POSITIVE } -
        signals.count { it == CoinIndicators.Reading.Signal.NEGATIVE }
}

/** The signals that count as a successfully loaded indicator: they hold a position on the sentiment scale. */
private val LOADED_SIGNALS = setOf(
    CoinIndicators.Reading.Signal.POSITIVE,
    CoinIndicators.Reading.Signal.NEGATIVE,
    CoinIndicators.Reading.Signal.NEUTRAL,
)

/**
 * The symmetric max magnitude `M` of the sentiment scale bar for the selected [timeframe]: the number of
 * **loaded** indicators (a reading exists AND its signal is POSITIVE / NEGATIVE / NEUTRAL), clamped to at
 * least 1. Each indicator that can't load — a missing reading, or a non-actionable signal
 * (NOT_AVAILABLE / INSUFFICIENT_DATA) — drops the `-M..M` scale by one on each end, i.e. removes 2 of its
 * positions. Pairs with [totalSentimentScore] (the value plotted on that scale), so the main-line pointer
 * reflects only the indicators that actually loaded.
 *
 * Note: an indicator counts as loaded by its signal alone, even when its numeric value is absent — consistent
 * with [totalSentimentScore], which also looks only at the signal.
 */
fun CoinIndicators.sentimentScaleMax(timeframe: CoinIndicators.Reading.Timeframe): Int =
    CoinIndicators.Reading.Type.entries
        .count { type -> findReading(type, timeframe)?.signal in LOADED_SIGNALS }
        .coerceAtLeast(1)

/**
 * Aggregate outlook of the coin for the selected [timeframe]: the sign of [totalSentimentScore], but only
 * once the score leaves a neutral dead-band around the centre of the `-M..M` scale — the ticks closest to
 * the centre read as [SentimentOutlook.NEUTRAL] rather than as a weak positive/negative.
 *
 * The band scales with [sentimentScaleMax] (`M`), so it stays proportional to how many indicators actually
 * loaded: a `+1` out of five indicators is barely off-centre, while a `+1` out of one is the whole scale.
 *
 * | `M` | band | decisive when |
 * |-----|------|---------------|
 * | 4, 5 | 2 | `abs(score) >= 3` |
 * | 1, 2, 3 | 0 | `abs(score) >= 1` |
 *
 * The band never widens as `M` shrinks, so losing an indicator can only make the outlook more decisive,
 * never flip it back to neutral. The single source of truth shared by the token summary headline and the
 * portfolio-review row badge
 */
fun CoinIndicators.sentimentOutlook(timeframe: CoinIndicators.Reading.Timeframe): SentimentOutlook {
    val score = totalSentimentScore(timeframe)
    val band = neutralBand(scaleMax = sentimentScaleMax(timeframe))

    return when {
        score > band -> SentimentOutlook.POSITIVE
        score < -band -> SentimentOutlook.NEGATIVE
        else -> SentimentOutlook.NEUTRAL
    }
}

/** Smallest scale that still fits the full two-tick neutral band on each side of the centre. */
private const val WIDE_BAND_MIN_SCALE = 4

private fun neutralBand(scaleMax: Int): Int = if (scaleMax >= WIDE_BAND_MIN_SCALE) 2 else 0