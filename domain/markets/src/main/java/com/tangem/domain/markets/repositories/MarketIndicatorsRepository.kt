package com.tangem.domain.markets.repositories

import arrow.core.Either
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.MarketIndicators
import kotlinx.coroutines.flow.Flow

/**
 * Repository of technical and social indicator readings per coin for the "For You" page.
 *
 * Readings are cached in a session (runtime) store keyed by uppercase coin symbol:
 * [fetchCoinIndicators] merges fresh API data into the store, [getCoinIndicatorsUpdates] exposes it —
 * so a consumer can render the already-stored readings immediately and refresh in the background.
 *
[REDACTED_AUTHOR]
 */
interface MarketIndicatorsRepository {

    /** Flow of session-cached indicator readings keyed by uppercase coin symbol. Starts with an empty map */
    fun getCoinIndicatorsUpdates(): Flow<Map<String, CoinIndicators>>

    /**
     * Fetches indicator readings and merges them into the session store
     * (observed via [getCoinIndicatorsUpdates])
     *
     * @param symbols    coin symbols to filter by (case-insensitive). `null` or empty — all supported coins
     * @param indicators indicator types to filter by. `null` or empty — all indicators
     */
    suspend fun fetchCoinIndicators(
        symbols: List<String>? = null,
        indicators: List<CoinIndicators.Reading.Type>? = null,
    ): Either<Throwable, Unit>

    /**
     * Fetches the current market indicators.
     *
     * A successful response with no data at all is a [MarketIndicators] whose every field is `null`,
     * not a [Either.Left] — only a transport or parsing failure is a `Left`.
     */
    suspend fun getMarketIndicators(
        fiatCurrencyCode: String,
        languageCode: String,
    ): Either<Throwable, MarketIndicators>
}