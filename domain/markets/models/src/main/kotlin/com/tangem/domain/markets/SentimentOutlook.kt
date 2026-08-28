package com.tangem.domain.markets

/**
 * Aggregate outlook of a coin over all its indicator readings for a single timeframe, as shown in the
 * token summary headline and on the portfolio review row badge.
 *
 * Derived from the indicator readings by [sentimentOutlook] — never sent by the backend, which only
 * labels each reading individually via [CoinIndicators.Reading.Signal]
 */
enum class SentimentOutlook {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
}