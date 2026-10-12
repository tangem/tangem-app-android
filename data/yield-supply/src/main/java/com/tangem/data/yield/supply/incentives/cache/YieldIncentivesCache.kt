package com.tangem.data.yield.supply.incentives.cache

import arrow.core.Either
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import kotlin.time.Duration

/**
 * In-memory cache of the incentives endpoints.
 *
 * `CacheRegistry` cannot express these windows: it writes `expireIn` but never reads it back, so its entries expire
 * once per process rather than after a duration.
 */
internal interface YieldIncentivesCache {

    /**
     * Only a [Either.Right] is stored, so a failed request is retried on the next call. Callers racing on the same
     * [key] perform a single [fetch]; different keys never block each other.
     */
    suspend fun <T : Any> getOrFetch(
        key: String,
        window: Duration,
        fetch: suspend () -> Either<YieldIncentivesError, T>,
    ): Either<YieldIncentivesError, T>

    suspend fun invalidate(key: String)
}