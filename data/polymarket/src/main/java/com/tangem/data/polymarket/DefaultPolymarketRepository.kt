package com.tangem.data.polymarket

import arrow.core.Either
import com.tangem.data.polymarket.converter.PolymarketEventConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.polymarket.PolymarketApi
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val eventConverter: PolymarketEventConverter,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketRepository {

    override suspend fun getEvents(): Either<DataError, List<PolymarketEvent>> = withContext(dispatchers.io) {
        Either.catch {
            polymarketApi.getEvents(limit = DEFAULT_LIMIT, cursor = null)
                .getOrThrow()
                .events
                .map(eventConverter::convert)
        }.mapLeft {
            // DataError exposes only NoInternetConnection as a network failure.
            DataError.NetworkError.NoInternetConnection
        }
    }

    private companion object {

        const val DEFAULT_LIMIT = 20
    }
}