package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateListener
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.features.polymarket.impl.positions.model.PolymarketPositionUMConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class PolymarketPositionsPreviewLoader @Inject constructor(
    private val repository: PolymarketPositionsRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val updateListener: PredictionPositionsUpdateListener,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    val state: StateFlow<PolymarketPositionsPreviewUM>
        field = MutableStateFlow<PolymarketPositionsPreviewUM>(PolymarketPositionsPreviewUM.Loading)

    private var pollingJob: Job? = null
    private var invalidationJob: Job? = null
    private var isResumed = false
    private var appliedRevision = 0L
    private var indexingRetry = INDEXING_RETRY_INTERVALS.size

    fun resume(
        scope: CoroutineScope,
        userWalletId: UserWalletId,
        onPositionClick: ((PolymarketPosition.Open) -> Unit)? = null,
    ) {
        isResumed = true
        invalidationJob?.cancel()
        val converter = PolymarketPositionUMConverter(onPositionClick)
        val onRetry = { if (isResumed) resume(scope, userWalletId, onPositionClick) }
        applyRevision(updateListener.currentRevision(userWalletId))
        restartPolling(scope = scope, userWalletId = userWalletId, onRetry = onRetry, converter = converter)
        invalidationJob = scope.launch {
            updateListener.revisionFlow(userWalletId).collect { revision ->
                if (revision == appliedRevision) return@collect
                applyRevision(revision)
                restartPolling(scope = scope, userWalletId = userWalletId, onRetry = onRetry, converter = converter)
            }
        }
    }

    suspend fun refresh(
        scope: CoroutineScope,
        userWalletId: UserWalletId,
        onPositionClick: ((PolymarketPosition.Open) -> Unit)? = null,
    ) {
        if (!isResumed) return
        val onRetry = { if (isResumed) resume(scope, userWalletId, onPositionClick) }
        restartPolling(
            scope = scope,
            userWalletId = userWalletId,
            onRetry = onRetry,
            converter = PolymarketPositionUMConverter(onPositionClick),
        ).await()
    }

    private fun applyRevision(revision: Long) {
        if (revision == appliedRevision) return
        appliedRevision = revision
        indexingRetry = 0
    }

    private fun restartPolling(
        scope: CoroutineScope,
        userWalletId: UserWalletId,
        onRetry: () -> Unit,
        converter: PolymarketPositionUMConverter,
    ): Deferred<Unit> {
        pollingJob?.cancel()
        val firstRead = CompletableDeferred<Unit>()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            if (state.value !is PolymarketPositionsPreviewUM.Content) {
                state.value = PolymarketPositionsPreviewUM.Loading
            }
            while (true) {
                refresh(userWalletId, onRetry, converter)
                firstRead.complete(Unit)
                val indexingDelay = INDEXING_RETRY_INTERVALS.getOrNull(indexingRetry)
                if (indexingDelay != null) indexingRetry++
                delay(indexingDelay ?: POLL_INTERVAL)
            }
        }
        pollingJob = job
        job.invokeOnCompletion { firstRead.cancel() }
        job.start()
        return firstRead
    }

    fun pause() {
        isResumed = false
        pollingJob?.cancel()
        pollingJob = null
        invalidationJob?.cancel()
        invalidationJob = null
    }

    private suspend fun refresh(
        userWalletId: UserWalletId,
        onRetry: () -> Unit,
        converter: PolymarketPositionUMConverter,
    ) {
        val result = withContext(dispatchers.default) {
            val addresses = deriveAddresses.stored(userWalletId) ?: return@withContext null
            repository.getOpenPositions(addresses = addresses, limit = PREVIEW_SIZE, offset = 0)
        }
        currentCoroutineContext().ensureActive()
        if (result == null) {
            showError(reason = "Stored addresses unavailable", onRetry = onRetry)
            return
        }
        result.fold(
            ifLeft = { showError(reason = it.toString(), onRetry = onRetry) },
            ifRight = { page ->
                state.value = PolymarketPositionsPreviewUM.Content(
                    positions = page.positions.take(PREVIEW_SIZE).map(converter::convert).toImmutableList(),
                )
            },
        )
    }

    private fun showError(reason: String, onRetry: () -> Unit) {
        TangemLogger.e("Failed to refresh Polymarket positions preview: $reason")
        val previous = state.value
        state.value = when (previous) {
            is PolymarketPositionsPreviewUM.Content -> previous.copy(onRetry = onRetry)
            else -> PolymarketPositionsPreviewUM.Error(onRetry)
        }
    }

    private companion object {
        const val PREVIEW_SIZE = 3
        val POLL_INTERVAL = 20.seconds
        // Successful reads can still return the pre-trade list while the backend indexes a fill.
        val INDEXING_RETRY_INTERVALS = listOf(2.seconds, 4.seconds, 8.seconds, 16.seconds)
    }
}