package com.tangem.feature.swap

import arrow.core.Either
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.tangem.feature.swap.domain.models.domain.SwapRating
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Decorates a [SwapFeedbackRemoteSource] with an in-memory cache of ratings keyed by tx external id */
internal class DefaultSwapFeedbackRepository(
    private val remoteSource: SwapFeedbackRemoteSource,
) : SwapFeedbackRepository {

    private val cache = MutableStateFlow(value = emptyMap<String, SwapRating>())
    private val loadMutex = Mutex()

    override fun observeRating(txExternalId: String): Flow<SwapRating?> {
        return cache.map { it[txExternalId] }.distinctUntilChanged()
    }

    override suspend fun fetchRatingIfNeeded(txExternalId: String) {
        loadMutex.withLock {
            if (cache.value.containsKey(txExternalId)) return

            remoteSource.getRating(txExternalId)
                .onRight { rating ->
                    val entry = rating?.let(SwapRating::Rated) ?: SwapRating.NotRated
                    cache.update { it + (txExternalId to entry) }
                }
                .onLeft { TangemLogger.e("SwapFeedbackRepository: failed to load rating: $it") }
        }
    }

    override suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit> {
        cache.update { it + (params.txExternalId to SwapRating.Rated(params.rating)) }

        return remoteSource.submitFeedback(params).onLeft { cache.update { ratings -> ratings - params.txExternalId } }
    }
}