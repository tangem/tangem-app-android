package com.tangem.feature.swap

import androidx.datastore.core.DataStore
import arrow.core.Either
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.tangem.feature.swap.domain.models.domain.SwapRating
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persists the rating state of every swap the user has seen, so the survey vendor is asked at most once
 * per transaction instead of once per screen open.
 *
 * The vendor caps API calls per account per rolling day and counts reads and writes alike, so a read
 * that only answers "has this swap been rated?" is the expensive part of the integration: it scales with
 * the number of times swap details are opened, while submits scale with the number of ratings.
 */
internal class DefaultSwapFeedbackRepository(
    private val remoteSource: SwapFeedbackRemoteSource,
    private val store: DataStore<SwapRatingsDTO>,
    private val dispatchers: CoroutineDispatcherProvider,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : SwapFeedbackRepository {

    /**
     * Transactions whose rating could not be read in this process. Without it an exhausted vendor quota
     * (HTTP 429) turns every re-open of swap details into another failing request, so the outage feeds
     * itself. Deliberately not persisted: an outage must not mark a transaction unreadable forever.
     */
    private val failedReads = mutableSetOf<String>()
    private val loadMutex = Mutex()

    override fun observeRating(txExternalId: String): Flow<SwapRating?> {
        return store.data
            .map { stored -> stored.ratings[txExternalId]?.toDomain() }
            .flowOn(dispatchers.io)
            // A read failure terminates the DataStore flow, which would leave the widget frozen for the rest
            // of the screen's life. Retry a few times, meanwhile reporting "unknown" so the user can rate.
            .retryWhen { error, attempt ->
                TangemLogger.e("SwapFeedbackRepository: failed to read stored ratings", error)
                emit(null)

                val canRetry = attempt < MAX_READ_RETRIES
                if (canRetry) delay(READ_RETRY_DELAY_MS)
                canRetry
            }
            .distinctUntilChanged()
    }

    override suspend fun fetchRatingIfNeeded(txExternalId: String) {
        loadMutex.withLock {
            if (txExternalId in failedReads) return
            if (readStored().containsKey(txExternalId)) return

            remoteSource.getRating(txExternalId)
                .onRight { rating ->
                    // A success means the vendor answers again, so every transaction parked by an earlier
                    // outage deserves another attempt
                    failedReads.clear()
                    save(txExternalId = txExternalId, rating = rating)
                }
                .onLeft { error ->
                    failedReads += txExternalId
                    TangemLogger.e("SwapFeedbackRepository: failed to load rating", error)
                }
        }
    }

    /**
     * The rating is stored only once the vendor has accepted it. Storing it up front would hide the failure
     * from the user — the stored value is what drives the "already rated" state — and, the store being
     * persistent, would survive a process death mid-request as a rating the vendor never received.
     */
    override suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit> {
        return remoteSource.submitFeedback(params)
            .onRight { save(txExternalId = params.txExternalId, rating = params.rating) }
    }

    private suspend fun readStored(): Map<String, StoredSwapRating> {
        return runSuspendCatching { store.data.first().ratings }
            .getOrElse { error ->
                TangemLogger.e("SwapFeedbackRepository: failed to read stored ratings", error)
                emptyMap()
            }
    }

    private suspend fun save(txExternalId: String, rating: Int?) {
        val entry = StoredSwapRating(rating = rating, savedAt = currentTimeMillis())

        runSuspendCatching {
            store.updateData { current ->
                current.copy(ratings = (current.ratings + (txExternalId to entry)).keepNewest())
            }
        }.onFailure { error ->
            TangemLogger.e("SwapFeedbackRepository: failed to store rating", error)
        }
    }

    private fun Map<String, StoredSwapRating>.keepNewest(): Map<String, StoredSwapRating> {
        if (size <= MAX_STORED_RATINGS) return this

        return entries
            .sortedByDescending { it.value.savedAt }
            .take(MAX_STORED_RATINGS)
            .associate { (txExternalId, entry) -> txExternalId to entry }
    }

    companion object {
        /** Unrated swaps are stored too, so the store grows with every swap seen and needs a ceiling */
        const val MAX_STORED_RATINGS = 500

        private const val MAX_READ_RETRIES = 3L
        private const val READ_RETRY_DELAY_MS = 1_000L
    }
}