package com.tangem.data.markets

import arrow.core.Either
import com.tangem.data.markets.converters.CoinIndicatorsConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.repositories.CoinIndicatorsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementation of [CoinIndicatorsRepository].
 *
 * Fetched readings are merged into a session [store] keyed by uppercase symbol (modelled on
 * `tokenExchangesStore` in [DefaultMarketsTokenRepository]), so consumers observing
 * [getCoinIndicatorsUpdates] render already-stored readings immediately while a fetch refreshes them.
 *
[REDACTED_AUTHOR]
 */
internal class DefaultCoinIndicatorsRepository(
    private val marketsApi: TangemTechMarketsApi,
    private val store: RuntimeStateStore<Map<String, CoinIndicators>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : CoinIndicatorsRepository {

    override fun getCoinIndicatorsUpdates(): Flow<Map<String, CoinIndicators>> = store.get()

    override suspend fun fetchCoinIndicators(
        symbols: List<String>?,
        indicators: List<CoinIndicators.Reading.Type>?,
    ): Either<Throwable, Unit> = Either.catch {
        withContext(dispatchers.io) {
            val fetched = marketsApi.getCoinIndicators(
                symbols = symbols?.toRequestParam(),
                indicators = indicators?.map(::toRequestValue)?.toRequestParam(),
            )
                .getOrThrow()
                .assets
                .map(CoinIndicatorsConverter::convert)

            // Merge instead of replace: a single-symbol refresh must not evict other coins' readings
            store.update { stored -> stored + fetched.associateBy { it.symbol.uppercase() } }
        }
    }

    private fun List<String>.toRequestParam(): String? {
        return takeIf { it.isNotEmpty() }?.joinToString(separator = ",")
    }

    /** Wire format is the lowercase enum name (e.g. `MA_CROSS` — `ma_cross`) */
    private fun toRequestValue(type: CoinIndicators.Reading.Type): String = type.name.lowercase()
}